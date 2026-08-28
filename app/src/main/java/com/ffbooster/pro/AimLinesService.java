package com.ffbooster.pro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * v13.0 — Pro Aim Lines overlay: a MUCH stronger aiming aid than the small
 * crosshair dot. Draws full-screen guide lines anchored on the exact same
 * calibrated aim point as CrosshairService:
 *
 *  Mode 0: خطوط كاملة   — full-screen thin cross lines through the aim point
 *                          (like sniper lines): you always see exactly where
 *                          your aim level is anywhere on screen.
 *  Mode 1: خط الرأس      — a single horizontal line at head level: trains
 *                          you to keep aim glued to enemy head height, so
 *                          the drag becomes nearly zero.
 *  Mode 2: مسطرة الركويل — recoil ruler: tick marks below the aim point to
 *                          pull down by the exact amount during spray, so
 *                          bullets stay on the head.
 *  Mode 3: الكل معاً     — everything at once.
 *
 * 100% legit: FLAG_NOT_TOUCHABLE static drawing — never touches the game,
 * never reads game memory, zero ban risk. Reuses the v12 true-center math
 * (getRealSize + cutout SHORT_EDGES) and the same xhair_cal_x/y calibration,
 * so the lines align perfectly with the game's own crosshair.
 */
public class AimLinesService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.LINES_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.LINES_STOP";

    private static final String CHANNEL_ID = "aimlines";
    private static final int NOTIF_ID = 1006;
    private static final String PREFS = "ffbooster";

    public static volatile boolean running = false;

    static final String[] MODE_NAMES = {"📐 خطوط كاملة", "➖ خط الرأس", "📏 مسطرة الركويل", "🎯 الكل معاً"};
    // Line transparency levels so the lines never distract from gameplay
    static final int[] ALPHAS = {90, 150, 210};
    static final String[] ALPHA_NAMES = {"خفيف", "متوسط", "واضح"};

    private WindowManager wm;
    private LinesView view;

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
        else view.invalidate();
        running = true;
        return START_STICKY;
    }

    private void addOverlay() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        view = new LinesView(this);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 0;
        lp.y = 0;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try { wm.addView(view, lp); } catch (Exception e) { stopSelf(); }
    }

    private class LinesView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Point real = new Point();
        private final int[] loc = new int[2];

        LinesView(Context ctx) { super(ctx); }

        @Override protected void onDraw(Canvas c) {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            int mode = sp.getInt("lines_mode", 0) % MODE_NAMES.length;
            int alpha = ALPHAS[sp.getInt("lines_alpha", 1) % ALPHAS.length];
            int baseColor = CrosshairService.COLORS[
                    sp.getInt("xhair_color", 0) % CrosshairService.COLORS.length];
            int offIdx = sp.getInt("xhair_offset", 0) % CrosshairService.OFFSETS_DP.length;

            // Same true-center math as CrosshairService v12 — perfect alignment
            Display d = wm.getDefaultDisplay();
            d.getRealSize(real);
            getLocationOnScreen(loc);
            float cx = real.x / 2f - loc[0] + sp.getInt("xhair_cal_x", 0);
            float cy = real.y / 2f - loc[1] + sp.getInt("xhair_cal_y", 0)
                    + dp(CrosshairService.OFFSETS_DP[offIdx]);

            float w = real.x - loc[0] + dp(50), h = real.y - loc[1] + dp(50);
            float left = -dp(50), top = -dp(50);

            paint.setColor(baseColor);
            paint.setAlpha(alpha);

            boolean fullCross = mode == 0 || mode == 3;
            boolean headLine = mode == 1 || mode == 3;
            boolean recoil = mode == 2 || mode == 3;

            if (fullCross) {
                paint.setStrokeWidth(Math.max(1.5f, dp(1)));
                // horizontal aim-level line across the whole screen,
                // with a small gap around center so the reticle stays clear
                float gap = dp(26);
                c.drawLine(left, cy, cx - gap, cy, paint);
                c.drawLine(cx + gap, cy, w, cy, paint);
                // vertical line
                c.drawLine(cx, top, cx, cy - gap, paint);
                c.drawLine(cx, cy + gap, cx, h, paint);
            }

            if (headLine && !fullCross) {
                // dashed-look head-level horizontal line (drawn as segments —
                // dash effects are unreliable on some hw canvases)
                paint.setStrokeWidth(Math.max(2f, dp(1.2f)));
                float dash = dp(14), space = dp(10), gap = dp(30);
                for (float x = left; x < w; x += dash + space) {
                    float x2 = Math.min(x + dash, w);
                    if (x2 > cx - gap && x < cx + gap) continue; // keep center clear
                    c.drawLine(x, cy, x2, cy, paint);
                }
            }

            if (recoil) {
                // Recoil ruler: ticks every 12dp below aim point, bigger tick
                // every 3rd — pull down along it during spray to counter recoil
                paint.setStrokeWidth(Math.max(2f, dp(1.3f)));
                float step = dp(12);
                for (int i = 1; i <= 8; i++) {
                    float y = cy + i * step;
                    float half = (i % 3 == 0) ? dp(10) : dp(5);
                    c.drawLine(cx - half, y, cx + half, y, paint);
                }
                // thin spine connecting the ticks
                paint.setAlpha(Math.max(40, alpha - 60));
                paint.setStrokeWidth(Math.max(1f, dp(0.7f)));
                c.drawLine(cx, cy + step * 0.5f, cx, cy + step * 8, paint);
                paint.setAlpha(alpha);
            }
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "خطوط التصويب", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_compass)
         .setContentTitle("📐 خطوط التصويب شغّالة")
         .setContentText("خطوط إرشادية ثابتة — لا تؤثر على اللمس أو اللعبة إطلاقاً")
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
