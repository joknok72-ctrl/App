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

    private TextView tvDeviceName, tvRam, tvStorage, tvBattery, tvBoostStatus, tvPingResult, tvTips, tvBoostStats, tvFfStatus, tvSession;
    private ProgressBar pbRam, pbStorage;
    private View pingCard;
    private Button btnBoost, btnLaunch, btnPing, btnGfx, btnMeta, btnSens, btnTools, btnCombos, btnCodes, btnGameMode, btnHud, btnReadiness, btnAutoPilot, btnXhair, btnXhairStyle;

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
        btnGameMode = findViewById(R.id.btnGameMode);
        btnHud = findViewById(R.id.btnHud);
        btnReadiness = findViewById(R.id.btnReadiness);
        btnAutoPilot = findViewById(R.id.btnAutoPilot);
        btnXhair = findViewById(R.id.btnXhair);
        btnXhairStyle = findViewById(R.id.btnXhairStyle);
        tvBoostStats = findViewById(R.id.tvBoostStats);
        tvFfStatus = findViewById(R.id.tvFfStatus);
        tvSession = findViewById(R.id.tvSession);

        tvTips.setText(
                "• 🎯 جديد v8.0: كروس هير عائم في منتصف الشاشة — دقة رهيبة للنو سكوب والهيب فاير، ما بيأثرش على اللمس (3 أشكال × 5 ألوان)\n" +
                "• 📊 جديد v8.0: عداد FPS حقيقي في الـ HUD — شوف الفريمات الفعلية وأنت بتلعب (🟢 50+ ممتاز | 🔴 أقل من 30 فيه مشكلة)\n" +
                "• 🔇 جديد v8.0: الطيار الآلي دلوقتي بيفعّل \"عدم الإزعاج\" تلقائياً أثناء اللعب — مفيش إشعار هيبوظ عليك كلتش (محتاج إذن DND مرة واحدة)\n" +
                "• ⚡ جديد v8.0: زر \"تسريع FF\" في شريط الإعدادات السريعة — اسحب الشاشة من فوق واضغطه من أي مكان (أضفه بزر القلم ✂)\n" +
                "• 🤖 \"الطيار الآلي\" — فعّله مرة واحدة وانسى! أول ما تفتح فري فاير: وضع الألعاب + HUD + الكروس هير يشتغلوا لوحدهم\n" +
                "• ⚡ جديد v7.0: دبل كليك على الـ HUD وأنت جوة اللعبة = تسريع فوري من غير ما تخرج من الماتش!\n" +
                "• 🧠 تسريع ذكي تكيفي — التطبيق يقرأ ضغط الرام ويقرر عدد موجات التنظيف لوحده (2←5 موجات)!\n" +
                "• 🏁 جديد v6.0: \"فحص جاهزية الرانكد\" — تقييم شامل (رام+حرارة+بطارية+بينج+جيتر) قبل ما تدخل رانكد\n" +
                "• 🎮 وضع الألعاب 2.0 — ذكي تكيفي: ينظف كل 12 ثانية لو الرام مخنوقة، ويرتاح لـ 45ث لو الوضع تمام (أوفر للبطارية)\n" +
                "• ⚔️ \"تشكيلات الشخصيات\" — أقوى 6 كومبوهات لميتا OB54 + دليل رفع الرانك\n" +
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
        btnGameMode.setOnClickListener(v -> toggleGameMode());
        btnHud.setOnClickListener(v -> toggleHud());
        btnReadiness.setOnClickListener(v -> doReadinessCheck());
        btnAutoPilot.setOnClickListener(v -> toggleAutoPilot());
        btnXhair.setOnClickListener(v -> toggleCrosshair());
        btnXhairStyle.setOnClickListener(v -> cycleCrosshairStyle());

        refreshStats();
        updateBoostStats();
        detectFreeFire();
        updateGameModeButton();
        updateHudButton();
        updateAutoPilotButton();
        updateSessionCard();
        updateXhairButtons();
        requestBatteryExemption();

        // Android 13+ needs runtime permission for the Game Mode notification
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(statsTick);          // live monitoring while visible
        updateBoostStats();
        detectFreeFire();
        updateGameModeButton();
        updateHudButton();
        updateAutoPilotButton();
        updateSessionCard();
        updateXhairButtons();
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

    // ---------- Smart Turbo engine (v6.0) ----------
    /**
     * One kill pass over all user apps. Android on 4GB devices restarts
     * killed processes aggressively, so Smart Turbo runs ADAPTIVE passes:
     * it reads RAM pressure first and decides 2→5 waves automatically,
     * verifying after each wave whether more cleaning is worth it.
     */
    private int boostPass(ActivityManager am) {
        int killed = 0;
        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (app.packageName.equals(getPackageName())) continue;
                if (app.packageName.equals(FF_PACKAGE) || app.packageName.equals(FF_MAX_PACKAGE)) continue;
                try {
                    am.killBackgroundProcesses(app.packageName);
                    killed++;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return killed;
    }

    /** Deletes our own cache dir — every MB counts on a 4GB device. */
    private void trimOwnCache() {
        try {
            java.io.File cache = getCacheDir();
            java.io.File[] files = cache.listFiles();
            if (files != null) for (java.io.File f : files) deleteRecursive(f);
        } catch (Exception ignored) {}
    }

    private void deleteRecursive(java.io.File f) {
        if (f.isDirectory()) {
            java.io.File[] kids = f.listFiles();
            if (kids != null) for (java.io.File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private int ramUsedPct() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return (int) ((mi.totalMem - mi.availMem) * 100 / Math.max(mi.totalMem, 1));
    }

    private void doBoost() {
        btnBoost.setEnabled(false);
        btnBoost.setText(R.string.boosting);
        tvBoostStatus.setVisibility(View.GONE);
        final long ramBefore = availRamMb();
        final int pressureBefore = ramUsedPct();

        executor.execute(() -> {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            // ── SMART TURBO (v6.0): decide the number of waves from RAM pressure.
            //    ≥90% used → 5 waves, ≥75% → 4, ≥60% → 3, else 2.
            int planned = pressureBefore >= 90 ? 5 : pressureBefore >= 75 ? 4 : pressureBefore >= 60 ? 3 : 2;
            final int plannedF = planned;
            ui.post(() -> Toast.makeText(this, "🧠 المحرك الذكي: ضغط الرام " + pressureBefore + "% ← خطة " + plannedF + " موجات", Toast.LENGTH_SHORT).show());

            int killed = 0;
            int wavesDone = 0;
            long lastAvail = availRamMb();
            for (int w = 1; w <= planned; w++) {
                final int wf = w;
                ui.post(() -> btnBoost.setText("🧠 الموجة " + wf + "/" + plannedF + " — تنظيف ذكي…"));
                killed = Math.max(killed, boostPass(am));
                if (w == planned) trimOwnCache();   // cache trim on final wave
                System.gc();
                sleep(800);
                wavesDone = w;
                // Smart early-exit: if a wave freed almost nothing and pressure
                // is already comfortable, stop — no point burning CPU/battery.
                long nowAvail = availRamMb();
                if (w >= 2 && nowAvail - lastAvail < 30 && ramUsedPct() < 70) break;
                lastAvail = nowAvail;
            }

            final int k = killed;
            final int waves = wavesDone;
            ui.post(() -> {
                long freed = availRamMb() - ramBefore;
                recordBoost(freed);
                btnBoost.setEnabled(true);
                btnBoost.setText(R.string.boost_btn);
                tvBoostStatus.setVisibility(View.VISIBLE);
                String freedTxt = freed > 0 ? " وتحرير ≈" + freed + " MB رام" : "";
                int pressureAfter = ramUsedPct();
                tvBoostStatus.setText("🧠 تسريع ذكي مكتمل! " + waves + " موجات — " + k + " تطبيق" + freedTxt
                        + "\n📉 ضغط الرام: " + pressureBefore + "% ← " + pressureAfter + "% — جاهز لفري فاير!");
                refreshStats();
                updateBoostStats();
            });
        });
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ---------- Launch Free Fire (v5.0: turbo + auto Game Mode) ----------
    private void launchFreeFire() {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(FF_PACKAGE);
        if (i == null) i = pm.getLaunchIntentForPackage(FF_MAX_PACKAGE);
        if (i != null) {
            Toast.makeText(this, "🚀 تسريع تيربو + تفعيل وضع الألعاب ثم التشغيل…", Toast.LENGTH_SHORT).show();
            final Intent launch = i;
            executor.execute(() -> {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                // Two quick turbo passes before launching for maximum free RAM
                boostPass(am);
                sleep(700);
                boostPass(am);
                System.gc();
                ui.post(() -> {
                    startGameMode();      // keep boosting in background while playing
                    startActivity(launch);
                });
            });
        } else {
            Toast.makeText(this, R.string.ff_not_found, Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Game Mode service control (v5.0) ----------
    private void startGameMode() {
        try {
            Intent svc = new Intent(this, GameModeService.class).setAction(GameModeService.ACTION_START);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            updateGameModeButton();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تفعيل وضع الألعاب", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopGameMode() {
        try {
            startService(new Intent(this, GameModeService.class).setAction(GameModeService.ACTION_STOP));
        } catch (Exception ignored) {}
        ui.postDelayed(this::updateGameModeButton, 300);
    }

    private void toggleGameMode() {
        if (GameModeService.running) {
            stopGameMode();
            Toast.makeText(this, "⏹ تم إيقاف وضع الألعاب", Toast.LENGTH_SHORT).show();
        } else {
            startGameMode();
            Toast.makeText(this, "🎮 وضع الألعاب شغّال! تنظيف تلقائي كل 30 ثانية + مراقبة الحرارة أثناء اللعب", Toast.LENGTH_LONG).show();
        }
    }

    private void updateGameModeButton() {
        if (btnGameMode == null) return;
        if (GameModeService.running) {
            btnGameMode.setText("⏹ إيقاف وضع الألعاب 2.0 (شغّال ✅)");
        } else {
            btnGameMode.setText("🎮 وضع الألعاب 2.0 — تنظيف ذكي تكيفي أثناء اللعب");
        }
    }

    // ---------- Floating in-game HUD control (v6.0) ----------
    private void toggleHud() {
        if (HudService.running) {
            try {
                startService(new Intent(this, HudService.class).setAction(HudService.ACTION_STOP));
            } catch (Exception ignored) {}
            Toast.makeText(this, "⏹ تم إخفاء الـ HUD", Toast.LENGTH_SHORT).show();
            ui.postDelayed(this::updateHudButton, 300);
            return;
        }
        // Overlay permission is mandatory for a floating HUD
        if (android.os.Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "🔐 فعّل إذن \"الظهور فوق التطبيقات\" لـ FF Booster ثم ارجع وفعّل الـ HUD", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                Toast.makeText(this, "افتح الإعدادات ← التطبيقات ← FF Booster ← الظهور فوق التطبيقات", Toast.LENGTH_LONG).show();
            }
            return;
        }
        try {
            Intent svc = new Intent(this, HudService.class).setAction(HudService.ACTION_START);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "📊 HUD شغّال! هيفضل ظاهر فوق فري فاير — اسحبه لأي مكان، اضغطه يصغر، مطولاً يقفل", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل الـ HUD", Toast.LENGTH_SHORT).show();
        }
        ui.postDelayed(this::updateHudButton, 500);
    }

    private void updateHudButton() {
        if (btnHud == null) return;
        if (HudService.running) {
            btnHud.setText("⏹ إخفاء HUD (ظاهر ✅)");
        } else {
            btnHud.setText("📊 HUD داخل اللعبة");
        }
    }

    // ---------- Auto-Pilot control (v7.0) ----------
    private void toggleAutoPilot() {
        if (AutoPilotService.running) {
            try { startService(new Intent(this, AutoPilotService.class).setAction(AutoPilotService.ACTION_STOP)); } catch (Exception ignored) {}
            Toast.makeText(this, "⏹ تم إيقاف الطيار الآلي", Toast.LENGTH_SHORT).show();
            ui.postDelayed(this::updateAutoPilotButton, 300);
            return;
        }
        // Needs Usage Access to detect when Free Fire opens
        if (!AutoPilotService.hasUsageAccess(this)) {
            Toast.makeText(this, "🔐 فعّل إذن \"الوصول للاستخدام\" لـ FF Booster ثم ارجع وفعّل الطيار الآلي", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "افتح الإعدادات ← التطبيقات ← وصول خاص ← الوصول للاستخدام", Toast.LENGTH_LONG).show();
            }
            return;
        }
        try {
            Intent svc = new Intent(this, AutoPilotService.class).setAction(AutoPilotService.ACTION_START);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            Toast.makeText(this, "🤖 الطيار الآلي شغّال! افتح فري فاير وكل حاجة هتشتغل لوحدها — مفيش حاجة تاني عليك!", Toast.LENGTH_LONG).show();
            // One-time ask for DND access so Auto-Pilot can silence notifications mid-match (v8.0)
            try {
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                if (nm != null && !nm.isNotificationPolicyAccessGranted() && !sp.getBoolean("asked_dnd", false)) {
                    sp.edit().putBoolean("asked_dnd", true).apply();
                    Toast.makeText(this, "🔇 اختياري: فعّل إذن \"عدم الإزعاج\" لـ FF Booster — عشان الطيار الآلي يكتم الإشعارات وأنت بتلعب", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل الطيار الآلي", Toast.LENGTH_SHORT).show();
        }
        ui.postDelayed(this::updateAutoPilotButton, 500);
    }

    private void updateAutoPilotButton() {
        if (btnAutoPilot == null) return;
        if (AutoPilotService.running) {
            btnAutoPilot.setText("🤖 الطيار الآلي شغّال ✅ — اضغط للإيقاف");
        } else {
            btnAutoPilot.setText("🤖 تفعيل الطيار الآلي — كل حاجة أوتوماتيك مع فري فاير");
        }
    }

    // ---------- Crosshair control (v8.0) ----------
    private void toggleCrosshair() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (CrosshairService.running) {
            try { startService(new Intent(this, CrosshairService.class).setAction(CrosshairService.ACTION_STOP)); } catch (Exception ignored) {}
            sp.edit().putBoolean("xhair_enabled", false).apply();
            Toast.makeText(this, "⏹ تم إخفاء الكروس هير", Toast.LENGTH_SHORT).show();
            ui.postDelayed(this::updateXhairButtons, 300);
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "🔐 فعّل إذن \"الظهور فوق التطبيقات\" ثم ارجع وفعّل الكروس هير", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
            return;
        }
        try {
            Intent svc = new Intent(this, CrosshairService.class).setAction(CrosshairService.ACTION_START);
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);
            sp.edit().putBoolean("xhair_enabled", true).apply();
            Toast.makeText(this, "🎯 الكروس هير ظهر في منتصف الشاشة — ما بيأثرش على اللمس إطلاقاً! ممتاز للنو سكوب", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تشغيل الكروس هير", Toast.LENGTH_SHORT).show();
        }
        ui.postDelayed(this::updateXhairButtons, 500);
    }

    private void cycleCrosshairStyle() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Cycle: advance style; when style wraps, advance color too
        int style = (sp.getInt("xhair_style", 0) + 1) % CrosshairService.STYLE_NAMES.length;
        int color = sp.getInt("xhair_color", 0);
        if (style == 0) color = (color + 1) % CrosshairService.COLORS.length;
        sp.edit().putInt("xhair_style", style).putInt("xhair_color", color).apply();
        Toast.makeText(this, "🎯 الشكل: " + CrosshairService.STYLE_NAMES[style]
                + " | اللون: " + CrosshairService.COLOR_NAMES[color], Toast.LENGTH_SHORT).show();
        // If showing, restart so the view redraws with the new style
        if (CrosshairService.running) {
            try {
                Intent svc = new Intent(this, CrosshairService.class).setAction(CrosshairService.ACTION_START);
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                else startService(svc);
            } catch (Exception ignored) {}
        }
        updateXhairButtons();
    }

    private void updateXhairButtons() {
        if (btnXhair == null) return;
        if (CrosshairService.running) {
            btnXhair.setText("⏹ إخفاء الكروس هير (ظاهر ✅)");
        } else {
            btnXhair.setText("🎯 كروس هير للتصويب");
        }
        if (btnXhairStyle != null) {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            int style = sp.getInt("xhair_style", 0) % CrosshairService.STYLE_NAMES.length;
            int color = sp.getInt("xhair_color", 0) % CrosshairService.COLORS.length;
            btnXhairStyle.setText("🎨 " + CrosshairService.STYLE_NAMES[style] + " — " + CrosshairService.COLOR_NAMES[color]);
        }
    }

    // ---------- Play session report (v7.0) ----------
    private void updateSessionCard() {
        if (tvSession == null) return;
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int sessions = sp.getInt("session_count", 0);
        if (sessions == 0) {
            tvSession.setText("لسه مفيش جلسات مسجلة — فعّل الطيار الآلي 🤖 والعب، وهتلاقي تقرير جلستك هنا");
            return;
        }
        long lastMin = sp.getLong("last_session_min", 0);
        float maxTemp = sp.getFloat("last_session_max_temp", 0);
        long totalMin = sp.getLong("total_play_min", 0);
        String tempTxt = maxTemp > 0
                ? String.format(Locale.US, " | أقصى حرارة: %.1f°م %s", maxTemp, maxTemp >= 42 ? "🔥" : "✅") : "";
        tvSession.setText(String.format(Locale.US,
                "📈 آخر جلسة: %d دقيقة%s\n🎮 إجمالي اللعب: %d دقيقة عبر %d جلسة",
                lastMin, tempTxt, totalMin, sessions));
    }

    // ---------- Battery optimization exemption (v7.0) ----------
    /** Asks once so Android doesn't kill Game Mode / HUD / Auto-Pilot mid-match. */
    private void requestBatteryExemption() {
        try {
            if (android.os.Build.VERSION.SDK_INT < 23) return;
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                if (!sp.getBoolean("asked_batt_exempt", false)) {
                    sp.edit().putBoolean("asked_batt_exempt", true).apply();
                    Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                    Toast.makeText(this, "🔋 اسمح للتطبيق يفضل شغّال — عشان وضع الألعاب ما يتقفلش وأنت في نص الماتش!", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception ignored) {}
    }

    // ---------- Ranked readiness check (v6.0) ----------
    /**
     * One-tap full readiness scan before entering Ranked:
     * RAM pressure + battery level + temperature + ping + jitter,
     * scored out of 100 with a clear GO / WAIT verdict.
     */
    private void doReadinessCheck() {
        btnReadiness.setEnabled(false);
        btnReadiness.setText("🏁 جاري الفحص الشامل…");
        pingCard.setVisibility(View.VISIBLE);
        tvPingResult.setText("🏁 فحص جاهزية الرانكد شغّال… (رام + بطارية + حرارة + بينج + جيتر)");

        executor.execute(() -> {
            int score = 100;
            StringBuilder sb = new StringBuilder();

            // 1) RAM pressure
            int ram = ramUsedPct();
            if (ram >= 90) { score -= 30; sb.append("🧠 الرام: ").append(ram).append("% 🔴 مخنوقة — اعمل تسريع ذكي الأول!\n"); }
            else if (ram >= 75) { score -= 15; sb.append("🧠 الرام: ").append(ram).append("% 🟡 مرتفعة — يفضل تسريع سريع\n"); }
            else sb.append("🧠 الرام: ").append(ram).append("% 🟢 ممتازة\n");

            // 2) Battery level + temperature
            try {
                Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batt != null) {
                    int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    float temp = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                    if (level >= 0 && level < 20) { score -= 20; sb.append("🔋 البطارية: ").append(level).append("% 🔴 ضعيفة — الهاتف هيخنق الأداء!\n"); }
                    else if (level >= 0 && level < 35) { score -= 8; sb.append("🔋 البطارية: ").append(level).append("% 🟡 اشحن شوية الأول لو ماتش طويل\n"); }
                    else if (level >= 0) sb.append("🔋 البطارية: ").append(level).append("% 🟢 تمام\n");
                    if (temp >= 42) { score -= 25; sb.append(String.format(Locale.US, "🌡 الحرارة: %.1f°م 🔴 ساخن — برّد الهاتف 5 دقايق!\n", temp)); }
                    else if (temp >= 38) { score -= 10; sb.append(String.format(Locale.US, "🌡 الحرارة: %.1f°م 🟡 دافي — شيل الجراب لو موجود\n", temp)); }
                    else sb.append(String.format(Locale.US, "🌡 الحرارة: %.1f°م 🟢 باردة\n", temp));
                }
            } catch (Exception ignored) {}

            // 3) Ping + jitter against FF server (5 samples)
            long[] samples = latencySamples("mpsg.freefiremobile.com", 5);
            if (samples == null) samples = latencySamples("ff.garena.com", 5);
            if (samples != null) {
                long min = Long.MAX_VALUE, max = 0, sum = 0;
                for (long s : samples) { min = Math.min(min, s); max = Math.max(max, s); sum += s; }
                long avg = sum / samples.length;
                long jitter = max - min;
                if (avg >= 150) { score -= 30; sb.append("📡 البينج: ").append(avg).append("ms 🔴 مرتفع جداً — متدخلش رانكد!\n"); }
                else if (avg >= 90) { score -= 15; sb.append("📡 البينج: ").append(avg).append("ms 🟡 مقبول — تجنب الكلوز فايت\n"); }
                else sb.append("📡 البينج: ").append(avg).append("ms 🟢 ممتاز\n");
                if (jitter >= 60) { score -= 15; sb.append("📉 الجيتر (التذبذب): ").append(jitter).append("ms 🔴 شبكة غير مستقرة — قرّب من الراوتر\n"); }
                else if (jitter >= 30) { score -= 7; sb.append("📉 الجيتر: ").append(jitter).append("ms 🟡 تذبذب متوسط\n"); }
                else sb.append("📉 الجيتر: ").append(jitter).append("ms 🟢 مستقر\n");
            } else {
                score -= 40;
                sb.append("📡 الشبكة: ❌ مفيش اتصال بسيرفرات فري فاير!\n");
            }

            score = Math.max(0, score);
            String verdict;
            if (score >= 85) verdict = "\n🏆 النتيجة: " + score + "/100 — ✅ ادخل رانكد دلوقتي! الجهاز والشبكة في أفضل حالة";
            else if (score >= 65) verdict = "\n⚖ النتيجة: " + score + "/100 — 🟡 تقدر تلعب بس عالج النقاط الصفرا فوق الأول";
            else verdict = "\n🛑 النتيجة: " + score + "/100 — 🔴 متدخلش رانكد دلوقتي! عالج المشاكل الحمرا وافحص تاني";

            final String result = "🏁 تقرير جاهزية الرانكد:\n\n" + sb + verdict;
            ui.post(() -> {
                tvPingResult.setText(result);
                btnReadiness.setEnabled(true);
                btnReadiness.setText("🏁 فحص جاهزية الرانكد");
            });
        });
    }

    /** N TCP-connect latency samples for jitter measurement; null if host unreachable. */
    private long[] latencySamples(String host, int n) {
        long[] out = new long[n];
        int ok = 0;
        for (int i = 0; i < n; i++) {
            try {
                Socket s = new Socket();
                long t0 = System.nanoTime();
                s.connect(new InetSocketAddress(host, 443), 3000);
                out[ok++] = (System.nanoTime() - t0) / 1_000_000;
                s.close();
            } catch (Exception ignored) {}
        }
        if (ok == 0) return null;
        if (ok < n) {
            long[] trimmed = new long[ok];
            System.arraycopy(out, 0, trimmed, 0, ok);
            return trimmed;
        }
        return out;
    }

    // ---------- Ping test 2.0 (v6.0: avg + jitter + stability) ----------
    private void doPingTest() {
        pingCard.setVisibility(View.VISIBLE);
        tvPingResult.setText(getString(R.string.ping_testing));
        btnPing.setEnabled(false);

        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            long bestMs = Long.MAX_VALUE;
            long ffJitter = -1;
            for (String[] srv : SERVERS) {
                long[] samples = latencySamples(srv[1], 4);
                String verdict;
                if (samples == null) {
                    verdict = "❌ غير متاح";
                } else {
                    long min = Long.MAX_VALUE, max = 0, sum = 0;
                    for (long s : samples) { min = Math.min(min, s); max = Math.max(max, s); sum += s; }
                    long avg = sum / samples.length;
                    long jitter = max - min;
                    if (ffJitter < 0 && srv[1].contains("freefire")) ffJitter = jitter;
                    String stability = jitter < 20 ? "↯ مستقر" : (jitter < 60 ? "↯ متوسط" : "↯ متذبذب!");
                    if (avg < 60) verdict = "🟢 ممتاز (" + avg + "ms ±" + jitter + ") " + stability;
                    else if (avg < 120) verdict = "🟡 جيد (" + avg + "ms ±" + jitter + ") " + stability;
                    else verdict = "🔴 مرتفع (" + avg + "ms ±" + jitter + ") " + stability;
                    if (avg < bestMs) bestMs = avg;
                }
                sb.append(srv[0]).append(": ").append(verdict).append("\n");
            }
            if (bestMs != Long.MAX_VALUE) {
                String overall = bestMs < 60 ? "🎮 اتصالك ممتاز للرانكد!"
                        : (bestMs < 120 ? "👍 اتصالك مقبول — تجنب الكلوز فايت البعيد"
                        : "⚠️ بينج مرتفع — لا تدخل رانكد الآن!");
                sb.append("\n").append(overall).append("\n");
                if (ffJitter >= 60) sb.append("⚠️ الجيتر عالي على سيرفر فري فاير — الرصاص هيتأخر حتى لو البينج شكله كويس!\n");
            }
            sb.append("💡 ± = الجيتر (التذبذب) — لو عالي: قرّب من الراوتر، جرّب خدعة وضع الطيران، أو استخدم بيانات 4G");
            final String result = sb.toString().trim();
            ui.post(() -> {
                tvPingResult.setText(result);
                btnPing.setEnabled(true);
            });
        });
    }

}
