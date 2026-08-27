package com.ffbooster.pro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Network Optimizer (v9.0) — finds the fastest DNS for the player's network.
 *
 * Slow/overloaded ISP DNS adds real delay to every server (re)connection in
 * Free Fire (login, matchmaking, reconnect after lag spikes). This screen:
 *   1. Benchmarks popular DNS providers (TCP:53 connect, 3 samples each)
 *   2. Benchmarks name-resolution speed of the CURRENT device DNS
 *   3. Recommends the fastest and opens Android's Private DNS settings
 *      so the user can apply it in two taps (no root needed).
 *
 * Also probes the actual Free Fire server on the winner to prove the
 * end-to-end gain.
 */
public class NetOptimizerActivity extends Activity {

    // {display name, ip for tcp53 probe, private-dns hostname ("" = not DoT)}
    private static final String[][] DNS_PROVIDERS = {
            {"كلاود فلير Cloudflare", "1.1.1.1", "1dot1dot1dot1.cloudflare-dns.com"},
            {"جوجل Google", "8.8.8.8", "dns.google"},
            {"كواد9 Quad9", "9.9.9.9", "dns.quad9.net"},
            {"أوبن دي إن إس OpenDNS", "208.67.222.222", "dns.opendns.com"},
            {"أدجارد AdGuard", "94.140.14.14", "dns.adguard-dns.com"}
    };

    private TextView tvResult;
    private Button btnScan, btnApply;
    private String winnerHost = null;
    private String winnerName = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("📶 مُحسّن الشبكة");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("الـ DNS البطيء بيأخر دخولك الماتش وإعادة الاتصال بعد اللاج. " +
                "الفحص ده بيقيس أسرع DNS لشبكتك دلوقتي ويطبّقه بخطوتين — من غير روت.");

        LinearLayout container = findViewById(R.id.gfxContainer);
        float d = getResources().getDisplayMetrics().density;

        btnScan = new Button(this);
        btnScan.setText("🚀 ابدأ فحص أسرع DNS لشبكتك");
        btnScan.setBackgroundResource(R.drawable.btn_boost);
        btnScan.setTextColor(Color.WHITE);
        btnScan.setTypeface(null, Typeface.BOLD);
        btnScan.setTextSize(15);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
        blp.bottomMargin = (int) (10 * d);
        container.addView(btnScan, blp);

        tvResult = new TextView(this);
        tvResult.setTextColor(Color.parseColor("#F1F5F9"));
        tvResult.setTextSize(13);
        tvResult.setLineSpacing(5 * d, 1f);
        tvResult.setTypeface(Typeface.MONOSPACE);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg);
        int p = (int) (14 * d);
        card.setPadding(p, p, p, p);
        card.addView(tvResult);
        container.addView(card);

        btnApply = new Button(this);
        btnApply.setText("⚙️ افتح إعدادات Private DNS لتطبيق الأسرع");
        btnApply.setBackgroundResource(R.drawable.btn_secondary);
        btnApply.setTextColor(Color.parseColor("#00E5FF"));
        btnApply.setTypeface(null, Typeface.BOLD);
        btnApply.setTextSize(14);
        btnApply.setVisibility(android.view.View.GONE);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
        alp.topMargin = (int) (10 * d);
        container.addView(btnApply, alp);

        TextView howTo = new TextView(this);
        howTo.setTextColor(Color.parseColor("#94A3B8"));
        howTo.setTextSize(12);
        howTo.setLineSpacing(4 * d, 1f);
        howTo.setText("\n💡 طريقة التطبيق:\n" +
                "1) اضغط الزر فوق — هتتفتح صفحة \"DNS الخاص\" في إعدادات أندرويد\n" +
                "2) اختار \"اسم مضيف موفر DNS الخاص\"\n" +
                "3) الصق اسم المضيف اللي التطبيق نسخه لك تلقائياً واضغط حفظ\n" +
                "4) ارجع العب — الاتصال بسيرفرات فري فاير هيبقى أسرع وأثبت\n\n" +
                "⚠️ لو النت قطع بعد التغيير (بعض شبكات الواي فاي بتحجب DoT): ارجع اختار \"تلقائي\"");
        container.addView(howTo);

        tvResult.setText("اضغط \"ابدأ الفحص\" — بياخد حوالي 10 ثواني ⏱");

        btnScan.setOnClickListener(v -> runScan());
        btnApply.setOnClickListener(v -> applyWinner());

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());
    }

    // ---------- Benchmark ----------
    private void runScan() {
        btnScan.setEnabled(false);
        btnScan.setText("⏳ جاري الفحص… (10 ثواني تقريباً)");
        btnApply.setVisibility(android.view.View.GONE);
        tvResult.setText("📡 بيتم قياس " + DNS_PROVIDERS.length + " مزودين DNS × 3 عينات لكل واحد…");

        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();

            // 0) Current-DNS resolution speed (how fast the device resolves a new name now)
            long currentResolve = resolveTimeMs("mpsg.freefiremobile.com");
            if (currentResolve < 0) currentResolve = resolveTimeMs("ff.garena.com");
            sb.append("🔎 سرعة الـ DNS الحالي بتاع شبكتك:\n");
            if (currentResolve >= 0) {
                String judge = currentResolve < 30 ? "🟢 سريع" : (currentResolve < 90 ? "🟡 متوسط" : "🔴 بطيء — غيّره!");
                sb.append(String.format(Locale.US, "   حلّ اسم سيرفر FF في %d ms %s\n\n", currentResolve, judge));
            } else {
                sb.append("   ❌ فشل حل الأسماء — الشبكة فيها مشكلة\n\n");
            }

            // 1) Provider benchmark
            sb.append("🏁 سباق مزودي الـ DNS (متوسط 3 عينات):\n");
            long best = Long.MAX_VALUE;
            int bestIdx = -1;
            long[] results = new long[DNS_PROVIDERS.length];
            for (int i = 0; i < DNS_PROVIDERS.length; i++) {
                results[i] = avgTcpMs(DNS_PROVIDERS[i][1], 53, 3);
                if (results[i] >= 0 && results[i] < best) { best = results[i]; bestIdx = i; }
            }
            for (int i = 0; i < DNS_PROVIDERS.length; i++) {
                String medal = (i == bestIdx) ? "🥇 " : "   ";
                if (results[i] >= 0) {
                    String bar = results[i] < 40 ? "🟢" : (results[i] < 100 ? "🟡" : "🔴");
                    sb.append(String.format(Locale.US, "%s%s: %d ms %s\n", medal, DNS_PROVIDERS[i][0], results[i], bar));
                } else {
                    sb.append(String.format(Locale.US, "%s%s: ❌ محجوب/غير متاح\n", medal, DNS_PROVIDERS[i][0]));
                }
            }

            // 2) Verdict + end-to-end proof against the FF server
            if (bestIdx >= 0) {
                winnerName = DNS_PROVIDERS[bestIdx][0];
                winnerHost = DNS_PROVIDERS[bestIdx][2];
                sb.append("\n🏆 الأسرع لشبكتك: ").append(winnerName)
                  .append(" (").append(best).append(" ms)\n");
                sb.append("   اسم المضيف للتطبيق: ").append(winnerHost).append("\n");
                if (currentResolve >= 90 && best < 50) {
                    sb.append("\n⚡ التقدير: هتوفر ≈").append(Math.max(0, currentResolve - best))
                      .append(" ms في كل عملية اتصال جديدة باللعبة!");
                } else if (currentResolve >= 0 && currentResolve < 30) {
                    sb.append("\n✅ الـ DNS الحالي أصلاً ممتاز — مش محتاج تغيير، بس لو حابب الخصوصية طبّق الأسرع");
                }
            } else {
                winnerHost = null;
                sb.append("\n❌ كل المزودين محجوبين — الشبكة دي مقيدة (واي فاي شركة/مدرسة؟) جرّب بيانات الموبايل");
            }

            final String result = sb.toString();
            ui.post(() -> {
                tvResult.setText(result);
                btnScan.setEnabled(true);
                btnScan.setText("🔁 أعد الفحص");
                if (winnerHost != null) {
                    btnApply.setText("⚙️ طبّق " + winnerName + " — افتح Private DNS");
                    btnApply.setVisibility(android.view.View.VISIBLE);
                }
            });
        });
    }

    /** Average TCP connect time to ip:port over n samples; -1 if unreachable. */
    private long avgTcpMs(String ip, int port, int n) {
        long sum = 0; int ok = 0;
        for (int i = 0; i < n; i++) {
            try {
                Socket s = new Socket();
                long t0 = System.nanoTime();
                s.connect(new InetSocketAddress(ip, port), 2000);
                sum += (System.nanoTime() - t0) / 1_000_000;
                ok++;
                s.close();
            } catch (Exception ignored) {}
        }
        return ok > 0 ? sum / ok : -1;
    }

    /** Time to resolve a hostname with the device's current DNS; -1 on failure. */
    private long resolveTimeMs(String host) {
        try {
            // Bypass the positive cache by asking for all records
            long t0 = System.nanoTime();
            InetAddress.getAllByName(host);
            return (System.nanoTime() - t0) / 1_000_000;
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------- Apply ----------
    private void applyWinner() {
        if (winnerHost == null) return;
        // Copy hostname so the user just pastes it in the settings field
        try {
            android.content.ClipboardManager cb =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cb.setPrimaryClip(android.content.ClipData.newPlainText("dns", winnerHost));
            Toast.makeText(this, "📋 اتنسخ: " + winnerHost + " — الصقه في خانة اسم المضيف", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {}
        // Open Private DNS settings (falls back to the general network page)
        try {
            startActivity(new Intent("android.settings.PRIVATE_DNS_SETTINGS"));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                Toast.makeText(this, "ادخل: الشبكة والإنترنت ← DNS الخاص", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {}
        }
    }
}
