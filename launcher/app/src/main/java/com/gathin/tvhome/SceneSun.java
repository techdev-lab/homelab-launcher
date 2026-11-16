package com.gathin.tvhome;

import java.util.List;

/**
 * "Follow the sun": which scene fits the time of day (docs/BATCH_S_SCENES.md). With a valid place it uses today's prayer
 * times (fajr, sunrise, asr, maghrib from {@link Prayer#times}); without one it uses the clock. No permission, no
 * network. Pure Java, unit tested on the JVM.
 */
final class SceneSun {
    private static final double DAWN_AFTER_SUNRISE = 0.75;     // hours: 45 minutes
    private static final double GOLDEN_AFTER_MAGHRIB = 0.25;   // hours: 15 minutes

    private SceneSun() {
    }

    /**
     * The scene to show at {@code nowHours} (local hours, 0 to 24). {@code prayerTimes} is the six-value array of
     * {@link Prayer#times} or null when there is no place; a short or NaN array counts as no place. Windows are half open,
     * so at an exact edge the later window wins. The result is always in {@code enabled} (Night when that is empty).
     */
    static String pick(double[] prayerTimes, double nowHours, List<String> enabled) {
        String want = usable(prayerTimes) ? byPrayer(prayerTimes, nowHours) : byClock(nowHours);
        if (SceneList.WATER.equals(want) && !SceneList.on(SceneList.enabled(enabled), SceneList.WATER)) {
            want = SceneList.FOG;
        }
        return SceneList.nearestEnabled(enabled, want);
    }

    /** As {@link #pick(double[], double, List)} for a place: no valid place gives the clock rule. */
    static String pick(double lat, double lng, int year, int month, int day, double tzHours, double nowHours,
                       List<String> enabled) {
        double[] times = null;
        if (Interests.validPlace(lat, lng)) times = Prayer.times(year, month, day, lat, lng, tzHours, false);
        return pick(times, nowHours, enabled);
    }

    /** "Follow the sun" on or off: the TV's choice, else config.json "scene_follow_sun", else off. A null means "not set". */
    static boolean follow(Boolean tvChoice, Boolean themeValue) {
        if (tvChoice != null) return tvChoice;
        return themeValue != null && themeValue;
    }

    private static boolean usable(double[] t) {
        if (t == null || t.length < 6) return false;
        for (int i = 0; i < 6; i++) if (Double.isNaN(t[i]) || Double.isInfinite(t[i])) return false;
        return true;
    }

    private static String byPrayer(double[] t, double now) {
        double fajr = t[0], sunrise = t[1], asr = t[3], maghrib = t[4];
        if (now >= fajr && now < sunrise + DAWN_AFTER_SUNRISE) return SceneList.DAWN;
        if (now >= sunrise + DAWN_AFTER_SUNRISE && now < asr) return SceneList.WATER;
        if (now >= asr && now < maghrib + GOLDEN_AFTER_MAGHRIB) return SceneList.GOLDEN;
        return SceneList.NIGHT;
    }

    /** No place: 05 to 08 dawn, 08 to 16 day, 16 to 19 golden, else night. */
    static String byClock(double now) {
        if (now >= 5 && now < 8) return SceneList.DAWN;
        if (now >= 8 && now < 16) return SceneList.WATER;
        if (now >= 16 && now < 19) return SceneList.GOLDEN;
        return SceneList.NIGHT;
    }
}
