package com.ffbooster.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Floating crosshair overlay (v8.0) — a small aim-dot drawn dead-center of
 * the screen, on top of Free Fire. Helps no-scope / hip-fire aim tremendously
 * on small phones.
 *
 * The overlay window is FLAG_NOT_TOUCHABLE: it NEVER intercepts any touch,
 * so gameplay is completely unaffected. Style (color + size) is read from
 * SharedPreferences so the user can cycle styles from the main screen.
 *
 * Zero timers, zero polling — a static draw costs literally nothing.
 */
public class CrosshairService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.XHAIR_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.XHAIR_STOP";

    private static final String CHANNEL_ID = "crosshair";
    private static final int NOTIF_ID = 1004;
    private static final String PREFS = "ffbooster";

    public static volatile boolean running = false;

    // Cycle-able styles: {color, style} — style 0=dot, 1=cross, 2=circle+dot
    static final int[] COLORS = {0xFF00E676, 0xFFFF1744, 0xFF00E5FF, 0xFFFFEA00, 0xFFFFFFFF};
    static final String[] COLOR_NAMES = {"أخضر", "أحمر", "سماوي", "أصفر", "أبيض"};
    static final String[] STYLE_NAMES = {"نقطة", "صليب", "دائرة + نقطة"};

    private WindowManager wm;
    private CrosshairView view;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        createChannel();
        startForeground(NOTIF_ID, buildNotification());
        if (view == null) addOverlay();
        else view.invalidate(); // refresh style if restarted with new prefs
        running = true;
        return START_STICKY;
    }

    private void addOverlay() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        view = new CrosshairView(this);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int size = dp(56);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;

        try { wm.addView(view, lp); } catch (Exception e) { stopSelf(); }
    }

    /** Tiny custom view that draws the crosshair according to saved prefs. */
    private class CrosshairView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CrosshairView(Context ctx) { super(ctx); }

        @Override protected void onDraw(Canvas c) {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            int color = COLORS[sp.getInt("xhair_color", 0) % COLORS.length];
            int style = sp.getInt("xhair_style", 0) % STYLE_NAMES.length;

            float cx = getWidth() / 2f, cy = getHeight() / 2f;

            // subtle black outline first for visibility on any background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            if (style == 0) c.drawCircle(cx, cy, dp(4) + 1.5f, paint);

            paint.setColor(color);
            switch (style) {
                case 0: // dot
                    c.drawCircle(cx, cy, dp(4), paint);
                    break;
                case 1: // cross
                    paint.setStrokeWidth(dp(2));
                    float arm = dp(10), gap = dp(3);
                    c.drawLine(cx - arm, cy, cx - gap, cy, paint);
                    c.drawLine(cx + gap, cy, cx + arm, cy, paint);
                    c.drawLine(cx, cy - arm, cx, cy - gap, paint);
                    c.drawLine(cx, cy + gap, cx, cy + arm, paint);
                    break;
                case 2: // circle + dot
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    c.drawCircle(cx, cy, dp(12), paint);
                    paint.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy, dp(3), paint);
                    break;
            }
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "كروس هير", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
         .setContentTitle("🎯 كروس هير شغّال")
         .setContentText("نقطة تصويب ثابتة في منتصف الشاشة — لا تؤثر على اللمس إطلاقاً")
         .setOngoing(true);
        return b.build();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (view != null && wm != null) {
            try { wm.removeView(view); } catch (Exception ignored) {}
            view = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
