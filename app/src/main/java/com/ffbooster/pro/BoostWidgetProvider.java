package com.ffbooster.pro;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.widget.RemoteViews;

import java.util.Locale;

/**
 * Home-screen 1x1 boost widget (v10.0).
 *
 * One tap on the widget = instant background RAM clean, right from the home
 * screen — no need to open the app at all. The widget shows live free RAM
 * and flashes the result after each boost.
 *
 * Long-press home screen → widgets → "⚡ FF Booster" to add it.
 */
public class BoostWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_BOOST = "com.ffbooster.pro.WIDGET_BOOST";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, buildViews(ctx, null));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_BOOST.equals(intent.getAction())) {
            // Show "boosting" immediately, then do the clean in the background
            pushToAll(ctx, buildViews(ctx, "⚡ جاري…"));
            final long before = availRamMb(ctx);
            new Thread(() -> {
                int killed = 0;
                try {
                    ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                    for (ApplicationInfo app : ctx.getPackageManager().getInstalledApplications(0)) {
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                        if (app.packageName.equals(ctx.getPackageName())) continue;
                        if (app.packageName.startsWith("com.dts.")) continue; // never touch Free Fire
                        try { am.killBackgroundProcesses(app.packageName); killed++; } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                long freed = Math.max(0, availRamMb(ctx) - before);
                pushToAll(ctx, buildViews(ctx,
                        String.format(Locale.US, "✅ +%dMB", freed)));
                // Revert to the idle label after 4 seconds
                try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                pushToAll(ctx, buildViews(ctx, null));
            }).start();
        }
    }

    private static void pushToAll(Context ctx, RemoteViews views) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, BoostWidgetProvider.class));
            for (int id : ids) mgr.updateAppWidget(id, views);
        } catch (Exception ignored) {}
    }

    private static RemoteViews buildViews(Context ctx, String statusOverride) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_boost);
        String label = statusOverride != null
                ? statusOverride
                : String.format(Locale.US, "⚡ %dMB", availRamMb(ctx));
        rv.setTextViewText(R.id.tvWidget, label);

        Intent boost = new Intent(ctx, BoostWidgetProvider.class).setAction(ACTION_BOOST);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 10, boost,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.tvWidget, pi);
        return rv;
    }

    private static long availRamMb(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.availMem / (1024 * 1024);
        } catch (Exception e) {
            return 0;
        }
    }
}
