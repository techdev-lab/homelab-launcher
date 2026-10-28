package com.gathin.tvhome;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * The two wallpaper sets inside the APK (assets/wallpapers/homelab and assets/wallpapers/serene). They are copied
 * once per app version into the app's private files, so the rotation code keeps working on plain files and the
 * pictures are not decoded from a compressed stream every few minutes. A failed copy leaves the plain background.
 */
final class BuiltInWallpapers {
    private static final String TAG = "TvHome.Walls";
    private static final String STAMP = ".version";

    private BuiltInWallpapers() {
    }

    static List<File> list(Context ctx, String set) {
        if (!Wallpapers.builtIn(set)) return Collections.emptyList();
        File dir = new File(new File(ctx.getFilesDir(), "builtin-wallpapers"), set);
        File stamp = new File(dir, STAMP);
        try {
            if (!BuildConfig.VERSION_NAME.equals(read(stamp))) copy(ctx.getAssets(), "wallpapers/" + set, dir, stamp);
        } catch (Exception e) {
            Log.w(TAG, set + ": " + e);
            return Collections.emptyList();
        }
        return Wallpapers.list(dir);
    }

    private static void copy(AssetManager assets, String from, File dir, File stamp) throws Exception {
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("cannot create " + dir);
        String[] names = assets.list(from);
        for (String name : names == null ? new String[0] : names) {
            if (!Wallpapers.accepted(name, 1)) continue;
            try (InputStream in = assets.open(from + "/" + name);
                 OutputStream out = new FileOutputStream(new File(dir, name))) {
                byte[] buf = new byte[16384];
                for (int r; (r = in.read(buf)) > 0; ) out.write(buf, 0, r);
            }
        }
        try (OutputStream out = new FileOutputStream(stamp)) {
            out.write(BuildConfig.VERSION_NAME.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static String read(File f) {
        if (!f.isFile() || f.length() > 64) return "";
        try (InputStream in = new java.io.FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = in.read(b);
            return n > 0 ? new String(b, 0, n, java.nio.charset.StandardCharsets.UTF_8) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
