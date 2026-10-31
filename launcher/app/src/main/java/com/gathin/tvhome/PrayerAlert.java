package com.gathin.tvhome;

/**
 * The visual prayer-time alert (docs/RAMADAN_PRAYER.md): for a few minutes after a prayer time the prayer card says
 * it is time for that prayer. No sound and no permission. The strip is rebuilt every minute, so the alert shows within
 * a minute of the time and ends by itself. Pure Java, unit tested on the JVM.
 */
final class PrayerAlert {
    static final int DEFAULT_MINUTES = 10, MAX_MINUTES = 30;

    private PrayerAlert() {
    }

    static boolean valid(int minutes) {
        return minutes >= 0 && minutes <= MAX_MINUTES;
    }

    /** The TV's choice, else "prayer_alert_minutes", else the default; 0 is a real value (off). Out of range counts as unset. */
    static int pick(Integer chosen, Integer theme) {
        if (chosen != null && valid(chosen)) return chosen;
        return theme != null && valid(theme) ? theme : DEFAULT_MINUTES;
    }

    /**
     * Index (into the six times) of the prayer whose alert window covers {@code nowHours}, or -1. Sunrise is not a
     * prayer, so only the five can alert; when windows overlap, the later prayer wins.
     */
    static int active(double[] times, double nowHours, int minutes) {
        if (times == null || times.length < 6 || minutes <= 0) return -1;
        int found = -1;
        for (int i : Prayer.FIVE) {
            if (nowHours >= times[i] && nowHours < times[i] + minutes / 60.0) found = i;
        }
        return found;
    }
}
