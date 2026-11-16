package com.gathin.tvhome;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * The text of the big clock of the Scenes look (docs/BATCH_S_SCENES.md). It follows the TV's 12 or 24 hour setting like
 * the header clock ("HH:mm" or "h:mm"), is built by hand so no locale changes the digits, and then takes the digit
 * style of {@link DigitStyle}. Pure Java, unit tested on the JVM.
 */
final class SceneClockText {
    private SceneClockText() {
    }

    /** "13:05" on a 24 hour clock, "1:05" on a 12 hour one (12:05 after midnight and at noon), digits in the style. */
    static String time(long millis, TimeZone zone, boolean is24, String digitStyle) {
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.setTimeInMillis(millis);
        int hour = c.get(Calendar.HOUR_OF_DAY), minute = c.get(Calendar.MINUTE);
        if (!is24) hour = hour % 12 == 0 ? 12 : hour % 12;
        String text = (is24 && hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
        return DigitStyle.apply(text, digitStyle);
    }

    /** "AM" or "PM" for a 12 hour clock, empty for a 24 hour one. */
    static String suffix(long millis, TimeZone zone, boolean is24) {
        if (is24) return "";
        Calendar c = Calendar.getInstance(zone == null ? TimeZone.getDefault() : zone);
        c.setTimeInMillis(millis);
        return c.get(Calendar.HOUR_OF_DAY) < 12 ? "AM" : "PM";
    }
}
