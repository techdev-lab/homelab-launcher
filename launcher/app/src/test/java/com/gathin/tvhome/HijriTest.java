package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Formatting only: ICU's Umm al-Qura arithmetic runs on the device (2026-10-21 = 1448-05-10, a Wednesday). */
public class HijriTest {
    private static final int[] OCT_21_2026 = {1448, 5, 10, 4};
    private static final String[] MONTHS = {
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Akhir", "Jumada al-Ula", "Jumada al-Akhirah",
            "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"};
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Test
    public void arabicOrderAndDigits() {
        // The Arabic names come from res/values-ar; digit-free placeholders check the order and the digits.
        String[] m = {"Ma", "Mb", "Mc", "Md", "Me", "Mf", "Mg", "Mh", "Mi", "Mj", "Mk", "Ml"};
        String[] d = {"Da", "Db", "Dc", "Dd", "De", "Df", "Dg"};
        assertEquals("Dd \u0661\u0660 Me \u0661\u0664\u0664\u0668", Hijri.format(OCT_21_2026, m, d, true));
    }

    @Test
    public void english() {
        assertEquals("Wed, 10 Jumada al-Ula 1448 AH", Hijri.format(OCT_21_2026, MONTHS, DAYS, false));
    }

    @Test
    public void badFieldsGiveEmptyText() {
        assertEquals("", Hijri.format(new int[]{1448, 13, 1, 1}, MONTHS, DAYS, true));
        assertEquals("", Hijri.format(new int[]{1448, 1, 1, 0}, MONTHS, DAYS, false));
    }

    @Test
    public void wrongNameListsGiveEmptyText() {
        assertEquals("", Hijri.format(OCT_21_2026, new String[11], DAYS, false));
        assertEquals("", Hijri.format(OCT_21_2026, MONTHS, null, true));
    }

    @Test
    public void ramadan() {
        assertTrue(Hijri.isRamadan(new int[]{1448, 9, 1, 1}));
        assertFalse(Hijri.isRamadan(OCT_21_2026));
    }

    @Test
    public void digits() {
        assertEquals("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669 ab", Hijri.arabicDigits("0123456789 ab"));
    }
}
