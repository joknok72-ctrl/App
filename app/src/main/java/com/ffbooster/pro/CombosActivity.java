package com.ffbooster.pro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Character combos screen (v4.0) — strongest OB54 meta character
 * combinations per play-style, pet pairings and a rank push guide.
 */
public class CombosActivity extends Activity {

    // {style icon+name, main skill, combo, why}
    private static final String[][] COMBOS = {
            {"⚔️ الراش والهجوم السريع", "Alok (Speed Remix)",
                    "Alok + Kelly + Hayato + Moco",
                    "هالة سرعة 16% + جري أسرع + ضرر يخترق الدروع عند نقص الدم + تعليم الأعداء المضروبين — أقوى تشكيلة راش مع MP40 في ميتا OB54"},
            {"🛡 الدفاع والبقاء (Rank Push)", "Chrono (Time Veil)",
                    "Chrono + Olivia + Luqueta + Mr. Waggor",
                    "درع يصد 1000 ضرر (بعد بافف OB54 كولداون 45 ثانية فقط!) + إحياء بدم أعلى + ماكس HP يزيد مع كل قتل + البطريق يعطيك جلو وول — الأفضل لرفع الرانك بأمان"},
            {"🎯 القنص والمدى البعيد", "Maro (Falcon Fervor)",
                    "Maro + Laura + Rafael + Falco",
                    "ضرر يزيد مع المسافة + دقة أعلى بالسكوب + القتلى بالقنص ينزفون بصمت — مثالية مع M82B كاسر الجلو وول بعد بافف OB54"},
            {"🤫 اللعب الخفي والالتفاف", "Wukong (Camo Decoy)",
                    "Wukong + Clu + Shirou + Night Panther",
                    "شجيرة وهمية تخدع الأعداء (بافف OB54) + كشف مواقع الجالسين + تعليم من ضربك — التفاف وقنص بيوت بدون ما حد يحس بيك"},
            {"⛑ الدعم والفريق (Squad)", "Kassie (Mutual Focus)",
                    "Kassie + Olivia + Dimitri + Ottero",
                    "تطبيب نفسك 60HP مع زميلك (Skill Boost الجديد) + إحياء ذاتي داخل المنطقة + استرجاع EP — عمود فقري السكواد في الرانكد"},
            {"💰 اقتصادية (بدون شخصيات مدفوعة)", "Kla + شخصيات مجانية",
                    "Kla + Kelly + Hayato + Spirit Fox",
                    "لو لسه بادئ: لكمات قاتلة + سرعة + اختراق دروع — كلها شخصيات رخيصة/مجانية وتنافس في البرونز للبلاتينيوم"}
    };

    // {pet, skill}
    private static final String[][] PETS = {
            {"🐧 Mr. Waggor", "يعطيك جلو وول مجاني كل فترة — أهم بت في اللعبة للرانكد"},
            {"🦅 Falco", "قفزة أسرع من الطيارة وهبوط أسرع — تاخد أفضل لووت قبل الكل"},
            {"🐼 Detective Panda", "يرجعلك دم مع كل قتل — مثالي للراش المتواصل"},
            {"🦊 Spirit Fox", "دم إضافي عند استخدام علب التطبيب — للبقاء أطول"},
            {"🐕 Rockie", "يقلل كولداون سكيل شخصيتك النشط — ممتاز مع Chrono وAlok"}
    };

    private static final String[] RANK_GUIDE = {
            "🥉 برونز → فضة: العب عادي واجمع كيلات — الرانك بيرتفع حتى بدون فوز",
            "🥈 فضة → ذهب: ابدأ العب بحذر، انزل أماكن هادية وخد لووت كامل قبل القتال",
            "🥇 ذهب → بلاتينيوم: البقاء أهم من الكيل! ادخل التوب 10 كل ماتش = نقاط مضمونة",
            "💎 بلاتينيوم → دايموند: استخدم تشكيلة الدفاع (Chrono) والعب على الزون — خليك دايماً جوه الآمن بدري",
            "🏆 دايموند → هيروك: سكواد ثابت + أدوار واضحة (راشر/سنايبر/سبورت) + كلام صوتي — مستحيل سولو",
            "⚡ نصيحة ذهبية: لو خسرت ماتشين ورا بعض وقّف العب ساعة — التلت (Tilt) بينزل رانكك أكتر",
            "📅 العب أول أسبوع في الموسم — الرانك بيكون أسهل قبل ما المحترفين يوصلوا هيروك"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("⚔️ تشكيلات الشخصيات (Combos)");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("أقوى تشكيلات شخصيات + بت لميتا OB54 حسب أسلوب لعبك، مع دليل رفع الرانك خطوة بخطوة. اختر التشكيلة اللي تناسب ستايلك وثبّتها في خانات الشخصيات داخل اللعبة.");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        addHeader(container, d, "🔥 أقوى 6 تشكيلات في ميتا OB54");
        for (String[] c : COMBOS) addComboCard(container, d, c[0], c[1], c[2], c[3]);

        addHeader(container, d, "🐾 أفضل البتس (Pets) للرانكد");
        for (String[] p : PETS) addSimpleCard(container, d, p[0], p[1]);

        addHeader(container, d, "📈 دليل رفع الرانك (Rank Push)");
        for (String g : RANK_GUIDE) addSimpleCard(container, d, null, g);

        TextView note = new TextView(this);
        note.setText("💡 جرّب التشكيلة في ماتشات كلاسيك الأول قبل ما تدخل بيها رانكد");
        note.setTextColor(Color.parseColor("#22C55E"));
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = (int) (12 * d);
        note.setLayoutParams(nlp);
        container.addView(note);

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }

    private void addHeader(LinearLayout parent, float d, String text) {
        TextView h = new TextView(this);
        h.setText(text);
        h.setTextColor(Color.parseColor("#FFB300"));
        h.setTextSize(17);
        h.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (14 * d);
        lp.bottomMargin = (int) (8 * d);
        h.setLayoutParams(lp);
        parent.addView(h);
    }

    private LinearLayout makeCard(float d) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int pad = (int) (13 * d);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        card.setLayoutParams(lp);
        return card;
    }

    private void addComboCard(LinearLayout parent, float d, String style, String mainSkill,
                              String combo, String why) {
        LinearLayout card = makeCard(d);

        TextView t = new TextView(this);
        t.setText(style);
        t.setTextColor(Color.parseColor("#F1F5F9"));
        t.setTextSize(15);
        t.setTypeface(null, Typeface.BOLD);
        card.addView(t);

        TextView m = new TextView(this);
        m.setText("السكيل الأساسي: " + mainSkill);
        m.setTextColor(Color.parseColor("#FF6A00"));
        m.setTextSize(13);
        m.setTypeface(null, Typeface.BOLD);
        card.addView(m);

        TextView c = new TextView(this);
        c.setText("التشكيلة: " + combo);
        c.setTextColor(Color.parseColor("#22C55E"));
        c.setTextSize(14);
        c.setTypeface(null, Typeface.BOLD);
        card.addView(c);

        TextView w = new TextView(this);
        w.setText(why);
        w.setTextColor(Color.parseColor("#8B98B8"));
        w.setTextSize(12);
        card.addView(w);

        parent.addView(card);
    }

    private void addSimpleCard(LinearLayout parent, float d, String title, String details) {
        LinearLayout card = makeCard(d);
        if (title != null) {
            TextView t = new TextView(this);
            t.setText(title);
            t.setTextColor(Color.parseColor("#F1F5F9"));
            t.setTextSize(15);
            t.setTypeface(null, Typeface.BOLD);
            card.addView(t);
        }
        TextView de = new TextView(this);
        de.setText(details);
        de.setTextColor(Color.parseColor("#8B98B8"));
        de.setTextSize(13);
        card.addView(de);
        parent.addView(card);
    }
}
