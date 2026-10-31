package com.gathin.tvhome;

/**
 * Days to add to the Hijri date, picked in Launcher settings or as "hijri_offset_days" (docs/RAMADAN_PRAYER.md).
 * The date comes from the Umm al-Qura calendar; a country that follows moon sighting can be a day away, and that
 * moves the start of Ramadan too. Pure Java, unit tested on the JVM; the calendar call is in {@link Hijri#today(int)}.
 */
final class HijriShift {
    static final int MIN = -2, MAX = 2;

    private HijriShift() {
    }

    static boolean valid(int days) {
        return days >= MIN && days <= MAX;
    }

    /** The TV's choice, else the theme's value, else 0; a value outside the range counts as unset, never clamped. */
    static int pick(Integer chosen, Integer theme) {
        if (chosen != null && valid(chosen)) return chosen;
        return theme != null && valid(theme) ? theme : 0;
    }
}
