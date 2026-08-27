package com.ffbooster.pro;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RAM Hogs analyzer (v9.0) — shows WHICH apps are choking the phone.
 *
 * A generic "boost" kills everything blindly; this screen names the actual
 * culprits so the player can force-stop the worst ones for good:
 *   • Ranks user apps by foreground usage in the last 24h (UsageStats) —
 *     heavily-used apps are exactly the ones that keep background services.
 *   • Flags notorious RAM-hungry apps (social/video) with a special badge.
 *   • Per-app actions: instant background kill + open App-Info to Force Stop
 *     (force stop is the ONLY user-space way to truly stop an app's services).
 *
 * No root, no special permissions beyond Usage Access (shared with Auto-Pilot).
 */
public class RamHogsActivity extends Activity {

    /** Apps famous for eating RAM/battery in the background on 4GB phones. */
    private static final String[] HEAVY_HITTERS = {
            "com.facebook.katana", "com.facebook.orca", "com.facebook.lite",
            "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
            "com.snapchat.android", "com.whatsapp", "org.telegram.messenger",
            "com.google.android.youtube", "video.like", "sg.bigo.live",
            "com.twitter.android", "com.spotify.music", "com.king.candycrushsaga"
    };

    private LinearLayout container;
    private float d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gfx);

        TextView title = findViewById(R.id.tvGfxTitle);
        title.setText("🐷 محلل التطبيقات الخانقة");
        TextView desc = findViewById(R.id.tvGfxDesc);
        desc.setText("مين اللي واكل الرام؟ القايمة دي بترتب تطبيقاتك حسب استخدامها آخر 24 ساعة — " +
                "الأكثر استخداماً هي اللي بتفضل شغالة في الخلفية وبتخنق فري فاير. " +
                "\"إيقاف إجباري\" هو الطريقة الوحيدة اللي بتوقف التطبيق فعلاً (مش هيرجع لوحده).");

        container = findViewById(R.id.gfxContainer);
        d = getResources().getDisplayMetrics().density;

        Button back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        buildList();
    }

    private void buildList() {
        container.removeAllViews();

        if (!AutoPilotService.hasUsageAccess(this)) {
            TextView tv = new TextView(this);
            tv.setTextColor(Color.parseColor("#FFB74D"));
            tv.setTextSize(14);
            tv.setLineSpacing(5 * d, 1f);
            tv.setText("🔐 المحلل محتاج إذن \"الوصول للاستخدام\" (نفس إذن الطيار الآلي) عشان يعرف مين أكتر تطبيقات استخدمتها.\n\nاضغط الزر تحت وفعّل FF Booster Pro ثم ارجع.");
            container.addView(tv);

            Button grant = new Button(this);
            grant.setText("⚙️ فتح إعدادات الوصول للاستخدام");
            grant.setBackgroundResource(R.drawable.btn_boost);
            grant.setTextColor(Color.WHITE);
            grant.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
            lp.topMargin = (int) (12 * d);
            container.addView(grant, lp);
            grant.setOnClickListener(v -> {
                try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
                catch (Exception e) { Toast.makeText(this, "افتح الإعدادات ← وصول خاص ← الوصول للاستخدام", Toast.LENGTH_LONG).show(); }
            });
            return;
        }

        // ---- Gather 24h foreground usage for user apps ----
        List<long[]> ranked = new ArrayList<>(); // {index into names, fgMillis}
        final List<String> pkgs = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(now - 24L * 3600_000, now);
            PackageManager pm = getPackageManager();

            for (Map.Entry<String, UsageStats> e : stats.entrySet()) {
                String pkg = e.getKey();
                long fg = e.getValue().getTotalTimeInForeground();
                if (fg < 60_000) continue;                       // ignore <1 min
                if (pkg.equals(getPackageName())) continue;      // ourselves
                if (pkg.startsWith("com.dts.")) continue;        // Free Fire itself
                ApplicationInfo ai;
                try { ai = pm.getApplicationInfo(pkg, 0); } catch (Exception ex) { continue; }
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // user apps only
                pkgs.add(pkg);
                labels.add(String.valueOf(pm.getApplicationLabel(ai)));
                ranked.add(new long[]{pkgs.size() - 1, fg});
            }
        } catch (Exception ignored) {}

        Collections.sort(ranked, (a, b) -> Long.compare(b[1], a[1]));

        if (ranked.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setTextColor(Color.parseColor("#94A3B8"));
            tv.setTextSize(14);
            tv.setText("✅ مفيش تطبيقات مستخدمة بكثافة آخر 24 ساعة — الرام المفروض مرتاحة!\n(أو نظام الجهاز لسه ما جمعش إحصائيات — جرّب بعد شوية)");
            container.addView(tv);
            return;
        }

        // ---- Summary header ----
        TextView summary = new TextView(this);
        summary.setTextColor(Color.parseColor("#00E5FF"));
        summary.setTextSize(13);
        summary.setTypeface(null, Typeface.BOLD);
        int shown = Math.min(ranked.size(), 12);
        summary.setText("🏆 أعلى " + shown + " تطبيقات استخداماً آخر 24 ساعة (الأعلى = الأخطر على الرام):");
        container.addView(summary);

        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (int i = 0; i < shown; i++) {
            long[] row = ranked.get(i);
            final String pkg = pkgs.get((int) row[0]);
            final String label = labels.get((int) row[0]);
            long fgMin = row[1] / 60_000;

            boolean heavy = false;
            for (String h : HEAVY_HITTERS) if (h.equals(pkg)) { heavy = true; break; }

            // Card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            int p = (int) (12 * d);
            card.setPadding(p, p, p, p);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.topMargin = (int) (8 * d);
            container.addView(card, clp);

            TextView tvName = new TextView(this);
            tvName.setTextColor(Color.parseColor("#F1F5F9"));
            tvName.setTextSize(14);
            tvName.setTypeface(null, Typeface.BOLD);
            String rankIcon = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "▪️";
            tvName.setText(rankIcon + " " + label + (heavy ? "  🐷 معروف بأكل الرام!" : ""));
            card.addView(tvName);

            TextView tvInfo = new TextView(this);
            tvInfo.setTextColor(Color.parseColor("#94A3B8"));
            tvInfo.setTextSize(12);
            tvInfo.setText(String.format(Locale.US, "استخدام آخر 24س: %d دقيقة", fgMin));
            card.addView(tvInfo);

            // Action row
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (40 * d));
            alp.topMargin = (int) (8 * d);
            card.addView(actions, alp);

            Button btnKill = new Button(this);
            btnKill.setText("⚡ تنظيف من الخلفية");
            styleSmall(btnKill, "#00E676");
            LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            klp.setMarginEnd((int) (6 * d));
            actions.addView(btnKill, klp);
            btnKill.setOnClickListener(v -> {
                try {
                    am.killBackgroundProcesses(pkg);
                    Toast.makeText(this, "⚡ اتنظف " + label + " من الخلفية", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "تعذر التنظيف", Toast.LENGTH_SHORT).show();
                }
            });

            Button btnForce = new Button(this);
            btnForce.setText("🛑 إيقاف إجباري");
            styleSmall(btnForce, "#FF5252");
            actions.addView(btnForce, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            btnForce.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + pkg)));
                    Toast.makeText(this, "اضغط \"إيقاف إجباري\" (Force stop) في الصفحة دي", Toast.LENGTH_LONG).show();
                } catch (Exception ignored) {}
            });
        }

        // Footer tip
        TextView tip = new TextView(this);
        tip.setTextColor(Color.parseColor("#94A3B8"));
        tip.setTextSize(12);
        tip.setLineSpacing(4 * d, 1f);
        tip.setText("\n💡 الفرق:\n" +
                "⚡ تنظيف من الخلفية = سريع لكن التطبيق ممكن يرجع يشتغل لوحده\n" +
                "🛑 إيقاف إجباري = التطبيق مش هيشتغل تاني غير لما تفتحه بنفسك — " +
                "اعمله للتطبيقات اللي عليها 🐷 قبل الرانكد وهتحس بفرق حقيقي");
        container.addView(tip);
    }

    private void styleSmall(Button b, String colorHex) {
        b.setBackgroundResource(R.drawable.btn_secondary);
        b.setTextColor(Color.parseColor(colorHex));
        b.setTextSize(12);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(0, 0, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildList(); // refresh after returning from settings / force stop
    }
}
