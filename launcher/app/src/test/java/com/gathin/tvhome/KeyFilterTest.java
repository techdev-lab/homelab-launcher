package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyFilterTest {
    /** AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS, spelled out so the test does not lean on the code it checks. */
    private static final int FILTER_BIT = 0x20;

    @Test
    public void theTvChoiceWinsThenTheThemeThenOn() {
        assertEquals(KeyFilter.OFF, KeyFilter.pick("off", "on"));
        assertEquals(KeyFilter.ON, KeyFilter.pick("on", "off"));
        assertEquals(KeyFilter.OFF, KeyFilter.pick("", "off"));
        assertEquals(KeyFilter.OFF, KeyFilter.pick(null, "off"));
        assertEquals(KeyFilter.ON, KeyFilter.pick(null, null));
        assertEquals(KeyFilter.ON, KeyFilter.pick("", ""));
    }

    @Test
    public void configSaysOffButTheTvSaysOnGivesOn() {
        assertEquals(KeyFilter.ON, KeyFilter.pick("on", "off"));
    }

    @Test
    public void anUnknownValueCountsAsUnset() {
        assertEquals("unknown TV choice falls to the theme", KeyFilter.OFF, KeyFilter.pick("maybe", "off"));
        assertEquals("unknown theme falls to on", KeyFilter.ON, KeyFilter.pick(null, "maybe"));
        assertEquals("both unknown", KeyFilter.ON, KeyFilter.pick("maybe", "maybe"));
        assertEquals("case matters, like every other option", KeyFilter.ON, KeyFilter.pick("OFF", null));
        assertEquals("a boolean is not a value", KeyFilter.ON, KeyFilter.pick("false", "false"));
        assertFalse(KeyFilter.valid(null));
        assertFalse(KeyFilter.valid(""));
        assertFalse(KeyFilter.valid(" off"));
        assertTrue(KeyFilter.valid("on"));
        assertTrue(KeyFilter.valid("off"));
    }

    @Test
    public void onlyOffTurnsTheFilterOff() {
        assertTrue(KeyFilter.filtering(KeyFilter.ON));
        assertFalse(KeyFilter.filtering(KeyFilter.OFF));
        assertTrue("an unknown value is the default, which filters", KeyFilter.filtering("maybe"));
        assertTrue(KeyFilter.filtering(null));
    }

    @Test
    public void onSetsOnlyTheFilterBit() {
        assertEquals(FILTER_BIT, KeyFilter.flags(KeyFilter.ON, 0));
        assertEquals(FILTER_BIT | 0x2, KeyFilter.flags(KeyFilter.ON, 0x2));
        assertEquals("already set stays set", FILTER_BIT | 0x2, KeyFilter.flags(KeyFilter.ON, FILTER_BIT | 0x2));
    }

    @Test
    public void offClearsOnlyTheFilterBitAndKeepsTheRest() {
        assertEquals(0, KeyFilter.flags(KeyFilter.OFF, FILTER_BIT));
        assertEquals(0x2 | 0x40, KeyFilter.flags(KeyFilter.OFF, FILTER_BIT | 0x2 | 0x40));
        assertEquals("already clear stays clear", 0x2, KeyFilter.flags(KeyFilter.OFF, 0x2));
    }

    @Test
    public void anUnknownChoiceKeepsTheFilterOn() {
        assertEquals(FILTER_BIT, KeyFilter.flags("maybe", 0));
        assertEquals(FILTER_BIT, KeyFilter.flags(null, 0));
    }

    @Test
    public void everyChoiceInAllIsValidAndOnComesFirst() {
        assertEquals(2, KeyFilter.ALL.length);
        assertEquals(KeyFilter.ON, KeyFilter.ALL[0]);
        for (String s : KeyFilter.ALL) assertTrue(KeyFilter.valid(s));
    }
}
