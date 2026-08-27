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
 * Floating in-game HUD 3.0 (v8.0) — a tiny draggable overlay that stays on
 * top of Free Fire showing a REAL display FPS counter (via Choreographer
 * frame callbacks), live free RAM, battery temperature, battery level and a
 * play-session timer.
 * Tap = collapse/expand. DOUBLE-TAP = instant RAM boost without leaving the
 * game. Drag to move anywhere. Long-press = close.
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
    private long sessionStart = 0;
    private volatile boolean boosting = false;

    // ---- Real FPS counter via Choreographer (v8.0) ----
    // Counts display vsync frames delivered to our overlay window. When the
    // system is under heavy load (game dropping frames) the display pipeline
    // slows and this reflects real perceived smoothness.
    private volatile int fps = 0;
    /** Last measured display FPS — read by AutoPilotService for session avg-FPS stats (v9.0). -1 = HUD off. */
    public static volatile int currentFps = -1;
    private int frameCount = 0;
    private long fpsWindowStart = 0;
    private android.view.Choreographer.FrameCallback frameCb;

    private void startFpsCounter() {
        fpsWindowStart = System.nanoTime();
        frameCount = 0;
        frameCb = new android.view.Choreographer.FrameCallback() {
            @Override public void doFrame(long frameTimeNanos) {
                frameCount++;
                long elapsed = frameTimeNanos - fpsWindowStart;
                if (elapsed >= 1_000_000_000L) {
                    fps = (int) (frameCount * 1_000_000_000L / elapsed);
                    currentFps = fps;
                    frameCount = 0;
                    fpsWindowStart = frameTimeNanos;
                }
                if (running) android.view.Choreographer.getInstance().postFrameCallback(this);
            }
        };
        android.view.Choreographer.getInstance().postFrameCallback(frameCb);
    }

    private void stopFpsCounter() {
        if (frameCb != null) {
            try { android.view.Choreographer.getInstance().removeFrameCallback(frameCb); } catch (Exception ignored) {}
            frameCb = null;
        }
    }

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
        if (sessionStart == 0) sessionStart = System.currentTimeMillis();
        running = true;
        if (frameCb == null) startFpsCounter();
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
            private long lastTapTime;
            private boolean moved;
            private final Runnable singleTap = () -> { expanded = !expanded; updateHud(); };

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
                        long now = System.currentTimeMillis();
                        long held = now - downTime;
                        if (!moved) {
                            if (held > 600) {                    // long press = close
                                stopSelf();
                            } else if (now - lastTapTime < 350) { // DOUBLE-TAP = instant boost
                                handler.removeCallbacks(singleTap);
                                lastTapTime = 0;
                                quickBoost();
                            } else {                             // maybe single tap
                                lastTapTime = now;
                                handler.postDelayed(singleTap, 360);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        try { wm.addView(hudView, lp); } catch (Exception e) { stopSelf(); }
    }

    // ---------- Double-tap instant boost (v7.0) — clean RAM without leaving the game ----------
    private void quickBoost() {
        if (boosting) return;
        boosting = true;
        hudView.setText("⚡ تسريع…");
        final long before = availRamMb();
        new Thread(() -> {
            int killed = 0;
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (android.content.pm.ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                    if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    if (app.packageName.equals(getPackageName())) continue;
                    if (app.packageName.startsWith("com.dts.")) continue; // never touch Free Fire
                    try { am.killBackgroundProcesses(app.packageName); killed++; } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            System.gc();
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            final long freed = Math.max(0, availRamMb() - before);
            final int k = killed;
            handler.post(() -> {
                boosting = false;
                hudView.setText("✅ " + k + " تطبيق | +" + freed + "MB");
                handler.postDelayed(this::updateHud, 1800);
            });
        }).start();
    }

    private long availRamMb() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    private void updateHud() {
        if (hudView == null || boosting) return;

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

        long sessMin = sessionStart > 0 ? (System.currentTimeMillis() - sessionStart) / 60_000 : 0;
        int f = fps;
        String fpsIcon = f >= 50 ? "🟢" : (f >= 30 ? "🟡" : "🔴");
        String text;
        if (expanded) {
            text = String.format(Locale.US, "%s %dFPS  %s RAM %dMB (%d%%)\n%s %.1f°C  🔋%d%%  ⏱%dد",
                    fpsIcon, f, ramIcon, availMb, usedPct, tempIcon, temp, level, sessMin);
        } else {
            text = fpsIcon + f + " " + ramIcon + availMb;
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
         .setContentTitle("📊 HUD 2.0 فري فاير شغّال")
         .setContentText("دبل كليك عليه = تسريع فوري ⚡ | مطولاً = إغلاق")
         .setOngoing(true);
        return b.build();
    }

    @Override
    public void onDestroy() {
        running = false;
        currentFps = -1;
        stopFpsCounter();
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
