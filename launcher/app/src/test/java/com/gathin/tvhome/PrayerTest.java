package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Reference values from an independent implementation: api.aladhan.com/v1/timings, method 4 (Umm Al-Qura),
 * timezone Asia/Riyadh (UTC+3, no DST), fetched 2026-10-21. Order: Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha.
 */
public class PrayerTest {
    private static final double RIYADH_LAT = 24.7136, RIYADH_LNG = 46.6753;
    private static final double MAKKAH_LAT = 21.4225, MAKKAH_LNG = 39.8262;
    private static final int TOLERANCE_MIN = 2;

    private static final String[] NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};

    private static void check(String[] expected, double[] got) {
        for (int i = 0; i < 6; i++) {
            String[] hm = expected[i].split(":");
            int want = Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);
            int have = (int) Math.round(got[i] * 60);
            assertTrue(NAMES[i] + ": want " + expected[i] + ", got " + Prayer.clock(got[i], false),
                    Math.abs(want - have) <= TOLERANCE_MIN);
        }
    }

    @Test
    public void riyadhAutumn() {
        check(new String[]{"04:36", "05:54", "11:38", "14:56", "17:21", "18:51"},
                Prayer.times(2026, 10, 21, RIYADH_LAT, RIYADH_LNG, 3, false));
    }

    @Test
    public void riyadhSummer() {
        check(new String[]{"03:33", "05:05", "11:55", "15:16", "18:45", "20:15"},
                Prayer.times(2026, 6, 21, RIYADH_LAT, RIYADH_LNG, 3, false));
    }

    @Test
    public void riyadhWinter() {
        check(new String[]{"05:09", "06:33", "11:51", "14:50", "17:09", "18:39"},
                Prayer.times(2026, 12, 21, RIYADH_LAT, RIYADH_LNG, 3, false));
    }

    @Test
    public void makkahAutumn() {
        check(new String[]{"05:02", "06:19", "12:05", "15:25", "17:52", "19:22"},
                Prayer.times(2026, 10, 21, MAKKAH_LAT, MAKKAH_LNG, 3, false));
    }

    /** Al-Ahsa (Al Hofuf, 25.3697 N 49.5865 E, OpenStreetMap), the reference place for these tests. */
    private static final double AHSA_LAT = 25.3697, AHSA_LNG = 49.5865;

    @Test
    public void alAhsaAutumn() {
        check(new String[]{"04:24", "05:43", "11:26", "14:44", "17:09", "18:39"},
                Prayer.times(2026, 10, 21, AHSA_LAT, AHSA_LNG, 3, false));
    }

    @Test
    public void alAhsaSummer() {
        check(new String[]{"03:19", "04:52", "11:43", "15:06", "18:35", "20:05"},
                Prayer.times(2026, 6, 21, AHSA_LAT, AHSA_LNG, 3, false));
    }

    @Test
    public void alAhsaWinter() {
        check(new String[]{"04:59", "06:23", "11:40", "14:37", "16:56", "18:26"},
                Prayer.times(2026, 12, 21, AHSA_LAT, AHSA_LNG, 3, false));
    }

    @Test
    public void ishaIsTwoHoursAfterMaghribInRamadan() {
        double[] t = Prayer.times(2026, 10, 21, RIYADH_LAT, RIYADH_LNG, 3, true);
        assertEquals(120, Math.round((t[Prayer.ISHA] - t[Prayer.MAGHRIB]) * 60));
    }

    @Test
    public void nextSkipsSunriseAndEndsAfterIsha() {
        double[] t = Prayer.times(2026, 10, 21, RIYADH_LAT, RIYADH_LNG, 3, false);
        assertEquals(Prayer.FAJR, Prayer.next(t, 1.0));
        assertEquals(Prayer.DHUHR, Prayer.next(t, 6.5));   // after sunrise, before Dhuhr
        assertEquals(Prayer.MAGHRIB, Prayer.next(t, 16.6));
        assertEquals(-1, Prayer.next(t, 23.0));
    }

    @Test
    public void clockAndCountdown() {
        assertEquals("17:21", Prayer.clock(17.35, false));
        assertEquals("5:21", Prayer.clock(17.35, true));
        assertEquals("12:05", Prayer.clock(12.0833, true));
        assertEquals("0:47", Prayer.countdown(16.567, 17.35));
        assertEquals("0:00", Prayer.countdown(18, 17));
    }
}
