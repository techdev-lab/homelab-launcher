package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SmallMemoryTest {
    private static final long MIB = 1024L * 1024;

    @Test
    public void twoGigabyteTvIsSmallEvenWithoutTheFlag() {
        assertTrue(SmallMemory.isSmall(false, 1938 * MIB));
    }

    @Test
    public void flagAloneMakesItSmall() {
        assertTrue(SmallMemory.isSmall(true, 8192 * MIB));
    }

    @Test
    public void fourGigabyteTvIsNotSmall() {
        assertFalse(SmallMemory.isSmall(false, 3800 * MIB));
    }

    @Test
    public void thresholdIsInclusiveAndOneByteOverIsNot() {
        assertTrue(SmallMemory.isSmall(false, SmallMemory.THRESHOLD_BYTES));
        assertFalse(SmallMemory.isSmall(false, SmallMemory.THRESHOLD_BYTES + 1));
    }

    @Test
    public void unknownMemoryFallsBackToNotSmall() {
        assertFalse(SmallMemory.isSmall(false, 0));
        assertFalse(SmallMemory.isSmall(false, -1));
        assertFalse(SmallMemory.isSmall(false, Long.MIN_VALUE));
        assertFalse(SmallMemory.isSmall(false, Long.MAX_VALUE));
        assertTrue(SmallMemory.isSmall(true, 0));
    }

    @Test
    public void smallTvWithNoChoiceGetsEffectsOff() {
        assertFalse(SmallMemory.resolve(null, null, true, true));
    }

    @Test
    public void tvChoiceOnBeatsTheGate() {
        assertTrue(SmallMemory.resolve(true, null, true, false));
        assertTrue(SmallMemory.resolve(true, false, true, false));
    }

    @Test
    public void tvChoiceOffBeatsAThemeThatSaysOn() {
        assertFalse(SmallMemory.resolve(false, true, false, true));
    }

    @Test
    public void themeValueBeatsTheGate() {
        assertTrue(SmallMemory.resolve(null, true, true, false));
        assertFalse(SmallMemory.resolve(null, false, false, true));
    }

    @Test
    public void largeTvKeepsTheResourceDefault() {
        assertTrue(SmallMemory.resolve(null, null, false, true));
        assertFalse(SmallMemory.resolve(null, null, false, false));
    }

    @Test
    public void describeNamesClassAndSize() {
        assertEquals("small (1938 MiB)", SmallMemory.describe(true, 1938 * MIB));
        assertEquals("normal (3800 MiB)", SmallMemory.describe(false, 3800 * MIB));
        assertEquals("normal (unknown)", SmallMemory.describe(false, 0));
    }
}
