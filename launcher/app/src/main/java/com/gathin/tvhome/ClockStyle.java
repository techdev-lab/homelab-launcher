package com.gathin.tvhome;

/**
 * Look of the clock in the header, picked in Launcher settings (docs/LOOK_OPTIONS.md). "plain" is the 2.9 header and
 * stays the default. "glass" puts the time on a small translucent capsule in the colour of the focused app. The capsule
 * only adds space at the sides, never above or below, so the header keeps its height and the dock its place. Pure
 * Java, unit tested on the JVM.
 */
final class ClockStyle {
    static final String PLAIN = "plain", GLASS = "glass";
    static final String[] ALL = {PLAIN, GLASS};

    private static final float GLASS_PAD_DP = 18f, GLASS_INSET_DP = 4f;

    private ClockStyle() {
    }

    /** The TV's choice, else config.json "clock_style", else plain; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : PLAIN;
    }

    static boolean valid(String s) {
        return PLAIN.equals(s) || GLASS.equals(s);
    }

    /** True when the time sits on a capsule. */
    static boolean capsule(String style) {
        return GLASS.equals(style);
    }

    /** Space at each side of the time, inside the capsule. */
    static float padDp(String style) {
        return capsule(style) ? GLASS_PAD_DP : 0f;
    }

    /**
     * How far the capsule is pulled in from the top and bottom of the clock view. The view is taller than the digits,
     * and the full height put the capsule right under the greeting line (seen on the TV).
     */
    static float insetDp(String style) {
        return capsule(style) ? GLASS_INSET_DP : 0f;
    }
}
