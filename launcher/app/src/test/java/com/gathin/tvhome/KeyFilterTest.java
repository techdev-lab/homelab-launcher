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
        assertTrue(KeyFilter.valid("home"));
        assertTrue(KeyFilter.valid("off"));
    }

    @Test
    public void homeIsPickedLikeTheOthers() {
        assertEquals(KeyFilter.HOME, KeyFilter.pick("home", "on"));
        assertEquals(KeyFilter.HOME, KeyFilter.pick("", "home"));
        assertEquals("the TV's choice still wins", KeyFilter.ON, KeyFilter.pick("on", "home"));
        assertEquals("case matters", KeyFilter.ON, KeyFilter.pick("Home", null));
    }

    @Test
    public void onAlwaysFiltersAndOffNever() {
        for (boolean front : new boolean[]{true, false}) {
            assertTrue(KeyFilter.filtering(KeyFilter.ON, front));
            assertFalse(KeyFilter.filtering(KeyFilter.OFF, front));
        }
    }

    @Test
    public void homeFiltersOnlyWhileALauncherScreenIsInFront() {
        assertTrue(KeyFilter.filtering(KeyFilter.HOME, true));
        assertFalse(KeyFilter.filtering(KeyFilter.HOME, false));
    }

    @Test
    public void anUnknownChoiceFiltersWhateverIsInFront() {
        for (boolean front : new boolean[]{true, false}) {
            assertTrue("an unknown value is the default, which filters", KeyFilter.filtering("maybe", front));
            assertTrue(KeyFilter.filtering(null, front));
            assertTrue("case matters", KeyFilter.filtering("HOME", front));
        }
    }

    @Test
    public void onSetsOnlyTheFilterBit() {
        assertEquals(FILTER_BIT, KeyFilter.flags(KeyFilter.ON, 0, false));
        assertEquals(FILTER_BIT | 0x2, KeyFilter.flags(KeyFilter.ON, 0x2, true));
        assertEquals("already set stays set", FILTER_BIT | 0x2, KeyFilter.flags(KeyFilter.ON, FILTER_BIT | 0x2, true));
    }

    @Test
    public void offClearsOnlyTheFilterBitAndKeepsTheRest() {
        assertEquals(0, KeyFilter.flags(KeyFilter.OFF, FILTER_BIT, true));
        assertEquals(0x2 | 0x40, KeyFilter.flags(KeyFilter.OFF, FILTER_BIT | 0x2 | 0x40, false));
        assertEquals("already clear stays clear", 0x2, KeyFilter.flags(KeyFilter.OFF, 0x2, true));
    }

    @Test
    public void homeFollowsTheFrontAndKeepsEveryOtherBit() {
        assertEquals(FILTER_BIT | 0x2, KeyFilter.flags(KeyFilter.HOME, 0x2, true));
        assertEquals("another app in front clears only the filter bit", 0x2 | 0x40,
                KeyFilter.flags(KeyFilter.HOME, FILTER_BIT | 0x2 | 0x40, false));
        assertEquals("leaving and coming back is symmetric", FILTER_BIT,
                KeyFilter.flags(KeyFilter.HOME, KeyFilter.flags(KeyFilter.HOME, FILTER_BIT, false), true));
    }

    @Test
    public void anUnknownChoiceKeepsTheFilterOn() {
        assertEquals(FILTER_BIT, KeyFilter.flags("maybe", 0, false));
        assertEquals(FILTER_BIT, KeyFilter.flags(null, 0, false));
    }

    @Test
    public void everyChoiceInAllIsValidAndOnComesFirst() {
        assertEquals(3, KeyFilter.ALL.length);
        assertEquals(KeyFilter.ON, KeyFilter.ALL[0]);
        assertEquals(KeyFilter.HOME, KeyFilter.ALL[1]);
        assertEquals(KeyFilter.OFF, KeyFilter.ALL[2]);
        for (String s : KeyFilter.ALL) assertTrue(KeyFilter.valid(s));
    }
}
