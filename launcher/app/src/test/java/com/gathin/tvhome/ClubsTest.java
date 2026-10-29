package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/** favorite_teams: the club card's choices, with no Android and no network. */
public class ClubsTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = 3_600_000L;
    private static final String HILAL = "929", NASSR = "817", CITY = "382";

    private static Interests.Match m(String code, String homeId, String awayId, String state, long start) {
        return new Interests.Match(code, code, homeId, awayId, "H" + homeId, "A" + awayId, "1", "0", state, "", start);
    }

    @Test
    public void idsAreDigitsDistinctAndAtMostTen() {
        List<String> raw = Arrays.asList("929", " 817 ", "929", "abc", "", "12/34", "1", "2", "3", "4", "5", "6",
                "7", "8", "9");
        List<String> ids = Interests.clubIds(raw);
        assertEquals(Interests.MAX_CLUBS, ids.size());
        assertEquals(Arrays.asList("929", "817", "1", "2", "3", "4", "5", "6", "7", "8"), ids);
    }

    @Test
    public void storedChoiceParses() {
        assertEquals(Arrays.asList(HILAL, NASSR), Interests.clubIds("929,817"));
        assertTrue(Interests.clubIds("").isEmpty());
        assertTrue(Interests.clubIds((String) null).isEmpty());
    }

    @Test
    public void eachClubGetsItsNextTwoMatchesInAnyCompetition() {
        List<Interests.Match> all = Arrays.asList(
                m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW + 72 * HOUR),
                m("afc.champions", HILAL, "790", Interests.Match.PRE, NOW + 24 * HOUR),
                m("ksa.1", "8363", HILAL, Interests.Match.PRE, NOW + 9 * 24 * HOUR),
                m("eng.1", CITY, "331", Interests.Match.PRE, NOW + 5 * HOUR));
        List<Interests.Match> picks = Interests.pickClubs(all, Arrays.asList(HILAL, CITY), NOW);
        assertEquals(3, picks.size());
        assertEquals("afc.champions", picks.get(0).code);
        assertEquals("ksa.1", picks.get(1).code);
        assertEquals(NOW + 72 * HOUR, picks.get(1).startMillis);
        assertEquals(CITY, picks.get(2).homeId);
    }

    @Test
    public void continentalMatchAfterTheLeagueMatchIsShown() {
        // The 2026-10-28 case: league on the 31st, AFC Champions League Elite on the 3rd.
        List<Interests.Match> all = Arrays.asList(
                m("afc.champions", HILAL, "790", Interests.Match.PRE, NOW + 6 * 24 * HOUR),
                m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW + 3 * 24 * HOUR));
        List<Interests.Match> picks = Interests.pickClubs(all, Collections.singletonList(HILAL), NOW);
        assertEquals(2, picks.size());
        assertEquals("ksa.1", picks.get(0).code);
        assertEquals("afc.champions", picks.get(1).code);
    }

    @Test
    public void derbyCopiesDoNotHideTheNextMatch() {
        // Al Hilal and Al Fateh both picked: their league match comes once from each schedule. Al Hilal's AFC match
        // must still be its second match (on the TV the two copies filled both places).
        String fateh = "13033";
        Interests.Match derby = m("ksa.1", HILAL, fateh, Interests.Match.PRE, NOW + 72 * HOUR);
        Interests.Match afc = m("afc.champions", HILAL, "790", Interests.Match.PRE, NOW + 6 * 24 * HOUR);
        Interests.Match fatehNext = m("ksa.1", "21827", fateh, Interests.Match.PRE, NOW + 8 * 24 * HOUR);
        java.util.List<Interests.Match> all = Arrays.asList(derby, afc, derby, fatehNext);
        java.util.List<Interests.Match> picks = Interests.pickClubs(all, Arrays.asList(HILAL, fateh), NOW);
        assertEquals(3, picks.size());
        assertEquals("ksa.1", picks.get(0).code);
        assertEquals("afc.champions", picks.get(1).code);
        assertEquals("21827", picks.get(2).homeId);
    }

    @Test
    public void starFollowsTheTeamIdOnEitherSide() {
        List<String> favs = Arrays.asList(HILAL, NASSR);
        assertTrue(Interests.involvesFavorite(m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW), favs));
        assertTrue(Interests.involvesFavorite(m("ksa.1", "8363", NASSR, Interests.Match.PRE, NOW), favs));
        assertFalse(Interests.involvesFavorite(m("ksa.1", "21964", "8363", Interests.Match.PRE, NOW), favs));
        assertFalse(Interests.involvesFavorite(new Interests.Match("ksa.1", "Al Hilal", "Al Fateh", "", "", "pre", "",
                NOW), favs));
        assertFalse(Interests.involvesFavorite(m("ksa.1", HILAL, "1", Interests.Match.PRE, NOW),
                Collections.<String>emptyList()));
    }

    @Test
    public void liveMatchWinsAndFinishedIsSkipped() {
        List<Interests.Match> all = Arrays.asList(
                m("ksa.1", "21827", HILAL, Interests.Match.POST, NOW - 30 * HOUR),
                m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW + 2 * HOUR),
                m("ksa.kings.cup", "21964", HILAL, Interests.Match.LIVE, NOW - HOUR));
        List<Interests.Match> picks = Interests.pickClubs(all, Collections.singletonList(HILAL), NOW);
        assertEquals(2, picks.size());
        assertEquals(Interests.Match.LIVE, picks.get(0).state);
        assertEquals("ksa.kings.cup", picks.get(0).code);
        assertEquals(Interests.Match.PRE, picks.get(1).state);
    }

    @Test
    public void derbyShowsOnceAndClubsKeepTheirOrder() {
        Interests.Match derby = m("ksa.1", NASSR, HILAL, Interests.Match.PRE, NOW + 48 * HOUR);
        List<Interests.Match> all = Arrays.asList(derby, derby,
                m("eng.1", CITY, "331", Interests.Match.PRE, NOW + 90 * HOUR));
        List<Interests.Match> picks = Interests.pickClubs(all, Arrays.asList(HILAL, NASSR, CITY), NOW);
        assertEquals(2, picks.size());
        assertEquals(NASSR, picks.get(0).homeId);
        assertEquals(CITY, picks.get(1).homeId);
    }

    @Test
    public void clubWindowCoversAnInternationalBreak() {
        List<Interests.Match> all = Collections.singletonList(
                m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW + 16 * 24 * HOUR));
        assertEquals(1, Interests.pickClubs(all, Collections.singletonList(HILAL), NOW).size());
        List<Interests.Match> tooFar = Collections.singletonList(
                m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW + 22 * 24 * HOUR));
        assertTrue(Interests.pickClubs(tooFar, Collections.singletonList(HILAL), NOW).isEmpty());
    }

    @Test
    public void nearKickoffIsTheWindowForLiveRefresh() {
        assertTrue(Interests.nearKickoff(m("ksa.1", HILAL, "1", Interests.Match.PRE, NOW + 10 * 60_000L), NOW));
        assertTrue(Interests.nearKickoff(m("ksa.1", HILAL, "1", Interests.Match.PRE, NOW - 2 * HOUR), NOW));
        assertTrue(Interests.nearKickoff(m("ksa.1", HILAL, "1", Interests.Match.LIVE, NOW - HOUR), NOW));
        assertFalse(Interests.nearKickoff(m("ksa.1", HILAL, "1", Interests.Match.PRE, NOW + 2 * HOUR), NOW));
        assertFalse(Interests.nearKickoff(m("ksa.1", HILAL, "1", Interests.Match.POST, NOW - HOUR), NOW));
    }

    @Test
    public void scoreboardCopyReplacesTheScheduleCopy() {
        Interests.Match old = m("ksa.1", HILAL, "13033", Interests.Match.PRE, NOW - 10 * 60_000L);
        Interests.Match other = m("eng.1", CITY, "331", Interests.Match.PRE, NOW + 5 * HOUR);
        Interests.Match fresh = new Interests.Match("ksa.1", "ksa.1", HILAL, "13033", "H", "A", "2", "1",
                Interests.Match.LIVE, "34'", NOW - 10 * 60_000L);
        List<Interests.Match> out = Interests.withUpdates(Arrays.asList(old, other), Collections.singletonList(fresh));
        assertEquals(Interests.Match.LIVE, out.get(0).state);
        assertEquals("2", out.get(0).homeScore);
        assertEquals(Interests.Match.PRE, out.get(1).state);
    }

    @Test
    public void cardSwitchesTurnEachCardOffOnItsOwn() {
        List<String> none = Collections.emptyList();
        Interests.Settings on = new Interests.Settings("ar", true, "", "", 25.37, 49.59, Arrays.asList("ksa.1"), none,
                none, none, true, Collections.emptyList(), Arrays.asList(HILAL), 5);
        assertTrue(on.prayer() && on.wantsWeather() && on.wantsMatches() && on.wantsClubs());
        Interests.Settings noPrayer = new Interests.Settings("ar", true, "", "", 25.37, 49.59, Arrays.asList("ksa.1"),
                none, none, none, true, Collections.emptyList(), Arrays.asList(HILAL), 5)
                .withCards(false, true, true, true);
        assertFalse(noPrayer.prayer());
        assertTrue("weather keeps the place when only the prayer card is off", noPrayer.wantsWeather());
        Interests.Settings quiet = new Interests.Settings("ar", true, "", "", 25.37, 49.59, Arrays.asList("ksa.1"),
                none, none, none, true, Collections.emptyList(), Arrays.asList(HILAL), 5)
                .withCards(true, false, false, false);
        assertTrue(quiet.prayer());
        assertFalse(quiet.wantsWeather() || quiet.wantsMatches() || quiet.wantsClubs());
    }

    @Test
    public void cardSwitchesNeverChangeTheSharedNone() {
        Interests.Settings off = Interests.Settings.NONE.withCards(false, false, false, false);
        assertFalse(off.showPrayer);
        assertTrue(Interests.Settings.NONE.showPrayer && Interests.Settings.NONE.showClubs);
    }

    @Test
    public void settingsWithoutClubsDoNotWantThem() {
        assertFalse(Interests.Settings.NONE.wantsClubs());
        List<String> none = Collections.emptyList();
        Interests.Settings s = new Interests.Settings("ar", false, "", "", Double.NaN, Double.NaN, none, none, none,
                none, false, Collections.emptyList(), Arrays.asList(HILAL), 5);
        assertTrue(s.wantsClubs());
        assertEquals(5, s.clubRotateSeconds);
    }
}
