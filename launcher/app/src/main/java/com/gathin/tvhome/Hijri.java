package com.gathin.tvhome;

import android.icu.util.IslamicCalendar;

/**
 * Hijri date by the Umm al-Qura calendar (Saudi Arabia's civil calendar), from the platform's ICU: no network,
 * no tables of our own. The date turns over at midnight like the Gregorian one, not at sunset. The formatting
 * is plain Java so it is unit-tested; ICU itself is only called on the device.
 */
final class Hijri {

    private Hijri() {
    }

    /** {year, month 1-12, day, dayOfWeek 1-7 (Sunday 1)} for today. */
    static int[] today() {
        IslamicCalendar c = new IslamicCalendar();
        c.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        return new int[]{c.get(IslamicCalendar.EXTENDED_YEAR), c.get(IslamicCalendar.MONTH) + 1,
                c.get(IslamicCalendar.DAY_OF_MONTH), c.get(IslamicCalendar.DAY_OF_WEEK)};
    }

    /** Today in the screen's language, with the month and weekday names from res/values(-ar)/strings.xml. */
    static String format(android.content.Context ctx, int[] ymdw, boolean arabic) {
        android.content.res.Resources r = ctx.getResources();
        return format(ymdw, r.getStringArray(R.array.hijri_months), r.getStringArray(R.array.hijri_days), arabic);
    }

    /**
     * Arabic: weekday, day, month, year in Arabic-Indic digits. Otherwise "Wed, 10 Jumada al-Ula 1448 AH".
     * {@code months} has 12 names (Muharram first), {@code days} 7 (Sunday first, Calendar.DAY_OF_WEEK order).
     */
    static String format(int[] ymdw, String[] months, String[] days, boolean arabic) {
        int y = ymdw[0], m = ymdw[1], d = ymdw[2], w = ymdw[3];
        if (m < 1 || m > 12 || w < 1 || w > 7) return "";
        if (months == null || months.length != 12 || days == null || days.length != 7) return "";
        if (arabic) return arabicDigits(days[w - 1] + " " + d + " " + months[m - 1] + " " + y);
        return days[w - 1] + ", " + d + " " + months[m - 1] + " " + y + " AH";
    }

    static boolean isRamadan(int[] ymdw) {
        return ymdw[1] == 9;
    }

    /** Western digits to Arabic-Indic (U+0660 to U+0669); everything else unchanged. */
    static String arabicDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= '0' && c <= '9' ? (char) ('\u0660' + (c - '0')) : c);
        }
        return b.toString();
    }
}
