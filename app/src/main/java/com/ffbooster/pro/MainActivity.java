package com.ffbooster.pro;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String FF_PACKAGE = "com.dts.freefireth";
    private static final String FF_MAX_PACKAGE = "com.dts.freefiremax";

    // Free Fire (Garena) server endpoints for latency probing
    private static final String[][] SERVERS = {
            {"سيرفر الشرق الأوسط (ME)", "mpsg.freefiremobile.com"},
            {"سيرفر جارينا الرئيسي", "ff.garena.com"},
            {"جوجل (مرجع عام)", "google.com"},
            {"كلاود فلير (مرجع سريع)", "1.1.1.1"}
    };

    private static final String PREFS = "ffbooster";

    private TextView tvDeviceName, tvRam, tvStorage, tvBattery, tvBoostStatus, tvPingResult, tvTips, tvBoostStats, tvFfStatus;
    private ProgressBar pbRam, pbStorage;
    private View pingCard;
    private Button btnBoost, btnLaunch, btnPing, btnGfx, btnMeta, btnSens, btnTools, btnCombos, btnCodes;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());

    // Live auto-refresh of device stats every 3 seconds (v3.0)
    private final Runnable statsTick = new Runnable() {
        @Override public void run() {
            refreshStats();
            ui.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvRam = findViewById(R.id.tvRam);
        tvStorage = findViewById(R.id.tvStorage);
        tvBattery = findViewById(R.id.tvBattery);
        tvBoostStatus = findViewById(R.id.tvBoostStatus);
        tvPingResult = findViewById(R.id.tvPingResult);
        tvTips = findViewById(R.id.tvTips);
        pbRam = findViewById(R.id.pbRam);
        pbStorage = findViewById(R.id.pbStorage);
        pingCard = findViewById(R.id.pingCard);
        btnBoost = findViewById(R.id.btnBoost);
        btnLaunch = findViewById(R.id.btnLaunch);
        btnPing = findViewById(R.id.btnPing);
        btnGfx = findViewById(R.id.btnGfx);
        btnMeta = findViewById(R.id.btnMeta);
        btnSens = findViewById(R.id.btnSens);
        btnTools = findViewById(R.id.btnTools);
        btnCombos = findViewById(R.id.btnCombos);
        btnCodes = findViewById(R.id.btnCodes);
        tvBoostStats = findViewById(R.id.tvBoostStats);
        tvFfStatus = findViewById(R.id.tvFfStatus);

        tvTips.setText(
                "• ⚔️ جديد v4.0: \"تشكيلات الشخصيات\" — أقوى 6 كومبوهات لميتا OB54 حسب أسلوب لعبك + دليل رفع الرانك\n" +
                "• 🎁 جديد v4.0: زر \"أكواد الاستدعاء\" يفتح موقع جارينا الرسمي لاستبدال أكواد الجوائز المجانية\n" +
                "• 🛠 افتح \"أدوات برو\" — مراقبة المعالج الحية + اختصارات ما قبل الرانكد بضغطة واحدة\n" +
                "• 🆕 افتح \"تحديث OB54\" لتعرف أقوى أسلحة الميتا الجديدة (MP40 الأول حالياً!)\n" +
                "• 🎯 طبّق \"حساسيات 2026\" المضبوطة لجهازك لأعلى نسبة هيدشوت\n" +
                "• فعّل وضع الطيران 5 ثواني ثم أطفئه قبل اللعب لتجديد الشبكة\n" +
                "• اشحن الهاتف فوق 30٪ — الهاتف يقلل الأداء عند البطارية الضعيفة\n" +
                "• لا تلعب أثناء الشحن (الحرارة تخفض الفريمات على معالج T610)\n" +
                "• امسح كاش فري فاير من الإعدادات مرة كل أسبوع (التحديثات الجديدة بتكبّر الكاش)\n" +
                "• استخدم إعدادات GFX الموجودة في التطبيق — مضبوطة لجهازك بالظبط");

        btnBoost.setOnClickListener(v -> doBoost());
        btnLaunch.setOnClickListener(v -> launchFreeFire());
        btnPing.setOnClickListener(v -> doPingTest());
        btnGfx.setOnClickListener(v -> startActivity(new Intent(this, GfxActivity.class)));
        btnMeta.setOnClickListener(v -> startActivity(new Intent(this, MetaActivity.class)));
        btnSens.setOnClickListener(v -> startActivity(new Intent(this, SensitivityActivity.class)));
        btnTools.setOnClickListener(v -> startActivity(new Intent(this, ToolsActivity.class)));
        btnCombos.setOnClickListener(v -> startActivity(new Intent(this, CombosActivity.class)));
        btnCodes.setOnClickListener(v -> openRedeemSite());

        refreshStats();
        updateBoostStats();
        detectFreeFire();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(statsTick);          // live monitoring while visible
        updateBoostStats();
        detectFreeFire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(statsTick);
    }

    // ---------- Device stats ----------
    private void refreshStats() {
        tvDeviceName.setText("📱 " + android.os.Build.BRAND.toUpperCase(Locale.US) + " "
                + android.os.Build.MODEL + " — أندرويد " + android.os.Build.VERSION.RELEASE);

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long totalMb = mi.totalMem / (1024 * 1024);
        long availMb = mi.availMem / (1024 * 1024);
        long usedMb = totalMb - availMb;
        int ramPct = (int) (usedMb * 100 / Math.max(totalMb, 1));
        tvRam.setText(String.format(Locale.US, "🧠 الرام: مستخدم %d MB من %d MB (متاح %d MB)", usedMb, totalMb, availMb));
        pbRam.setProgress(ramPct);

        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        long totalGb = fs.getTotalBytes() / (1024L * 1024 * 1024);
        long freeGb = fs.getAvailableBytes() / (1024L * 1024 * 1024);
        int stPct = (int) ((totalGb - freeGb) * 100 / Math.max(totalGb, 1));
        tvStorage.setText(String.format(Locale.US, "💾 التخزين: متاح %d GB من %d GB", freeGb, totalGb));
        pbStorage.setProgress(stPct);

        Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batt != null) {
            int temp = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            float c = temp / 10f;
            String heat = c < 35 ? "✅ ممتازة" : (c < 42 ? "⚠️ متوسطة" : "🔥 مرتفعة — برّد الهاتف!");
            tvBattery.setText(String.format(Locale.US, "🔋 البطارية: %d%%  |  الحرارة: %.1f°م %s", level, c, heat));
        }
    }

    // ---------- Free Fire detection (v4.0) ----------
    private void detectFreeFire() {
        if (tvFfStatus == null) return;
        PackageManager pm = getPackageManager();
        StringBuilder sb = new StringBuilder();
        boolean found = false;

        String[][] editions = {
                {FF_PACKAGE, "🔥 فري فاير (العادية)"},
                {FF_MAX_PACKAGE, "💎 فري فاير MAX"}
        };
        for (String[] ed : editions) {
            try {
                PackageInfo pi = pm.getPackageInfo(ed[0], 0);
                found = true;
                sb.append(ed[1]).append(" — مثبتة ✅\n");
                sb.append("   الإصدار: ").append(pi.versionName);
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(ed[0], 0);
                    long apkMb = new java.io.File(ai.sourceDir).length() / (1024 * 1024);
                    if (apkMb > 0) sb.append("  |  حجم APK: ").append(apkMb).append(" MB");
                } catch (Exception ignored) {}
                sb.append('\n');
            } catch (Exception ignored) {}
        }

        if (found) {
            sb.append("\n💡 تأكد أن اللعبة محدّثة لآخر إصدار (OB54) من المتجر قبل الرانكد");
            tvFfStatus.setText(sb.toString().trim());
        } else {
            tvFfStatus.setText("❌ فري فاير غير مثبتة على هذا الجهاز!\nثبّت اللعبة من متجر Google Play أولاً لتستفيد من كل مميزات التطبيق");
        }
    }

    // ---------- Official redeem codes site (v4.0) ----------
    private void openRedeemSite() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://reward.ff.garena.com/")));
            Toast.makeText(this, "🎁 موقع جارينا الرسمي — سجّل دخول بحساب اللعبة واستبدل الأكواد", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Boost statistics (v3.0) ----------
    private void updateBoostStats() {
        if (tvBoostStats == null) return;
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int count = sp.getInt("boost_count", 0);
        long freedTotal = sp.getLong("freed_total_mb", 0);
        if (count > 0) {
            tvBoostStats.setVisibility(View.VISIBLE);
            tvBoostStats.setText(String.format(Locale.US,
                    "🏆 إجمالي التسريعات: %d مرة  |  رام محرر تراكمياً: ≈%d MB", count, freedTotal));
        } else {
            tvBoostStats.setVisibility(View.GONE);
        }
    }

    private void recordBoost(long freedMb) {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.edit()
          .putInt("boost_count", sp.getInt("boost_count", 0) + 1)
          .putLong("freed_total_mb", sp.getLong("freed_total_mb", 0) + Math.max(freedMb, 0))
          .apply();
    }

    private long availRamMb() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    // ---------- RAM Boost ----------
    private void doBoost() {
        btnBoost.setEnabled(false);
        btnBoost.setText(R.string.boosting);
        tvBoostStatus.setVisibility(View.GONE);
        final long ramBefore = availRamMb();

        executor.execute(() -> {
            int killed = 0;
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            try {
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                for (ApplicationInfo app : apps) {
                    // Skip system apps, ourselves, and Free Fire itself
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    if (app.packageName.equals(getPackageName())) continue;
                    if (app.packageName.equals(FF_PACKAGE) || app.packageName.equals(FF_MAX_PACKAGE)) continue;
                    try {
                        am.killBackgroundProcesses(app.packageName);
                        killed++;
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            // Suggest GC to reclaim memory
            System.gc();

            final int k = killed;
            ui.postDelayed(() -> {
                long freed = availRamMb() - ramBefore;
                recordBoost(freed);
                btnBoost.setEnabled(true);
                btnBoost.setText(R.string.boost_btn);
                tvBoostStatus.setVisibility(View.VISIBLE);
                String freedTxt = freed > 0 ? " وتحرير ≈" + freed + " MB رام" : "";
                tvBoostStatus.setText("✅ تم تنظيف " + k + " تطبيق من الخلفية" + freedTxt + " — الرام جاهز لفري فاير!");
                refreshStats();
                updateBoostStats();
            }, 1200);
        });
    }

    // ---------- Launch Free Fire ----------
    private void launchFreeFire() {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(FF_PACKAGE);
        if (i == null) i = pm.getLaunchIntentForPackage(FF_MAX_PACKAGE);
        if (i != null) {
            // Boost first, then launch
            Toast.makeText(this, "⚡ جاري التسريع ثم التشغيل…", Toast.LENGTH_SHORT).show();
            final Intent launch = i;
            executor.execute(() -> {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                try {
                    for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                        if (app.packageName.equals(getPackageName())) continue;
                        if (app.packageName.startsWith("com.dts.")) continue;
                        try { am.killBackgroundProcesses(app.packageName); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                ui.post(() -> startActivity(launch));
            });
        } else {
            Toast.makeText(this, R.string.ff_not_found, Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Ping test ----------
    private void doPingTest() {
        pingCard.setVisibility(View.VISIBLE);
        tvPingResult.setText(getString(R.string.ping_testing));
        btnPing.setEnabled(false);

        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            long bestMs = Long.MAX_VALUE;
            for (String[] srv : SERVERS) {
                long ms = measureLatency(srv[1]);
                String verdict;
                if (ms < 0) verdict = "❌ غير متاح";
                else if (ms < 60) verdict = "🟢 ممتاز (" + ms + "ms)";
                else if (ms < 120) verdict = "🟡 جيد (" + ms + "ms)";
                else verdict = "🔴 مرتفع (" + ms + "ms)";
                if (ms > 0 && ms < bestMs) bestMs = ms;
                sb.append(srv[0]).append(": ").append(verdict).append("\n");
            }
            if (bestMs != Long.MAX_VALUE) {
                String overall = bestMs < 60 ? "🎮 اتصالك ممتاز للرانكد!"
                        : (bestMs < 120 ? "👍 اتصالك مقبول — تجنب الكلوز فايت البعيد"
                        : "⚠️ بينج مرتفع — لا تدخل رانكد الآن!");
                sb.append("\n").append(overall).append("\n");
            }
            sb.append("💡 لو البينج فوق 100ms: قرّب من الراوتر، جرّب خدعة وضع الطيران من أدوات برو، أو استخدم بيانات 4G");
            final String result = sb.toString().trim();
            ui.post(() -> {
                tvPingResult.setText(result);
                btnPing.setEnabled(true);
            });
        });
    }

    /** TCP connect latency (works without root, unlike ICMP on some devices). */
    private long measureLatency(String host) {
        int[] ports = {443, 80};
        for (int port : ports) {
            try {
                long best = Long.MAX_VALUE;
                for (int i = 0; i < 3; i++) {
                    Socket s = new Socket();
                    long t0 = System.nanoTime();
                    s.connect(new InetSocketAddress(host, port), 3000);
                    long dt = (System.nanoTime() - t0) / 1_000_000;
                    s.close();
                    if (dt < best) best = dt;
                }
                return best;
            } catch (Exception ignored) {}
        }
        // Fallback: system ping binary
        try {
            Process p = Runtime.getRuntime().exec("ping -c 2 -W 2 " + host);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("time=")) {
                    String t = line.substring(line.indexOf("time=") + 5);
                    t = t.split(" ")[0];
                    return (long) Float.parseFloat(t);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
