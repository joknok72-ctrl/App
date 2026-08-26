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
 * OB54 Update Center — meta weapons, character balance changes and
 * skill boosts based on the official OB54 patch notes (June 2026).
 */
public class MetaActivity extends Activity {

    // {icon+name, verdict, details}
    private static final String[][] META_WEAPONS = {
            {"🔫 MP40 (بافف قوي 🔥)", "أقوى SMG في اللعبة الآن",
                    "اختراق دروع +10% | دقة +5% | سرعة تبديل +25%\nأصبح Gold — اشتره من الماكينة بـ400 عملة (حد 5 مرات)\nالميتا رقم 1 للمعارك القريبة"},
            {"🔫 PARAFAL (بافف)", "أفضل AR للمدى المتوسط/البعيد",
                    "اختراق دروع +5% | أصبح Gold\nيضرب قوي جداً مع سكوب 2x — التقطه من الترسانات"},
            {"🔫 M590 (بافف)", "شوتغن الرعب الجديد",
                    "مدى الهيدشوت +15% | سرعة تبديل +20%\nمع شخصية نيكيتا لا يزال قوياً رغم النيرف"},
            {"🎯 M82B (بافف كبير)", "قناصة كسر الجلو وول",
                    "ضرر +6% | اختراق +10% | مدى +20%\nيخترق الجلو وول ويعلّم عليه — مضاد الجدران الأول"},
            {"⚔️ AUG-II", "متوفر في الماكينة",
                    "600 عملة FF (حد مرتين بالماتش) — قوي جداً لو معك عملات"},
            {"🛡 Shield Gun (نيرف)", "لم يعد يحمي 100%",
                    "الآن يقلل الضرر 5% فقط بدل الحماية الكاملة — لا تعتمد عليه"}
    };

    private static final String[][] CHARACTERS = {
            {"🟢 Chrono (بافف)", "الكولداون نزل من 60 لـ45 ثانية — ارجع العبه! الدرع يصد 1000 ضرر"},
            {"🟢 Olivia (بافف)", "التطبيب الجماعي زاد لـ90% ومدى 20 متر — أفضل هيلر للسكواد"},
            {"🔴 Oscar (نيرف)", "الكولداون زاد من 45 لـ60 ثانية — استخدم الاندفاعة بحكمة"},
            {"🔴 Nero (نيرف قوي)", "الدمية أصبحت 1HP فقط (تنكسر برصاصة!) والضرر نزل لـ6/ثانية"},
            {"🔴 Maro (نيرف)", "ضرر المسافة نزل من 25% لـ20% — لا يزال جيد للقنص"},
            {"🔴 Nikita (نيرف)", "سرعة إعادة التعبئة نزلت من 30% لـ20% — لا تزال قوية مع M590"}
    };

    private static final String[][] SKILL_BOOSTS = {
            {"Alok", "Speed Remix: مدى الهالة 10م + سرعة 16% | أو Heal Remix: تطبيب 20HP/ثانية"},
            {"Chrono", "Time Veil: درع لا يُرى من الخارج! | أو Time Drift: الدرع يتحرك معك"},
            {"Wukong", "Camo Decoy: يترك نسخة وهمية تتحرك | أو Cloned Camo: 3 شجيرات بدل واحدة"},
            {"Homer", "UAV Shockwave: كشف الأعداء 120م حول الإصابة | أو Super Drone: تطير بسرعة +15%"},
            {"Skyler", "Sticky Rhythm: منطقة تلاحق العدو وتكسر جدرانه | أو Split Rhythm: يكسر 3 جدران معاً"},
            {"Kassie", "Mutual Focus: تطبب نفسها 60HP مع الزميل | أو Echoed Bond: نسخ مصغرة تضرب معك"}
    };

    private static final String[] TACTICS = {
            "💰 الميتا الجديدة: اجمع عملات FF من أول اللعبة — الماكينات فيها MP40 وAUG-II ودروع",
            "🎯 لما يتبقى 10 لاعبين: الكل ياخذ Weapon Awakener تلقائياً — سلاحك يصير ذخيرة لا نهائية!",
            "⛑ الإحياء الفوري: تقدر تحيي زميلك في مكانه مرة واحدة (قبل الزون الرابع) — 10 ثواني",
            "🗺 أثناء القفز: شوف ألوان المناطق (أحمر/ذهبي/بنفسجي) — الأحمر أعلى لووت وأخطر",
            "🏭 المصنع في برمودا اتبدل بساحة احتفال الذكرى التاسعة — لووت جديد ومباني جديدة",
            "❌ خرائط Bermuda 2.0 وAlpine اتشالت من اللعبة نهائياً",
            "👊 الفيست بامب الجديد: بعد ما تقتل عدو اضغط زر المصافحة مع زملائك",
            "📺 راجع الماتش بعد الرانكد: خريطة حرارية للأعداء + مسارات فريقك — تعلم منها!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("🆕 مركز تحديث OB54");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("آخر تحديث رسمي (يونيو 2026) — الذكرى التاسعة. كل الميتا الجديدة هنا حسب ملاحظات التحديث الرسمية من Garena.\n⏳ التحديث القادم OB55: متوقع 16 سبتمبر 2026 (سلاح FO12 الجديد)");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        addHeader(container, d, "🔥 أسلحة الميتا بعد OB54");
        for (String[] w : META_WEAPONS) addCard(container, d, w[0], w[1], w[2]);

        addHeader(container, d, "⚖️ تغييرات الشخصيات (البالانس)");
        for (String[] c : CHARACTERS) addCard(container, d, c[0], null, c[1]);

        addHeader(container, d, "⚡ Skill Boosts الجديدة (داخل الماتش)");
        for (String[] s : SKILL_BOOSTS) addCard(container, d, "👤 " + s[0], null, s[1]);

        addHeader(container, d, "🧠 تكتيكات الفوز في الميتا الجديدة");
        for (String t : TACTICS) addCard(container, d, null, null, t);

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

    private void addCard(LinearLayout parent, float d, String title, String verdict, String details) {
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
        if (verdict != null) {
            TextView v = new TextView(this);
            v.setText(verdict);
            v.setTextColor(Color.parseColor("#22C55E"));
            v.setTextSize(13);
            v.setTypeface(null, Typeface.BOLD);
            card.addView(v);
        }
        if (details != null) {
            TextView de = new TextView(this);
            de.setText(details);
            de.setTextColor(Color.parseColor("#8B98B8"));
            de.setTextSize(13);
            card.addView(de);
        }
        parent.addView(card);
    }
}
