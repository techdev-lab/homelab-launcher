package com.gathin.tvhome;

/**
 * The five prayer times, computed on the device from a fixed place in config.json: no network and no location
 * permission. The method (Umm al-Qura by default: Fajr at 18.5 degrees, Isha 90 minutes after Maghrib, 120 in
 * Ramadan) and the Asr convention can be changed; see {@link Method}. The sun's position follows the praytimes.org
 * formulas, refined twice at each time. PrayerTest checks every method against an independent implementation
 * (api.aladhan.com, same method number and offsets) to within 2 minutes. A method gives the times of that method,
 * not a promise that they match a country's printed calendar.
 */
final class Prayer {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    /** The five prayers, in order, as indexes into {@link #times}. */
    static final int[] FIVE = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};

    /** Display names are R.array.prayer_names (res/values and values-ar), in the order of {@link #times}. */

    private static final double RISE_SET_ANGLE = 0.833;
    /** Asr when an object's shadow is its length plus its noon shadow (standard), or twice its length (Hanafi). */
    static final int ASR_STANDARD = 1, ASR_HANAFI = 2;
    /** A manual adjustment is at most this many minutes either way ("prayer_adjust"). */
    static final int MAX_ADJUST_MINUTES = 30;
    private static final int[] NO_OFFSETS = new int[6];

    /**
     * A calculation method as api.aladhan.com defines it: the Fajr angle, Isha as an angle or as minutes after
     * Maghrib, Maghrib as sunset or minutes after it, and the fixed minute offsets the method adds per time.
     */
    static final class Method {
        final String key;
        /** The aladhan.com method number, for checking a time against theirs. */
        final int aladhanId;
        final double fajrAngle;
        /** Isha by angle when greater than 0, else {@link #ishaMinutes} after Maghrib. */
        final double ishaAngle;
        final int ishaMinutes;
        /** Isha minutes in Ramadan (Umm al-Qura: 120); equal to ishaMinutes for every other method. */
        final int ramadanIshaMinutes;
        /** Maghrib this many minutes after sunset (Jordan: 5). */
        final int maghribMinutes;
        /** Minutes added to {FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA} after the computation. */
        final int[] offsets;

        Method(String key, int aladhanId, double fajrAngle, double ishaAngle, int ishaMinutes, int ramadanIshaMinutes,
               int maghribMinutes, int[] offsets) {
            this.key = key;
            this.aladhanId = aladhanId;
            this.fajrAngle = fajrAngle;
            this.ishaAngle = ishaAngle;
            this.ishaMinutes = ishaMinutes;
            this.ramadanIshaMinutes = ramadanIshaMinutes;
            this.maghribMinutes = maghribMinutes;
            this.offsets = offsets;
        }
    }

    private static Method angles(String key, int id, double fajr, double isha) {
        return new Method(key, id, fajr, isha, 0, 0, 0, NO_OFFSETS);
    }

    static final Method UMM_AL_QURA = new Method("umm_al_qura", 4, 18.5, 0, 90, 120, 0, NO_OFFSETS);

    /** Every method the launcher offers, in the order Launcher settings lists them (R.array.prayer_method_names). */
    static final Method[] METHODS = {
            UMM_AL_QURA,
            angles("egypt", 5, 19.5, 17.5),
            new Method("gulf", 8, 19.5, 0, 90, 90, 0, NO_OFFSETS),
            new Method("dubai", 16, 18.2, 18.2, 0, 0, 0, new int[]{0, 0, 3, 0, 3, 0}),
            new Method("qatar", 10, 18, 0, 90, 90, 0, NO_OFFSETS),
            angles("kuwait", 9, 18, 17.5),
            new Method("jordan", 23, 18, 18, 0, 0, 5, NO_OFFSETS),
            new Method("morocco", 21, 19, 17, 0, 0, 0, new int[]{0, 0, 5, 0, 5, 0}),
            angles("algeria", 19, 18, 17),
            angles("tunisia", 18, 18, 18),
            new Method("turkey", 13, 18, 17, 0, 0, 0, new int[]{0, -7, 5, 4, 7, 0}),
            angles("mwl", 3, 18, 17),
            angles("isna", 2, 15, 15),
            angles("karachi", 1, 18, 18),
    };

    /** The method with this config.json key, or null when the key names none (the caller says so on screen). */
    static Method method(String key) {
        for (Method m : METHODS) if (m.key.equals(key)) return m;
        return null;
    }

    /** {@link #ASR_HANAFI} for "hanafi", else {@link #ASR_STANDARD}. */
    static int asrFactor(String key) {
        return "hanafi".equals(key) ? ASR_HANAFI : ASR_STANDARD;
    }

    private Prayer() {
    }

    /**
     * Local clock times in hours (e.g. 17.35) for {FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA}, by Umm al-Qura.
     *
     * @param tzHours the UTC offset that applies on that date, e.g. 3 for Asia/Riyadh
     */
    static double[] times(int year, int month, int day, double lat, double lng, double tzHours, boolean ramadan) {
        return times(year, month, day, lat, lng, tzHours, ramadan, UMM_AL_QURA, ASR_STANDARD, NO_OFFSETS);
    }

    /**
     * As above with a method, an Asr factor ({@link #ASR_STANDARD} or {@link #ASR_HANAFI}) and the viewer's own
     * minute adjustments per time (each clamped to {@link #MAX_ADJUST_MINUTES}; null or short means none).
     */
    static double[] times(int year, int month, int day, double lat, double lng, double tzHours, boolean ramadan,
                          Method method, int asrFactor, int[] adjust) {
        double jd = julian(year, month, day) - lng / (15 * 24.0);
        boolean ishaByAngle = method.ishaAngle > 0;
        double[] t = {5, 6, 12, 13, 18, 18};
        for (int i = 0; i < 2; i++) {
            t = new double[]{
                    sunAngleTime(jd, t[0], method.fajrAngle, lat, true),
                    sunAngleTime(jd, t[1], RISE_SET_ANGLE, lat, true),
                    midDay(jd, t[2]),
                    asrTime(jd, t[3], lat, asrFactor),
                    sunAngleTime(jd, t[4], RISE_SET_ANGLE, lat, false),
                    ishaByAngle ? sunAngleTime(jd, t[5], method.ishaAngle, lat, false) : 0};
        }
        double shift = tzHours - lng / 15;
        double sunset = t[4] + shift;
        double maghrib = sunset + method.maghribMinutes / 60.0;
        double isha = ishaByAngle ? t[5] + shift
                : maghrib + (ramadan ? method.ramadanIshaMinutes : method.ishaMinutes) / 60.0;
        double[] out = {t[0] + shift, t[1] + shift, t[2] + shift, t[3] + shift, maghrib, isha};
        for (int i = 0; i < out.length; i++) {
            int own = adjust != null && i < adjust.length
                    ? Math.max(-MAX_ADJUST_MINUTES, Math.min(MAX_ADJUST_MINUTES, adjust[i])) : 0;
            out[i] += (method.offsets[i] + own) / 60.0;
        }
        return out;
    }

    /** Index (into {@link #times}) of the next of the five prayers after {@code nowHours}, or -1 after Isha. */
    static int next(double[] times, double nowHours) {
        for (int i : FIVE) if (times[i] > nowHours) return i;
        return -1;
    }

    /** "17:21" style, rounded to the nearest minute; 12-hour without am/pm when {@code twelveHour}. */
    static String clock(double hours, boolean twelveHour) {
        int total = (int) Math.round(hours * 60);
        total = ((total % 1440) + 1440) % 1440;
        int h = total / 60, m = total % 60;
        if (twelveHour) h = h % 12 == 0 ? 12 : h % 12;
        return h + ":" + (m < 10 ? "0" : "") + m;
    }

    /**
     * Tomorrow's time (local hours on tomorrow's clock) on today's clock, so {@link #countdown} from tonight is right
     * even when the clock changes at midnight: 4:30 after a one-hour step back is 5:30 on tonight's clock.
     */
    static double onTodaysClock(double tomorrowHours, double tzToday, double tzTomorrow) {
        return tomorrowHours + (tzToday - tzTomorrow);
    }

    /** "0:47" from now to {@code target}, both in hours of the same day. */
    static String countdown(double nowHours, double target) {
        int mins = (int) Math.ceil((target - nowHours) * 60 - 1e-9);
        if (mins < 0) mins = 0;
        return mins / 60 + ":" + (mins % 60 < 10 ? "0" : "") + mins % 60;
    }

    // ---- astronomy: the standard low-precision solar formulas (also used by praytimes.org) -------------

    static double julian(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    /** {declination in degrees, equation of time in hours} at Julian day {@code jd}. */
    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * d;
        double ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15;
        double eqt = q / 15 - fixHour(ra);
        double decl = darcsin(dsin(e) * dsin(l));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd, double t) {
        double eqt = sunPosition(jd + t / 24)[1];
        return fixHour(12 - eqt);
    }

    private static double sunAngleTime(double jd, double t, double angle, double lat, boolean beforeNoon) {
        double decl = sunPosition(jd + t / 24)[0];
        double noon = midDay(jd, t);
        double x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        double span = darccos(Math.max(-1, Math.min(1, x))) / 15;
        return noon + (beforeNoon ? -span : span);
    }

    private static double asrTime(double jd, double t, double lat, int factor) {
        double decl = sunPosition(jd + t / 24)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - decl)));
        return sunAngleTime(jd, t, angle, lat, false);
    }

    private static double dsin(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double dcos(double d) {
        return Math.cos(Math.toRadians(d));
    }

    private static double dtan(double d) {
        return Math.tan(Math.toRadians(d));
    }

    private static double darcsin(double x) {
        return Math.toDegrees(Math.asin(x));
    }

    private static double darccos(double x) {
        return Math.toDegrees(Math.acos(x));
    }

    private static double darctan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double darccot(double x) {
        return Math.toDegrees(Math.atan(1 / x));
    }

    private static double fixAngle(double a) {
        a = a - 360 * Math.floor(a / 360);
        return a < 0 ? a + 360 : a;
    }

    private static double fixHour(double a) {
        a = a - 24 * Math.floor(a / 24);
        return a < 0 ? a + 24 : a;
    }
}
