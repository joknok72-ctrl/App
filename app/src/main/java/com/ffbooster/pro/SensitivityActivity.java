package com.ffbooster.pro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 2026 pro sensitivity settings tuned for Realme C25Y
 * (4GB RAM, 60Hz screen) — based on 2026 mid-range device guides.
 */
public class SensitivityActivity extends Activity {

    // {label, value(0-200), note}
    private static final String[][] SENS = {
            {"العام (General)", "174", "الأهم! للسحب السريع لفوق (Drag Headshot)"},
            {"الريد دوت (Red Dot)", "155", "للمعارك القريبة بدون سكوب — SMG وAR"},
            {"سكوب 2x", "150", "للمدى المتوسط — مثالي مع PARAFAL"},
            {"سكوب 4x", "140", "للسبراي البعيد المتحكم فيه"},
            {"سكوب القناصة (Sniper)", "70", "منخفض للتحكم الدقيق مع M82B وAWM"},
            {"الكاميرا الحرة (Free Look)", "140", "لمراقبة المحيط أثناء الجري"}
    };

    private static final String[][] BUTTONS = {
            {"🔘 حجم زر الضرب", "49", "الحجم المثالي لجهاز 4GB — لا كبير يعمي ولا صغير يفلت"},
            {"📍 مكان زر الضرب", "تحت عداد الأحياء (Alive Count)", "مساحة كافية للسحب لفوق بسلاسة"},
            {"🎮 وضع عجلة الحركة", "Fixed (ثابت)", "ميزة جديدة في OB54! العجلة ما تتحركش مع صباعك — ثبات أكثر"}
    };

    private static final String[] HEADSHOT_TIPS = {
            "🎯 طريقة الدراغ هيدشوت: صوّب على الصدر ← اضغط الضرب واسحب لفوق بسرعة في نفس اللحظة",
            "🔫 أفضل أسلحة الهيدشوت بعد OB54: MP40 (الميتا الأول) ثم M590 للصدام القريب",
            "👆 العب بصباعين على الأقل (Thumbs) — والأفضل 3 أو 4 أصابع مع الوقت",
            "⏱ تمرّن 10 دقايق يومياً في التدريب (Training) على الرؤوس المتحركة",
            "📵 فعّل وضع عدم الإزعاج (DND) قبل الرانكد — الإشعارات بتفصل تركيزك",
            "🖐 لو إيدك بتعرق: استخدم فينجر سليف (Finger Sleeves) — فرق رهيب على الشاشات اللمسية"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("🎯 حساسيات 2026 الاحترافية");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("أفضل حساسيات مثبتة لعام 2026 مضبوطة لفئة هاتفك (4GB رام / شاشة 60Hz) لأعلى نسبة هيدشوت. طبّقها من: الإعدادات ← الحساسية داخل فري فاير، ثم تمرّن يوم واحد للتأقلم.");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        addHeader(container, d, "🎚 قيم الحساسية (انسخها بالظبط)");
        for (String[] s : SENS) addSensCard(container, d, s[0], Integer.parseInt(s[1]), s[2]);

        addHeader(container, d, "🔘 إعدادات الأزرار");
        for (String[] b : BUTTONS) addTextCard(container, d, b[0], b[1], b[2]);

        addHeader(container, d, "☠️ أسرار الهيدشوت");
        for (String t : HEADSHOT_TIPS) addTextCard(container, d, null, null, t);

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }

    private void addHeader(LinearLayout parent, float d, String text) {
        TextView h = new TextView(this);
        h.setText(text);
        h.setTextColor(Color.parseColor("#FFB300"));
        h.setTextSize(17);
        h.setTypeface(null, Typeface.BOLD);
        h.setGravity(Gravity.START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (14 * d);
        lp.bottomMargin = (int) (8 * d);
        h.setLayoutParams(lp);
        parent.addView(h);
    }

    private void addSensCard(LinearLayout parent, float d, String label, int value, String note) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int pad = (int) (13 * d);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.parseColor("#F1F5F9"));
        t.setTextSize(15);
        t.setTypeface(null, Typeface.BOLD);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(t);

        TextView v = new TextView(this);
        v.setText(String.valueOf(value));
        v.setTextColor(Color.parseColor("#FF6A00"));
        v.setTextSize(20);
        v.setTypeface(null, Typeface.BOLD);
        row.addView(v);
        card.addView(row);

        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(200);
        pb.setProgress(value);
        pb.getProgressDrawable().setTint(Color.parseColor("#FF6A00"));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (8 * d));
        plp.topMargin = (int) (6 * d);
        pb.setLayoutParams(plp);
        card.addView(pb);

        TextView n = new TextView(this);
        n.setText(note);
        n.setTextColor(Color.parseColor("#8B98B8"));
        n.setTextSize(12);
        card.addView(n);

        parent.addView(card);
    }

    private void addTextCard(LinearLayout parent, float d, String title, String value, String note) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int pad = (int) (13 * d);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        card.setLayoutParams(lp);

        if (title != null) {
            TextView t = new TextView(this);
            t.setText(title);
            t.setTextColor(Color.parseColor("#F1F5F9"));
            t.setTextSize(15);
            t.setTypeface(null, Typeface.BOLD);
            card.addView(t);
        }
        if (value != null) {
            TextView v = new TextView(this);
            v.setText(value);
            v.setTextColor(Color.parseColor("#22C55E"));
            v.setTextSize(14);
            v.setTypeface(null, Typeface.BOLD);
            card.addView(v);
        }
        if (note != null) {
            TextView n = new TextView(this);
            n.setText(note);
            n.setTextColor(Color.parseColor("#8B98B8"));
            n.setTextSize(13);
            card.addView(n);
        }
        parent.addView(card);
    }
}
