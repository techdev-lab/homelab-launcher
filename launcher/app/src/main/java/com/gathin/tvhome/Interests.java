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
            return validPlace(prayerLat, prayerLng);
        }

        boolean wantsMatches() {
            return !sportsLeagues.isEmpty() || !sportsCards.isEmpty();
        }

        /** Every league to fetch once: sports_leagues, then the ones only named in sports_cards. */
        List<String> leaguesToFetch() {
            java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>(sportsLeagues);
            for (List<String> card : sportsCards) all.addAll(card);
            return new java.util.ArrayList<>(all);
        }

        boolean wantsNews() {
            return !newsFeeds.isEmpty() || !newsFeedsEn.isEmpty();
        }

        /** Weather uses the prayer place, so it needs the same real coordinates. */
        boolean wantsWeather() {
            return weather && prayer();
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
