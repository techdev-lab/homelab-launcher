package com.gathin.tvhome;

/**
 * How dark the veil over a wallpaper picture is, in percent black (docs/LAYOUT_OPTIONS.md). 2.9 drew a fixed veil of
 * 35 percent so the clock and the cards keep their contrast on a bright picture; that stays the default. Picked in
 * Launcher settings or as "wallpaper_dim" in config.json. Pure Java, unit tested on the JVM.
 */
final class WallpaperDim {
    static final int DEFAULT = 35, MAX = 80;
    /** The chips in Launcher settings: light, normal (the default), deep and night. */
    static final int[] CHOICES = {15, 35, 55, 75};

    private WallpaperDim() {
    }

    static boolean valid(int percent) {
        return percent >= 0 && percent <= MAX;
    }

    /** The TV's choice, else config.json "wallpaper_dim", else 35; a value outside 0 to 80 counts as unset, never clamped. */
    static int pick(Integer chosen, Integer theme) {
        if (chosen != null && valid(chosen)) return chosen;
        return theme != null && valid(theme) ? theme : DEFAULT;
    }

    /** The veil as an ARGB colour: black with this much alpha. A bad percent gives the default veil. */
    static int argb(int percent) {
        int p = valid(percent) ? percent : DEFAULT;
        return Math.round(p * 255f / 100f) << 24;
    }
}
