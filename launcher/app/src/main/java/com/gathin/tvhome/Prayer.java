package com.gathin.tvhome;

/**
 * The five prayer times by the Umm al-Qura method (Fajr at 18.5 degrees, Isha 90 minutes after Maghrib, 120 in
 * Ramadan, Asr by shadow factor 1), computed on the device from a fixed place in config.json: no network and no
 * location permission. The sun's position follows the praytimes.org formulas, refined twice at each time.
 * Checked in PrayerTest against an independent implementation (api.aladhan.com, method 4) to within 2 minutes.
 */
final class Prayer {
    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    /** The five prayers, in order, as indexes into {@link #times}. */
    static final int[] FIVE = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};

    /** Display names are R.array.prayer_names (res/values and values-ar), in the order of {@link #times}. */

    private static final double FAJR_ANGLE = 18.5;
    private static final double RISE_SET_ANGLE = 0.833;

    private Prayer() {
    }

    /**
     * Local clock times in hours (e.g. 17.35) for {FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA}.
     *
     * @param tzHours the UTC offset that applies on that date, e.g. 3 for Asia/Riyadh
     */
    static double[] times(int year, int month, int day, double lat, double lng, double tzHours, boolean ramadan) {
        double jd = julian(year, month, day) - lng / (15 * 24.0);
        double[] t = {5, 6, 12, 13, 18};
        for (int i = 0; i < 2; i++) {
            t = new double[]{
                    sunAngleTime(jd, t[0], FAJR_ANGLE, lat, true),
                    sunAngleTime(jd, t[1], RISE_SET_ANGLE, lat, true),
                    midDay(jd, t[2]),
                    asrTime(jd, t[3], lat),
                    sunAngleTime(jd, t[4], RISE_SET_ANGLE, lat, false)};
        }
        double shift = tzHours - lng / 15;
        double maghrib = t[4] + shift;
        return new double[]{t[0] + shift, t[1] + shift, t[2] + shift, t[3] + shift, maghrib,
                maghrib + (ramadan ? 120 : 90) / 60.0};
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

    private static double asrTime(double jd, double t, double lat) {
        double decl = sunPosition(jd + t / 24)[0];
        double angle = -darccot(1 + dtan(Math.abs(lat - decl)));
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
