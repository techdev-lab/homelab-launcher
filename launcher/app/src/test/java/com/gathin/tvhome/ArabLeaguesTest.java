package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** The TheSportsDB leagues (Egypt, UAE, Qatar, Morocco): codes, statuses and the card's pick. */
public class ArabLeaguesTest {
    private static final long NOW = 1_793_200_000_000L;
    private static final long HOUR = 3_600_000L;

    @Test
    public void codesAreTheSportsDbLeagueIds() {
        assertTrue(Interests.isTsdb("tsdb.4829"));
        assertTrue(Interests.validLeague("tsdb.4829"));
        assertFalse(Interests.isTsdb("ksa.1"));
        assertFalse(Interests.isTsdb("tsdb."));
        assertFalse(Interests.isTsdb("tsdb.48a9"));
        assertFalse(Interests.isTsdb(null));
    }

    @Test
    public void postponedAndCancelledNeverShowAsUpcoming() {
        assertEquals(Interests.Match.PRE, Interests.tsdbState("NS"));
        assertEquals(Interests.Match.PRE, Interests.tsdbState(null));
        assertEquals(Interests.Match.LIVE, Interests.tsdbState("2H"));
        assertEquals(Interests.Match.POST, Interests.tsdbState("FT"));
        assertEquals("", Interests.tsdbState("PST"));
        assertEquals("", Interests.tsdbState("CANC"));
        Interests.Match postponed = new Interests.Match("UAE Pro League", "tsdb.4678", "t137832", "t137838",
                "Al Ain", "Ajman", "", "", Interests.tsdbState("PST"), "", NOW + 3 * HOUR);
        Interests.Match next = new Interests.Match("UAE Pro League", "tsdb.4678", "t1", "t2", "A", "B", "", "",
                Interests.tsdbState("NS"), "", NOW + 30 * HOUR);
        assertNull(Interests.pickMatch(Collections.singletonList(postponed), Collections.<String>emptyList(), NOW));
        assertEquals(next, Interests.pickMatch(Arrays.asList(postponed, next), Collections.<String>emptyList(), NOW));
    }

    @Test
    public void dayIsIsoInUtc() {
        // 1_793_200_000_000 ms = 2026-10-28T15:06:40Z
        assertEquals("2026-10-28", Interests.tsdbDay(NOW, 0));
        assertEquals("2026-11-04", Interests.tsdbDay(NOW, 7));
    }
}
