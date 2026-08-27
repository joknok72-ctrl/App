package com.ffbooster.pro;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Floating in-game HUD (v6.0) — a tiny draggable overlay that stays on top
 * of Free Fire showing live free RAM, battery temperature and battery level.
 * Tap = collapse/expand. Drag to move anywhere. Long-press = close.
 *
 * Ultra-lightweight: one TextView, updates every 2s, no layout inflation.
 */
public class HudService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.HUD_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.HUD_STOP";

    private static final String CHANNEL_ID = "hud";
    private static final int NOTIF_ID = 1002;
    private static final long REFRESH_MS = 2_000;

    public static volatile boolean running = false;

    private WindowManager wm;
    private TextView hudView;
    private WindowManager.LayoutParams lp;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean expanded = true;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateHud();
            handler.postDelayed(this, REFRESH_MS);
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
        if (!canDrawOverlays()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIF_ID, buildNotification());
        if (hudView == null) addHud();
        running = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    // ---------- HUD view ----------
    private void addHud() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        hudView = new TextView(this);
        hudView.setTextColor(Color.WHITE);
        hudView.setTextSize(11);
        hudView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        hudView.setPadding(dp(10), dp(5), dp(10), dp(5));
        hudView.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC0D1117);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFFF6D00);
        hudView.setBackground(bg);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(8);
        lp.y = dp(80);

        hudView.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            private long downTime;
            private boolean moved;

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = lp.x; startY = lp.y;
                        touchX = e.getRawX(); touchY = e.getRawY();
                        downTime = System.currentTimeMillis();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - touchX);
                        int dy = (int) (e.getRawY() - touchY);
                        if (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4)) moved = true;
                        lp.x = startX + dx;
                        lp.y = startY + dy;
                        try { wm.updateViewLayout(hudView, lp); } catch (Exception ignored) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        long held = System.currentTimeMillis() - downTime;
                        if (!moved) {
                            if (held > 600) {           // long press = close
                                stopSelf();
                            } else {                     // tap = collapse/expand
                                expanded = !expanded;
                                updateHud();
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        try { wm.addView(hudView, lp); } catch (Exception e) { stopSelf(); }
    }

    private void updateHud() {
        if (hudView == null) return;

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long availMb = mi.availMem / (1024 * 1024);
        long totalMb = mi.totalMem / (1024 * 1024);
        int usedPct = (int) ((totalMb - availMb) * 100 / Math.max(totalMb, 1));

        float temp = -1; int level = -1;
        try {
            Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batt != null) {
                temp = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            }
        } catch (Exception ignored) {}

        String ramIcon = usedPct >= 90 ? "🔴" : (usedPct >= 75 ? "🟡" : "🟢");
        String tempIcon = temp >= 42 ? "🔥" : (temp >= 38 ? "🟡" : "❄");

        String text;
        if (expanded) {
            text = String.format(Locale.US, "%s RAM %dMB (%d%%)\n%s %.1f°C  🔋%d%%",
                    ramIcon, availMb, usedPct, tempIcon, temp, level);
        } else {
            text = ramIcon + " " + availMb;
        }
        hudView.setText(text);

        // Border turns red under danger so the player notices instantly
        GradientDrawable bg = (GradientDrawable) hudView.getBackground();
        if (usedPct >= 90 || temp >= 42) bg.setStroke(dp(1), 0xFFFF1744);
        else bg.setStroke(dp(1), 0xFFFF6D00);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---------- Notification ----------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "HUD داخل اللعبة", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_view)
         .setContentTitle("📊 HUD فري فاير شغّال")
         .setContentText("اسحبه لأي مكان — اضغط مطولاً عليه للإغلاق")
         .setOngoing(true);
        return b.build();
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(tick);
        if (hudView != null && wm != null) {
            try { wm.removeView(hudView); } catch (Exception ignored) {}
            hudView = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
