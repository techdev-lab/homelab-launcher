package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The launcher's settings: the values in res/values/config.xml, with an optional overlay read from
 * {@code config.json} in the app's external files directory
 * ({@code /sdcard/Android/data/com.gathin.tvhome/files/}). Every key in the JSON is optional and an
 * unknown or badly typed key is ignored, so a theme can change the dock and the look without a rebuild:
 * {@code tools/theme.sh themes/<name>} pushes a theme folder there and restarts Home.
 *
 * Read once in {@link MainActivity#onCreate}; nothing here touches the network.
 */
final class Config {
    private static final String TAG = "TvHome.Config";
    static final String FILE_NAME = "config.json";
    static final String WALLPAPER_NAME = "wallpaper.jpg";
    static final String SETTINGS_NAME = "launcher_settings";
    /** Clubs picked on the TV, "929,817"; kept across theme pushes (they are the viewer's, not the theme's). */
    static final String FAVORITE_TEAMS_KEY = "favorite_teams";
    /** Wallpaper set picked on the TV (Wallpapers modes); a theme push or "Follow theme" clears it. */
    static final String WALLPAPER_MODE_KEY = "wallpaper_mode";
    /** Plain background colour picked on the TV (an ARGB int); cleared with the other look choices. */
    static final String BACKGROUND_COLOR_KEY = "background_color";
    /** Launcher language picked on the TV: "system", "ar" or "en"; wins over config.json "language". */
    static final String LANGUAGE_KEY = "language";
    /** Strip text size picked on the TV (StripSize): kept across theme pushes, like the clubs. */
    static final String TEXT_SIZE_KEY = "text_size";
    /** Which strip cards show, picked on the TV: prayer, weather, football leagues, favourite clubs. */
    static final String[] CARD_KEYS = {"card_prayer", "card_weather", "card_football", "card_clubs"};
    private static final String THEME_REVISION_NAME = "theme_revision";
    private static final String THEME_SIGNATURE_KEY = "theme_signature";

    final List<String> dockApps;
    /** Grid apps fill the dock slots dock_apps leaves free (config.xml, fill_dock); on by default, false leaves the slots empty. */
    final boolean fillDock;
    final List<String> hiddenApps;
    final String liveTvPackage;
    final String liveTvAction;
    final String sourcePanelService;
    final String sourcePanelAction;
    final String inputAction;
    final int columns;
    final boolean animations;
    final boolean advancedEffects;
    final int rowsBelowDock;
    final boolean shamsiDate;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): stiff spring, short fades, no replay. */
    final boolean reducedMotion;
    final String vpnApp;
    final boolean showHomeSettingsShortcut;
    /** Background colour behind everything, #RRGGBB; black unless a theme says otherwise. */
    final int backgroundColor;
    /** Time-of-day backdrop, 0 to 100 (config.xml, sky_intensity); 0 keeps the plain background. */
    final int skyIntensity;
    /** Remote keycodes that open Settings through HomeKeyService (config.xml, settings_key_keycodes). */
    final int[] settingsKeycodes;
    /** The Settings button opens the quick settings panel instead of the TV's Settings (settings_key_panel). */
    final boolean settingsKeyPanel;
    /** Full-screen picture behind the launcher, or null for the plain colour. */
    final File wallpaper;
    /** Whether the overlay file was present and parsed. */
    final boolean overlayApplied;
    /** Festive skin from a theme (festival, festival_until, greeting, accent_color); {@link Festival#NONE} if none. */
    final Festival festival;
    /** Language, Hijri date and the interests strip (language, hijri_date, prayer_*, sports_*, news_feeds, weather). */
    final Interests.Settings interests;
    /**
     * Remote keycode to app package, handled while Home is in front (config.json "app_keys", e.g.
     * {"190": "com.google.android.youtube.tv"}). The Chromecast remote's YouTube button sends 190
     * (KEYCODE_BUTTON_3), which only the disabled stock launcher answered.
     */
    final Map<Integer, String> appKeys;
    /**
     * Pictures to rotate through (config.json "wallpaper_mode": "rotate", files in wallpapers/ next to config.json),
     * sorted by name; empty unless rotating. "plain" shows no picture, "fixed" (or no key) the single wallpaper.jpg.
     */
    final List<File> wallpapers;
    /** Minutes per picture while rotating (config.json "wallpaper_interval_min", 1 to 240, default 5). */
    final int wallpaperMinutes;
    /** The wallpaper mode in force (Wallpapers.mode): plain, fixed, rotate, homelab or serene. */
    final String wallpaperMode;
    /** Text size of the interests strip (StripSize): small, medium or large. */
    final String textSize;

    private Config(List<String> dockApps, boolean fillDock, List<String> hiddenApps, String liveTvPackage,
                   String liveTvAction, String sourcePanelService, String sourcePanelAction, String inputAction,
                   int columns, int rowsBelowDock, boolean animations, boolean advancedEffects,
                   boolean shamsiDate, boolean reducedMotion, String vpnApp,
                   boolean showHomeSettingsShortcut, int backgroundColor, int skyIntensity,
                   int[] settingsKeycodes, boolean settingsKeyPanel, File wallpaper, boolean overlayApplied,
                   Festival festival, Interests.Settings interests, Map<Integer, String> appKeys,
                   List<File> wallpapers, int wallpaperMinutes, String wallpaperMode,
                   String textSize) {
        this.dockApps = Collections.unmodifiableList(dockApps);
        this.fillDock = fillDock;
        this.hiddenApps = Collections.unmodifiableList(hiddenApps);
        this.liveTvPackage = liveTvPackage;
        this.liveTvAction = liveTvAction;
        this.sourcePanelService = sourcePanelService;
        this.sourcePanelAction = sourcePanelAction;
        this.inputAction = inputAction;
        this.columns = columns;
        this.rowsBelowDock = rowsBelowDock;
        this.animations = animations;
        this.advancedEffects = advancedEffects;
        this.shamsiDate = shamsiDate;
        this.reducedMotion = reducedMotion;
        this.vpnApp = vpnApp;
        this.showHomeSettingsShortcut = showHomeSettingsShortcut;
        this.backgroundColor = backgroundColor;
        this.skyIntensity = skyIntensity;
        this.settingsKeycodes = settingsKeycodes;
        this.settingsKeyPanel = settingsKeyPanel;
        this.wallpaper = wallpaper;
        this.overlayApplied = overlayApplied;
        this.festival = festival;
        this.interests = interests;
        this.appKeys = Collections.unmodifiableMap(appKeys);
        this.wallpapers = Collections.unmodifiableList(wallpapers);
        this.wallpaperMinutes = wallpaperMinutes;
        this.wallpaperMode = wallpaperMode;
        this.textSize = textSize;
    }

    /** The folder the theme tools push into; null when external storage is unavailable. */
    static File dir(Context ctx) {
        return ctx.getExternalFilesDir(null);
    }

    static Config load(Context ctx) {
        Resources res = ctx.getResources();
        List<String> dock = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.dock_apps)));
        boolean fillDock = res.getBoolean(R.bool.fill_dock);
        List<String> hidden = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.hidden_apps)));
        String liveTv = res.getString(R.string.live_tv_package).trim();
        String liveTvAction = res.getString(R.string.live_tv_action).trim();
        String panelService = res.getString(R.string.source_panel_service).trim();
        String panelAction = res.getString(R.string.source_panel_action).trim();
        String inputAction = res.getString(R.string.input_action).trim();
        int columns = res.getInteger(R.integer.columns);
        int rowsBelow = res.getInteger(R.integer.rows_below_dock);
        boolean animations = res.getBoolean(R.bool.animations);
        boolean advancedEffects = res.getBoolean(R.bool.advanced_effects);
        boolean shamsi = res.getBoolean(R.bool.shamsi_date);
        boolean reducedMotion = res.getBoolean(R.bool.reduced_motion);
        String vpn = res.getString(R.string.vpn_app).trim();
        boolean homeSettings = res.getBoolean(R.bool.show_home_settings_shortcut);
        int background = ctx.getColor(R.color.theme_ground);
        int skyIntensity = res.getInteger(R.integer.sky_intensity);
        int[] settingsKeycodes = res.getIntArray(R.array.settings_key_keycodes);
        boolean settingsKeyPanel = res.getBoolean(R.bool.settings_key_panel);
        boolean applied = false;

        File dir = dir(ctx);
        File file = dir != null ? new File(dir, FILE_NAME) : null;
        JSONObject json = read(file);
        if (json != null) {
            applied = true;
            dock = stringList(json, "dock_apps", dock);
            fillDock = bool(json, "fill_dock", fillDock);
            hidden = stringList(json, "hidden_apps", hidden);
            liveTv = string(json, "live_tv_package", liveTv);
            liveTvAction = string(json, "live_tv_action", liveTvAction);
            panelService = string(json, "source_panel_service", panelService);
            panelAction = string(json, "source_panel_action", panelAction);
            inputAction = string(json, "input_action", inputAction);
            columns = integer(json, "columns", columns, 1, 8);
            rowsBelow = integer(json, "rows_below_dock", rowsBelow, 0, 3);
            animations = bool(json, "animations", animations);
            advancedEffects = bool(json, "advanced_effects", advancedEffects);
            shamsi = bool(json, "shamsi_date", shamsi);
            reducedMotion = bool(json, "reduced_motion", reducedMotion);
            vpn = string(json, "vpn_app", vpn);
            homeSettings = bool(json, "show_home_settings_shortcut", homeSettings);
            background = color(json, "background_color", background);
            skyIntensity = integer(json, "sky_intensity", skyIntensity, 0, 100);
            settingsKeycodes = intArray(json, "settings_key_keycodes", settingsKeycodes);
            settingsKeyPanel = bool(json, "settings_key_panel", settingsKeyPanel);
        }
        // A theme push or reset invalidates choices made on the TV. On first use, remember the
        // current theme without clearing choices from an older launcher version.
        SharedPreferences choices = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
        syncThemeChoices(choices, dir, file);
        int chosenColumns = choices.getInt("columns", columns);
        if (chosenColumns >= 4 && chosenColumns <= 7) columns = chosenColumns;
        animations = choices.getBoolean("animations", animations);
        advancedEffects = choices.getBoolean("advanced_effects", advancedEffects);
        if (choices.contains(BACKGROUND_COLOR_KEY)) background = 0xFF000000 | choices.getInt(BACKGROUND_COLOR_KEY, background);
        File wallpaper = dir != null ? new File(dir, WALLPAPER_NAME) : null;
        if (wallpaper != null && !(wallpaper.isFile() && wallpaper.length() > 0)) wallpaper = null;
        // A theme can also switch the wallpaper off while the file is still there.
        if (json != null && json.has("wallpaper") && !bool(json, "wallpaper", true)) wallpaper = null;
        String wallMode = Wallpapers.mode(choices.getString(WALLPAPER_MODE_KEY, ""),
                json == null ? "" : string(json, "wallpaper_mode", ""), json != null, wallpaper != null);
        List<File> rotation = new ArrayList<>();
        if (Wallpapers.PLAIN.equals(wallMode)) wallpaper = null;
        else if (Wallpapers.ROTATE.equals(wallMode) && dir != null) rotation = Wallpapers.list(new File(dir, Wallpapers.DIR_NAME));
        else if (Wallpapers.builtIn(wallMode)) rotation = BuiltInWallpapers.list(ctx, wallMode);
        int wallMinutes = json == null ? 5 : integer(json, "wallpaper_interval_min", 5, 1, 240);
        // A festive skin comes only from a theme; config.xml has no festival.
        Festival festival = json == null ? Festival.NONE : Festival.parse(string(json, "festival", ""),
                string(json, "festival_until", ""), string(json, "greeting", ""), string(json, "accent_color", ""));

        return new Config(dock, fillDock, hidden, liveTv, liveTvAction, panelService, panelAction, inputAction,
                Math.max(1, columns), Math.max(0, rowsBelow), animations, advancedEffects,
                shamsi, reducedMotion, vpn, homeSettings, background,
                Math.max(0, Math.min(100, skyIntensity)), settingsKeycodes, settingsKeyPanel, wallpaper, applied,
                festival, interests(json, choices), appKeys(json), rotation, wallMinutes, wallMode,
                StripSize.pick(choices.getString(TEXT_SIZE_KEY, ""), json == null ? "" : string(json, "text_size", "")));
    }

    /** Default: the YouTube button. A theme's "app_keys" object replaces the whole map ({} turns it off). */
    private static Map<Integer, String> appKeys(JSONObject json) {
        Map<Integer, String> keys = new HashMap<>();
        keys.put(190, "com.google.android.youtube.tv");
        Object v = json == null ? null : json.opt("app_keys");
        if (!(v instanceof JSONObject)) return keys;
        JSONObject o = (JSONObject) v;
        Map<Integer, String> out = new HashMap<>();
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String k = it.next();
            Object pkg = o.opt(k);
            try {
                int code = Integer.parseInt(k.trim());
                if (code > 0 && pkg instanceof String && ((String) pkg).matches("[A-Za-z0-9_.]{3,150}")) {
                    out.put(code, (String) pkg);
                }
            } catch (NumberFormatException ignored) {
                // not a keycode: skip this entry
            }
        }
        return out;
    }

    /** The interests keys; absent or bad values switch that part off. Nothing here fetches anything. */
    private static Interests.Settings interests(JSONObject json, SharedPreferences choices) {
        // Clubs picked on the TV (LauncherSettings > Favourite clubs) win over the theme's favorite_teams.
        List<String> clubs = choices.contains(FAVORITE_TEAMS_KEY)
                ? Interests.clubIds(choices.getString(FAVORITE_TEAMS_KEY, ""))
                : json == null ? new ArrayList<>() : Interests.clubIds(idList(json, "favorite_teams"));
        if (!Feeds.AVAILABLE) clubs = new ArrayList<>();
        int rotate = json == null ? Interests.CLUB_ROTATE_DEFAULT_S : integer(json, "club_rotate_seconds",
                Interests.CLUB_ROTATE_DEFAULT_S, Interests.CLUB_ROTATE_MIN_S, Interests.CLUB_ROTATE_MAX_S);
        if (json == null) {
            if (clubs.isEmpty()) return Interests.Settings.NONE;
            return new Interests.Settings("system", false, "", "", Double.NaN, Double.NaN, new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Feeds.AVAILABLE, new ArrayList<>(),
                    clubs, rotate).withCards(true, true, true, choices.getBoolean(CARD_KEYS[3], true));
        }
        String lang = choices.getString(LANGUAGE_KEY, "");
        if (!"ar".equals(lang) && !"en".equals(lang) && !"system".equals(lang)) lang = language(json);
        return new Interests.Settings(lang, bool(json, "hijri_date", false),
                string(json, "prayer_city", ""), string(json, "prayer_city_en", ""),
                number(json, "prayer_lat"), number(json, "prayer_lng"),
                stringList(json, "sports_leagues", new ArrayList<>()),
                stringList(json, "sports_teams", new ArrayList<>()),
                stringList(json, "news_feeds", new ArrayList<>()),
                stringList(json, "news_feeds_en", new ArrayList<>()),
                // The offline build has no network, so the key cannot switch it on there.
                Feeds.AVAILABLE && bool(json, "weather", true),
                leagueGroups(json, "sports_cards"), clubs, rotate).withCards(
                choices.getBoolean(CARD_KEYS[0], true), choices.getBoolean(CARD_KEYS[1], true),
                choices.getBoolean(CARD_KEYS[2], true), choices.getBoolean(CARD_KEYS[3], true));
    }

    /** Team ids may be written as strings ("929") or numbers (929); both are read as text. */
    private static List<String> idList(JSONObject o, String key) {
        JSONArray a = o.optJSONArray(key);
        List<String> out = new ArrayList<>();
        for (int i = 0; a != null && i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String) out.add(((String) item).trim());
            else if (item instanceof Integer || item instanceof Long) out.add(String.valueOf(item));
        }
        return out;
    }

    /** "sports_cards": up to 4 groups of up to 8 valid league codes; bad entries are dropped, empty groups too. */
    static List<List<String>> leagueGroups(JSONObject o, String key) {
        List<List<String>> out = new ArrayList<>();
        JSONArray a = o.optJSONArray(key);
        for (int i = 0; a != null && i < a.length() && out.size() < 4; i++) {
            JSONArray g = a.optJSONArray(i);
            List<String> codes = new ArrayList<>();
            for (int j = 0; g != null && j < g.length() && codes.size() < 8; j++) {
                String c = g.optString(j, "").trim();
                if (Interests.validLeague(c) && !codes.contains(c)) codes.add(c);
            }
            if (!codes.isEmpty()) out.add(codes);
        }
        return out;
    }

    private static String language(JSONObject json) {
        String l = string(json, "language", "system");
        return "ar".equals(l) || "en".equals(l) ? l : "system";
    }

    /**
     * Only the language, read before the activity has its resources (MainActivity.attachBaseContext), so the
     * whole screen, layout-ar included, comes up in that language. "system" leaves the TV's language.
     */
    static String language(Context ctx) {
        String chosen = ctx.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, "");
        if ("ar".equals(chosen) || "en".equals(chosen) || "system".equals(chosen)) return chosen;
        File dir = dir(ctx);
        JSONObject json = read(dir != null ? new File(dir, FILE_NAME) : null);
        return json == null ? "system" : language(json);
    }

    private static double number(JSONObject o, String key) {
        Object v = o.opt(key);
        return v instanceof Number ? ((Number) v).doubleValue() : Double.NaN;
    }

    private static void syncThemeChoices(SharedPreferences choices, File dir, File config) {
        File revision = dir == null ? null : new File(dir, THEME_REVISION_NAME);
        String token = revisionToken(revision);
        String signature = fileStamp(config) + ":" + token;
        String seen = choices.getString(THEME_SIGNATURE_KEY, null);
        if (seen == null) {
            // Existing preferences survive an app upgrade, unless the first launch follows an
            // explicit theme push or reset by the updated tools.
            SharedPreferences.Editor editor = choices.edit();
            if (!"absent".equals(token)) editor.remove("columns").remove("animations")
                    .remove("advanced_effects").remove(WALLPAPER_MODE_KEY).remove(BACKGROUND_COLOR_KEY);
            editor.putString(THEME_SIGNATURE_KEY, signature).commit();
        } else if (!seen.equals(signature)) {
            choices.edit().remove("columns").remove("animations").remove("advanced_effects").remove(WALLPAPER_MODE_KEY).remove(BACKGROUND_COLOR_KEY)
                    .putString(THEME_SIGNATURE_KEY, signature).commit();
        }
    }

    private static String fileStamp(File file) {
        return file != null && file.isFile() ? file.length() + "@" + file.lastModified() : "absent";
    }

    private static String revisionToken(File file) {
        if (file == null || !file.isFile() || file.length() > 64) return "absent";
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int n = in.read(bytes);
            return n > 0 ? new String(bytes, 0, n, StandardCharsets.US_ASCII) : "absent";
        } catch (Exception ignored) {
            return "absent";
        }
    }

    /** Reads and parses the overlay; any problem means "no overlay", never a crash on the home screen. */
    private static JSONObject read(File file) {
        if (file == null || !file.isFile()) return null;
        if (file.length() > 64 * 1024) {
            Log.w(TAG, "ignoring " + file + ": larger than 64 KB");
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            String text = new String(buf, 0, n, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);   // Windows editors add a BOM
            return new JSONObject(text);
        } catch (Exception e) {
            Log.w(TAG, "ignoring " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String string(JSONObject o, String key, String fallback) {
        Object v = o.opt(key);
        return v instanceof String ? ((String) v).trim() : fallback;
    }

    private static boolean bool(JSONObject o, String key, boolean fallback) {
        Object v = o.opt(key);
        return v instanceof Boolean ? (Boolean) v : fallback;
    }

    private static int integer(JSONObject o, String key, int fallback, int min, int max) {
        Object v = o.opt(key);
        if (!(v instanceof Number)) return fallback;
        int i = ((Number) v).intValue();
        return i < min || i > max ? fallback : i;
    }

    private static int color(JSONObject o, String key, int fallback) {
        Object v = o.opt(key);
        if (!(v instanceof String)) return fallback;
        try {
            int c = Color.parseColor(((String) v).trim());
            return c | 0xFF000000;   // always opaque: the window behind is black anyway
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Positive keycodes only (vendor remotes use codes in the thousands); a list with anything else in it is ignored as a whole. */
    private static int[] intArray(JSONObject o, String key, int[] fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        int[] out = new int[a.length()];
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (!(item instanceof Number)) return fallback;
            out[i] = ((Number) item).intValue();
            if (out[i] < 1) return fallback;
        }
        return out;
    }

    private static List<String> stringList(JSONObject o, String key, List<String> fallback) {
        Object v = o.opt(key);
        if (!(v instanceof JSONArray)) return fallback;
        JSONArray a = (JSONArray) v;
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            Object item = a.opt(i);
            if (item instanceof String && !((String) item).trim().isEmpty()) out.add(((String) item).trim());
        }
        return out;
    }
}
