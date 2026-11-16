package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

public class SceneClockTextTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** The text with each digit moved to Arabic-Indic (U+0660 to U+0669), written without Arabic characters in the source. */
    private static String arabic(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) b.append(c >= (char) 48 && c <= (char) 57 ? (char) (0x0660 + c - 48) : c);
        return b.toString();
    }

    private static long at(int hour, int minute) {
        Calendar c = Calendar.getInstance(UTC);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 16, hour, minute, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void aTwentyFourHourClockPadsTheHour() {
        assertEquals("00:05", SceneClockText.time(at(0, 5), UTC, true, "latin"));
        assertEquals("13:45", SceneClockText.time(at(13, 45), UTC, true, "latin"));
        assertEquals("", SceneClockText.suffix(at(13, 45), UTC, true));
    }

    @Test
    public void aTwelveHourClockShowsTwelveAtMidnightAndNoon() {
        assertEquals("12:05", SceneClockText.time(at(0, 5), UTC, false, "latin"));
        assertEquals("AM", SceneClockText.suffix(at(0, 5), UTC, false));
        assertEquals("12:00", SceneClockText.time(at(12, 0), UTC, false, "latin"));
        assertEquals("PM", SceneClockText.suffix(at(12, 0), UTC, false));
        assertEquals("1:45", SceneClockText.time(at(13, 45), UTC, false, "latin"));
        assertEquals("11:59", SceneClockText.time(at(23, 59), UTC, false, "latin"));
    }

    @Test
    public void arabicDigitsMapEveryDigitAndKeepTheColon() {
        assertEquals(arabic("12:05"), SceneClockText.time(at(12, 5), UTC, true, "arabic"));
        assertEquals(arabic("01:23"), SceneClockText.time(at(1, 23), UTC, true, "arabic"));
        assertEquals(arabic("09:48"), SceneClockText.time(at(9, 48), UTC, true, "arabic"));
    }

    @Test
    public void anUnknownDigitStyleActsAsLatinAndANullZoneDoesNotThrow() {
        assertEquals("13:45", SceneClockText.time(at(13, 45), UTC, true, "klingon"));
        assertEquals("13:45", SceneClockText.time(at(13, 45), UTC, true, null));
        SceneClockText.time(at(13, 45), null, true, "latin");
        SceneClockText.suffix(at(13, 45), null, false);
    }

    @Test
    public void theZoneShiftsTheHour() {
        TimeZone riyadh = TimeZone.getTimeZone("Asia/Riyadh");
        assertEquals("15:00", SceneClockText.time(at(12, 0), riyadh, true, "latin"));
    }
}
