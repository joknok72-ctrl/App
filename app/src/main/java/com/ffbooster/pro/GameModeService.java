package com.ffbooster.pro;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Game Mode 2.0 foreground service (v6.0) — the real speed engine, now ADAPTIVE.
 *
 * While the player is in Free Fire this service keeps working in the
 * background, but instead of a fixed 30s interval it reads RAM pressure
 * each cycle and adapts:
 *   • RAM ≥ 90% used → clean every 12s (emergency — lag imminent)
 *   • RAM ≥ 75% used → clean every 20s (high pressure)
 *   • otherwise      → clean every 45s (relaxed — saves battery/CPU)
 * It also monitors battery temperature, warning through the notification
 * when the phone gets hot enough to throttle the Unisoc T610.
 */
public class GameModeService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.GAMEMODE_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.GAMEMODE_STOP";

    private static final String CHANNEL_ID = "gamemode";
    private static final int NOTIF_ID = 1001;
    // Adaptive intervals (v6.0)
    private static final long INTERVAL_EMERGENCY_MS = 12_000;
    private static final long INTERVAL_HIGH_MS = 20_000;
    private static final long INTERVAL_RELAXED_MS = 45_000;

    public static volatile boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int cycles = 0;
    private int totalKilled = 0;
    private volatile int lastRamPct = 0;
    private volatile long nextInterval = INTERVAL_RELAXED_MS;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            executor.execute(() -> {
                int ramPct = readRamUsedPct();
                lastRamPct = ramPct;
                // Adaptive engine: clean harder when RAM pressure is high
                int killed = silentBoost();
                if (ramPct >= 90) {
                    // Emergency: second pass to catch auto-restarting apps
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    killed += silentBoost();
                    nextInterval = INTERVAL_EMERGENCY_MS;
                } else if (ramPct >= 75) {
                    nextInterval = INTERVAL_HIGH_MS;
                } else {
                    nextInterval = INTERVAL_RELAXED_MS;
                }
                totalKilled += killed;
                cycles++;
                float temp = readBatteryTemp();
                handler.post(() -> {
                    updateNotification(temp);
                    handler.removeCallbacks(tick);
                    handler.postDelayed(tick, nextInterval);
                });
            });
        }
    };

    private int readRamUsedPct() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long total = mi.totalMem, avail = mi.availMem;
            return (int) ((total - avail) * 100 / Math.max(total, 1));
        } catch (Exception e) { return 0; }
    }

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
        cycles = 0;
        totalKilled = 0;
        startForeground(NOTIF_ID, buildNotification("🎮 وضع الألعاب 2.0 شغّال — تنظيف ذكي تكيفي حسب ضغط الرام", false));
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(tick);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ---------- Silent boost pass ----------
    private int silentBoost() {
        int killed = 0;
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (app.packageName.equals(getPackageName())) continue;
                if (app.packageName.startsWith("com.dts.")) continue; // never touch Free Fire
                try {
                    am.killBackgroundProcesses(app.packageName);
                    killed++;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return killed;
    }

    private float readBatteryTemp() {
        try {
            Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batt != null) {
                return batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    // ---------- Notification ----------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "وضع الألعاب", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("تسريع تلقائي مستمر أثناء لعب فري فاير");
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String text, boolean hot) {
        Intent stopIntent = new Intent(this, GameModeService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 2, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
         .setContentTitle(hot ? "🔥 تحذير حرارة — برّد الهاتف!" : "⚡ FF Booster — وضع الألعاب")
         .setContentText(text)
         .setStyle(new Notification.BigTextStyle().bigText(text))
         .setContentIntent(openPi)
         .setOngoing(true)
         .addAction(new Notification.Action.Builder(null, "⏹ إيقاف وضع الألعاب", stopPi).build());
        return b.build();
    }

    private void updateNotification(float temp) {
        boolean hot = temp >= 42;
        String tempTxt = temp > 0 ? String.format(Locale.US, "%.1f°م", temp) : "غير متاح";
        String mode = lastRamPct >= 90 ? "🔴 طوارئ (كل 12ث)" : (lastRamPct >= 75 ? "🟡 مكثّف (كل 20ث)" : "🟢 مسترخي (كل 45ث)");
        String text;
        if (hot) {
            text = "درجة الحرارة " + tempTxt + " — المعالج هيقلل الفريمات! وقّف اللعب 5 دقايق أو شيل الجراب"
                    + "\nالوضع الذكي: " + mode + " | الرام: " + lastRamPct + "%"
                    + "\nدورات: " + cycles + " | تطبيقات منظفة: " + totalKilled;
        } else {
            text = "تنظيف ذكي شغّال ✅ الحرارة: " + tempTxt + " | الرام: " + lastRamPct + "%"
                    + "\nالوضع الذكي: " + mode
                    + "\nدورات: " + cycles + " | تطبيقات منظفة: " + totalKilled;
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text, hot));
    }
}
