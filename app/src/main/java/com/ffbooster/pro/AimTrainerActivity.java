package com.ffbooster.pro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

/**
 * Aim Trainer (v10.0) — a real reaction & accuracy drill, Free Fire style.
 *
 * 20 targets appear one-by-one at random positions. The player taps each as
 * fast as possible. We measure:
 *   • Average reaction time (ms) — the #1 skill for clutches and 1v1s
 *   • Accuracy % (hits vs misses — tapping empty space counts as a miss)
 *   • A combined score with a Free Fire rank-style grade (برونز → هيروك)
 * Best score is persisted so the player can track improvement.
 *
 * Warm up 2-3 rounds before Ranked — reaction times measurably improve
 * after a short warm-up.
 */
public class AimTrainerActivity extends Activity {

    private static final String PREFS = "ffbooster";
    private static final int TOTAL_TARGETS = 20;

    private GameView gameView;
    private TextView tvStats;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();

    // Round state
    private boolean roundActive = false;
    private int targetsShown = 0;
    private int hits = 0;
    private int misses = 0;
    private long totalReaction = 0;
    private long targetShownAt = 0;
    // Current target (px) — radius shrinks as the round progresses
    private float tx, ty, tr;
    private boolean targetVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D1117"));

        tvStats = new TextView(this);
        tvStats.setTextColor(Color.parseColor("#F1F5F9"));
        tvStats.setTextSize(14);
        float d = getResources().getDisplayMetrics().density;
        int p = (int) (12 * d);
        tvStats.setPadding(p, p, p, p);
        tvStats.setLineSpacing(4 * d, 1f);
        root.addView(tvStats);

        FrameLayout arena = new FrameLayout(this);
        gameView = new GameView();
        arena.addView(gameView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(arena, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        showIdleStats();
    }

    private void showIdleStats() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int bestScore = sp.getInt("aim_best_score", 0);
        long bestReact = sp.getLong("aim_best_react", 0);
        String best = bestScore > 0
                ? String.format(Locale.US, "🏆 أفضل نتيجة: %d (%s) | أسرع رد فعل متوسط: %d ms",
                    bestScore, gradeFor(bestScore), bestReact)
                : "لسه معملتش أي جولة — سجّلك هيظهر هنا";
        tvStats.setText("🎯 تمرين الإيم — اضغط أي مكان للبدء\n" +
                TOTAL_TARGETS + " هدف هيظهروا واحد ورا التاني، اضربهم بأسرع ما يمكن!\n" + best);
        gameView.invalidate();
    }

    private void startRound() {
        roundActive = true;
        targetsShown = 0;
        hits = 0;
        misses = 0;
        totalReaction = 0;
        targetVisible = false;
        tvStats.setText("🔥 استعد…");
        ui.postDelayed(this::nextTarget, 700);
    }

    private void nextTarget() {
        if (!roundActive) return;
        if (targetsShown >= TOTAL_TARGETS) { endRound(); return; }
        targetsShown++;

        float d = getResources().getDisplayMetrics().density;
        // Radius shrinks: 34dp → 18dp over the round (harder = better training)
        tr = (34 - 16f * (targetsShown - 1) / (TOTAL_TARGETS - 1)) * d;
        int w = gameView.getWidth(), h = gameView.getHeight();
        if (w < 10 || h < 10) { ui.postDelayed(this::nextTarget, 200); return; }
        float margin = tr + 8 * d;
        tx = margin + rng.nextFloat() * (w - 2 * margin);
        ty = margin + rng.nextFloat() * (h - 2 * margin);

        targetVisible = true;
        targetShownAt = System.currentTimeMillis();
        tvStats.setText(String.format(Locale.US, "🎯 هدف %d/%d  |  ✅ %d  ❌ %d",
                targetsShown, TOTAL_TARGETS, hits, misses));
        gameView.invalidate();
    }

    private void onTap(float x, float y) {
        if (!roundActive) { startRound(); return; }
        if (!targetVisible) return;

        float dx = x - tx, dy = y - ty;
        boolean hit = dx * dx + dy * dy <= tr * tr * 1.15f; // small forgiveness like FF hitbox
        if (hit) {
            hits++;
            totalReaction += (System.currentTimeMillis() - targetShownAt);
            targetVisible = false;
            gameView.invalidate();
            ui.postDelayed(this::nextTarget, 150 + rng.nextInt(350)); // random gap = real reactions
        } else {
            misses++;
            tvStats.setText(String.format(Locale.US, "🎯 هدف %d/%d  |  ✅ %d  ❌ %d",
                    targetsShown, TOTAL_TARGETS, hits, misses));
        }
    }

    private void endRound() {
        roundActive = false;
        targetVisible = false;
        gameView.invalidate();

        long avgReact = hits > 0 ? totalReaction / hits : 999;
        int accuracy = (hits + misses) > 0 ? hits * 100 / (hits + misses) : 0;
        // Score: fast reactions & high accuracy both matter (like real fights)
        int score = (int) Math.max(0, Math.min(100,
                (accuracy * 0.5) + Math.max(0, (600 - avgReact) / 600.0 * 100) * 0.5));
        String grade = gradeFor(score);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int bestScore = sp.getInt("aim_best_score", 0);
        boolean newBest = score > bestScore;
        if (newBest) {
            sp.edit().putInt("aim_best_score", score).putLong("aim_best_react", avgReact).apply();
        }

        String reactJudge = avgReact < 350 ? "🟢 رد فعل محترف!" : avgReact < 500 ? "🟡 كويس — كمّل تمرين" : "🔴 محتاج إحماء أكتر";
        tvStats.setText(String.format(Locale.US,
                "🏁 النتيجة: %d/100 — رتبتك: %s%s\n" +
                "⚡ متوسط رد الفعل: %d ms %s\n" +
                "🎯 الدقة: %d%% (✅ %d ضربة | ❌ %d غلط)\n" +
                "اضغط أي مكان لجولة جديدة — العب 2-3 جولات قبل الرانكد!",
                score, grade, newBest ? " 🆕 رقم قياسي جديد!" : "",
                avgReact, reactJudge, accuracy, hits, misses));
    }

    private String gradeFor(int score) {
        if (score >= 85) return "هيروك 👑";
        if (score >= 70) return "ماستر 💎";
        if (score >= 55) return "دايموند 🔷";
        if (score >= 40) return "بلاتينيوم ⚪";
        if (score >= 25) return "جولد 🟡";
        return "برونز 🟤";
    }

    /** Arena view: draws the target (FF-style rings) and handles taps. */
    private class GameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GameView() { super(AimTrainerActivity.this); }

        @Override protected void onDraw(Canvas c) {
            // Subtle grid so the arena doesn't look empty
            paint.setColor(0xFF161B22);
            paint.setStrokeWidth(1);
            for (int i = 0; i < getWidth(); i += 90) c.drawLine(i, 0, i, getHeight(), paint);
            for (int i = 0; i < getHeight(); i += 90) c.drawLine(0, i, getWidth(), i, paint);

            if (!roundActive && !targetVisible) {
                paint.setColor(0xFF30363D);
                paint.setTextSize(46);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText("🎯", getWidth() / 2f, getHeight() / 2f, paint);
                return;
            }
            if (!targetVisible) return;

            // FF-style target: red ring, white ring, red center
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFF1744);
            c.drawCircle(tx, ty, tr, paint);
            paint.setColor(Color.WHITE);
            c.drawCircle(tx, ty, tr * 0.62f, paint);
            paint.setColor(0xFFFF1744);
            c.drawCircle(tx, ty, tr * 0.28f, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                onTap(e.getX(), e.getY());
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (roundActive) { roundActive = false; showIdleStats(); }
    }
}
