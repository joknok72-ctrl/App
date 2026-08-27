package com.ffbooster.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Auto-Pilot service (v7.0) — the "set it and forget it" brain.
 *
 * Watches (via UsageStats events) which app is in the foreground.
 * The moment Free Fire comes to the front it automatically:
 *   1. starts Game Mode 2.0 (adaptive background cleaning)
 *   2. shows the in-game HUD (if overlay permission granted)
 *   3. shows the crosshair overlay (if the user enabled it)
 *   4. enables Do-Not-Disturb so no notification ruins the match (v8.0)
 *   5. starts a play-session recorder (duration + max temperature)
 * When Free Fire goes to the background/closes it stops the services,
 * restores the previous notification filter, and saves a session report.
 *
 * Polling is a light 4s UsageEvents query — negligible battery cost.
 */
public class AutoPilotService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.AUTOPILOT_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.AUTOPILOT_STOP";

    private static final String CHANNEL_ID = "autopilot";
    private static final int NOTIF_ID = 1003;
    private static final long POLL_MS = 4_000;
    private static final String PREFS = "ffbooster";

    public static volatile boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ffInForeground = false;
    private long sessionStart = 0;
    private float sessionMaxTemp = 0;
    private int prevInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;
    private boolean dndApplied = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            boolean ffNow = isFreeFireForeground();
            if (ffNow && !ffInForeground) onFreeFireOpened();
            else if (!ffNow && ffInForeground) onFreeFireClosed();
            if (ffInForeground) trackTemp();
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        running = true;
        startForeground(NOTIF_ID, buildNotification("🤖 الطيار الآلي مستعد — افتح فري فاير وهيشتغل كل حاجة لوحده"));
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    // ---------- Foreground app detection (UsageStats events) ----------
    static boolean hasUsageAccess(Context ctx) {
        try {
            UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            UsageEvents ev = usm.queryEvents(now - 60_000, now);
            return ev != null && ev.hasNextEvent();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isFreeFireForeground() {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            UsageEvents events = usm.queryEvents(now - 120_000, now);
            String lastFg = null;
            UsageEvents.Event e = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                int t = e.getEventType();
                if (t == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= 29 && t == UsageEvents.Event.ACTIVITY_RESUMED)) {
                    lastFg = e.getPackageName();
                }
            }
            return lastFg != null && lastFg.startsWith("com.dts.");
        } catch (Exception ex) {
            return false;
        }
    }

    // ---------- Session lifecycle ----------
    private void onFreeFireOpened() {
        ffInForeground = true;
        sessionStart = System.currentTimeMillis();
        sessionMaxTemp = 0;
        trackTemp();

        // 1) Game Mode 2.0
        try {
            Intent gm = new Intent(this, GameModeService.class).setAction(GameModeService.ACTION_START);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(gm);
            else startService(gm);
        } catch (Exception ignored) {}

        // 2) In-game HUD (only if overlay permission granted)
        boolean overlayOk = Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this);
        try {
            if (overlayOk) {
                Intent hud = new Intent(this, HudService.class).setAction(HudService.ACTION_START);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(hud);
                else startService(hud);
            }
        } catch (Exception ignored) {}

        // 3) Crosshair overlay if the user enabled it (v8.0)
        try {
            SharedPreferences sp0 = getSharedPreferences(PREFS, MODE_PRIVATE);
            if (overlayOk && sp0.getBoolean("xhair_enabled", false)) {
                Intent xh = new Intent(this, CrosshairService.class).setAction(CrosshairService.ACTION_START);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(xh);
                else startService(xh);
            }
        } catch (Exception ignored) {}

        // 4) Auto-DND: silence notifications during the match (v8.0)
        applyDnd();

        updateNotification("🎮 فري فاير مفتوحة! ✅ وضع الألعاب + HUD"
                + (dndApplied ? " + كتم الإشعارات 🔇" : "")
                + " اشتغلوا تلقائياً — العب براحتك");
    }

    private void onFreeFireClosed() {
        ffInForeground = false;

        // Stop the helpers
        try { startService(new Intent(this, GameModeService.class).setAction(GameModeService.ACTION_STOP)); } catch (Exception ignored) {}
        try { startService(new Intent(this, HudService.class).setAction(HudService.ACTION_STOP)); } catch (Exception ignored) {}
        try { startService(new Intent(this, CrosshairService.class).setAction(CrosshairService.ACTION_STOP)); } catch (Exception ignored) {}

        // Restore notifications exactly as they were (v8.0)
        restoreDnd();

        // Save session report
        long durMin = Math.max(1, (System.currentTimeMillis() - sessionStart) / 60_000);
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
          .putLong("last_session_min", durMin)
          .putFloat("last_session_max_temp", sessionMaxTemp)
          .putLong("last_session_end", System.currentTimeMillis())
          .putLong("total_play_min", sp.getLong("total_play_min", 0) + durMin)
          .putInt("session_count", sp.getInt("session_count", 0) + 1)
          .apply();

        String tempTxt = sessionMaxTemp > 0
                ? String.format(java.util.Locale.US, " | أقصى حرارة: %.1f°م", sessionMaxTemp) : "";
        updateNotification("📈 جلسة انتهت: " + durMin + " دقيقة" + tempTxt
                + "\n🤖 الطيار الآلي مستعد للجلسة الجاية");
    }

    // ---------- Auto-DND (v8.0) ----------
    private void applyDnd() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                prevInterruptionFilter = nm.getCurrentInterruptionFilter();
                if (prevInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                    dndApplied = true;
                }
            }
        } catch (Exception ignored) { dndApplied = false; }
    }

    private void restoreDnd() {
        try {
            if (dndApplied) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                    nm.setInterruptionFilter(prevInterruptionFilter);
                }
            }
        } catch (Exception ignored) {}
        dndApplied = false;
    }

    private void trackTemp() {
        try {
            Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batt != null) {
                float t = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                if (t > sessionMaxTemp) sessionMaxTemp = t;
            }
        } catch (Exception ignored) {}
    }

    // ---------- Notification ----------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "الطيار الآلي", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text) {
        Intent stopIntent = new Intent(this, AutoPilotService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 3, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 4, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_compass)
         .setContentTitle("🤖 FF Booster — الطيار الآلي")
         .setContentText(text)
         .setStyle(new Notification.BigTextStyle().bigText(text))
         .setContentIntent(openPi)
         .setOngoing(true)
         .addAction(new Notification.Action.Builder(null, "⏹ إيقاف الطيار الآلي", stopPi).build());
        return b.build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(tick);
        if (ffInForeground) onFreeFireClosed();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
