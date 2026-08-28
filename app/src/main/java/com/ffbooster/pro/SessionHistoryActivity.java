package com.ffbooster.pro;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Session history (v10.0) — the last 10 play sessions recorded by Auto-Pilot.
 *
 * Each entry: end-time | duration | max temp | avg FPS. Stored as a compact
 * '#'-separated string in SharedPreferences (no DB needed for 10 rows).
 * Row format: endMillis,durMin,maxTemp10x,avgFps
 *
 * Shows a trend verdict: is the phone getting hotter / FPS dropping across
 * sessions? That's the signal to clean the phone or let it cool.
 */
public class SessionHistoryActivity extends Activity {

    private static final String PREFS = "ffbooster";
    static final String KEY_HISTORY = "session_history";
    static final int MAX_ROWS = 10;

    /** Called by AutoPilotService after each session to append a row. */
    static void append(SharedPreferences sp, long endMillis, long durMin, float maxTemp, int avgFps) {
        String row = endMillis + "," + durMin + "," + (int) (maxTemp * 10) + "," + avgFps;
        String old = sp.getString(KEY_HISTORY, "");
        String merged = old.isEmpty() ? row : row + "#" + old;   // newest first
        String[] rows = merged.split("#");
        if (rows.length > MAX_ROWS) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < MAX_ROWS; i++) {
                if (i > 0) sb.append('#');
                sb.append(rows[i]);
            }
            merged = sb.toString();
        }
        sp.edit().putString(KEY_HISTORY, merged).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("📜 سجل جلسات اللعب");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("آخر 10 جلسات سجلها الطيار الآلي 🤖 — تابع حرارة جهازك ومتوسط الفريمات عبر الوقت. " +
                "لو الحرارة بتزيد أو الـ FPS بينزل جلسة ورا جلسة = الجهاز محتاج راحة أو تنظيف.");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String history = sp.getString(KEY_HISTORY, "");

        if (history.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setTextSize(14);
            empty.setLineSpacing(5 * d, 1f);
            empty.setText("لسه مفيش جلسات في السجل.\n\nفعّل الطيار الآلي 🤖 من الشاشة الرئيسية والعب فري فاير — " +
                    "كل جلسة هتتسجل هنا تلقائياً بالمدة والحرارة ومتوسط الـ FPS.");
            container.addView(empty);
        } else {
            String[] rows = history.split("#");

            // ---- Trend analysis over the recorded sessions ----
            float firstTemp = -1, lastTemp = -1;
            int firstFps = -1, lastFps = -1;
            for (int i = rows.length - 1; i >= 0; i--) {  // oldest → newest
                String[] f = rows[i].split(",");
                if (f.length < 4) continue;
                try {
                    float t = Integer.parseInt(f[2]) / 10f;
                    int fps = Integer.parseInt(f[3]);
                    if (t > 0) { if (firstTemp < 0) firstTemp = t; lastTemp = t; }
                    if (fps > 0) { if (firstFps < 0) firstFps = fps; lastFps = fps; }
                } catch (Exception ignored) {}
            }
            StringBuilder trend = new StringBuilder();
            if (rows.length >= 3) {
                if (lastTemp > 0 && firstTemp > 0) {
                    float diff = lastTemp - firstTemp;
                    if (diff >= 3) trend.append("🔥 الحرارة زادت ").append(String.format(Locale.US, "%.1f", diff)).append("°م عبر الجلسات — ريّح الجهاز أو شيل الجراب!\n");
                    else if (diff <= -2) trend.append("❄ الحرارة بتتحسن عبر الجلسات — ممتاز!\n");
                }
                if (lastFps > 0 && firstFps > 0) {
                    int diff = lastFps - firstFps;
                    if (diff <= -8) trend.append("📉 الـ FPS نزل ").append(-diff).append(" فريم عبر الجلسات — اعمل تسريع ذكي وامسح كاش اللعبة\n");
                    else if (diff >= 8) trend.append("📈 الـ FPS اتحسن ").append(diff).append(" فريم — استمر!\n");
                }
            }
            if (trend.length() > 0) {
                TextView tvTrend = new TextView(this);
                tvTrend.setTextColor(Color.parseColor("#FFB74D"));
                tvTrend.setTextSize(13);
                tvTrend.setTypeface(null, Typeface.BOLD);
                tvTrend.setLineSpacing(4 * d, 1f);
                tvTrend.setText("📊 تحليل الاتجاه:\n" + trend.toString().trim());
                LinearLayout trendCard = new LinearLayout(this);
                trendCard.setOrientation(LinearLayout.VERTICAL);
                trendCard.setBackgroundResource(R.drawable.card_bg);
                int tp = (int) (12 * d);
                trendCard.setPadding(tp, tp, tp, tp);
                trendCard.addView(tvTrend);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                tlp.bottomMargin = (int) (10 * d);
                container.addView(trendCard, tlp);
            }

            // ---- Session rows (newest first) ----
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", Locale.US);
            for (String row : rows) {
                String[] f = row.split(",");
                if (f.length < 4) continue;
                long end, dur; float temp; int fps;
                try {
                    end = Long.parseLong(f[0]);
                    dur = Long.parseLong(f[1]);
                    temp = Integer.parseInt(f[2]) / 10f;
                    fps = Integer.parseInt(f[3]);
                } catch (Exception e) { continue; }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundResource(R.drawable.card_bg);
                int p = (int) (12 * d);
                card.setPadding(p, p, p, p);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                clp.topMargin = (int) (8 * d);
                container.addView(card, clp);

                TextView tvHead = new TextView(this);
                tvHead.setTextColor(Color.parseColor("#F1F5F9"));
                tvHead.setTextSize(13);
                tvHead.setTypeface(null, Typeface.BOLD);
                tvHead.setText("🎮 " + fmt.format(new Date(end)) + "  —  " + dur + " دقيقة");
                card.addView(tvHead);

                TextView tvBody = new TextView(this);
                tvBody.setTextColor(Color.parseColor("#94A3B8"));
                tvBody.setTextSize(12);
                String tempTxt = temp > 0
                        ? String.format(Locale.US, "🌡 أقصى حرارة: %.1f°م %s", temp,
                            temp >= 42 ? "🔥" : temp >= 38 ? "🟡" : "✅")
                        : "🌡 الحرارة: غير مسجلة";
                String fpsTxt = fps > 0
                        ? String.format(Locale.US, "  |  📊 متوسط FPS: %d %s", fps,
                            fps >= 50 ? "🟢" : fps >= 30 ? "🟡" : "🔴")
                        : "  |  📊 FPS: شغّل الـ HUD عشان يتسجل";
                tvBody.setText(tempTxt + fpsTxt);
                card.addView(tvBody);
            }

            // ---- Clear history button ----
            Button clear = new Button(this);
            clear.setText("🗑 مسح السجل");
            clear.setBackgroundResource(R.drawable.btn_secondary);
            clear.setTextColor(Color.parseColor("#FF5252"));
            clear.setTextSize(13);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (46 * d));
            blp.topMargin = (int) (12 * d);
            container.addView(clear, blp);
            clear.setOnClickListener(v -> {
                sp.edit().remove(KEY_HISTORY).apply();
                Toast.makeText(this, "🗑 اتمسح السجل", Toast.LENGTH_SHORT).show();
                recreate();
            });
        }

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }
}
