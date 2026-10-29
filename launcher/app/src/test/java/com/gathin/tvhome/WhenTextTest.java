package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

/** The kick-off line of a match card: short, isolated clock and date, one digit style (2026-10-28 bug on the TV). */
public class WhenTextTest {
    /** Thursday 2026-10-29 14:17 UTC = 17:17 in Riyadh. */
    private static final long KICK_OFF = 1_793_283_420_000L;
    private static final TimeZone RIYADH = TimeZone.getTimeZone("Asia/Riyadh");
    private static final String LRI = "\u2066", PDI = "\u2069";

    @Test
    public void englishIsShortWithIsolatedDateAndClock() {
        assertEquals("Thu " + LRI + "29 Oct" + PDI + " \u00B7 " + LRI + "17:17" + PDI,
                Interests.whenText(KICK_OFF, Locale.ENGLISH, RIYADH, true));
        assertEquals("Thu " + LRI + "29 Oct" + PDI + " \u00B7 " + LRI + "5:17" + PDI + " PM",
                Interests.whenText(KICK_OFF, Locale.ENGLISH, RIYADH, false));
    }

    @Test
    public void arabicKeepsTheClockInsideAnIsolateAndTheMarkerOutside() {
        Locale ar = new Locale("ar");
        for (boolean h24 : new boolean[]{true, false}) {
            String s = Interests.whenText(KICK_OFF, ar, RIYADH, h24);
            assertEquals(s, 2, count(s, LRI));
            assertEquals(s, 2, count(s, PDI));
            assertFalse("starts with the day name, not a number", s.startsWith(LRI));
            // The day name in Arabic, then day/month: never the month's name, which made the line too long.
            assertTrue(s, s.length() <= 26);
            int clockStart = s.lastIndexOf(LRI), clockEnd = s.lastIndexOf(PDI);
            String clock = s.substring(clockStart + 1, clockEnd);
            assertTrue("hour and minutes in one isolate: " + clock, clock.length() >= 4 && clock.length() <= 5);
            if (h24) assertTrue(s.endsWith(PDI));
            else assertFalse("AM/PM word outside the isolate", s.endsWith(PDI));
            assertOneDigitStyle(s);
        }
    }

    private static int count(String s, String part) {
        int n = 0;
        for (int i = s.indexOf(part); i >= 0; i = s.indexOf(part, i + 1)) n++;
        return n;
    }

    /** Western digits or Arabic-Indic digits, never both on one line. */
    private static void assertOneDigitStyle(String s) {
        boolean latin = false, arabic = false;
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') latin = true;
            if (c >= '\u0660' && c <= '\u0669') arabic = true;
        }
        assertFalse("mixed digits in " + s, latin && arabic);
    }
}
