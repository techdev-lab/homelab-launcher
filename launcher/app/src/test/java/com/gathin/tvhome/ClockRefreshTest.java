package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClockRefreshTest {
    @Test
    public void androidTwelveAndNewerNeedNoPush() {
        assertFalse(ClockRefresh.needed(31));
        assertFalse(ClockRefresh.needed(34));
        assertFalse(ClockRefresh.needed(Integer.MAX_VALUE));
    }

    @Test
    public void androidElevenAndOlderNeedThePush() {
        assertTrue(ClockRefresh.needed(30));
        assertTrue(ClockRefresh.needed(28));
        assertTrue("the minSdk floor, Android 8", ClockRefresh.needed(26));
    }

    @Test
    public void theDelayLandsJustAfterTheNextMinute() {
        long now = 10 * ClockRefresh.MINUTE_MS + 20_000L;   // 20 s into a minute
        assertEquals(40_000L + ClockRefresh.MARGIN_MS, ClockRefresh.delayToNextMinute(now));
    }

    @Test
    public void onTheBoundaryTheNextTickIsAFullMinuteAway() {
        assertEquals(ClockRefresh.MINUTE_MS + ClockRefresh.MARGIN_MS,
                ClockRefresh.delayToNextMinute(5 * ClockRefresh.MINUTE_MS));
    }

    @Test
    public void justBeforeTheBoundaryTheDelayIsTiny() {
        assertEquals(1L + ClockRefresh.MARGIN_MS,
                ClockRefresh.delayToNextMinute(5 * ClockRefresh.MINUTE_MS - 1));
    }

    @Test
    public void theDelayStaysInsideOneMinuteForAnyClockValue() {
        long[] samples = {0L, 1L, 59_999L, 60_000L, 1_700_000_000_123L, Long.MAX_VALUE, -1L, -60_000L, Long.MIN_VALUE};
        for (long now : samples) {
            long d = ClockRefresh.delayToNextMinute(now);
            assertTrue("never zero or negative for " + now, d > ClockRefresh.MARGIN_MS - 1);
            assertTrue("never more than a minute plus the margin for " + now,
                    d <= ClockRefresh.MINUTE_MS + ClockRefresh.MARGIN_MS);
        }
    }
}
