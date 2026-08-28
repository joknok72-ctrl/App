package com.ffbooster.pro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Headshot Master Guide (v11.0) — the complete legit playbook for landing
 * headshots easily in Free Fire: the drag technique step-by-step, sensitivity
 * values tuned for this device class, best headshot weapons, HUD button
 * placement, and situational drills. Pure knowledge + training — no cheats.
 */
public class HeadshotGuideActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("🎯 دليل الهيدشوت الشامل");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("كل أسرار الهيدشوت في مكان واحد — التقنية الصح + الإعدادات المضبوطة لجهازك + التمرين. " +
                "طبّق الدليل ده مع \"مدرب الدراغ\" وهتلاحظ الفرق من أول يوم. (كله قانوني 100% — مهارة مش غش)");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        addSection(container, d, "1️⃣ تقنية الدراغ هيدشوت — الحركة الأساسية",
                "دي أهم مهارة في اللعبة كلها. 90% من الهيدشوتات بتيجي منها:\n\n" +
                "① وجّه الكروس هير على **جسم** العدو (مش راسه!)\n" +
                "② اضغط زر النار و**في نفس اللمسة** اسحب صوابعك لفوق بسرعة\n" +
                "③ الكروس هير هيطلع من الجسم للراس والرصاص هيمشي معاه\n" +
                "④ سيب الزر وأنت على الراس\n\n" +
                "💡 السر: البُل (Recoil) الطبيعي للسلاح بيساعدك — السلاح أصلاً بيرفع لفوق، " +
                "فسحبتك + البُل = الرصاص يتركز في الراس.\n\n" +
                "⚠️ أشهر غلطة: التصويب على الراس مباشرة — لو العدو اتحرك تبقى ضيعت كل الطلقات. " +
                "التصويب على الجسم أضمن والدراغ يطلعك للراس.");

        addSection(container, d, "2️⃣ حساسيات الهيدشوت لجهازك (Realme C25Y)",
                "الإعدادات ← الحساسية — القيم دي مضبوطة لشاشة 6.5 بوصة ومعالج T610:\n\n" +
                "• العامة (General): 95-100 ← عشان الدراغ يبقى سريع\n" +
                "• النقطة الحمراء (Red Dot): 90-95\n" +
                "• سكوب 2x: 85-90 ← أهم واحدة للدراغ بالـ AR\n" +
                "• سكوب 4x: 70-80\n" +
                "• قناصة AWM: 45-55 ← دقة أهم من السرعة\n" +
                "• نظرة حرة (Free Look): 60-70\n\n" +
                "💡 لو الدراغ بيعدي الراس وبيطلع فوق منه: نزّل العامة 5 درجات.\n" +
                "💡 لو الدراغ مش بيوصل للراس: ارفع العامة 5 درجات.\n" +
                "جرّب في وضع التدريب 10 دقايق بعد كل تعديل.");

        addSection(container, d, "3️⃣ أفضل أسلحة الهيدشوت (ميتا OB54)",
                "• MP40 🥇 — ملك الكلوز رينج: أسرع معدل ضرب، دراغ واحد = نوك\n" +
                "• M1887 (الشوزن) — ضربة راس واحدة قريبة = خلاص\n" +
                "• UMP — ثابت جداً وسهل التحكم للمبتدئين في الدراغ\n" +
                "• M4A1/SCAR — للميد رينج مع سكوب 2x ودراغ خفيف\n" +
                "• M82B/AWM — القنص: استهدف الراس مباشرة (مفيش دراغ في السكوب الكامل)\n\n" +
                "💡 العب بسلاحين: MP40 للقريب + AR بسكوب 2x للمتوسط.");

        addSection(container, d, "4️⃣ مكان زر النار الصح (HUD)",
                "الدراغ محتاج مساحة لصوابعك تتحرك:\n\n" +
                "• حط زر النار الشمال في **نص الشاشة الشمال** (مش تحت خالص)\n" +
                "• كبّر حجمه لـ 50-60% عشان صوابعك ما تفلتش منه أثناء السحب\n" +
                "• فعّل زر نار يمين إضافي صغير فوق يمين — للطلقات السريعة\n" +
                "• العب بـ 3 صوابع لو تقدر: شمال للحركة، سبابة يمين للنار، سبابة شمال للتصويب\n\n" +
                "💡 من الإعدادات ← واجهة التحكم ← عدّل وجرّب في التدريب قبل الرانكد.");

        addSection(container, d, "5️⃣ خطة التمرين اليومية (15 دقيقة)",
                "① 5 دقايق: \"مدرب الدراغ هيدشوت\" هنا في التطبيق — لحد ما توصل 70%+ هيدشوت\n" +
                "② 5 دقايق: وضع التدريب في اللعبة — دراغ على الدمى من مسافات مختلفة\n" +
                "③ 5 دقايق: ماتش كلاش سكواد — طبّق الدراغ على لاعبين حقيقيين\n\n" +
                "💡 فعّل الكروس هير بوضع \"مستوى الرأس 🎯\" من الشاشة الرئيسية — " +
                "هيعوّد عينك تفضل مركزة على ارتفاع الراس طول الوقت، " +
                "فأول ما عدو يظهر يبقى الدراغ أقصر وأسرع.\n\n" +
                "⏱ بعد أسبوع من التمرين اليومي: نسبة الهيدشوت بتاعتك هتتضاعف.");

        addSection(container, d, "6️⃣ أسرار المحترفين",
                "• الجلوس المفاجئ (Crouch) أثناء الدراغ = صعب يصيبوك وأنت بتصيب\n" +
                "• القفز + دراغ (Jump-shot) ضد الشوزن القريب\n" +
                "• في الـ 1v1: خلي ضهرك لحائط — العدو قدامك بس والدراغ أسهل\n" +
                "• اضرب أول ما تشوف مش أول ما يشوفك — الثانية الأولى بتحسم\n" +
                "• الرصاص الأول من أي سلاح أدق رصاصة — خليه في الراس (Tap الأول ثم دراغ)\n" +
                "• متلعبش رانكد والبينج فوق 100ms — الدراغ هيتسجل متأخر وهتحس إنك بتخطئ (افحص بالتطبيق الأول!)");

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }

    private void addSection(LinearLayout container, float d, String header, String body) {
        TextView tvH = new TextView(this);
        tvH.setTextColor(Color.parseColor("#FFB300"));
        tvH.setTextSize(15);
        tvH.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = (int) (14 * d);
        hlp.bottomMargin = (int) (6 * d);
        container.addView(tvH, hlp);
        tvH.setText(header);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int p = (int) (14 * d);
        card.setPadding(p, p, p, p);
        container.addView(card, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvB = new TextView(this);
        tvB.setTextColor(Color.parseColor("#F1F5F9"));
        tvB.setTextSize(13);
        tvB.setLineSpacing(5 * d, 1f);
        tvB.setText(body.replace("**", ""));
        card.addView(tvB);
    }
}
