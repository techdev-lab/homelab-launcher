package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private TextView followThemeChoice;
    private TextView clubsChoice;
    private static final int CLUBS_REQUEST = 1;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;
    private String language;
    private String wallMode;
    private int backgroundColor;
    private boolean[] cards;
    private String textSize;
    /** Look options (docs/LOOK_OPTIONS.md): the plate behind the dock, the tile corners and the header clock. */
    private String dockStyle, tileCorners, clockStyle;
    private String prayerMethod;
    private int asrFactor;
    private TextView prayerMethodChoice;
    private float density;

    /** One row of choices: the views in screen order and the value each one stands for. */
    private final List<TextView> languageViews = new ArrayList<>(), wallViews = new ArrayList<>(),
            colourViews = new ArrayList<>(), cardViews = new ArrayList<>(), motionViews = new ArrayList<>(),
            textSizeViews = new ArrayList<>(), asrViews = new ArrayList<>(), dockStyleViews = new ArrayList<>(),
            tileCornersViews = new ArrayList<>(), clockStyleViews = new ArrayList<>();

    private static final String[] LANGUAGES = {"system", "ar", "en"};
    private static final String[] WALL_SETS = {Wallpapers.HOMELAB, Wallpapers.SERENE, Wallpapers.KIDS};
    private static final String MOTION_CALM = "calm", MOTION_LIVELY = "lively", MOTION_OFF = "off";
    private static final String[] MOTIONS = {MOTION_CALM, MOTION_LIVELY, MOTION_OFF};

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Language.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        density = getResources().getDisplayMetrics().density;
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        // The language in force: the TV choice or config.json, else the app language Android keeps for the launcher.
        String inForce = Language.pick(Config.language(this), Language.appLocaleTag(this));
        language = "ar".equals(inForce) || "en".equals(inForce) ? inForce : "system";
        wallMode = config.wallpaperMode;
        backgroundColor = config.backgroundColor;
        Interests.Settings s = config.interests;
        cards = new boolean[]{s.showPrayer, s.showWeather, s.showSports, s.showClubs};
        textSize = config.textSize;
        dockStyle = config.dockStyle;
        tileCorners = config.tileCorners;
        clockStyle = config.clockStyle;
        prayerMethod = s.prayerMethodKey;
        asrFactor = s.asrFactor;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
            });
        }
        // Keep the numeric choices increasing in the reading direction. Reorder the actual views so D-pad
        // focus follows their positions without changing the saved values.
        if (rtl()) {
            LinearLayout row = (LinearLayout) columnChoices[0].getParent();
            row.removeAllViews();
            for (int i = columnChoices.length - 1; i >= 0; i--) row.addView(columnChoices[i]);
        }

        int[] languageNames = {R.string.language_system, R.string.language_arabic, R.string.language_english};
        for (int i = 0; i < LANGUAGES.length; i++) {
            final String tag = LANGUAGES[i];
            languageViews.add(chip(getString(languageNames[i]), v -> setLanguage(tag)));
        }
        fill(R.id.languageRow, languageViews);

        int[] wallNames = {R.string.wallpapers_homelab, R.string.wallpapers_serene, R.string.wallpapers_kids};
        for (int i = 0; i < WALL_SETS.length; i++) {
            final String mode = WALL_SETS[i];
            wallViews.add(chip(getString(wallNames[i]), v -> {
                wallMode = mode;
                choices.edit().putString(Config.WALLPAPER_MODE_KEY, mode).apply();
                changed();
            }));
        }
        fill(R.id.wallpaperRow, wallViews);

        String[] colourNames = getResources().getStringArray(R.array.plain_colour_names);
        for (int i = 0; i < Wallpapers.PLAIN_COLOURS.length; i++) {
            final int colour = Wallpapers.PLAIN_COLOURS[i];
            TextView c = chip(colourNames[i], v -> {
                wallMode = Wallpapers.PLAIN;
                backgroundColor = colour;
                choices.edit().putString(Config.WALLPAPER_MODE_KEY, Wallpapers.PLAIN)
                        .putInt(Config.BACKGROUND_COLOR_KEY, colour).apply();
                changed();
            });
            c.setBackground(swatch(colour));
            c.setTextSize(14);
            colourViews.add(c);
        }
        fill(R.id.colourRow, colourViews);

        int[] cardNames = {R.string.card_prayer, R.string.card_weather, R.string.card_football, R.string.card_clubs};
        for (int i = 0; i < Config.CARD_KEYS.length; i++) {
            final int index = i;
            TextView c = chip(getString(cardNames[i]), v -> {
                cards[index] = !cards[index];
                choices.edit().putBoolean(Config.CARD_KEYS[index], cards[index]).apply();
                changed();
            });
            // Weather, football and clubs come from the network: the offline build has none of them.
            if (i > 0 && !Feeds.AVAILABLE) c.setVisibility(android.view.View.GONE);
            cardViews.add(c);
        }
        fill(R.id.cardsRow, cardViews);

        int[] sizeNames = {R.string.text_size_small, R.string.text_size_medium, R.string.text_size_large};
        for (int i = 0; i < StripSize.ALL.length; i++) {
            final String size = StripSize.ALL[i];
            textSizeViews.add(chip(getString(sizeNames[i]), v -> {
                if (size.equals(textSize)) return;
                textSize = size;
                // The viewer's choice, like the clubs: a theme push does not clear it.
                choices.edit().putString(Config.TEXT_SIZE_KEY, size).apply();
                changed();
            }));
        }
        fill(R.id.textSizeRow, textSizeViews);

        // The three look options: the viewer's choices, like the text size, so a theme push does not clear them.
        int[] dockNames = {R.string.dock_style_band, R.string.dock_style_floating};
        for (int i = 0; i < DockStyle.ALL.length; i++) {
            final String style = DockStyle.ALL[i];
            dockStyleViews.add(chip(getString(dockNames[i]), v -> {
                if (style.equals(dockStyle)) return;
                dockStyle = style;
                choices.edit().putString(Config.DOCK_STYLE_KEY, style).apply();
                changed();
            }));
        }
        fill(R.id.dockStyleRow, dockStyleViews);

        int[] cornerNames = {R.string.tile_corners_normal, R.string.tile_corners_soft};
        for (int i = 0; i < TileShape.ALL.length; i++) {
            final String shape = TileShape.ALL[i];
            tileCornersViews.add(chip(getString(cornerNames[i]), v -> {
                if (shape.equals(tileCorners)) return;
                tileCorners = shape;
                choices.edit().putString(Config.TILE_CORNERS_KEY, shape).apply();
                changed();
            }));
        }
        fill(R.id.tileCornersRow, tileCornersViews);

        int[] clockNames = {R.string.clock_style_plain, R.string.clock_style_glass};
        for (int i = 0; i < ClockStyle.ALL.length; i++) {
            final String style = ClockStyle.ALL[i];
            clockStyleViews.add(chip(getString(clockNames[i]), v -> {
                if (style.equals(clockStyle)) return;
                clockStyle = style;
                choices.edit().putString(Config.CLOCK_STYLE_KEY, style).apply();
                changed();
            }));
        }
        fill(R.id.clockStyleRow, clockStyleViews);

        prayerMethodChoice = findViewById(R.id.prayerMethodChoice);
        prayerMethodChoice.setOnClickListener(v -> choosePrayerMethod());
        int[] asrNames = {R.string.asr_standard, R.string.asr_hanafi};
        int[] asrValues = {Prayer.ASR_STANDARD, Prayer.ASR_HANAFI};
        for (int i = 0; i < asrNames.length; i++) {
            final int factor = asrValues[i];
            asrViews.add(chip(getString(asrNames[i]), v -> {
                if (factor == asrFactor) return;
                asrFactor = factor;
                // Like the clubs: the viewer's choice, kept when a theme is pushed.
                choices.edit().putString(Config.ASR_KEY, factor == Prayer.ASR_HANAFI ? "hanafi" : "standard").apply();
                changed();
            }));
        }
        fill(R.id.asrRow, asrViews);

        int[] motionNames = {R.string.motion_calm, R.string.motion_lively, R.string.motion_off};
        for (int i = 0; i < MOTIONS.length; i++) {
            final String motion = MOTIONS[i];
            motionViews.add(chip(getString(motionNames[i]), v -> setMotion(motion)));
        }
        fill(R.id.motionRow, motionViews);

        clubsChoice = findViewById(R.id.clubsChoice);
        // Matches need the network: the offline build has no INTERNET, so the row would promise nothing.
        clubsChoice.setVisibility(Feeds.AVAILABLE ? android.view.View.VISIBLE : android.view.View.GONE);
        clubsChoice.setOnClickListener(v ->
                startActivityForResult(new Intent(this, ClubsActivity.class), CLUBS_REQUEST));
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove("animations").remove("advanced_effects")
                    .remove(Config.WALLPAPER_MODE_KEY).remove(Config.BACKGROUND_COLOR_KEY).apply();
            Config current = Config.load(this);
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            wallMode = current.wallpaperMode;
            backgroundColor = current.backgroundColor;
            changed();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        refresh();
        languageViews.get(indexOf(LANGUAGES, language)).requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CLUBS_REQUEST && resultCode == RESULT_OK) changed();
    }

    /**
     * The launcher's language. Saved for the launcher (it wins over config.json) and handed to Android as the app's
     * own language, which also restarts this screen in it. "system" clears both and follows the TV again.
     */
    private void setLanguage(String tag) {
        if (tag.equals(language)) return;
        language = tag;
        choices.edit().putString(Config.LANGUAGE_KEY, tag).apply();
        setResult(RESULT_OK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager manager = getSystemService(LocaleManager.class);
            if (manager != null) {
                manager.setApplicationLocales("system".equals(tag) ? LocaleList.getEmptyLocaleList()
                        : LocaleList.forLanguageTags(tag));
                return;
            }
        }
        recreate();
    }

    private void setMotion(String motion) {
        if (motion.equals(motion())) return;
        if (MOTION_LIVELY.equals(motion)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.motion_lively)
                    .setMessage(R.string.advanced_effects_warning)
                    .setPositiveButton(R.string.enable, (dialog, which) -> saveMotion(true, true))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            saveMotion(MOTION_CALM.equals(motion), false);
        }
    }

    private void saveMotion(boolean on, boolean lively) {
        animations = on;
        advancedEffects = on && lively;
        choices.edit().putBoolean("animations", animations).putBoolean("advanced_effects", advancedEffects).apply();
        changed();
    }

    private String motion() {
        if (!animations) return MOTION_OFF;
        return advancedEffects ? MOTION_LIVELY : MOTION_CALM;
    }

    private void changed() {
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) columnChoices[i].setSelected(columns == i + 4);
        for (int i = 0; i < LANGUAGES.length; i++) languageViews.get(i).setSelected(LANGUAGES[i].equals(language));
        for (int i = 0; i < WALL_SETS.length; i++) wallViews.get(i).setSelected(WALL_SETS[i].equals(wallMode));
        for (int i = 0; i < Wallpapers.PLAIN_COLOURS.length; i++) {
            colourViews.get(i).setSelected(Wallpapers.PLAIN.equals(wallMode)
                    && (Wallpapers.PLAIN_COLOURS[i] & 0xFFFFFF) == (backgroundColor & 0xFFFFFF));
        }
        for (int i = 0; i < cards.length; i++) cardViews.get(i).setSelected(cards[i]);
        for (int i = 0; i < StripSize.ALL.length; i++) textSizeViews.get(i).setSelected(StripSize.ALL[i].equals(textSize));
        for (int i = 0; i < DockStyle.ALL.length; i++) dockStyleViews.get(i).setSelected(DockStyle.ALL[i].equals(dockStyle));
        for (int i = 0; i < TileShape.ALL.length; i++) tileCornersViews.get(i).setSelected(TileShape.ALL[i].equals(tileCorners));
        for (int i = 0; i < ClockStyle.ALL.length; i++) clockStyleViews.get(i).setSelected(ClockStyle.ALL[i].equals(clockStyle));
        asrViews.get(0).setSelected(asrFactor != Prayer.ASR_HANAFI);
        asrViews.get(1).setSelected(asrFactor == Prayer.ASR_HANAFI);
        int m = methodIndex(prayerMethod);
        prayerMethodChoice.setText(getString(R.string.prayer_method_choice,
                m >= 0 ? getResources().getStringArray(R.array.prayer_method_names)[m] : prayerMethod));
        for (int i = 0; i < MOTIONS.length; i++) motionViews.get(i).setSelected(MOTIONS[i].equals(motion()));
        followThemeChoice.setSelected(!hasOnTvChoices());
        clubsChoice.setText(getString(R.string.favorite_clubs_state,
                Config.load(this).interests.favoriteTeams.size(), Interests.MAX_CLUBS));
    }

    /** The list of methods in Prayer.METHODS order; OK on one saves it and closes the list. */
    private void choosePrayerMethod() {
        String[] names = getResources().getStringArray(R.array.prayer_method_names);
        new AlertDialog.Builder(this)
                .setTitle(R.string.prayer_method_label)
                .setSingleChoiceItems(names, methodIndex(prayerMethod), (dialog, which) -> {
                    prayerMethod = Prayer.METHODS[which].key;
                    choices.edit().putString(Config.PRAYER_METHOD_KEY, prayerMethod).apply();
                    dialog.dismiss();
                    changed();
                })
                .show();
    }

    private static int methodIndex(String key) {
        for (int i = 0; i < Prayer.METHODS.length; i++) if (Prayer.METHODS[i].key.equals(key)) return i;
        return -1;
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains("animations")
                || choices.contains("advanced_effects") || choices.contains(Config.WALLPAPER_MODE_KEY)
                || choices.contains(Config.BACKGROUND_COLOR_KEY);
    }

    private TextView chip(String label, android.view.View.OnClickListener click) {
        TextView v = (TextView) LayoutInflater.from(this).inflate(R.layout.item_choice, null, false);
        v.setText(label);
        v.setOnClickListener(click);
        return v;
    }

    /** Adds a row's chips in reading order: right to left on an Arabic or Hebrew screen, so focus follows. */
    private void fill(int rowId, List<TextView> views) {
        LinearLayout row = findViewById(rowId);
        for (int i = 0; i < views.size(); i++) {
            TextView v = views.get(rtl() ? views.size() - 1 - i : i);
            row.addView(v, new LinearLayout.LayoutParams(0, (int) (52 * density), 1f));
            ((LinearLayout.LayoutParams) v.getLayoutParams()).setMarginEnd((int) (10 * density));
        }
    }

    /** A colour chip: the colour itself, a green edge when chosen and a white one under focus. */
    private StateListDrawable swatch(int colour) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(colour, 0xFFFFFFFF, 3));
        s.addState(new int[]{android.R.attr.state_selected}, rounded(colour, 0xFF57D294, 2));
        s.addState(new int[]{}, rounded(colour, 0x33FFFFFF, 1));
        return s;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(14 * density);
        d.setStroke(Math.round(strokeDp * density), stroke);
        return d;
    }

    private boolean rtl() {
        return getResources().getBoolean(R.bool.rtl_app_grid);
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return 0;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
