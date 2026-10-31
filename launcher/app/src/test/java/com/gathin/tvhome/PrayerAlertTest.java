package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PrayerAlertTest {

    // Fajr 4:30, Sunrise 5:50, Dhuhr 12:00, Asr 15:30, Maghrib 18:00, Isha 19:30 (hours of the day).
    private static final double[] T = {4.5, 5.8333, 12.0, 15.5, 18.0, 19.5};

    @Test
    public void theOptionIsZeroToThirtyMinutesAndTenByDefault() {
        assertEquals(10, PrayerAlert.DEFAULT_MINUTES);
        assertTrue(PrayerAlert.valid(0));
        assertTrue(PrayerAlert.valid(30));
        assertFalse(PrayerAlert.valid(-1));
        assertFalse(PrayerAlert.valid(31));
    }

    @Test
    public void theTvChoiceWinsThenTheThemeThenTheDefault() {
        assertEquals(5, PrayerAlert.pick(5, 15));
        assertEquals("a TV choice of zero turns it off", 0, PrayerAlert.pick(0, 15));
        assertEquals(15, PrayerAlert.pick(null, 15));
        assertEquals(PrayerAlert.DEFAULT_MINUTES, PrayerAlert.pick(null, null));
        assertEquals("bad TV choice falls to the theme", 15, PrayerAlert.pick(99, 15));
        assertEquals("bad theme falls to the default", PrayerAlert.DEFAULT_MINUTES, PrayerAlert.pick(null, -4));
    }

    @Test
    public void thePrayerIsActiveFromItsTimeUntilTheWindowEnds() {
        assertEquals("just before", -1, PrayerAlert.active(T, 11.99, 10));
        assertEquals("at the time", Prayer.DHUHR, PrayerAlert.active(T, 12.0, 10));
        assertEquals("last minute", Prayer.DHUHR, PrayerAlert.active(T, 12.0 + 9.9 / 60, 10));
        assertEquals("window over", -1, PrayerAlert.active(T, 12.0 + 10.0 / 60, 10));
        assertEquals(-1, PrayerAlert.active(T, 13.0, 10));
    }

    @Test
    public void eachOfTheFivePrayersAlertsAndSunriseNever() {
        assertEquals(Prayer.FAJR, PrayerAlert.active(T, 4.52, 10));
        assertEquals(Prayer.ASR, PrayerAlert.active(T, 15.52, 10));
        assertEquals(Prayer.MAGHRIB, PrayerAlert.active(T, 18.05, 10));
        assertEquals(Prayer.ISHA, PrayerAlert.active(T, 19.55, 10));
        assertEquals("sunrise is not a prayer", -1, PrayerAlert.active(T, 5.84, 10));
        assertEquals(-1, PrayerAlert.active(T, 5.9, 30));
    }

    @Test
    public void zeroOrNegativeMinutesNeverAlert() {
        assertEquals(-1, PrayerAlert.active(T, 12.0, 0));
        assertEquals(-1, PrayerAlert.active(T, 12.0, -5));
    }

    @Test
    public void whenWindowsOverlapTheLaterPrayerWins() {
        // Not real times, but the rule must still pick exactly one.
        double[] close = {4.5, 5.8, 12.0, 12.1, 18.0, 19.5};
        assertEquals(Prayer.ASR, PrayerAlert.active(close, 12.12, 30));
    }

    @Test
    public void badInputIsHandled() {
        assertEquals(-1, PrayerAlert.active(null, 12.0, 10));
        assertEquals(-1, PrayerAlert.active(new double[3], 12.0, 10));
        assertEquals(-1, PrayerAlert.active(T, Double.NaN, 10));
    }
}
