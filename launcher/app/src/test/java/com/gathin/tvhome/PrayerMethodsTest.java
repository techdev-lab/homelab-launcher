package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.Test;

/**
 * Every calculation method against api.aladhan.com (same method number, its default offsets, ANGLE_BASED), fetched
 * on 2026-10-28 for two dates (autumn and mid-June) and one city per method: each time within 2 minutes.
 */
public class PrayerMethodsTest {
    private static final String[] NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};

    /** date, city, lat, lng, aladhan method, school (0 standard, 1 Hanafi), zone, six times. */
    // Casablanca rows use UTC: aladhan's time zone data put Africa/Casablanca at UTC+0 on both dates (Morocco keeps
    // UTC+1 outside Ramadan), so its clock times are an hour behind; the TV uses its own zone, which is right.
    private static final String[][] ALADHAN = {
            {"2026-10-28", "Riyadh", "24.7136", "46.6753", "4", "0", "Asia/Riyadh", "04:39 05:58 11:37 14:52 17:16 18:46"},
            {"2027-06-15", "Riyadh", "24.7136", "46.6753", "4", "0", "Asia/Riyadh", "03:32 05:04 11:54 15:15 18:44 20:14"},
            {"2026-10-28", "Cairo", "30.0444", "31.2357", "5", "0", "Africa/Cairo", "05:38 07:06 12:39 15:48 18:11 19:30"},
            {"2026-10-28", "Dubai", "25.2048", "55.2708", "16", "0", "Asia/Dubai", "05:06 06:24 12:06 15:17 17:44 18:59"},
            {"2026-10-28", "Dubai", "25.2048", "55.2708", "8", "0", "Asia/Dubai", "05:00 06:24 12:03 15:17 17:41 19:11"},
            {"2026-10-28", "Doha", "25.2854", "51.531", "10", "0", "Asia/Qatar", "04:22 05:39 11:18 14:32 16:56 18:26"},
            {"2026-10-28", "Casablanca", "33.5731", "-7.5898", "21", "0", "UTC", "05:17 06:46 12:19 15:19 17:47 19:01"},
            {"2026-10-28", "Kuwait", "29.3759", "47.9774", "9", "0", "Asia/Kuwait", "04:38 05:58 11:32 14:42 17:05 18:23"},
            {"2026-10-28", "Amman", "31.9539", "35.9106", "23", "0", "Asia/Amman", "05:27 06:49 12:20 15:27 17:55 19:13"},
            {"2026-10-28", "Tunis", "36.8065", "10.1815", "18", "0", "Africa/Tunis", "05:12 06:39 12:03 15:03 17:27 18:54"},
            {"2026-10-28", "Algiers", "36.7538", "3.0588", "19", "0", "Africa/Algiers", "05:40 07:07 12:32 15:32 17:55 19:17"},
            {"2026-10-28", "Karachi", "24.8607", "67.0011", "1", "1", "Asia/Karachi", "05:20 06:37 12:16 16:18 17:55 19:12"},
            {"2026-10-28", "Istanbul", "41.0082", "28.9784", "13", "0", "Europe/Istanbul", "05:57 07:23 12:53 15:45 18:12 19:32"},
            {"2026-10-28", "Riyadh", "24.7136", "46.6753", "3", "0", "Asia/Riyadh", "04:41 05:58 11:37 14:52 17:16 18:28"},
            {"2026-10-28", "Riyadh", "24.7136", "46.6753", "2", "0", "Asia/Riyadh", "04:54 05:58 11:37 14:52 17:16 18:20"},
            {"2027-06-15", "Cairo", "30.0444", "31.2357", "5", "0", "Africa/Cairo", "04:08 05:53 12:56 16:31 19:58 21:31"},
            {"2027-06-15", "Dubai", "25.2048", "55.2708", "16", "0", "Asia/Dubai", "03:58 05:28 12:22 15:42 19:13 20:41"},
            {"2027-06-15", "Dubai", "25.2048", "55.2708", "8", "0", "Asia/Dubai", "03:51 05:28 12:19 15:42 19:10 20:40"},
            {"2027-06-15", "Doha", "25.2854", "51.531", "10", "0", "Asia/Qatar", "03:14 04:43 11:34 14:57 18:25 19:55"},
            {"2027-06-15", "Casablanca", "33.5731", "-7.5898", "21", "0", "UTC", "03:29 05:20 12:36 16:15 19:47 21:19"},
            {"2027-06-15", "Kuwait", "29.3759", "47.9774", "9", "0", "Asia/Kuwait", "03:13 04:48 11:49 15:22 18:49 20:21"},
            {"2027-06-15", "Amman", "31.9539", "35.9106", "23", "0", "Asia/Amman", "03:50 05:30 12:37 16:17 19:49 21:24"},
            {"2027-06-15", "Tunis", "36.8065", "10.1815", "18", "0", "Africa/Tunis", "03:08 05:00 12:20 16:11 19:40 21:32"},
            {"2027-06-15", "Algiers", "36.7538", "3.0588", "19", "0", "Africa/Algiers", "03:37 05:28 12:48 16:40 20:08 21:52"},
            {"2027-06-15", "Karachi", "24.8607", "67.0011", "1", "1", "Asia/Karachi", "04:13 05:42 12:32 17:15 19:23 20:52"},
            {"2027-06-15", "Istanbul", "41.0082", "28.9784", "13", "0", "Europe/Istanbul", "03:24 05:24 13:10 17:09 20:45 22:36"},
            {"2027-06-15", "Riyadh", "24.7136", "46.6753", "3", "0", "Asia/Riyadh", "03:35 05:04 11:54 15:15 18:44 20:07"},
            {"2027-06-15", "Riyadh", "24.7136", "46.6753", "2", "0", "Asia/Riyadh", "03:51 05:04 11:54 15:15 18:44 19:56"},
    };

    @Test
    public void everyMethodMatchesAladhanWithinTwoMinutes() {
        int checked = 0;
        for (String[] r : ALADHAN) {
            Prayer.Method m = null;
            for (Prayer.Method x : Prayer.METHODS) if (x.aladhanId == Integer.parseInt(r[4])) m = x;
            assertNotNull("no method for aladhan id " + r[4], m);
            LocalDate d = LocalDate.parse(r[0]);
            double tz = ZoneId.of(r[6]).getRules().getOffset(d.atTime(12, 0).atZone(ZoneId.of(r[6])).toInstant())
                    .getTotalSeconds() / 3600.0;
            int asr = "1".equals(r[5]) ? Prayer.ASR_HANAFI : Prayer.ASR_STANDARD;
            double[] got = Prayer.times(d.getYear(), d.getMonthValue(), d.getDayOfMonth(), Double.parseDouble(r[2]),
                    Double.parseDouble(r[3]), tz, false, m, asr, null);
            String[] want = r[7].split(" ");
            for (int i = 0; i < 6; i++) {
                LocalTime w = LocalTime.parse(want[i]);
                double diff = got[i] * 60 - (w.getHour() * 60 + w.getMinute());
                assertTrue(r[0] + " " + r[1] + " " + m.key + " " + NAMES[i] + ": want " + want[i] + ", got "
                        + Prayer.clock(got[i], false), Math.abs(diff) <= 2.0);
                checked++;
            }
        }
        assertEquals(28 * 6, checked);
    }

    @Test
    public void everyOfferedMethodIsChecked() {
        for (Prayer.Method m : Prayer.METHODS) {
            boolean found = false;
            for (String[] r : ALADHAN) found |= Integer.parseInt(r[4]) == m.aladhanId;
            assertTrue(m.key + " has no reference row", found);
        }
    }

    @Test
    public void defaultIsUmmAlQuraUnchanged() {
        double[] old = Prayer.times(2026, 10, 28, 25.3833, 49.5869, 3, false);
        double[] uaq = Prayer.times(2026, 10, 28, 25.3833, 49.5869, 3, false, Prayer.UMM_AL_QURA,
                Prayer.ASR_STANDARD, new int[6]);
        for (int i = 0; i < 6; i++) assertEquals(old[i], uaq[i], 1e-9);
        assertEquals(Prayer.UMM_AL_QURA, Prayer.METHODS[0]);
    }

    @Test
    public void hanafiAsrIsLaterAndOnlyAsrMoves() {
        double[] std = Prayer.times(2026, 10, 28, 24.86, 67.0, 5, false, Prayer.method("karachi"), Prayer.ASR_STANDARD, null);
        double[] han = Prayer.times(2026, 10, 28, 24.86, 67.0, 5, false, Prayer.method("karachi"), Prayer.ASR_HANAFI, null);
        assertTrue(han[Prayer.ASR] - std[Prayer.ASR] > 0.5);
        for (int i : new int[]{Prayer.FAJR, Prayer.DHUHR, Prayer.MAGHRIB, Prayer.ISHA}) assertEquals(std[i], han[i], 1e-9);
    }

    @Test
    public void manualAdjustmentIsBoundedPerTime() {
        double[] base = Prayer.times(2026, 10, 28, 24.71, 46.68, 3, false);
        double[] adj = Prayer.times(2026, 10, 28, 24.71, 46.68, 3, false, Prayer.UMM_AL_QURA, Prayer.ASR_STANDARD,
                new int[]{2, 0, 0, 0, -3, 90});
        assertEquals(2, Math.round((adj[Prayer.FAJR] - base[Prayer.FAJR]) * 60));
        assertEquals(-3, Math.round((adj[Prayer.MAGHRIB] - base[Prayer.MAGHRIB]) * 60));
        assertEquals(Prayer.MAX_ADJUST_MINUTES, Math.round((adj[Prayer.ISHA] - base[Prayer.ISHA]) * 60));
        // Isha by minutes follows Maghrib: a Maghrib adjustment is not added twice.
        assertEquals(base[Prayer.ASR], adj[Prayer.ASR], 1e-9);
    }

    @Test
    public void ramadanOnlyChangesUmmAlQuraIsha() {
        double[] q = Prayer.times(2026, 3, 1, 25.29, 51.53, 3, false, Prayer.method("qatar"), 1, null);
        double[] qr = Prayer.times(2026, 3, 1, 25.29, 51.53, 3, true, Prayer.method("qatar"), 1, null);
        assertEquals(q[Prayer.ISHA], qr[Prayer.ISHA], 1e-9);
        double[] u = Prayer.times(2026, 3, 1, 25.29, 51.53, 3, true);
        assertEquals(120, Math.round((u[Prayer.ISHA] - u[Prayer.MAGHRIB]) * 60));
    }

    @Test
    public void tomorrowsFajrAcrossAClockChange() {
        // Egypt leaves summer time on the night of 2026-10-29: +3 today, +2 tomorrow. Fajr at 4:40 tomorrow is
        // 5:40 on tonight's clock, so from 22:00 the countdown is 7:40, not 6:40.
        assertEquals("7:40", Prayer.countdown(22.0, Prayer.onTodaysClock(4 + 40 / 60.0, 3, 2) + 24));
        assertEquals(4.5, Prayer.onTodaysClock(4.5, 3, 3), 1e-9);
    }

    @Test
    public void unknownMethodIsNullNotAQuietFallback() {
        assertNull(Prayer.method("egyptian"));
        assertNull(Prayer.method(""));
        assertNotNull(Prayer.method("egypt"));
        assertEquals(Prayer.ASR_HANAFI, Prayer.asrFactor("hanafi"));
        assertEquals(Prayer.ASR_STANDARD, Prayer.asrFactor("shafii"));
    }
}
