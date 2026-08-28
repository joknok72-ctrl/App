package com.ffbooster.pro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

/**
 * Drag-Headshot Trainer (v11.0) — trains the EXACT Free Fire headshot motion.
 *
 * In Free Fire the pro headshot technique is the "drag": you press the fire
 * button and, in the SAME touch, drag upward so the crosshair snaps from the
 * enemy's body to the head. This trainer reproduces that motion 1:1:
 *
 *   • An enemy silhouette (body + head) appears at a random position/size
 *     (near = big, far = small — like real fights at different ranges).
 *   • The player must PRESS on the body zone and DRAG UP to the head zone,
 *     then RELEASE while on the head. Exactly like a real drag-shot.
 *   • Tap on the head directly = "tap-shot" (counts, but drags score more,
 *     because in the real game your finger starts on the fire button).
 *   • Measures: headshot %, average drag time (ms), drag smoothness, streak.
 *   • 15 enemies per round, mixed distances. Grades like FF ranks.
 *
 * This is a legitimate training tool: it builds the actual muscle memory of
 * the drag motion so the player lands headshots in the real game. No game
 * files are touched, no cheating of any kind.
 */
public class HeadshotTrainerActivity extends Activity {

    private static final String PREFS = "ffbooster";
    private static final int TOTAL_ENEMIES = 15;

    private GameView gameView;
    private TextView tvStats;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();

    // Round state
    private boolean roundActive = false;
    private int enemiesShown = 0;
    private int headshots = 0;       // finished on the head (drag or tap)
    private int dragHeadshots = 0;   // proper body→head drags (the real skill)
    private int bodyShots = 0;       // released on the body = body damage only
    private int misses = 0;          // released outside the enemy
    private int streak = 0, bestStreak = 0;
    private long totalDragMs = 0;    // sum of drag durations for drag-headshots
    private long enemyShownAt = 0;

    // Current enemy geometry (px)
    private float ex, ey;      // body center
    private float bodyW, bodyH, headR;
    private boolean enemyVisible = false;

    // Touch tracking for the drag
    private boolean touchStartedOnBody = false;
    private long touchStartMs = 0;
    private float touchStartY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D1117"));

        tvStats = new TextView(this);
        tvStats.setTextColor(Color.parseColor("#F1F5F9"));
        tvStats.setTextSize(13);
        float d = getResources().getDisplayMetrics().density;
        int p = (int) (12 * d);
        tvStats.setPadding(p, p, p, p);
        tvStats.setLineSpacing(4 * d, 1f);
        root.addView(tvStats);

        gameView = new GameView();
        root.addView(gameView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        showIdle();
    }

    private void showIdle() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int bestPct = sp.getInt("hs_best_pct", 0);
        int bestStrk = sp.getInt("hs_best_streak", 0);
        String best = bestPct > 0
                ? String.format(Locale.US, "🏆 أفضل نسبة هيدشوت: %d%% | أطول سلسلة: %d 🔥", bestPct, bestStrk)
                : "لسه معملتش أي جولة — رقمك القياسي هيظهر هنا";
        tvStats.setText("🎯 مدرب الدراغ هيدشوت — اضغط أي مكان للبدء\n" +
                "التقنية: اضغط على جسم العدو ↓ واسحب لفوق ↑ لرأسه وسيب صوابعك وهو على الراس\n" +
                "(نفس حركة زر النار في فري فاير بالظبط!) — " + TOTAL_ENEMIES + " عدو\n" + best);
        gameView.invalidate();
    }

    private void startRound() {
        roundActive = true;
        enemiesShown = 0;
        headshots = 0; dragHeadshots = 0; bodyShots = 0; misses = 0;
        streak = 0; bestStreak = 0; totalDragMs = 0;
        enemyVisible = false;
        tvStats.setText("🔥 استعد… افتكر: اضغط الجسم واسحب للراس!");
        ui.postDelayed(this::nextEnemy, 800);
    }

    private void nextEnemy() {
        if (!roundActive) return;
        if (enemiesShown >= TOTAL_ENEMIES) { endRound(); return; }
        enemiesShown++;

        float d = getResources().getDisplayMetrics().density;
        int w = gameView.getWidth(), h = gameView.getHeight();
        if (w < 10 || h < 10) { ui.postDelayed(this::nextEnemy, 200); return; }

        // Distance simulation: scale 0.55 (far) … 1.15 (close)
        float scale = 0.55f + rng.nextFloat() * 0.6f;
        bodyW = 46 * d * scale;
        bodyH = 88 * d * scale;
        headR = 15 * d * scale;

        float marginX = bodyW / 2 + 12 * d;
        float topMargin = headR * 2 + 24 * d;
        float bottomMargin = bodyH / 2 + 16 * d;
        ex = marginX + rng.nextFloat() * (w - 2 * marginX);
        ey = topMargin + bodyH / 2 + rng.nextFloat() * (h - topMargin - bottomMargin - bodyH / 2);

        enemyVisible = true;
        enemyShownAt = System.currentTimeMillis();
        updateHudLine();
        gameView.invalidate();
    }

    private void updateHudLine() {
        tvStats.setText(String.format(Locale.US,
                "🎯 عدو %d/%d  |  🎯 هيدشوت: %d (دراغ: %d)  |  💪 جسم: %d  ❌ %d  |  🔥 سلسلة: %d",
                enemiesShown, TOTAL_ENEMIES, headshots, dragHeadshots, bodyShots, misses, streak));
    }

    private float headCx() { return ex; }
    private float headCy() { return ey - bodyH / 2 - headR * 0.8f; }

    private boolean onBody(float x, float y) {
        return Math.abs(x - ex) <= bodyW / 2 * 1.2f && Math.abs(y - ey) <= bodyH / 2 * 1.15f;
    }

    private boolean onHead(float x, float y) {
        float dx = x - headCx(), dy = y - headCy();
        return dx * dx + dy * dy <= headR * headR * 2.1f; // forgiving like FF head hitbox
    }

    private void resolveShot(float upX, float upY, boolean wasDragFromBody, long dragMs) {
        if (!enemyVisible) return;

        if (onHead(upX, upY)) {
            headshots++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
            if (wasDragFromBody) {
                dragHeadshots++;
                totalDragMs += dragMs;
            }
            enemyVisible = false;
            gameView.flash(wasDragFromBody ? 0x5500E676 : 0x5500E5FF);
            ui.postDelayed(this::nextEnemy, 220 + rng.nextInt(380));
        } else if (onBody(upX, upY)) {
            bodyShots++;
            streak = 0;
            enemyVisible = false;
            gameView.flash(0x55FFEA00);
            ui.postDelayed(this::nextEnemy, 220 + rng.nextInt(380));
        } else {
            misses++;
            streak = 0;
        }
        updateHudLine();
        gameView.invalidate();
    }

    private void endRound() {
        roundActive = false;
        enemyVisible = false;
        gameView.invalidate();

        int totalShots = headshots + bodyShots + misses;
        int hsPct = totalShots > 0 ? headshots * 100 / totalShots : 0;
        long avgDrag = dragHeadshots > 0 ? totalDragMs / dragHeadshots : 0;

        // Score: headshot% dominates; proper drags get a bonus
        int dragBonus = headshots > 0 ? dragHeadshots * 20 / headshots : 0;   // up to +20
        int score = Math.min(100, hsPct * 80 / 100 + dragBonus);
        String grade = score >= 85 ? "هيروك 👑" : score >= 70 ? "ماستر 💎"
                : score >= 55 ? "دايموند 🔷" : score >= 40 ? "بلاتينيوم ⚪"
                : score >= 25 ? "جولد 🟡" : "برونز 🟤";

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean newBest = hsPct > sp.getInt("hs_best_pct", 0);
        if (newBest) sp.edit().putInt("hs_best_pct", hsPct).apply();
        if (bestStreak > sp.getInt("hs_best_streak", 0))
            sp.edit().putInt("hs_best_streak", bestStreak).apply();

        String dragJudge;
        if (dragHeadshots == 0) dragJudge = "🔴 كل الهيدشوتات كانت ضغط مباشر — اتدرب على السحب من الجسم للراس، دي التقنية الحقيقية!";
        else if (avgDrag <= 250) dragJudge = "🟢 دراغ سريع جداً (" + avgDrag + "ms) — مستوى محترفين!";
        else if (avgDrag <= 400) dragJudge = "🟡 دراغ كويس (" + avgDrag + "ms) — قلل المسافة اللي صوابعك بيقطعها";
        else dragJudge = "🔴 الدراغ بطيء (" + avgDrag + "ms) — ارفع حساسية العامة والـ 2x شوية";

        tvStats.setText(String.format(Locale.US,
                "🏁 النتيجة: %d/100 — %s%s\n" +
                "🎯 نسبة الهيدشوت: %d%% (%d من %d طلقة)\n" +
                "↕️ دراغ صحيح (جسم→راس): %d من %d هيدشوت | %s\n" +
                "🔥 أطول سلسلة: %d هيدشوت ورا بعض\n" +
                "اضغط أي مكان لجولة جديدة — كرر التمرين لحد ما الدراغ يبقى عادة!",
                score, grade, newBest ? " 🆕 رقم قياسي!" : "",
                hsPct, headshots, totalShots,
                dragHeadshots, headshots, dragJudge, bestStreak));
    }

    /** Arena: draws the enemy silhouette and handles press-drag-release. */
    private class GameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int flashColor = 0;

        GameView() { super(HeadshotTrainerActivity.this); }

        void flash(int color) {
            flashColor = color;
            invalidate();
            ui.postDelayed(() -> { flashColor = 0; invalidate(); }, 130);
        }

        @Override protected void onDraw(Canvas c) {
            // Grid backdrop
            paint.setColor(0xFF161B22);
            paint.setStrokeWidth(1);
            for (int i = 0; i < getWidth(); i += 90) c.drawLine(i, 0, i, getHeight(), paint);
            for (int i = 0; i < getHeight(); i += 90) c.drawLine(0, i, getWidth(), i, paint);

            if (flashColor != 0) c.drawColor(flashColor);

            if (!roundActive && !enemyVisible) {
                paint.setColor(0xFF30363D);
                paint.setTextSize(42);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText("🎯 اضغط للبدء", getWidth() / 2f, getHeight() / 2f, paint);
                return;
            }
            if (!enemyVisible) return;

            float hx = headCx(), hy = headCy();

            // Body (torso + legs suggestion) — dark red silhouette like a dummy
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF8B2635);
            RectF body = new RectF(ex - bodyW / 2, ey - bodyH / 2, ex + bodyW / 2, ey + bodyH / 2);
            c.drawRoundRect(body, bodyW * 0.25f, bodyW * 0.25f, paint);

            // Head — brighter, the prize
            paint.setColor(0xFFFF1744);
            c.drawCircle(hx, hy, headR, paint);
            // Head ring highlight
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(0xFFFFEA00);
            c.drawCircle(hx, hy, headR + 4, paint);

            // Drag hint arrow from body to head (only for the first 3 enemies)
            if (enemiesShown <= 3) {
                paint.setColor(0xAA00E5FF);
                paint.setStrokeWidth(5);
                c.drawLine(ex, ey, hx, hy + headR + 8, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(26);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText("↑ اسحب", ex, ey + bodyH / 2 + 34, paint);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!roundActive) { startRound(); return true; }
                    if (!enemyVisible) return true;
                    touchStartedOnBody = onBody(e.getX(), e.getY()) && !onHead(e.getX(), e.getY());
                    touchStartMs = System.currentTimeMillis();
                    touchStartY = e.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!roundActive || !enemyVisible) return true;
                    long dragMs = System.currentTimeMillis() - touchStartMs;
                    boolean draggedUp = (touchStartY - e.getY()) > headR; // real upward pull
                    boolean properDrag = touchStartedOnBody && draggedUp;
                    resolveShot(e.getX(), e.getY(), properDrag, dragMs);
                    touchStartedOnBody = false;
                    return true;
            }
            return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (roundActive) { roundActive = false; showIdle(); }
    }
}
