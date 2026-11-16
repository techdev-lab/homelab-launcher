package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SceneRotationTest {
    @Test
    public void secondsAreClampedToFiveToSixHundred() {
        assertEquals(5, SceneRotation.clamp(0));
        assertEquals(5, SceneRotation.clamp(-30));
        assertEquals(5, SceneRotation.clamp(3));
        assertEquals(600, SceneRotation.clamp(601));
        assertEquals(600, SceneRotation.clamp(Integer.MAX_VALUE));
        assertEquals(20, SceneRotation.clamp(20));
    }

    @Test
    public void theTvChoiceBeatsConfigWhichBeatsTheDefault() {
        assertEquals(60, SceneRotation.pick(60, 30));
        assertEquals(30, SceneRotation.pick(null, 30));
        assertEquals(20, SceneRotation.pick(null, null));
        assertEquals(5, SceneRotation.pick(null, 1));        // a bad config value is clamped, not ignored
        assertEquals(600, SceneRotation.pick(9999, 30));
    }

    @Test
    public void theSceneIsAFunctionOfTheClockSoEveryTvAgrees() {
        assertEquals(0, SceneRotation.indexAt(0, 20, 5));
        assertEquals(0, SceneRotation.indexAt(19_999, 20, 5));
        assertEquals(1, SceneRotation.indexAt(20_000, 20, 5));
        assertEquals(4, SceneRotation.indexAt(80_000, 20, 5));
        assertEquals(0, SceneRotation.indexAt(100_000, 20, 5));   // wraps
    }

    @Test
    public void noScenesGivesMinusOneAndANegativeTimeCountsAsZero() {
        assertEquals(-1, SceneRotation.indexAt(1_000, 20, 0));
        assertEquals(-1, SceneRotation.indexAt(1_000, 20, -2));
        assertEquals(0, SceneRotation.indexAt(-5_000, 20, 3));
    }

    @Test
    public void theWaitToTheNextSlotIsAlwaysBetweenOneMillisecondAndTheSlot() {
        assertEquals(20_000, SceneRotation.msToNext(0, 20));
        assertEquals(1, SceneRotation.msToNext(19_999, 20));
        assertEquals(20_000, SceneRotation.msToNext(20_000, 20));
        assertEquals(5_000, SceneRotation.msToNext(-1, 1));   // seconds clamped to 5, time to 0
        for (long t = 0; t < 100_000; t += 777) {
            long w = SceneRotation.msToNext(t, 20);
            assertTrue(w >= 1 && w <= 20_000);
        }
    }
}
