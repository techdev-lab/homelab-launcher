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
 * The wallpaper sets inside the APK (assets/wallpapers/homelab, serene and kids). They are copied
 * once per app version into the app's private files, so the rotation code keeps working on plain files and the
 * pictures are not decoded from a compressed stream every few minutes. A failed copy leaves the plain background.
 */
final class BuiltInWallpapers {
    private static final String TAG = "TvHome.Walls";
    private static final String STAMP = ".version";
    /** Changes when the format of the bundled pictures changes, so one clean copy happens even with the same versionName. */
    static final String FORMAT = "webp1";

    private BuiltInWallpapers() {
    }

    static List<File> list(Context ctx, String set) {
        if (!Wallpapers.builtIn(set)) return Collections.emptyList();
        File dir = new File(new File(ctx.getFilesDir(), "builtin-wallpapers"), set);
        File stamp = new File(dir, STAMP);
        try {
            if (!stampValue().equals(read(stamp))) copy(ctx.getAssets(), "wallpapers/" + set, dir, stamp);
        } catch (Exception e) {
            Log.w(TAG, set + ": " + e);
            return Collections.emptyList();
        }
        return Wallpapers.list(dir);
    }

    private static void copy(AssetManager assets, String from, File dir, File stamp) throws Exception {
        clearSet(dir);
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
            out.write(stampValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /** What the stamp file holds: the app version and the picture format. */
    static String stampValue() {
        return BuildConfig.VERSION_NAME + "+" + FORMAT;
    }

    /**
     * Removes the files of one built-in set (the stamp included) before a fresh copy, so pictures of an older version or
     * format never stay beside the new ones. Only plain files inside {@code setDir} go; the folder itself, other folders
     * and a user's own pictures (which live in files/wallpapers, never here) are not touched.
     */
    static void clearSet(File setDir) {
        File[] old = setDir == null ? null : setDir.listFiles();
        if (old == null) return;
        for (File f : old) if (f.isFile()) f.delete();
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
