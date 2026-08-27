package com.ffbooster.pro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

/**
 * Pro Tools screen (v3.0) — quick device shortcuts and a live CPU monitor,
 * everything a Free Fire player needs before a ranked match.
 */
public class ToolsActivity extends Activity {

    private static final String FF_PACKAGE = "com.dts.freefireth";
    private static final String FF_MAX_PACKAGE = "com.dts.freefiremax";

    private TextView tvCpuLive, tvNet;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable cpuTick = new Runnable() {
        @Override public void run() {
            updateCpuLive();
            ui.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("🛠 أدوات برو");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("اختصارات سريعة تجهّز هاتفك قبل الرانكد — كل أداة بضغطة واحدة بدل ما تدوّر في الإعدادات.");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        // ---- Live CPU monitor card ----
        addHeader(container, d, "📈 مراقبة المعالج الحية (Unisoc T610)");
        LinearLayout cpuCard = makeCard(d);
        tvCpuLive = new TextView(this);
        tvCpuLive.setTextColor(Color.parseColor("#F1F5F9"));
        tvCpuLive.setTextSize(13);
        tvCpuLive.setTypeface(Typeface.MONOSPACE);
        cpuCard.addView(tvCpuLive);
        container.addView(cpuCard);

        // ---- Network status card ----
        addHeader(container, d, "📶 حالة الشبكة الآن");
        LinearLayout netCard = makeCard(d);
        tvNet = new TextView(this);
        tvNet.setTextColor(Color.parseColor("#F1F5F9"));
        tvNet.setTextSize(14);
        netCard.addView(tvNet);
        container.addView(netCard);

        // ---- Quick action buttons ----
        addHeader(container, d, "⚡ اختصارات جاهزة قبل الماتش");

        addToolButton(container, d, "🧹 مسح كاش فري فاير",
                "افتح صفحة اللعبة ← التخزين ← مسح الكاش (Cache) — يحل معظم التهنيج بعد التحديثات",
                v -> openFreeFireSettings());

        addToolButton(container, d, "📵 وضع عدم الإزعاج (DND)",
                "فعّله قبل الرانكد — الإشعارات والمكالمات بتقطع اللعب وبتنزّل الفريمات",
                v -> openSafely(new Intent("android.settings.ZEN_MODE_SETTINGS"),
                        new Intent(Settings.ACTION_SOUND_SETTINGS)));

        addToolButton(container, d, "🔋 إعدادات البطارية",
                "أوقف توفير الطاقة أثناء اللعب — توفير الطاقة يخنق معالج T610 ويهبط الفريمات",
                v -> openSafely(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                        new Intent(Settings.ACTION_SETTINGS)));

        addToolButton(container, d, "📡 إعدادات الواي فاي",
                "بدّل بين الواي فاي والبيانات لو البينج مرتفع — جرّب فحص البينج بعد التبديل",
                v -> openSafely(new Intent(Settings.ACTION_WIFI_SETTINGS),
                        new Intent(Settings.ACTION_WIRELESS_SETTINGS)));

        addToolButton(container, d, "✈️ خدعة تجديد الشبكة",
                "افتح وضع الطيران 5 ثواني ثم اقفله — يجدد الاتصال بالبرج ويقلل تذبذب البينج",
                v -> openSafely(new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS),
                        new Intent(Settings.ACTION_WIRELESS_SETTINGS)));

        addToolButton(container, d, "🗑 تفريغ مساحة التخزين",
                "لو التخزين شبه ممتلئ فري فاير بتهنّج — احذف الملفات الكبيرة غير المهمة",
                v -> openSafely(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
                        new Intent(Settings.ACTION_SETTINGS)));

        TextView note = new TextView(this);
        note.setText("💡 روتين ما قبل الرانكد المثالي:\nتسريع فائق ← مسح الكاش ← DND ← فحص البينج ← شغّل اللعبة");
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

        updateNetworkStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        ui.post(cpuTick);
        updateNetworkStatus();
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(cpuTick);
    }

    // ---------- Quick actions ----------
    private void openFreeFireSettings() {
        String pkg = null;
        try {
            getPackageManager().getPackageInfo(FF_PACKAGE, 0);
            pkg = FF_PACKAGE;
        } catch (Exception e) {
            try {
                getPackageManager().getPackageInfo(FF_MAX_PACKAGE, 0);
                pkg = FF_MAX_PACKAGE;
            } catch (Exception ignored) {}
        }
        if (pkg == null) {
            Toast.makeText(this, R.string.ff_not_found, Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + pkg));
        try {
            startActivity(i);
            Toast.makeText(this, "ادخل: التخزين ← مسح الكاش (Clear Cache)", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الإعدادات", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSafely(Intent primary, Intent fallback) {
        try {
            startActivity(primary);
        } catch (Exception e) {
            try { startActivity(fallback); } catch (Exception ignored) {
                Toast.makeText(this, "تعذر فتح الإعدادات على هذا الجهاز", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- Live CPU ----------
    private void updateCpuLive() {
        StringBuilder sb = new StringBuilder();
        int cores = Runtime.getRuntime().availableProcessors();
        sb.append("عدد الأنوية: ").append(cores).append('\n');
        for (int i = 0; i < cores; i++) {
            long cur = readKhz("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
            long max = readKhz("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
            if (cur > 0 && max > 0) {
                int pct = (int) (cur * 100 / max);
                sb.append(String.format(Locale.US, "نواة %d: %4d MHz  %s %d%%\n",
                        i, cur / 1000, bar(pct), pct));
            }
        }
        if (sb.length() < 20) sb.append("(قراءة تردد المعالج غير متاحة على هذا الجهاز)");
        tvCpuLive.setText(sb.toString().trim());
    }

    private String bar(int pct) {
        int filled = Math.max(0, Math.min(8, pct / 13));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 8; i++) b.append(i < filled ? '█' : '░');
        return b.toString();
    }

    private long readKhz(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return -1;
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line = r.readLine();
            r.close();
            return Long.parseLong(line.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------- Network ----------
    private void updateNetworkStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                String type = ni.getType() == ConnectivityManager.TYPE_WIFI ? "واي فاي 📶" : "بيانات الجوال 📱";
                tvNet.setText("متصل عبر: " + type + "\n💡 للرانكد: الأفضل واي فاي قريب من الراوتر، ولو بينجه مرتفع جرّب 4G");
            } else {
                tvNet.setText("❌ لا يوجد اتصال بالإنترنت — فعّل الواي فاي أو البيانات");
            }
        } catch (Exception e) {
            tvNet.setText("تعذر قراءة حالة الشبكة");
        }
    }

    // ---------- UI helpers ----------
    private LinearLayout makeCard(float d) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int pad = (int) (14 * d);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (10 * d);
        card.setLayoutParams(lp);
        return card;
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

    private void addToolButton(LinearLayout parent, float d, String label, String hint,
                               android.view.View.OnClickListener action) {
        LinearLayout card = makeCard(d);

        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.parseColor("#F1F5F9"));
        b.setTextSize(15);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackgroundResource(R.drawable.btn_secondary);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (48 * d));
        b.setLayoutParams(blp);
        b.setOnClickListener(action);
        card.addView(b);

        TextView h = new TextView(this);
        h.setText(hint);
        h.setTextColor(Color.parseColor("#8B98B8"));
        h.setTextSize(12);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = (int) (6 * d);
        h.setLayoutParams(hlp);
        card.addView(h);

        parent.addView(card);
    }
}
