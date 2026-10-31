package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FeedRetryTest {

    private static final long NORMAL = 15 * 60_000L;
    private static final long MINUTE = 60_000L;

    @Test
    public void withDataTheNormalRhythmStays() {
        assertEquals(NORMAL, FeedRetry.waitMs(true, 0, NORMAL));
        assertEquals(NORMAL, FeedRetry.waitMs(true, 7, NORMAL));
    }

    @Test
    public void withoutDataTheNextFetchComesSoonerAndBacksOff() {
        assertEquals(1 * MINUTE, FeedRetry.waitMs(false, 0, NORMAL));
        assertEquals(1 * MINUTE, FeedRetry.waitMs(false, 1, NORMAL));
        assertEquals(2 * MINUTE, FeedRetry.waitMs(false, 2, NORMAL));
        assertEquals(4 * MINUTE, FeedRetry.waitMs(false, 3, NORMAL));
        assertEquals(8 * MINUTE, FeedRetry.waitMs(false, 4, NORMAL));
        assertEquals("after four tries it is the normal rhythm again", NORMAL, FeedRetry.waitMs(false, 5, NORMAL));
    }

    @Test
    public void theBackOffNeverOverflowsAndNeverExceedsTheNormalWait() {
        assertEquals(NORMAL, FeedRetry.waitMs(false, 1000, NORMAL));
        assertEquals(NORMAL, FeedRetry.waitMs(false, Integer.MAX_VALUE, NORMAL));
        assertEquals(1 * MINUTE, FeedRetry.waitMs(false, -3, NORMAL));
        assertEquals("the smallest int must not wrap around", 1 * MINUTE, FeedRetry.waitMs(false, Integer.MIN_VALUE, NORMAL));
        assertEquals("a short normal wait is a ceiling too", 2 * MINUTE, FeedRetry.waitMs(false, 4, 2 * MINUTE));
    }

    @Test
    public void aNetworkComingBackRefetchesOnlyWhenThereIsNothingToShow() {
        long now = 1_000_000L;
        assertTrue(FeedRetry.refetchOnNetwork(true, false, now - 60_000L, now));
        assertFalse("there is data: keep the normal rhythm", FeedRetry.refetchOnNetwork(true, true, now - 60_000L, now));
        assertFalse("every card is off: nothing to fetch", FeedRetry.refetchOnNetwork(false, false, now - 60_000L, now));
    }

    @Test
    public void aFlappingNetworkDoesNotCauseAStormOfFetches() {
        long now = 1_000_000L;
        assertFalse(FeedRetry.refetchOnNetwork(true, false, now - 5_000L, now));
        assertFalse(FeedRetry.refetchOnNetwork(true, false, now - 14_999L, now));
        assertTrue(FeedRetry.refetchOnNetwork(true, false, now - 15_000L, now));
        assertTrue("never fetched yet", FeedRetry.refetchOnNetwork(true, false, 0L, now));
        assertFalse("a clock that went backwards is not a reason to fetch", FeedRetry.refetchOnNetwork(true, false, now + 10_000L, now));
    }

    @Test
    public void theNetworkFetchWaitsAShortWhileForTheConnectionToSettle() {
        assertEquals(3_000L, FeedRetry.NETWORK_DELAY_MS);
        assertTrue(FeedRetry.NETWORK_DELAY_MS < FeedRetry.MIN_GAP_ON_NETWORK_MS);
    }
}
