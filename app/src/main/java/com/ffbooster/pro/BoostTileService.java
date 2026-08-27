package com.ffbooster.pro;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile (v8.0) — one-tap RAM boost from the notification
 * shade, from ANY app, without even opening FF Booster.
 *
 * Requires API 24+ (our minSdk) — Tile label shows the result briefly.
 */
public class BoostTileService extends TileService {

    private volatile boolean busy = false;

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t != null) {
            t.setState(Tile.STATE_INACTIVE);
            t.setLabel("⚡ تسريع FF");
            t.updateTile();
        }
    }

    @Override
    public void onClick() {
        if (busy) return;
        busy = true;

        Tile t = getQsTile();
        if (t != null) {
            t.setState(Tile.STATE_ACTIVE);
            t.setLabel("جاري التسريع…");
            t.updateTile();
        }

        new Thread(() -> {
            long before = availRamMb();
            int killed = 0;
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    if (app.packageName.equals(getPackageName())) continue;
                    if (app.packageName.startsWith("com.dts.")) continue; // never Free Fire
                    try { am.killBackgroundProcesses(app.packageName); killed++; } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            System.gc();
            try { Thread.sleep(700); } catch (InterruptedException ignored) {}
            long freed = Math.max(0, availRamMb() - before);

            final String result = "✅ +" + freed + "MB (" + killed + ")";
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setLabel(result);
                tile.updateTile();
            }
            // Restore the label after 4 seconds
            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
            Tile tile2 = getQsTile();
            if (tile2 != null) {
                tile2.setState(Tile.STATE_INACTIVE);
                tile2.setLabel("⚡ تسريع FF");
                tile2.updateTile();
            }
            busy = false;
        }).start();
    }

    private long availRamMb() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }
}
