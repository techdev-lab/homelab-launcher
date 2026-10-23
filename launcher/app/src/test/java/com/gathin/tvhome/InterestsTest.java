package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class InterestsTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = 3_600_000L;

    private static Interests.Match m(String home, String away, String state, long start) {
        return new Interests.Match("ksa.1", home, away, "0", "0", state, "", start);
    }

    @Test
    public void liveBeatsUpcoming() {
        List<Interests.Match> all = Arrays.asList(
                m("Al Hilal", "Al Nassr", Interests.Match.PRE, NOW + HOUR),
                m("Al Ahli", "Al Ittihad", Interests.Match.LIVE, NOW - HOUR));
        assertEquals("Al Ahli", Interests.pickMatch(all, Collections.emptyList(), NOW).home);
    }

    @Test
    public void earliestUpcomingWithinAWeek() {
        List<Interests.Match> all = Arrays.asList(
                m("A", "B", Interests.Match.PRE, NOW + 5 * HOUR),
                m("C", "D", Interests.Match.PRE, NOW + 2 * HOUR),
                m("E", "F", Interests.Match.PRE, NOW + 30 * HOUR),
                m("G", "H", Interests.Match.PRE, NOW + 8 * 24 * HOUR));
        assertEquals("C", Interests.pickMatch(all, Collections.emptyList(), NOW).home);
    }

    @Test
    public void finishedAndFarAwayAreNeverShown() {
        List<Interests.Match> all = Arrays.asList(
                m("A", "B", Interests.Match.POST, NOW - HOUR),
                m("C", "D", Interests.Match.PRE, NOW + 8 * 24 * HOUR));
        assertNull(Interests.pickMatch(all, Collections.emptyList(), NOW));
    }

    @Test
    public void teamFilterMatchesEitherSideIgnoringCase() {
        List<Interests.Match> all = Arrays.asList(
                m("Al Fayha", "Al Hazem", Interests.Match.LIVE, NOW),
                m("Al Nassr", "Al Hilal", Interests.Match.PRE, NOW + HOUR));
        assertEquals("Al Nassr", Interests.pickMatch(all, Collections.singletonList("hilal"), NOW).home);
    }

    @Test
    public void oneMatchPerLeagueSoALiveCupGameDoesNotHideTheLeague() {
        List<Interests.Match> all = Arrays.asList(
                new Interests.Match("Saudi Pro League", "Al Fayha", "Al Hazem", "0", "0", Interests.Match.PRE, "", NOW + 18 * HOUR),
                new Interests.Match("Saudi Pro League", "Al Khaleej", "Al Ahli", "0", "0", Interests.Match.PRE, "", NOW + 22 * HOUR),
                new Interests.Match("UEFA Champions League", "Aston Villa", "Viking FK", "0", "0", Interests.Match.LIVE, "19'", NOW),
                new Interests.Match("UEFA Champions League", "Old", "Game", "1", "0", Interests.Match.POST, "", NOW - 30 * HOUR));
        List<Interests.Match> picks = Interests.pickMatches(all, Collections.emptyList(), NOW);
        assertEquals(2, picks.size());
        assertEquals("Al Fayha", picks.get(0).home);
        assertEquals("Aston Villa", picks.get(1).home);
    }

    @Test
    public void espnDayIsUtcAndCrossesMonthAndYear() {
        long oct22Late = java.time.Instant.parse("2026-10-22T22:30:00Z").toEpochMilli();
        assertEquals("20261022", Interests.espnDay(oct22Late, 0));
        assertEquals("20261023", Interests.espnDay(oct22Late, 1));
        assertEquals("20261029", Interests.espnDay(oct22Late, Interests.LOOKAHEAD_DAYS));
        long oct31 = java.time.Instant.parse("2026-10-31T12:00:00Z").toEpochMilli();
        assertEquals("20261101", Interests.espnDay(oct31, 1));
        long dec30 = java.time.Instant.parse("2026-12-30T12:00:00Z").toEpochMilli();
        assertEquals("20270102", Interests.espnDay(dec30, 3));
    }

    @Test
    public void finishedMatchdayMeansLookFurther() {
        // What Feeds sees on 22 Oct evening: today's matchday all finished, so pickMatch is null and it asks
        // ESPN for the next day, whose upcoming match then fills the card.
        List<Interests.Match> today = Arrays.asList(
                m("Al Fayha", "Al Hazem", Interests.Match.POST, NOW - 6 * HOUR),
                m("Al Fateh", "Al Shabab", Interests.Match.POST, NOW - 5 * HOUR));
        assertNull(Interests.pickMatch(today, Collections.emptyList(), NOW));
        List<Interests.Match> plusTomorrow = new java.util.ArrayList<>(today);
        plusTomorrow.add(m("Al Hilal", "Al Ahli", Interests.Match.PRE, NOW + 17 * HOUR));
        assertEquals("Al Hilal", Interests.pickMatch(plusTomorrow, Collections.emptyList(), NOW).home);
    }

    private static Interests.Match coded(String code, String home, String state, long start) {
        return new Interests.Match(code + " name", code, home, "x", "0", "0", state, "", start);
    }

    @Test
    public void sportsCardsPickOneMatchPerGroupInCardOrder() {
        List<Interests.Match> all = Arrays.asList(
                coded("eng.1", "Arsenal", Interests.Match.PRE, NOW + 30 * HOUR),
                coded("esp.1", "Real Madrid", Interests.Match.LIVE, NOW - HOUR),
                coded("ksa.1", "Al Hilal", Interests.Match.PRE, NOW + 20 * HOUR),
                coded("ksa.1", "Al Fateh", Interests.Match.POST, NOW - 5 * HOUR));
        List<List<String>> cards = Arrays.asList(
                Collections.singletonList("ksa.1"),
                Arrays.asList("uefa.champions", "eng.1", "esp.1"));
        List<Interests.Match> picks = Interests.pickCards(all, cards, Collections.emptyList(), NOW);
        assertEquals(2, picks.size());
        assertEquals("Al Hilal", picks.get(0).home);
        // Live beats upcoming across the whole foreign group, whatever league it is in.
        assertEquals("Real Madrid", picks.get(1).home);
    }

    @Test
    public void sportsCardGroupWithNothingToShowIsLeftOut() {
        List<Interests.Match> all = Collections.singletonList(coded("ksa.1", "Al Fateh", Interests.Match.POST, NOW - HOUR));
        List<List<String>> cards = Arrays.asList(Collections.singletonList("ksa.1"), Collections.singletonList("eng.1"));
        assertTrue(Interests.pickCards(all, cards, Collections.emptyList(), NOW).isEmpty());
    }

    @Test
    public void leaguesToFetchIsTheUnionWithoutRepeats() {
        Interests.Settings s = new Interests.Settings("system", false, "", "", Double.NaN, Double.NaN,
                Arrays.asList("ksa.1"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                true, Arrays.asList(Collections.singletonList("ksa.1"), Arrays.asList("eng.1", "esp.1")));
        assertEquals(Arrays.asList("ksa.1", "eng.1", "esp.1"), s.leaguesToFetch());
        assertTrue(s.wantsMatches());
        assertFalse(s.wantsNews());
    }

    @Test
    public void cityAndFeedsFollowTheScreenLanguage() {
        Interests.Settings s = new Interests.Settings("system", true, "Al-Ahsa (Arabic name)", "Al-Ahsa", 25.37, 49.59,
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList("https://ar.example/rss"),
                Collections.singletonList("https://en.example/rss"));
        assertEquals("Al-Ahsa (Arabic name)", s.city(true));
        assertEquals("Al-Ahsa", s.city(false));
        assertEquals("https://ar.example/rss", s.feeds(true).get(0));
        assertEquals("https://en.example/rss", s.feeds(false).get(0));
        Interests.Settings arOnly = new Interests.Settings("system", true, "Al-Ahsa (Arabic name)", "", 25.37, 49.59,
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList("https://ar.example/rss"),
                Collections.emptyList());
        assertEquals("Al-Ahsa (Arabic name)", arOnly.city(false));
        assertEquals("https://ar.example/rss", arOnly.feeds(false).get(0));
    }

    @Test
    public void inputsAreValidatedBeforeUse() {
        assertTrue(Interests.validLeague("ksa.1"));
        assertTrue(Interests.validLeague("uefa.champions"));
        assertFalse(Interests.validLeague("../x"));
        assertFalse(Interests.validLeague("ksa.1?x=1"));
        assertTrue(Interests.validFeed("https://feeds.bbci.co.uk/arabic/rss.xml"));
        assertFalse(Interests.validFeed("http://example.com/rss"));
        assertFalse(Interests.validFeed("https://user@example.com/rss"));
        assertFalse(Interests.validPlace(0, 0));
        assertFalse(Interests.validPlace(Double.NaN, 46));
        assertTrue(Interests.validPlace(24.7136, 46.6753));
    }

    @Test
    public void weatherCodesGroupIntoConditions() {
        assertEquals(Interests.SKY_CLEAR, Interests.sky(0));
        assertEquals(Interests.SKY_PARTLY, Interests.sky(2));
        assertEquals(Interests.SKY_CLOUDY, Interests.sky(3));
        assertEquals(Interests.SKY_FOG, Interests.sky(48));
        assertEquals(Interests.SKY_DRIZZLE, Interests.sky(55));
        assertEquals(Interests.SKY_RAIN, Interests.sky(63));
        assertEquals(Interests.SKY_RAIN, Interests.sky(81));
        assertEquals(Interests.SKY_SNOW, Interests.sky(86));
        assertEquals(Interests.SKY_STORM, Interests.sky(95));
        assertEquals(Interests.SKY_UNKNOWN, Interests.sky(-1));
        assertEquals(Interests.SKY_UNKNOWN, Interests.sky(42));
    }

    @Test
    public void temperatureRoundsToWholeDegrees() {
        assertEquals("31°", new Interests.Weather(30.6, 0).temperature());
        assertEquals("-3°", new Interests.Weather(-2.6, 0).temperature());
        assertEquals("0°", new Interests.Weather(-0.4, 0).temperature());
    }

    @Test
    public void weatherNeedsTheSwitchAndARealPlace() {
        List<String> none = Collections.emptyList();
        assertTrue(new Interests.Settings("system", false, "", "", 25.37, 49.59, none, none, none, none, true)
                .wantsWeather());
        assertFalse(new Interests.Settings("system", false, "", "", 25.37, 49.59, none, none, none, none, false)
                .wantsWeather());
        assertFalse(new Interests.Settings("system", false, "", "", 0, 0, none, none, none, none, true)
                .wantsWeather());
        assertFalse(Interests.Settings.NONE.wantsWeather());
    }
}
