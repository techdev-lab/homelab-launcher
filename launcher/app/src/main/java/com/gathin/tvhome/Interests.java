package com.gathin.tvhome;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The interests strip under the clock: prayer times (computed here, offline), and in the online build
 * matches and news (fetched by {@link Feeds}). Pure data and choices, no Android or network calls, so the
 * rules are unit-tested on the JVM.
 */
final class Interests {

    private Interests() {
    }

    /** config.json keys for the strip; every one optional, see Launcher_Kit/theme-arabic/README.md. */
    static final class Settings {
        static final Settings NONE = new Settings("system", false, "", "", Double.NaN, Double.NaN,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                false);

        /** "ar", "en" or "system" (the TV's language). */
        final String language;
        final boolean hijriDate;
        final String prayerCity;
        /** The city's English name for the English screen ("Al-Ahsa"); empty falls back to prayerCity. */
        final String prayerCityEn;
        final double prayerLat;
        final double prayerLng;
        /** ESPN league codes such as "ksa.1" or "uefa.champions". */
        final List<String> sportsLeagues;
        /** Team names to keep, matched case-insensitively against either side; empty keeps every match. */
        final List<String> sportsTeams;
        /** HTTPS RSS or Atom feeds for the Arabic screen ("news_feeds"). */
        final List<String> newsFeeds;
        /** Feeds for the English screen ("news_feeds_en"); empty falls back to newsFeeds. */
        final List<String> newsFeedsEn;
        /** Current weather at prayer_lat/prayer_lng ("weather"); on by default in the online build only. */
        final boolean weather;
        /**
         * "sports_cards": one match card per group of league codes, side by side, e.g. [["ksa.1"],
         * ["uefa.champions", "eng.1"]]. Empty keeps the single card that rotates through sports_leagues.
         */
        final List<List<String>> sportsCards;
        /**
         * "favorite_teams" (or the clubs picked on the TV): ESPN team ids, at most {@link #MAX_CLUBS}, in the order
         * the club card shows them. Each club's next or live match in any competition (league, cup, continental).
         */
        final List<String> favoriteTeams;
        /** Seconds each club stays on the club card before the next one ("club_rotate_seconds", 3 to 60). */
        final int clubRotateSeconds;
        /** Card switches from Launcher settings: a card that is off never shows, whatever config.json says. */
        boolean showPrayer = true, showWeather = true, showSports = true, showClubs = true;

        /** "prayer_method" as written (TV choice, else config.json); shown on the prayer card when it names none. */
        String prayerMethodKey = Prayer.UMM_AL_QURA.key;
        /** The method in force, or null when {@link #prayerMethodKey} names no method: no times are made up then. */
        Prayer.Method prayerMethod = Prayer.UMM_AL_QURA;
        /** {@link Prayer#ASR_STANDARD} or {@link Prayer#ASR_HANAFI} ("asr_method"). */
        int asrFactor = Prayer.ASR_STANDARD;
        /** "prayer_adjust": the viewer's own minutes per time, {FAJR..ISHA}, each at most 30 either way. */
        int[] prayerAdjust = new int[6];

        /** A copy of every field; the with* methods change the copy, never this object (NONE is shared). */
        private Settings copy() {
            Settings s = new Settings(language, hijriDate, prayerCity, prayerCityEn, prayerLat, prayerLng,
                    sportsLeagues, sportsTeams, newsFeeds, newsFeedsEn, this.weather, sportsCards, favoriteTeams,
                    clubRotateSeconds);
            s.showPrayer = showPrayer;
            s.showWeather = showWeather;
            s.showSports = showSports;
            s.showClubs = showClubs;
            s.prayerMethodKey = prayerMethodKey;
            s.prayerMethod = prayerMethod;
            s.asrFactor = asrFactor;
            s.prayerAdjust = prayerAdjust.clone();
            return s;
        }

        /** A copy with the TV's card switches applied. */
        Settings withCards(boolean prayer, boolean weather, boolean sports, boolean clubs) {
            Settings s = copy();
            s.showPrayer = prayer;
            s.showWeather = weather;
            s.showSports = sports;
            s.showClubs = clubs;
            return s;
        }

        /** A copy with the prayer method (by key), the Asr factor and the manual minutes. */
        Settings withPrayer(String methodKey, int asr, int[] adjust) {
            Settings s = copy();
            s.prayerMethodKey = methodKey;
            s.prayerMethod = Prayer.method(methodKey);
            s.asrFactor = asr == Prayer.ASR_HANAFI ? Prayer.ASR_HANAFI : Prayer.ASR_STANDARD;
            s.prayerAdjust = adjust != null && adjust.length == 6 ? adjust.clone() : new int[6];
            return s;
        }

        /** The six times for that date at the prayer place, or null when the method is unknown (see prayerMethod). */
        double[] prayerTimes(int year, int month, int day, double tzHours, boolean ramadan) {
            if (prayerMethod == null) return null;
            return Prayer.times(year, month, day, prayerLat, prayerLng, tzHours, ramadan, prayerMethod, asrFactor,
                    prayerAdjust);
        }

        Settings(String language, boolean hijriDate, String prayerCity, String prayerCityEn, double prayerLat,
                 double prayerLng, List<String> sportsLeagues, List<String> sportsTeams, List<String> newsFeeds,
                 List<String> newsFeedsEn) {
            this(language, hijriDate, prayerCity, prayerCityEn, prayerLat, prayerLng, sportsLeagues, sportsTeams,
                    newsFeeds, newsFeedsEn, false);
        }

        Settings(String language, boolean hijriDate, String prayerCity, String prayerCityEn, double prayerLat,
                 double prayerLng, List<String> sportsLeagues, List<String> sportsTeams, List<String> newsFeeds,
                 List<String> newsFeedsEn, boolean weather) {
            this(language, hijriDate, prayerCity, prayerCityEn, prayerLat, prayerLng, sportsLeagues, sportsTeams,
                    newsFeeds, newsFeedsEn, weather, Collections.emptyList());
        }

        Settings(String language, boolean hijriDate, String prayerCity, String prayerCityEn, double prayerLat,
                 double prayerLng, List<String> sportsLeagues, List<String> sportsTeams, List<String> newsFeeds,
                 List<String> newsFeedsEn, boolean weather, List<List<String>> sportsCards) {
            this(language, hijriDate, prayerCity, prayerCityEn, prayerLat, prayerLng, sportsLeagues, sportsTeams,
                    newsFeeds, newsFeedsEn, weather, sportsCards, Collections.emptyList(), CLUB_ROTATE_DEFAULT_S);
        }

        Settings(String language, boolean hijriDate, String prayerCity, String prayerCityEn, double prayerLat,
                 double prayerLng, List<String> sportsLeagues, List<String> sportsTeams, List<String> newsFeeds,
                 List<String> newsFeedsEn, boolean weather, List<List<String>> sportsCards,
                 List<String> favoriteTeams, int clubRotateSeconds) {
            this.favoriteTeams = Collections.unmodifiableList(favoriteTeams);
            this.clubRotateSeconds = clubRotateSeconds;
            this.sportsCards = Collections.unmodifiableList(sportsCards);
            this.weather = weather;
            this.language = language;
            this.hijriDate = hijriDate;
            this.prayerCity = prayerCity;
            this.prayerCityEn = prayerCityEn;
            this.prayerLat = prayerLat;
            this.prayerLng = prayerLng;
            this.sportsLeagues = Collections.unmodifiableList(sportsLeagues);
            this.sportsTeams = Collections.unmodifiableList(sportsTeams);
            this.newsFeeds = Collections.unmodifiableList(newsFeeds);
            this.newsFeedsEn = Collections.unmodifiableList(newsFeedsEn);
        }

        String city(boolean arabic) {
            return arabic || prayerCityEn.isEmpty() ? prayerCity : prayerCityEn;
        }

        /** The feeds for the screen's language: news follows the TV's language like everything else. */
        List<String> feeds(boolean arabic) {
            return arabic || newsFeedsEn.isEmpty() ? newsFeeds : newsFeedsEn;
        }

        /** Prayer times need a real place: 0,0 (the template's placeholder) or out-of-range values mean off. */
        boolean prayer() {
            return showPrayer && validPlace(prayerLat, prayerLng);
        }

        boolean wantsMatches() {
            return showSports && (!sportsLeagues.isEmpty() || !sportsCards.isEmpty());
        }

        /** Every league to fetch once: sports_leagues, then the ones only named in sports_cards. */
        List<String> leaguesToFetch() {
            java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>(sportsLeagues);
            for (List<String> card : sportsCards) all.addAll(card);
            return new java.util.ArrayList<>(all);
        }

        boolean wantsClubs() {
            return showClubs && !favoriteTeams.isEmpty();
        }

        boolean wantsNews() {
            return !newsFeeds.isEmpty() || !newsFeedsEn.isEmpty();
        }

        /** Weather uses the prayer place, so it needs the same real coordinates (not the prayer card itself). */
        boolean wantsWeather() {
            return showWeather && weather && validPlace(prayerLat, prayerLng);
        }
    }

    /** Current conditions from Open-Meteo: temperature in °C and the WMO weather code. */
    static final class Weather {
        final double tempC;
        final int code;

        Weather(double tempC, int code) {
            this.tempC = tempC;
            this.code = code;
        }

        /** Whole degrees, "31°" (a long, so -0.4 shows as 0, never -0). */
        String temperature() {
            return Math.round(tempC) + "°";
        }
    }

    static final int SKY_UNKNOWN = -1, SKY_CLEAR = 0, SKY_PARTLY = 1, SKY_CLOUDY = 2, SKY_FOG = 3,
            SKY_DRIZZLE = 4, SKY_RAIN = 5, SKY_SNOW = 6, SKY_STORM = 7;

    /** Groups the WMO codes Open-Meteo returns into the few conditions the card names. */
    static int sky(int code) {
        if (code == 0) return SKY_CLEAR;
        if (code == 1 || code == 2) return SKY_PARTLY;
        if (code == 3) return SKY_CLOUDY;
        if (code == 45 || code == 48) return SKY_FOG;
        if (code >= 51 && code <= 57) return SKY_DRIZZLE;
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) return SKY_RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return SKY_SNOW;
        if (code >= 95 && code <= 99) return SKY_STORM;
        return SKY_UNKNOWN;
    }

    static boolean validPlace(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng)) return false;
        if (lat == 0 && lng == 0) return false;
        // Above about 60 degrees the twilight angles stop existing in summer; no TV of ours is there.
        return lat >= -60 && lat <= 60 && lng >= -180 && lng <= 180;
    }

    /** An ESPN league code is used in a URL path: letters, digits, dots and underscores only. */
    static boolean validLeague(String code) {
        return code != null && code.matches("[a-z0-9._]{1,40}");
    }

    /** Feeds must be HTTPS, short and plain; anything else is dropped rather than half-trusted. */
    static boolean validFeed(String url) {
        return url != null && url.length() <= 300 && url.startsWith("https://") && !url.contains(" ")
                && !url.contains("@");
    }

    /** Up to ten favourite clubs: one schedule request each, so the cap keeps a refresh small. */
    static final int MAX_CLUBS = 10;
    static final int CLUB_ROTATE_DEFAULT_S = 5, CLUB_ROTATE_MIN_S = 3, CLUB_ROTATE_MAX_S = 60;
    /** A club can go two weeks without a match over an international break, so look further than a league. */
    static final int CLUB_LOOKAHEAD_DAYS = 21;

    /** An ESPN team id goes into a URL path: digits only. */
    static boolean validTeamId(String id) {
        return id != null && id.matches("[0-9]{1,9}");
    }

    /** Valid, distinct ids in their order, cut at {@link #MAX_CLUBS}; anything else is dropped. */
    static List<String> clubIds(List<String> raw) {
        List<String> out = new java.util.ArrayList<>();
        for (String r : raw) {
            String id = r == null ? "" : r.trim();
            if (validTeamId(id) && !out.contains(id) && out.size() < MAX_CLUBS) out.add(id);
        }
        return out;
    }

    /** The on-TV choice is stored as "929,817,382"; empty or junk gives an empty list. */
    static List<String> clubIds(String joined) {
        if (joined == null || joined.trim().isEmpty()) return Collections.emptyList();
        return clubIds(java.util.Arrays.asList(joined.split(",")));
    }

    /**
     * Matches per club on the club card: the live or next one and the one after, so a continental or cup match
     * two days after a league match is not hidden behind it (Al Hilal: league on Saturday, AFC on Tuesday).
     */
    static final int CLUB_MATCHES = 2;

    /**
     * Up to {@link #CLUB_MATCHES} matches for each club that has something to show, a live one first, then the next
     * kick-offs within {@link #CLUB_LOOKAHEAD_DAYS}; clubs in their order, each club's matches by time. A derby
     * between two favourites shows once.
     */
    static List<Match> pickClubs(List<Match> all, List<String> teamIds, long now) {
        List<Match> out = new java.util.ArrayList<>();
        for (String id : teamIds) {
            List<Match> mine = new java.util.ArrayList<>();
            for (Match m : all) {
                if (!id.equals(m.homeId) && !id.equals(m.awayId)) continue;
                boolean live = Match.LIVE.equals(m.state);
                boolean soon = Match.PRE.equals(m.state) && m.startMillis >= now - 5 * 60_000L
                        && m.startMillis <= now + CLUB_LOOKAHEAD_DAYS * 24 * 3_600_000L;
                if (!live && !soon) continue;
                // A derby between two favourites arrives twice (once from each club's schedule): one copy only, or
                // the copies would take both of the club's places and hide its next match (seen on the TV, 2.9.0).
                boolean dup = false;
                for (Match o : mine) dup |= sameMatch(o, m);
                if (!dup) mine.add(m);
            }
            mine.sort((a, b) -> {
                boolean la = Match.LIVE.equals(a.state), lb = Match.LIVE.equals(b.state);
                if (la != lb) return la ? -1 : 1;
                return Long.compare(a.startMillis, b.startMillis);
            });
            int taken = 0;
            for (Match pick : mine) {
                if (taken == CLUB_MATCHES) break;
                boolean seen = false;
                for (Match o : out) seen |= sameMatch(o, pick);
                if (!seen) out.add(pick);
                taken++;
            }
        }
        return out;
    }

    /** True when either side of the match is one of the favourite clubs, by ESPN id (names differ by language). */
    static boolean involvesFavorite(Match m, List<String> teamIds) {
        return (!m.homeId.isEmpty() && teamIds.contains(m.homeId)) || (!m.awayId.isEmpty() && teamIds.contains(m.awayId));
    }

    /** Left-to-right isolate and its end (U+2066, U+2069): a time or date keeps its order inside an Arabic line. */
    static final char LRI = '\u2066', PDI = '\u2069';

    /**
     * The kick-off line of a match card, short enough for the narrowest card: the full day name, day/month and the
     * clock on the Arabic screen ("Thursday 29/10, 9:17 PM" in Arabic words and digits), "Thu 29 Oct, 9:17 PM" on
     * the English one, with a middle dot between date and clock. The date and the clock sit in left-to-right
     * isolates, so a right-to-left line can never reverse "21:17" or cut it into "...:17"; the AM/PM word stays
     * outside the isolate, where the line's own direction places it. One locale formats every part, so the digits
     * match.
     */
    static String whenText(long millis, Locale loc, java.util.TimeZone tz, boolean twentyFour) {
        boolean ar = "ar".equals(loc.getLanguage());
        String day = format(ar ? "EEEE" : "EEE", loc, tz, millis);
        String date = format(ar ? "d/M" : "d MMM", loc, tz, millis);
        String clock = format(twentyFour ? "H:mm" : "h:mm", loc, tz, millis);
        String marker = twentyFour ? "" : " " + format("a", loc, tz, millis);
        return day + " " + LRI + date + PDI + " \u00B7 " + LRI + clock + PDI + marker;
    }

    private static String format(String pattern, Locale loc, java.util.TimeZone tz, long millis) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(pattern, loc);
        f.setTimeZone(tz);
        return f.format(new java.util.Date(millis));
    }

    /** The same fixture seen from two sources (a club schedule and a scoreboard): competition and both team ids. */
    static boolean sameMatch(Match a, Match b) {
        return !a.homeId.isEmpty() && a.code.equals(b.code) && a.homeId.equals(b.homeId) && a.awayId.equals(b.awayId);
    }

    /**
     * Club schedules are fetched rarely (they are large); a match that is live or about to start is refreshed
     * from that day's scoreboard instead, so the score and minute stay current.
     */
    static boolean nearKickoff(Match m, long now) {
        if (Match.LIVE.equals(m.state)) return true;
        return Match.PRE.equals(m.state) && m.startMillis <= now + 15 * 60_000L
                && m.startMillis >= now - 3 * 3_600_000L;
    }

    /** {@code all} with each match replaced by its fresh copy from {@code fresh}, when there is one. */
    static List<Match> withUpdates(List<Match> all, List<Match> fresh) {
        List<Match> out = new java.util.ArrayList<>(all.size());
        for (Match m : all) {
            Match use = m;
            for (Match f : fresh) {
                if (sameMatch(m, f)) {
                    use = f;
                    break;
                }
            }
            out.add(use);
        }
        return out;
    }

    static final class Match {
        static final String PRE = "pre", LIVE = "in", POST = "post";

        /** ESPN's display name ("Saudi Pro League"), shown on the card. */
        final String league;
        /** The ESPN code it was fetched with ("ksa.1"), used to group sports_cards. */
        final String code;
        /** ESPN team ids, "" when unknown: the key for the Arabic team names (NamesAr). */
        final String homeId;
        final String awayId;
        final String home;
        final String away;
        final String homeScore;
        final String awayScore;
        /** {@link #PRE}, {@link #LIVE} or {@link #POST}. */
        final String state;
        /** The minute while live ("67'"), else empty. */
        final String clock;
        final long startMillis;

        Match(String league, String home, String away, String homeScore, String awayScore, String state,
              String clock, long startMillis) {
            this(league, league, home, away, homeScore, awayScore, state, clock, startMillis);
        }

        Match(String league, String code, String home, String away, String homeScore, String awayScore,
              String state, String clock, long startMillis) {
            this(league, code, "", "", home, away, homeScore, awayScore, state, clock, startMillis);
        }

        Match(String league, String code, String homeId, String awayId, String home, String away, String homeScore,
              String awayScore, String state, String clock, long startMillis) {
            this.league = league;
            this.code = code;
            this.homeId = homeId;
            this.awayId = awayId;
            this.home = home;
            this.away = away;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.state = state;
            this.clock = clock;
            this.startMillis = startMillis;
        }
    }

    static final class Headline {
        final String title;
        final String source;

        Headline(String title, String source) {
            this.title = title;
            this.source = source;
        }
    }

    static boolean wantsTeam(List<String> teams, Match m) {
        if (teams.isEmpty()) return true;
        String h = m.home.toLowerCase(Locale.ROOT), a = m.away.toLowerCase(Locale.ROOT);
        for (String t : teams) {
            String k = t.trim().toLowerCase(Locale.ROOT);
            if (!k.isEmpty() && (h.contains(k) || a.contains(k))) return true;
        }
        return false;
    }

    /**
     * The one match worth the card: a live one first, else the next kick-off within 7 days (a league plays about
     * weekly, so a 24-hour window left the card empty most days). Finished matches are never shown, so the card
     * never passes old data off as current.
     */
    /** {@link #pickMatch} per league, leagues in the order the matches came (the order of sports_leagues). */
    static List<Match> pickMatches(List<Match> all, List<String> teams, long now) {
        java.util.LinkedHashMap<String, List<Match>> byLeague = new java.util.LinkedHashMap<>();
        for (Match m : all) {
            List<Match> l = byLeague.get(m.league);
            if (l == null) byLeague.put(m.league, l = new java.util.ArrayList<>());
            l.add(m);
        }
        List<Match> out = new java.util.ArrayList<>();
        for (List<Match> l : byLeague.values()) {
            Match m = pickMatch(l, teams, now);
            if (m != null) out.add(m);
        }
        return out;
    }

    /** {@link #pickMatch} per sports_cards group, in the cards' order; a group with nothing to show is left out. */
    static List<Match> pickCards(List<Match> all, List<List<String>> cards, List<String> teams, long now) {
        List<Match> out = new java.util.ArrayList<>();
        for (List<String> codes : cards) {
            List<Match> mine = new java.util.ArrayList<>();
            for (Match m : all) if (codes.contains(m.code)) mine.add(m);
            Match pick = pickMatch(mine, teams, now);
            if (pick != null) out.add(pick);
        }
        return out;
    }

    /** Days {@link Feeds} looks ahead, one ESPN request per day, until a match is worth the card. */
    static final int LOOKAHEAD_DAYS = 7;

    /**
     * ESPN's {@code ?dates=} value, yyyyMMdd in UTC, {@code plusDays} after {@code now}. Soccer scoreboards take
     * one day or a month but reject a range ("20261022-20261029" answers HTTP 400), so Feeds walks day by day.
     */
    static String espnDay(long now, int plusDays) {
        return java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                .plusDays(plusDays).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * TheSportsDB league codes, "tsdb.4829": the leagues ESPN does not carry (Egypt 4829, UAE 4678, Qatar 4663,
     * Morocco 4520). Its free API gives the next fixtures but no live scores.
     */
    static final String TSDB_PREFIX = "tsdb.";

    static boolean isTsdb(String code) {
        if (code == null || !code.startsWith(TSDB_PREFIX)) return false;
        String id = code.substring(TSDB_PREFIX.length());
        return id.matches("[0-9]{1,7}");
    }

    /**
     * A TheSportsDB event status as one of ours: not started is {@link Match#PRE}, playing {@link Match#LIVE},
     * finished {@link Match#POST}; postponed, cancelled, abandoned or anything unknown is "", which no card shows.
     */
    static String tsdbState(String status) {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        switch (s) {
            case "":
            case "NS":
            case "TBD":
            case "NOT STARTED":
                return Match.PRE;
            case "1H": case "HT": case "2H": case "ET": case "BT": case "P": case "LIVE": case "INT":
                return Match.LIVE;
            case "FT": case "AET": case "PEN": case "AOT": case "AP": case "MATCH FINISHED":
                return Match.POST;
            default:
                return "";
        }
    }

    /** TheSportsDB's {@code ?d=} value, yyyy-MM-dd in UTC, {@code plusDays} after {@code now}. */
    static String tsdbDay(long now, int plusDays) {
        return java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                .plusDays(plusDays).toString();
    }

    static Match pickMatch(List<Match> all, List<String> teams, long now) {
        Match next = null;
        for (Match m : all) {
            if (!wantsTeam(teams, m)) continue;
            if (Match.LIVE.equals(m.state)) return m;
            if (Match.PRE.equals(m.state) && m.startMillis >= now - 5 * 60_000L
                    && m.startMillis <= now + 7 * 24 * 3_600_000L
                    && (next == null || m.startMillis < next.startMillis)) next = m;
        }
        return next;
    }
}
