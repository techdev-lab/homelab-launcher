package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HijriShiftTest {

    @Test
    public void onlyWholeDaysFromMinusTwoToPlusTwoAreValid() {
        for (int d = -2; d <= 2; d++) assertTrue("day " + d, HijriShift.valid(d));
        assertFalse(HijriShift.valid(-3));
        assertFalse(HijriShift.valid(3));
        assertFalse(HijriShift.valid(Integer.MAX_VALUE));
        assertFalse(HijriShift.valid(Integer.MIN_VALUE));
    }

    @Test
    public void theTvChoiceWinsThenTheThemeThenZero() {
        assertEquals(1, HijriShift.pick(1, -1));
        assertEquals(-2, HijriShift.pick(-2, 2));
        assertEquals("a TV choice of zero is a choice", 0, HijriShift.pick(0, 2));
        assertEquals(2, HijriShift.pick(null, 2));
        assertEquals(0, HijriShift.pick(null, null));
    }

    @Test
    public void aValueOutsideTheRangeIsIgnoredNotClamped() {
        assertEquals("bad TV choice falls to the theme", -1, HijriShift.pick(9, -1));
        assertEquals("bad theme falls to zero", 0, HijriShift.pick(null, 5));
        assertEquals(0, HijriShift.pick(7, -7));
    }
}
