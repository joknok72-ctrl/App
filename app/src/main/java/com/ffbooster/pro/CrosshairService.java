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
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Floating crosshair overlay (v12.0 — precision rewrite).
 *
 * WHY THE REWRITE: v11 used a small 84dp window with Gravity.CENTER +
 * FLAG_LAYOUT_NO_LIMITS. On cutout (punch-hole) phones in landscape, the
 * system centers the window inside the "safe" area — NOT the real display —
 * so the dot appeared shifted sideways vs the game's true crosshair
 * (confirmed by user screenshot on Realme C25Y).
 *
 * THE FIX:
 *  1. The overlay window now spans the FULL display (MATCH_PARENT +
 *     LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES + Gravity.TOP|START).
 *  2. We compute the TRUE display center ourselves via Display.getRealSize()
 *     and map it into view coordinates with getLocationOnScreen(), so the
 *     crosshair lands on the exact physical center pixel — same point the
 *     game engine uses.
 *  3. Manual calibration: xhair_cal_x / xhair_cal_y prefs (px), adjustable
 *     live over the game via a touchable arrow panel (ACTION_CALIBRATE).
 *
 * The crosshair window itself stays FLAG_NOT_TOUCHABLE — it never affects
 * gameplay touches. Only the temporary calibration panel is touchable, and
 * it sits at the screen edge away from game controls.
 */
public class CrosshairService extends Service {

    public static final String ACTION_START = "com.ffbooster.pro.XHAIR_START";
    public static final String ACTION_STOP = "com.ffbooster.pro.XHAIR_STOP";
    public static final String ACTION_CALIBRATE = "com.ffbooster.pro.XHAIR_CALIBRATE";

    private static final String CHANNEL_ID = "crosshair";
    private static final int NOTIF_ID = 1004;
    private static final String PREFS = "ffbooster";

    public static volatile boolean running = false;

    // Cycle-able styles
    static final int[] COLORS = {0xFF00E676, 0xFFFF1744, 0xFF00E5FF, 0xFFFFEA00, 0xFFFFFFFF, 0xFFFF4DFF};
    static final String[] COLOR_NAMES = {"أخضر", "أحمر", "سماوي", "أصفر", "أبيض", "وردي"};
    // v12.0: two new styles — "FF أصلي" mimics the game's own hip-fire reticle
    // (circle with 4 gaps + center dot) and "T" for one-tap drag shooters.
    static final String[] STYLE_NAMES = {"نقطة", "صليب", "دائرة + نقطة", "FF أصلي 🎯", "حرف T"};
    static final float[] SIZES = {0.7f, 1.0f, 1.45f};
    static final String[] SIZE_NAMES = {"صغير", "متوسط", "كبير"};
    // v11.0 headshot mode: vertical offset in dp (negative = higher on screen)
    static final int[] OFFSETS_DP = {0, -28, -52};
    static final String[] OFFSET_NAMES = {"وسط (عادي)", "مستوى الرأس 🎯", "رأس بعيد (قنص)"};

    private WindowManager wm;
    private CrosshairView view;
    private LinearLayout calPanel; // calibration arrows (touchable, temporary)

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
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

        if (ACTION_CALIBRATE.equals(action)) showCalibrationPanel();
        return START_STICKY;
    }

    private void addOverlay() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        view = new CrosshairView(this);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // v12.0: full-display window — we position the crosshair ourselves
        // from real display metrics, so cutout insets can no longer shift it.
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
            // extend into the punch-hole area exactly like the game does
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try { wm.addView(view, lp); } catch (Exception e) { stopSelf(); }
    }

    /** Draws the crosshair on the TRUE physical center of the display. */
    private class CrosshairView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Point real = new Point();
        private final int[] loc = new int[2];

        CrosshairView(Context ctx) { super(ctx); }

        @Override protected void onDraw(Canvas c) {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            int color = COLORS[sp.getInt("xhair_color", 0) % COLORS.length];
            int style = sp.getInt("xhair_style", 0) % STYLE_NAMES.length;
            float k = SIZES[sp.getInt("xhair_size", 1) % SIZES.length];
            int offIdx = sp.getInt("xhair_offset", 0) % OFFSETS_DP.length;

            // ---- TRUE center computation (the v12.0 fix) ----
            Display d = wm.getDefaultDisplay();
            d.getRealSize(real); // full physical resolution incl. cutout area
            getLocationOnScreen(loc); // where this view actually sits
            float cx = real.x / 2f - loc[0] + sp.getInt("xhair_cal_x", 0);
            float cy = real.y / 2f - loc[1] + sp.getInt("xhair_cal_y", 0)
                    + dp(OFFSETS_DP[offIdx]);

            paint.setStyle(Paint.Style.FILL);

            switch (style) {
                case 0: // dot (black outline for contrast)
                    paint.setColor(Color.BLACK);
                    c.drawCircle(cx, cy, dp(4) * k + 1.5f, paint);
                    paint.setColor(color);
                    c.drawCircle(cx, cy, dp(4) * k, paint);
                    break;
                case 1: { // cross
                    paint.setColor(color);
                    paint.setStrokeWidth(Math.max(2f, dp(2) * k));
                    float arm = dp(10) * k, gap = dp(3) * k;
                    c.drawLine(cx - arm, cy, cx - gap, cy, paint);
                    c.drawLine(cx + gap, cy, cx + arm, cy, paint);
                    c.drawLine(cx, cy - arm, cx, cy - gap, paint);
                    c.drawLine(cx, cy + gap, cx, cy + arm, paint);
                    break;
                }
                case 2: // circle + dot
                    paint.setColor(color);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(2f, dp(2) * k));
                    c.drawCircle(cx, cy, dp(12) * k, paint);
                    paint.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy, dp(3) * k, paint);
                    break;
                case 3: { // FF أصلي — game-style reticle: 4 arc segments + dot
                    paint.setColor(color);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(Math.max(2.5f, dp(2) * k));
                    float r = dp(11) * k;
                    android.graphics.RectF oval =
                            new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
                    // 4 arcs of 60° with 30° gaps at N/E/S/W — like FF's reticle
                    for (int a = 15; a < 360; a += 90) c.drawArc(oval, a, 60, false, paint);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.BLACK);
                    c.drawCircle(cx, cy, dp(2.5f) * k + 1f, paint);
                    paint.setColor(color);
                    c.drawCircle(cx, cy, dp(2.5f) * k, paint);
                    break;
                }
                case 4: { // T-shape — favored by drag-headshot players
                    paint.setColor(color);
                    paint.setStrokeWidth(Math.max(2.5f, dp(2) * k));
                    float arm = dp(10) * k, gap = dp(3) * k;
                    c.drawLine(cx - arm, cy, cx - gap, cy, paint); // left
                    c.drawLine(cx + gap, cy, cx + arm, cy, paint); // right
                    c.drawLine(cx, cy + gap, cx, cy + arm, paint); // down only
                    paint.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy, dp(2) * k, paint);
                    break;
                }
            }
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }
    }

    // ================= Calibration panel (v12.0) =================

    /** Touchable arrow panel shown over the game to nudge the crosshair. */
    private void showCalibrationPanel() {
        if (calPanel != null) return; // already showing

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        calPanel = new LinearLayout(this);
        calPanel.setOrientation(LinearLayout.HORIZONTAL);
        calPanel.setBackgroundColor(0xE6101822);
        int pad = dp(6);
        calPanel.setPadding(pad, pad, pad, pad);

        calPanel.addView(calBtn("◀", () -> nudge(-dp(2), 0)));
        calPanel.addView(calBtn("▶", () -> nudge(dp(2), 0)));
        calPanel.addView(calBtn("▲", () -> nudge(0, -dp(2))));
        calPanel.addView(calBtn("▼", () -> nudge(0, dp(2))));
        calPanel.addView(calBtn("↺", () -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putInt("xhair_cal_x", 0).putInt("xhair_cal_y", 0).apply();
            if (view != null) view.invalidate();
        }));
        calPanel.addView(calBtn("✔ تم", this::hideCalibrationPanel));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(4);

        try { wm.addView(calPanel, lp); } catch (Exception ignored) { calPanel = null; }
    }

    private TextView calBtn(String label, Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        int p = dp(10);
        tv.setPadding(p, dp(4), p, dp(4));
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    private void nudge(int dx, int dy) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit().putInt("xhair_cal_x", sp.getInt("xhair_cal_x", 0) + dx)
                 .putInt("xhair_cal_y", sp.getInt("xhair_cal_y", 0) + dy)
                 .apply();
        if (view != null) view.invalidate();
    }

    private void hideCalibrationPanel() {
        if (calPanel != null && wm != null) {
            try { wm.removeView(calPanel); } catch (Exception ignored) {}
            calPanel = null;
        }
    }

    // ================================================================

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
         .setContentTitle("🎯 كروس هير شغّال (دقة v12)")
         .setContentText("متمركز على المنتصف الفعلي للشاشة — لا يؤثر على اللمس إطلاقاً")
         .setOngoing(true);
        return b.build();
    }

    @Override
    public void onDestroy() {
        running = false;
        hideCalibrationPanel();
        if (view != null && wm != null) {
            try { wm.removeView(view); } catch (Exception ignored) {}
            view = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
