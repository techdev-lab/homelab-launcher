package com.gathin.tvhome;

import android.util.Log;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/**
 * Online build only: matches from ESPN's public scoreboard and team schedule JSON (and, for the leagues ESPN lacks,
 * TheSportsDB's free API), headlines from the RSS/Atom feeds in
 * config.json and the current weather from Open-Meteo. HTTPS only, no redirects followed, short timeouts, capped sizes, plain parsers: no WebView, no
 * SDK, no proxy. Called off the main thread; any failure returns an empty list so the card simply hides.
 *
 * ESPN's endpoint is public but undocumented: if it changes, matches disappear and nothing else breaks.
 */
final class Feeds {
    static final boolean AVAILABLE = true;

    private static final String TAG = "TvHome.Feeds";
    private static final String ESPN = "https://site.api.espn.com/apis/site/v2/sports/soccer/";
    private static final String OPEN_METEO = "https://api.open-meteo.com/v1/forecast";
    /** TheSportsDB v1 with its documented public key "123" (not a secret): next fixtures, no live scores. */
    private static final String TSDB = "https://www.thesportsdb.com/api/v1/json/123/";
    /**
     * The free key documents 30 requests a minute, but answered 429 at one every 2.2 s in a test on 2026-10-28:
     * at least 4 s between two of ours, and after a 429 no call for {@link #TSDB_BACKOFF_MS}.
     */
    private static final long TSDB_GAP_MS = 4_000;
    private static final long TSDB_BACKOFF_MS = 10 * 60_000L;
    private static long tsdbBlockedUntil;
    /** Fixtures change slowly and the free API has no live scores: a league is fetched at most once an hour. */
    private static final long TSDB_REFRESH_MS = 60 * 60_000L;
    private static long lastTsdbCall;
    private static final java.util.Map<String, List<Interests.Match>> tsdbCache = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> tsdbFetched = new java.util.HashMap<>();
    private static final int TIMEOUT_MS = 8000;
    /** A big club's schedule for the season is about 320 KB; a scoreboard or feed is far smaller. */
    private static final int MAX_BYTES = 768 * 1024;
    private static final int MAX_HEADLINES_PER_FEED = 5;

    private Feeds() {
    }

    static List<Interests.Match> matches(Interests.Settings s) {
        List<Interests.Match> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String league : s.leaguesToFetch()) {
            if (!Interests.validLeague(league)) continue;
            if (Interests.isTsdb(league)) {
                out.addAll(tsdbLeague(league, s.sportsTeams, now));
                continue;
            }
            // The plain scoreboard is only the current matchday: once it has finished the card went empty for
            // the rest of the week. Walk forward one day at a time until there is a match to show.
            List<Interests.Match> got = new ArrayList<>();
            for (int day = -1; day <= Interests.LOOKAHEAD_DAYS; day++) {
                String url = ESPN + league + "/scoreboard" + (day < 0 ? "" : "?dates=" + Interests.espnDay(now, day));
                try {
                    got.addAll(scoreboard(get(url), league));
                } catch (Exception e) {
                    Log.w(TAG, "matches " + league + " " + (day < 0 ? "now" : "+" + day + "d") + ": " + e);
                    break;
                }
                if (Interests.pickMatch(got, s.sportsTeams, now) != null) break;
            }
            out.addAll(got);
        }
        return out;
    }

    /**
     * Every listed club's coming fixtures in all competitions: ESPN's "all" team schedule names each event's
     * league, so a club's league, cup and continental matches come from one request per club.
     */
    static List<Interests.Match> clubSchedules(List<String> teamIds) {
        List<Interests.Match> out = new ArrayList<>();
        for (String id : Interests.clubIds(teamIds)) {
            try {
                out.addAll(schedule(get(ESPN + "all/teams/" + id + "/schedule?fixture=true")));
            } catch (Exception e) {
                Log.w(TAG, "club " + id + ": " + e);
            }
        }
        return out;
    }

    /**
     * The club matches that are live or about to start, refreshed from their competition's scoreboard for that
     * day (small, and it carries the score and the minute). One request per competition and day; a failure keeps
     * the schedule's copy.
     */
    static List<Interests.Match> refreshLive(List<Interests.Match> all, long now) {
        java.util.LinkedHashSet<String> boards = new java.util.LinkedHashSet<>();
        for (Interests.Match m : all) {
            if (Interests.nearKickoff(m, now) && Interests.validLeague(m.code)) {
                boards.add(m.code + "/scoreboard?dates=" + Interests.espnDay(m.startMillis, 0));
                // ESPN files a day by US Eastern time: a kick-off just after midnight UTC sits on the day before.
                boards.add(m.code + "/scoreboard?dates=" + Interests.espnDay(m.startMillis - 5 * 3_600_000L, 0));
            }
        }
        if (boards.isEmpty()) return all;
        List<Interests.Match> fresh = new ArrayList<>();
        for (String board : boards) {
            String code = board.substring(0, board.indexOf('/'));
            try {
                fresh.addAll(scoreboard(get(ESPN + board), code));
            } catch (Exception e) {
                Log.w(TAG, "live " + code + ": " + e);
            }
        }
        return Interests.withUpdates(all, fresh);
    }

    /**
     * The coming fixtures of a TheSportsDB league: the next event, and when that one is postponed or missing, the
     * league's days one by one (at most {@link Interests#LOOKAHEAD_DAYS}) until one is worth the card. Cached for an
     * hour; a failure keeps the last good copy, or nothing.
     */
    private static List<Interests.Match> tsdbLeague(String code, List<String> teams, long now) {
        Long at = tsdbFetched.get(code);
        List<Interests.Match> cached = tsdbCache.get(code);
        if (at != null && cached != null && now - at < TSDB_REFRESH_MS) return cached;
        String id = code.substring(Interests.TSDB_PREFIX.length());
        List<Interests.Match> got = new ArrayList<>();
        try {
            got.addAll(tsdbEvents(tsdbGet(TSDB + "eventsnextleague.php?id=" + id), code));
            for (int day = 0; Interests.pickMatch(got, teams, now) == null && day <= Interests.LOOKAHEAD_DAYS; day++) {
                got.addAll(tsdbEvents(tsdbGet(TSDB + "eventsday.php?d=" + Interests.tsdbDay(now, day) + "&l=" + id), code));
            }
        } catch (Exception e) {
            Log.w(TAG, "league " + code + ": " + e);
            return cached != null ? cached : Collections.emptyList();
        }
        tsdbCache.put(code, got);
        tsdbFetched.put(code, now);
        return got;
    }

    /** One request to TheSportsDB, spaced {@link #TSDB_GAP_MS} after the last, so the free limit is never hit. */
    private static synchronized byte[] tsdbGet(String url) throws Exception {
        if (System.currentTimeMillis() < tsdbBlockedUntil) throw new IOException("rate limited, waiting");
        long wait = lastTsdbCall + TSDB_GAP_MS - System.currentTimeMillis();
        if (wait > 0) Thread.sleep(wait);
        try {
            return get(url);
        } catch (IOException e) {
            if (String.valueOf(e.getMessage()).contains("HTTP 429")) {
                tsdbBlockedUntil = System.currentTimeMillis() + TSDB_BACKOFF_MS;
            }
            throw e;
        } finally {
            lastTsdbCall = System.currentTimeMillis();
        }
    }

    /** {"events": [...]} from TheSportsDB; times are UTC ("strTimestamp": "2026-10-29T14:00:00"). */
    private static List<Interests.Match> tsdbEvents(byte[] body, String code) throws Exception {
        List<Interests.Match> out = new ArrayList<>();
        JSONArray events = new JSONObject(new String(body, StandardCharsets.UTF_8)).optJSONArray("events");
        for (int i = 0; events != null && i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            String home = e.optString("strHomeTeam", ""), away = e.optString("strAwayTeam", "");
            String homeId = e.optString("idHomeTeam", ""), awayId = e.optString("idAwayTeam", "");
            if (home.isEmpty() || away.isEmpty() || !homeId.matches("[0-9]{1,9}") || !awayId.matches("[0-9]{1,9}")) continue;
            String stamp = e.optString("strTimestamp", "");
            if (stamp.isEmpty()) stamp = e.optString("dateEvent", "") + "T" + e.optString("strTime", "");
            long start;
            try {
                start = Instant.parse(stamp.endsWith("Z") ? stamp : stamp + "Z").toEpochMilli();
            } catch (Exception ex) {
                continue;
            }
            String state = Interests.tsdbState(e.optString("strStatus", ""));
            String hs = e.isNull("intHomeScore") ? "" : e.optString("intHomeScore", "");
            String as = e.isNull("intAwayScore") ? "" : e.optString("intAwayScore", "");
            // Team ids get a "t" so they never collide with ESPN ids in names_ar.json or the favourite clubs.
            out.add(new Interests.Match(e.optString("strLeague", code), code, "t" + homeId, "t" + awayId, home, away,
                    hs, as, state, "", start));
        }
        return out;
    }

    private static List<Interests.Match> schedule(byte[] body) throws Exception {
        List<Interests.Match> out = new ArrayList<>();
        JSONArray events = new JSONObject(new String(body, StandardCharsets.UTF_8)).optJSONArray("events");
        for (int i = 0; events != null && i < events.length(); i++) {
            JSONObject ev = events.optJSONObject(i);
            JSONObject lg = ev != null ? ev.optJSONObject("league") : null;
            String code = lg != null ? lg.optString("slug", "") : "";
            if (!Interests.validLeague(code)) continue;
            Interests.Match m = match(ev, lg.optString("name", code), code);
            if (m != null) out.add(m);
        }
        return out;
    }

    private static List<Interests.Match> scoreboard(byte[] body, String league) throws Exception {
        List<Interests.Match> out = new ArrayList<>();
        JSONObject root = new JSONObject(new String(body, StandardCharsets.UTF_8));
        JSONArray events = root.optJSONArray("events");
        String leagueName = league;
        JSONArray leagues = root.optJSONArray("leagues");
        if (leagues != null && leagues.length() > 0) {
            leagueName = leagues.optJSONObject(0).optString("name", league);
        }
        for (int i = 0; events != null && i < events.length(); i++) {
            Interests.Match m = match(events.optJSONObject(i), leagueName, league);
            if (m != null) out.add(m);
        }
        return out;
    }

    private static Interests.Match match(JSONObject ev, String league, String code) {
        if (ev == null) return null;
        JSONObject comp = ev.optJSONArray("competitions") != null
                ? ev.optJSONArray("competitions").optJSONObject(0) : null;
        JSONArray sides = comp != null ? comp.optJSONArray("competitors") : null;
        if (sides == null || sides.length() != 2) return null;
        String home = "", away = "", hs = "", as = "", homeId = "", awayId = "";
        for (int i = 0; i < 2; i++) {
            JSONObject c = sides.optJSONObject(i);
            if (c == null) return null;
            JSONObject team = c.optJSONObject("team");
            String name = team != null ? team.optString("shortDisplayName", team.optString("displayName", "")) : "";
            String id = team != null ? team.optString("id", "") : "";
            if ("home".equals(c.optString("homeAway"))) {
                home = name;
                homeId = id;
                hs = score(c);
            } else {
                away = name;
                awayId = id;
                as = score(c);
            }
        }
        // A scoreboard puts the status on the event, a team schedule on the competition.
        JSONObject status = ev.optJSONObject("status");
        if (status == null && comp != null) status = comp.optJSONObject("status");
        JSONObject type = status != null ? status.optJSONObject("type") : null;
        String state = type != null ? type.optString("state", "") : "";
        String clock = status != null && Interests.Match.LIVE.equals(state) ? status.optString("displayClock", "") : "";
        long start;
        try {
            // ESPN writes "2026-10-22T18:00Z" without seconds, which Instant.parse rejects.
            String date = ev.optString("date", "");
            if (date.length() == 17 && date.endsWith("Z")) date = date.substring(0, 16) + ":00Z";
            start = Instant.parse(date).toEpochMilli();
        } catch (Exception e) {
            return null;
        }
        if (home.isEmpty() || away.isEmpty()) return null;
        return new Interests.Match(league, code, homeId, awayId, home, away, hs, as, state, clock, start);
    }

    /** A scoreboard gives the score as "2", a team schedule as {"displayValue": "2", ...}. */
    private static String score(JSONObject competitor) {
        Object v = competitor.opt("score");
        if (v instanceof JSONObject) return ((JSONObject) v).optString("displayValue", "");
        return v instanceof String ? (String) v : "";
    }

    static List<Interests.Headline> headlines(List<String> feeds) {
        List<Interests.Headline> out = new ArrayList<>();
        for (String url : feeds) {
            if (!Interests.validFeed(url)) continue;
            try {
                out.addAll(rss(get(url), host(url)));
            } catch (Exception e) {
                Log.w(TAG, "news " + host(url) + ": " + e);
            }
        }
        return out;
    }

    /**
     * Current temperature and WMO weather code from Open-Meteo (free, no key) for the prayer place; null on any
     * failure so the card hides. Coordinates go out at 2 decimals (about 1 km), enough for a city's weather.
     */
    static Interests.Weather weather(double lat, double lng) {
        if (!Interests.validPlace(lat, lng)) return null;
        String url = String.format(Locale.ROOT, "%s?latitude=%.2f&longitude=%.2f"
                + "&current=temperature_2m,weather_code&temperature_unit=celsius", OPEN_METEO, lat, lng);
        try {
            JSONObject cur = new JSONObject(new String(get(url), StandardCharsets.UTF_8)).optJSONObject("current");
            if (cur == null) return null;
            double t = cur.optDouble("temperature_2m", Double.NaN);
            if (Double.isNaN(t) || t < -90 || t > 70) return null;
            return new Interests.Weather(t, cur.optInt("weather_code", -1));
        } catch (Exception e) {
            Log.w(TAG, "weather: " + e);
            return null;
        }
    }

    /** RSS {@code <item><title>} or Atom {@code <entry><title>}, the first few of each feed. */
    private static List<Interests.Headline> rss(byte[] body, String source) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        p.setInput(new java.io.ByteArrayInputStream(body), null);
        List<Interests.Headline> out = new ArrayList<>();
        boolean inItem = false;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT && out.size() < MAX_HEADLINES_PER_FEED;
             ev = p.next()) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                if ("item".equals(n) || "entry".equals(n)) inItem = true;
                else if (inItem && "title".equals(n)) {
                    String t = p.nextText().replaceAll("\\s+", " ").trim();
                    if (!t.isEmpty()) out.add(new Interests.Headline(t, source));
                    inItem = false;
                }
            }
        }
        return out.isEmpty() ? Collections.emptyList() : out;
    }

    private static String host(String url) {
        try {
            return new URL(url).getHost().replaceFirst("^(www|feeds)\\.", "");
        } catch (Exception e) {
            return "";
        }
    }

    private static byte[] get(String url) throws IOException {
        HttpsURLConnection c = (HttpsURLConnection) new URL(url).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("User-Agent", "tvhome");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                for (int r; (r = in.read(chunk)) > 0; ) {
                    if (buf.size() + r > MAX_BYTES) throw new IOException("larger than " + MAX_BYTES);
                    buf.write(chunk, 0, r);
                }
                return buf.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }
}
