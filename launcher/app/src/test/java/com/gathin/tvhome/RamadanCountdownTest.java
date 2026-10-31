package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RamadanCountdownTest {

    // Fajr 4:30, Maghrib 18:00 today; the next Fajr 4:29, on today's clock plus 24 hours.
    private static final double FAJR = 4.5, MAGHRIB = 18.0, FAJR_NEXT = 24 + 4.4833;

    private static RamadanCountdown.Result at(boolean today, boolean tomorrow, double now) {
        return RamadanCountdown.of(today, tomorrow, now, FAJR, MAGHRIB, FAJR_NEXT);
    }

    @Test
    public void beforeFajrItCountsDownToTheEndOfSuhoor() {
        RamadanCountdown.Result r = at(true, true, 2.0);
        assertEquals(RamadanCountdown.SUHOOR, r.kind);
        assertEquals(FAJR, r.target, 1e-9);
    }

    @Test
    public void fromFajrUntilMaghribItCountsDownToIftar() {
        RamadanCountdown.Result atFajr = at(true, true, FAJR);
        assertEquals(RamadanCountdown.IFTAR, atFajr.kind);
        assertEquals(MAGHRIB, atFajr.target, 1e-9);
        assertEquals(RamadanCountdown.IFTAR, at(true, true, 12.0).kind);
        assertEquals(RamadanCountdown.IFTAR, at(true, true, MAGHRIB - 0.01).kind);
    }

    @Test
    public void fromMaghribItCountsDownToTomorrowsSuhoorWhenTomorrowIsRamadan() {
        RamadanCountdown.Result r = at(true, true, MAGHRIB);
        assertEquals(RamadanCountdown.SUHOOR, r.kind);
        assertEquals(FAJR_NEXT, r.target, 1e-9);
        assertEquals(RamadanCountdown.SUHOOR, at(true, true, 23.9).kind);
    }

    @Test
    public void afterMaghribOnTheLastDayThereIsNoCard() {
        assertEquals(RamadanCountdown.NONE, at(true, false, MAGHRIB).kind);
        assertEquals(RamadanCountdown.NONE, at(true, false, 22.0).kind);
        // The day itself still counts down to its iftar.
        assertEquals(RamadanCountdown.IFTAR, at(true, false, 10.0).kind);
    }

    @Test
    public void theEveningBeforeTheFirstFastShowsTheFirstSuhoor() {
        RamadanCountdown.Result r = at(false, true, 19.0);
        assertEquals(RamadanCountdown.SUHOOR, r.kind);
        assertEquals(FAJR_NEXT, r.target, 1e-9);
    }

    @Test
    public void outsideRamadanThereIsNoCardAtAnyHour() {
        for (double h = 0; h < 24; h += 0.5) assertEquals("hour " + h, RamadanCountdown.NONE, at(false, false, h).kind);
        // Before Maghrib on the day before Ramadan: nothing yet.
        assertEquals(RamadanCountdown.NONE, at(false, true, 10.0).kind);
        assertEquals(RamadanCountdown.NONE, at(false, true, 2.0).kind);
    }

    @Test
    public void nanTimesGiveNoCard() {
        assertEquals(RamadanCountdown.NONE, RamadanCountdown.of(true, true, 12.0, Double.NaN, MAGHRIB, FAJR_NEXT).kind);
        assertEquals(RamadanCountdown.NONE, RamadanCountdown.of(true, true, Double.NaN, FAJR, MAGHRIB, FAJR_NEXT).kind);
    }
}
