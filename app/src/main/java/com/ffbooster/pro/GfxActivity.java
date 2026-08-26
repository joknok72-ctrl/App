package com.ffbooster.pro;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * GFX settings screen — optimal Free Fire graphics settings tuned
 * specifically for Realme C25Y (Unisoc T610, 4GB RAM, 720x1600 display).
 */
public class GfxActivity extends Activity {

    private static final String[][] SETTINGS = {
            {"🖥 الجرافيك (Graphics)", "Smooth (سموث)", "أهم إعداد! يرفع الفريمات لأقصى حد على معالج T610"},
            {"🎯 الفريمات (FPS)", "High (عالي)", "هاتفك شاشته 60Hz — اختر High للثبات الكامل"},
            {"👤 الظلال (Shadows)", "إيقاف ❌", "الظلال تستهلك المعالج بشدة بدون فائدة قتالية"},
            {"✨ الرسوم المتحركة عالية الدقة", "إيقاف ❌", "توفير كبير في الرام (4GB فقط)"},
            {"🔫 آثار الرصاص", "تشغيل ✅", "مهمة للتصويب — استهلاكها بسيط"},
            {"🌈 جودة الألوان", "Standard (قياسي)", "الألوان الزاهية تستهلك GPU أكثر"},
            {"📏 الدقة داخل اللعبة", "منخفضة/افتراضية", "شاشتك HD+ أصلاً — الدقة العالية بتهنّج"},
            {"🔊 صوت عالي الجودة", "إيقاف ❌", "يخفف الحمل على المعالج أثناء الزحمة"},
            {"👁 مدى الرؤية", "متوسط", "توازن مثالي بين كشف الأعداء والأداء"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        for (String[] s : SETTINGS) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            int pad = (int) (14 * d);
            card.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (10 * d);
            card.setLayoutParams(lp);

            TextView title = new TextView(this);
            title.setText(s[0]);
            title.setTextColor(Color.parseColor("#F1F5F9"));
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(title);

            TextView value = new TextView(this);
            value.setText("الإعداد الأمثل: " + s[1]);
            value.setTextColor(Color.parseColor("#FFB300"));
            value.setTextSize(14);
            value.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(value);

            TextView why = new TextView(this);
            why.setText(s[2]);
            why.setTextColor(Color.parseColor("#8B98B8"));
            why.setTextSize(12);
            card.addView(why);

            container.addView(card);
        }

        TextView note = new TextView(this);
        note.setText("⚠️ طبّق هذه الإعدادات من داخل فري فاير:\nالإعدادات ← العرض (Display)");
        note.setTextColor(Color.parseColor("#22C55E"));
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        container.addView(note);

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }
}
