package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClockStyleTest {

    @Test
    public void tvChoiceWinsThenThemeThenPlain() {
        assertEquals("glass", ClockStyle.pick("glass", "plain"));
        assertEquals("glass", ClockStyle.pick("", "glass"));
        assertEquals("plain", ClockStyle.pick("", ""));
        assertEquals("plain", ClockStyle.pick(null, null));
        // An unknown value counts as unset: the failure case that keeps today's look.
        assertEquals("plain", ClockStyle.pick("frosted", "big"));
    }

    @Test
    public void everyListedValueIsValid() {
        for (String s : ClockStyle.ALL) assertTrue(s, ClockStyle.valid(s));
        assertFalse(ClockStyle.valid("frosted"));
        assertEquals("plain", ClockStyle.ALL[0]);
    }

    @Test
    public void plainHasNoCapsuleAndNoPadding() {
        assertFalse(ClockStyle.capsule("plain"));
        assertEquals(0f, ClockStyle.padDp("plain"), 0f);
        // Anything unknown draws like plain, so a bad theme value never changes the header.
        assertFalse(ClockStyle.capsule("weird"));
        assertEquals(0f, ClockStyle.padDp(null), 0f);
    }

    @Test
    public void glassHasACapsuleWithSidePaddingOnly() {
        assertTrue(ClockStyle.capsule("glass"));
        assertEquals(18f, ClockStyle.padDp("glass"), 0f);
    }
}
