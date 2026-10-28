package com.gathin.tvhome;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Rotating wallpapers: JPEG or PNG files in {@code wallpapers/} beside config.json, shown in name order, one every
 * {@code wallpaper_interval_min} minutes (MainActivity). Pure file and index rules, unit-tested.
 */
final class Wallpapers {
    static final String DIR_NAME = "wallpapers";
    /** More would only cost storage on a TV; a theme ships a handful. */
    static final int MAX_FILES = 30;
    /** A file over this is skipped rather than decoded (12 MP JPEGs are still well under it). */
    static final long MAX_BYTES = 8L * 1024 * 1024;

    /** wallpaper_mode values. HOMELAB and SERENE are the two sets built into the APK (assets/wallpapers/). */
    static final String PLAIN = "plain", FIXED = "fixed", ROTATE = "rotate", HOMELAB = "homelab", SERENE = "serene";
    static final String[] BUILT_IN = {HOMELAB, SERENE};

    private Wallpapers() {
    }

    static boolean builtIn(String mode) {
        return HOMELAB.equals(mode) || SERENE.equals(mode);
    }

    static boolean known(String mode) {
        return PLAIN.equals(mode) || FIXED.equals(mode) || ROTATE.equals(mode) || builtIn(mode);
    }

    /**
     * The wallpaper mode in force: a choice made on the TV first, then config.json's wallpaper_mode. With no
     * config.json and no wallpaper.jpg (a fresh install from the release APK) the Serene set shows, so the screen
     * is never a bare colour out of the box. Unknown values fall back to what the TV had before this key existed.
     */
    static String mode(String chosenOnTv, String fromConfig, boolean hasConfig, boolean hasFixedPicture) {
        if (known(chosenOnTv)) return chosenOnTv;
        if (known(fromConfig)) return fromConfig;
        if (!hasConfig && !hasFixedPicture) return SERENE;
        return FIXED;
    }

    static List<File> list(File dir) {
        List<File> out = new ArrayList<>();
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return out;
        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
        for (File f : files) {
            if (out.size() >= MAX_FILES) break;
            if (f.isFile() && accepted(f.getName(), f.length())) out.add(f);
        }
        return out;
    }

    static boolean accepted(String name, long bytes) {
        String n = name.toLowerCase(Locale.ROOT);
        boolean picture = n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
        return picture && !n.startsWith(".") && bytes > 0 && bytes <= MAX_BYTES;
    }

    /**
     * Which picture shows at {@code minuteOfEpoch}: every TV with the same set shows the same one at the same time,
     * and Home coming back mid-interval keeps the current picture instead of restarting the cycle.
     */
    static int indexAt(long minuteOfEpoch, int intervalMinutes, int count) {
        if (count <= 0) return -1;
        long slot = minuteOfEpoch / Math.max(1, intervalMinutes);
        return (int) (slot % count);
    }
}
