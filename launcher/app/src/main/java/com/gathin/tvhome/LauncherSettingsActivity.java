package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LocaleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remote-friendly settings owned by the launcher; system settings stay one row away. Four glass section cards with one
 * showing at a time, a strip of section tabs, one explanation line that follows focus and a footer of system buttons
 * (docs/SETTINGS_GLASS.md). Arrow keys are decided by {@link SettingsNav}; every chip saves what it always saved.
 */
// The supportsRtl flag is off, so the screen mirrors itself by hand for Arabic and Hebrew (the rtl field): the gravities are explicit.
@SuppressLint("RtlHardcoded")
public final class LauncherSettingsActivity extends Activity {
    private SharedPreferences choices;
    private TextView followThemeChoice;
    private TextView clubsChoice;
    private TextView hiddenAppsChoice;
    private static final int CLUBS_REQUEST = 1, HIDDEN_APPS_REQUEST = 2;
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
    private int hijriShift, prayerAlertMinutes, clubSeconds;
    /** Layout options (docs/LAYOUT_OPTIONS.md): the digits and the veil over a wallpaper picture in percent. */
    private String digits, dockPosition;
    /** Whether the key service filters the remote's keys (docs/KEY_FILTER.md): on or off. */
    private String keyFilter;
    private int wallpaperDim;
    private TextView prayerMethodChoice;
    private float density;
    private boolean rtl;

    /** One row of choices: the views in reading order and the value each one stands for. */
    private final List<View> columnViews = new ArrayList<>(), languageViews = new ArrayList<>(),
            wallViews = new ArrayList<>(), colourViews = new ArrayList<>(), cardViews = new ArrayList<>(),
            motionViews = new ArrayList<>(), textSizeViews = new ArrayList<>(), asrViews = new ArrayList<>(),
            dockStyleViews = new ArrayList<>(), tileCornersViews = new ArrayList<>(), clockStyleViews = new ArrayList<>(),
            hijriShiftViews = new ArrayList<>(), alertViews = new ArrayList<>(), digitsViews = new ArrayList<>(),
            dimViews = new ArrayList<>(), dockPositionViews = new ArrayList<>(), keyFilterViews = new ArrayList<>(),
            footerViews = new ArrayList<>();

    /** The chips for the prayer alert: minutes, 0 is off. Any other value in config.json still works, it just has no chip. */
    private static final int[] ALERT_CHOICES = {0, 5, 10, 15};

    private static final String[] LANGUAGES = {"system", "ar", "en"};
    private static final String[] WALL_SETS = {Wallpapers.HOMELAB, Wallpapers.SERENE, Wallpapers.KIDS};
    private static final String MOTION_CALM = "calm", MOTION_LIVELY = "lively", MOTION_OFF = "off";
    private static final String[] MOTIONS = {MOTION_CALM, MOTION_LIVELY, MOTION_OFF};

    private static final int LOOK = 0, DOCK = 1, PRAYER = 2, MOTION = 3, SECTIONS = 4;
    private static final int LABEL_DP = 150, CHIP_GAP_DP = 10, ROW_GAP_DP = 10;
    private static final int THUMB_W_DP = 112, THUMB_H_DP = 63;
    /** Room above and below the whole rows of a scrolling section, and how far a focused chip may grow past its row. */
    private static final int ROW_MARGIN_DP = 4, GROW_DP = 5;
    private static final long BUMP_WINDOW_MS = 1500;

    /** A row of a section: its chips (some may be hidden in this build), the hint that follows focus, its container. */
    private static final class Row {
        final int section;
        final String hint;
        final List<? extends View> chips;
        final View container;

        Row(int section, String hint, List<? extends View> chips, View container) {
            this.section = section;
            this.hint = hint;
            this.chips = chips;
            this.container = container;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<LinearLayout> sectionViews = new ArrayList<>();
    private final List<TextView> tabs = new ArrayList<>();
    private final List<String> sectionHints = new ArrayList<>();
    private final List<ImageView> thumbViews = new ArrayList<>();
    /** What a view stands for: its place in the navigation model and the line that explains it. */
    private final Map<View, SettingsNav.Spot> spots = new HashMap<>();
    private final Map<View, String> hints = new HashMap<>();
    private View[][][] grid;
    private SettingsNav nav;
    private int activeSection = -1;
    private boolean switching;
    private ScrollView sectionScroll;
    private ImageView moreMark;
    /** The first row of the visible section that is on screen; the sections scroll by whole rows (RowWindow). */
    private int topRow;
    private final EdgeBump bump = new EdgeBump(BUMP_WINDOW_MS);
    private final List<String> sectionTitles = new ArrayList<>();
    /** Puts the explanation of the focused view back after "press again" has shown for a moment. */
    private final Runnable restoreHint = this::putHintBack;
    private FrameLayout sectionHost;
    private TextView explainLine;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Language.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        density = getResources().getDisplayMetrics().density;
        rtl = getResources().getBoolean(R.bool.rtl_app_grid);
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
        cards = new boolean[]{s.showPrayer, s.showWeather, s.showSports, s.showClubs, s.showRamadan};
        hijriShift = s.hijriShift;
        prayerAlertMinutes = s.prayerAlertMinutes;
        clubSeconds = s.clubRotateSeconds;
        textSize = config.textSize;
        dockStyle = config.dockStyle;
        tileCorners = config.tileCorners;
        clockStyle = config.clockStyle;
        digits = config.digits;
        wallpaperDim = config.wallpaperDim;
        dockPosition = config.dockPosition;
        keyFilter = config.keyFilter;
        prayerMethod = s.prayerMethodKey;
        asrFactor = s.asrFactor;

        sectionScroll = findViewById(R.id.sectionScroll);
        moreMark = findViewById(R.id.moreMark);
        explainLine = findViewById(R.id.explainLine);
        explainLine.setGravity((rtl ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        ((TextView) findViewById(R.id.settingsTitle)).setGravity((rtl ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        sectionHost = findViewById(R.id.sectionHost);
        // Room for the rows to be scrolled to the top whatever is below them (the scroll is set by alignRows, not by focus).
        sectionHost.setPadding(dp(8), dp(ROW_MARGIN_DP), dp(8), getResources().getDisplayMetrics().heightPixels);
        sectionHost.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> alignRows(getCurrentFocus()));
        sectionScroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> alignRows(getCurrentFocus()));
        for (int i = 0; i < SECTIONS; i++) {
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            section.setClipChildren(false);   // the focused chip grows a little beyond its row
            sectionViews.add(section);
        }
        buildTabs();
        buildLook();
        buildDock();
        buildPrayer();
        buildMotion();
        buildFooter();
        buildNav();
        findViewById(R.id.settingsRoot).getViewTreeObserver()
                .addOnGlobalFocusChangeListener((oldFocus, newFocus) -> focusMoved(oldFocus, newFocus));
        showSection(LOOK);
        refresh();
        View start = languageViews.get(indexOf(LANGUAGES, language));
        start.requestFocus();
        focusMoved(null, start);
        loadThumbnails();
    }

    // ---- the sections ----------------------------------------------------------------------------

    private void buildTabs() {
        int[] titles = {R.string.settings_section_look, R.string.settings_section_dock, R.string.settings_section_prayer,
                R.string.settings_section_motion};
        int[] hintTexts = {R.string.settings_section_look_hint, R.string.settings_section_dock_hint,
                R.string.settings_section_prayer_hint, R.string.settings_section_motion_hint};
        int[] icons = {R.drawable.ic_picture, R.drawable.ic_apps, R.drawable.ic_section_prayer, R.drawable.ic_section_motion};
        int sand = getColor(R.color.theme_sand);
        for (int i = 0; i < SECTIONS; i++) {
            final int section = i;
            TextView tab = new TextView(this);
            tab.setText(titles[i]);
            tab.setTextSize(15);
            tab.setTextColor(sand);
            tab.setSingleLine(true);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(dp(40));
            tab.setPadding(dp(14), 0, dp(16), 0);
            tab.setBackgroundResource(R.drawable.settings_tab_bg);
            Drawable icon = getDrawable(icons[i]);
            tab.setCompoundDrawablesWithIntrinsicBounds(rtl ? null : icon, null, rtl ? icon : null, null);
            tab.setCompoundDrawablePadding(dp(8));
            tab.setCompoundDrawableTintList(ColorStateList.valueOf(sand));
            tab.setFocusable(true);
            tab.setClickable(true);
            // OK on a tab goes into its section, like Down.
            tab.setOnClickListener(v -> {
                SettingsNav.Spot to = nav.move(SettingsNav.tab(section), SettingsNav.DOWN);
                View target = viewAt(to);
                if (target != null) target.requestFocus();
            });
            tab.setOnKeyListener(keys);
            tabs.add(tab);
            sectionHints.add(getString(hintTexts[i]));
            sectionTitles.add(getString(titles[i]));
        }
        LinearLayout strip = findViewById(R.id.tabStrip);
        fill(strip, tabs, ViewGroup.LayoutParams.WRAP_CONTENT, 8);
        if (rtl) {
            // The title is at the right edge on the mirrored screen, the tabs at the left.
            LinearLayout header = findViewById(R.id.headerRow);
            View title = findViewById(R.id.settingsTitle);
            header.removeView(title);
            header.addView(title, 0);
            header.removeView(strip);
            header.addView(strip, 0);
        }
    }

    private void buildLook() {
        int[] languageNames = {R.string.language_system, R.string.language_arabic, R.string.language_english};
        for (int i = 0; i < LANGUAGES.length; i++) {
            final String tag = LANGUAGES[i];
            languageViews.add(chip(getString(languageNames[i]), v -> setLanguage(tag)));
        }
        addRow(LOOK, R.string.language_label, getString(R.string.language_hint), languageViews);

        // Each set shows its first picture, decoded from the APK's own assets (loadThumbnails).
        int[] wallNames = {R.string.wallpapers_homelab, R.string.wallpapers_serene, R.string.wallpapers_kids};
        for (int i = 0; i < WALL_SETS.length; i++) {
            final String mode = WALL_SETS[i];
            ImageView thumb = new ImageView(this);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackgroundColor(0x22FFFFFF);
            thumb.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8 * density);
                }
            });
            thumbViews.add(thumb);
            wallViews.add(previewChip(thumb, THUMB_W_DP, THUMB_H_DP, getString(wallNames[i]), v -> {
                wallMode = mode;
                choices.edit().putString(Config.WALLPAPER_MODE_KEY, mode).apply();
                changed();
            }));
        }
        addRow(LOOK, R.string.wallpapers_label, getString(R.string.wallpapers_hint), wallViews);

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
        addRow(LOOK, R.string.plain_colour_label, getString(R.string.plain_colour_hint), colourViews);

        for (int percent : WallpaperDim.CHOICES) {
            dimViews.add(chip(getString(R.string.wallpaper_dim_percent, percent), v -> {
                if (percent == wallpaperDim) return;
                wallpaperDim = percent;
                choices.edit().putInt(Config.WALLPAPER_DIM_KEY, percent).apply();
                changed();
            }));
        }
        addRow(LOOK, R.string.wallpaper_dim_label, getString(R.string.wallpaper_dim_hint), dimViews);

        // Digits show a sample of what they draw.
        int[] digitNames = {R.string.digits_latin, R.string.digits_arabic};
        for (int i = 0; i < DigitStyle.ALL.length; i++) {
            final String style = DigitStyle.ALL[i];
            TextView sample = new TextView(this);
            sample.setText(DigitStyle.ARABIC.equals(style) ? Hijri.arabicDigits("123") : "123");
            sample.setTextSize(22);
            sample.setTextColor(0xFFFFFFFF);
            sample.setGravity(Gravity.CENTER);
            digitsViews.add(previewChip(sample, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    getString(digitNames[i]), v -> {
                if (style.equals(digits)) return;
                digits = style;
                choices.edit().putString(Config.DIGITS_KEY, style).apply();
                changed();
            }));
        }
        addRow(LOOK, R.string.digits_label, getString(R.string.digits_hint), digitsViews);

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
        addRow(LOOK, R.string.clock_style_label, getString(R.string.clock_style_hint), clockStyleViews);
    }

    private void buildDock() {
        // The dock position shows a little drawing of the screen, mirrored on the Arabic one like the real screen.
        int[] positionNames = {R.string.dock_position_bottom, R.string.dock_position_side};
        for (int i = 0; i < DockPosition.ALL.length; i++) {
            final String position = DockPosition.ALL[i];
            dockPositionViews.add(previewChip(new DockPreview(this, position, rtl), 80, 45, getString(positionNames[i]), v -> {
                if (position.equals(dockPosition)) return;
                dockPosition = position;
                choices.edit().putString(Config.DOCK_POSITION_KEY, position).apply();
                changed();
            }));
        }
        addRow(DOCK, R.string.dock_position_label, getString(R.string.dock_position_hint), dockPositionViews);

        // The look options: the viewer's choices, like the text size, so a theme push does not clear them.
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
        addRow(DOCK, R.string.dock_style_label, getString(R.string.dock_style_hint), dockStyleViews);

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
        addRow(DOCK, R.string.tile_corners_label, getString(R.string.tile_corners_hint), tileCornersViews);

        for (int i = 0; i < 4; i++) {
            final int value = i + 4;
            columnViews.add(chip(String.valueOf(value), v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
            }));
        }
        addRow(DOCK, R.string.columns_label, getString(R.string.columns_hint), columnViews);

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
        addRow(DOCK, R.string.text_size_label, getString(R.string.text_size_hint), textSizeViews);

        int[] cardNames = {R.string.card_prayer, R.string.card_weather, R.string.card_football, R.string.card_clubs,
                R.string.card_ramadan};
        for (int i = 0; i < Config.CARD_KEYS.length; i++) {
            final int index = i;
            TextView c = chip(getString(cardNames[i]), v -> {
                cards[index] = !cards[index];
                choices.edit().putBoolean(Config.CARD_KEYS[index], cards[index]).apply();
                changed();
            });
            // Weather, football and clubs come from the network: the offline build has none of them.
            if (i > 0 && i < 4 && !Feeds.AVAILABLE) c.setVisibility(View.GONE);
            cardViews.add(c);
        }
        addRow(DOCK, R.string.cards_label, getString(R.string.cards_hint), cardViews);

        clubsChoice = chip("", v -> startActivityForResult(new Intent(this, ClubsActivity.class), CLUBS_REQUEST));
        // Matches need the network: the offline build has no INTERNET, so the row would promise nothing.
        if (Feeds.AVAILABLE) {
            addRow(DOCK, 0, getString(R.string.favorite_clubs_hint, Interests.MAX_CLUBS, clubSeconds), single(clubsChoice));
        }
        hiddenAppsChoice = chip("", v -> startActivityForResult(new Intent(this, HiddenAppsActivity.class),
                HIDDEN_APPS_REQUEST));
        addRow(DOCK, 0, getString(R.string.hidden_apps_hint), single(hiddenAppsChoice));
    }

    private void buildPrayer() {
        prayerMethodChoice = chip("", v -> choosePrayerMethod());
        addRow(PRAYER, R.string.prayer_method_label, getString(R.string.prayer_method_hint), single(prayerMethodChoice));

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
        addRow(PRAYER, R.string.asr_label, getString(R.string.prayer_method_hint), asrViews);

        // Prayer alert and Hijri date shift: the viewer's choices, kept when a theme is pushed, like the Asr convention.
        for (int minutes : ALERT_CHOICES) {
            alertViews.add(chip(minutes == 0 ? getString(R.string.prayer_alert_off)
                    : getString(R.string.prayer_alert_minutes, minutes), v -> {
                if (minutes == prayerAlertMinutes) return;
                prayerAlertMinutes = minutes;
                choices.edit().putInt(Config.PRAYER_ALERT_KEY, minutes).apply();
                changed();
            }));
        }
        addRow(PRAYER, R.string.prayer_alert_label, getString(R.string.prayer_alert_hint), alertViews);

        for (int d = HijriShift.MIN; d <= HijriShift.MAX; d++) {
            final int days = d;
            hijriShiftViews.add(chip(d > 0 ? "+" + d : String.valueOf(d), v -> {
                if (days == hijriShift) return;
                hijriShift = days;
                choices.edit().putInt(Config.HIJRI_SHIFT_KEY, days).apply();
                changed();
            }));
        }
        addRow(PRAYER, R.string.hijri_shift_label, getString(R.string.hijri_shift_hint), hijriShiftViews);
    }

    private void buildMotion() {
        int[] motionNames = {R.string.motion_calm, R.string.motion_lively, R.string.motion_off};
        for (int i = 0; i < MOTIONS.length; i++) {
            final String motion = MOTIONS[i];
            motionViews.add(chip(getString(motionNames[i]), v -> setMotion(motion)));
        }
        addRow(MOTION, R.string.motion_label, getString(R.string.motion_hint), motionViews);

        int[] keyFilterNames = {R.string.key_filter_on, R.string.key_filter_off};
        for (int i = 0; i < KeyFilter.ALL.length; i++) {
            final String choice = KeyFilter.ALL[i];
            keyFilterViews.add(chip(getString(keyFilterNames[i]), v -> {
                if (choice.equals(keyFilter)) return;
                keyFilter = choice;
                // The viewer's choice, like the text size: a theme push does not clear it. The key service listens.
                choices.edit().putString(Config.KEY_FILTER_KEY, choice).apply();
                changed();
            }));
        }
        addRow(MOTION, R.string.key_filter_label, getString(R.string.key_filter_hint), keyFilterViews);
    }

    /** The three buttons that are not preferences, under the card and reachable from every section. */
    private void buildFooter() {
        followThemeChoice = chip(getString(R.string.follow_theme), v -> {
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
        TextView defaultHome = chip(getString(R.string.home_settings), v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        TextView tvSettings = chip(getString(R.string.tv_settings), v -> open(new Intent(Settings.ACTION_SETTINGS)));
        for (TextView b : new TextView[]{followThemeChoice, defaultHome, tvSettings}) {
            b.setTextSize(14);
            b.setMaxLines(2);
            footerViews.add(b);
        }
        hints.put(followThemeChoice, getString(R.string.follow_theme_hint));
        hints.put(defaultHome, getString(R.string.home_settings_help));
        hints.put(tvSettings, getString(R.string.tv_settings_hint));
        fill((LinearLayout) findViewById(R.id.footerRow), footerViews, ViewGroup.LayoutParams.WRAP_CONTENT, CHIP_GAP_DP);
    }

    // ---- navigation ------------------------------------------------------------------------------

    /** Hands the model the chips that exist in this build and maps every view to its place. */
    private void buildNav() {
        int[][] counts = new int[SECTIONS][];
        grid = new View[SECTIONS][][];
        for (int s = 0; s < SECTIONS; s++) {
            List<View[]> shown = new ArrayList<>();
            for (Row row : rows) {
                if (row.section != s) continue;
                List<View> visible = new ArrayList<>();
                for (View chip : row.chips) if (chip.getVisibility() == View.VISIBLE) visible.add(chip);
                if (visible.isEmpty()) {
                    row.container.setVisibility(View.GONE);
                    continue;
                }
                int r = shown.size();
                for (int c = 0; c < visible.size(); c++) {
                    spots.put(visible.get(c), new SettingsNav.Spot(s, r, c));
                    hints.put(visible.get(c), row.hint);
                }
                shown.add(visible.toArray(new View[0]));
            }
            grid[s] = shown.toArray(new View[0][]);
            counts[s] = new int[shown.size()];
            for (int r = 0; r < shown.size(); r++) counts[s][r] = shown.get(r).length;
        }
        nav = new SettingsNav(counts, footerViews.size(), rtl);
        for (int i = 0; i < tabs.size(); i++) hints.put(tabs.get(i), sectionHints.get(i));
    }

    /** The four arrow keys, answered by the model; anything it cannot place falls through to Android's focus search. */
    private final View.OnKeyListener keys = (v, keyCode, event) -> {
        int dir = direction(keyCode);
        if (dir < 0 || nav == null) return false;
        SettingsNav.Spot from = spotOf(v);
        if (from == null) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        SettingsNav.Spot to = nav.move(from, dir);
        if (to.equals(nav.clamp(from))) {   // an edge: stay, never wrap
            bump.reset();
            return true;
        }
        if (nav.crossesSection(from, dir)) {
            // Leaving a row for another section takes a second press, and never a key that is held down.
            if (event.getRepeatCount() > 0) return true;
            if (!bump.pass(dir, SystemClock.uptimeMillis())) {
                explainLine.setText(getString(R.string.settings_press_again, sectionTitles.get(to.section)));
                explainLine.removeCallbacks(restoreHint);
                explainLine.postDelayed(restoreHint, BUMP_WINDOW_MS);
                return true;
            }
        } else {
            bump.reset();
        }
        View target = viewAt(to);
        if (target == null) return false;
        if (to.row != SettingsNav.FOOTER && to.section != activeSection) showSection(to.section);
        target.requestFocus();
        return true;
    };

    private static int direction(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return SettingsNav.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return SettingsNav.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return SettingsNav.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return SettingsNav.RIGHT;
            default: return -1;
        }
    }

    private SettingsNav.Spot spotOf(View v) {
        int tab = tabs.indexOf(v);
        if (tab >= 0) return SettingsNav.tab(tab);
        int footer = footerViews.indexOf(v);
        if (footer >= 0) return new SettingsNav.Spot(Math.max(0, activeSection), SettingsNav.FOOTER, footer);
        return spots.get(v);
    }

    private View viewAt(SettingsNav.Spot spot) {
        if (spot.row == SettingsNav.TABS) return spot.chip < tabs.size() ? tabs.get(spot.chip) : null;
        if (spot.row == SettingsNav.FOOTER) return spot.chip < footerViews.size() ? footerViews.get(spot.chip) : null;
        if (spot.section >= grid.length || spot.row >= grid[spot.section].length
                || spot.chip >= grid[spot.section][spot.row].length) return null;
        return grid[spot.section][spot.row][spot.chip];
    }

    /**
     * Shows one section card. Only the showing section is attached to the screen, so a hidden one can never hold focus
     * or leave a stale picture (hiding with GONE left the old section painted on the TV after a key across sections).
     */
    private void showSection(int section) {
        if (section == activeSection) return;
        activeSection = section;
        // Taking the focused chip off the screen makes Android focus the first tab on its own; that must not switch back.
        switching = true;
        try {
            sectionHost.removeAllViews();
            sectionHost.addView(sectionViews.get(section), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        } finally {
            switching = false;
        }
        for (int i = 0; i < SECTIONS; i++) tabs.get(i).setSelected(i == section);
        topRow = 0;
        sectionScroll.scrollTo(0, 0);

    }

    /** Focus follows the tabs to their section, and the explanation line follows focus. */
    private void putHintBack() {
        String hint = hints.get(getCurrentFocus());
        if (hint != null) explainLine.setText(hint);
    }

    private void focusMoved(View oldFocus, View newFocus) {
        if (oldFocus != null && oldFocus.getScaleX() != 1f) oldFocus.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
        if (newFocus == null) return;
        int tab = tabs.indexOf(newFocus);
        if (tab >= 0 && !switching) showSection(tab);
        String hint = hints.get(newFocus);
        if (hint != null) explainLine.setText(hint);
        if (animations && tab < 0 && hints.containsKey(newFocus)) {
            // Grows by a few dp at most, however wide the chip is, so the ring is never cut at the edge of the card.
            float grow = Math.min(0.04f, dp(GROW_DP) * 2f / Math.max(1, newFocus.getWidth()));
            newFocus.animate().scaleX(1f + grow).scaleY(1f + grow).setDuration(120).start();
        }
        alignRows(newFocus);
    }

    /**
     * Scrolls the visible section by whole rows: the focused row decides the first row shown, the scroll area is cut right
     * under the last row that fits whole, and the arrow under the card says whether there are more rows (RowWindow).
     */
    private void alignRows(View focus) {
        if (activeSection < 0) return;
        LinearLayout section = sectionViews.get(activeSection);
        int height = sectionScroll.getHeight();
        if (height == 0 || section.getHeight() == 0) return;
        List<View> shown = new ArrayList<>();
        for (int i = 0; i < section.getChildCount(); i++) {
            if (section.getChildAt(i).getVisibility() == View.VISIBLE) shown.add(section.getChildAt(i));
        }
        int n = shown.size();
        if (n == 0) return;
        int[] tops = new int[n], bottoms = new int[n];
        for (int i = 0; i < n; i++) {
            tops[i] = shown.get(i).getTop();
            bottoms[i] = shown.get(i).getBottom();
        }
        int margin = dp(ROW_MARGIN_DP), usable = height - 2 * margin;
        int focused = topRow;
        View row = focus;
        while (row != null && row.getParent() != section) row = row.getParent() instanceof View ? (View) row.getParent() : null;
        if (row != null && shown.indexOf(row) >= 0) focused = shown.indexOf(row);
        boolean scrolls = RowWindow.scrolls(tops, bottoms, usable);
        int padBottom = 0, target = 0, last = n - 1;
        if (scrolls) {
            topRow = RowWindow.top(tops, bottoms, usable, topRow, focused);
            last = RowWindow.last(tops, bottoms, usable, topRow);
            padBottom = Math.max(0, height - (bottoms[last] - tops[topRow]) - 2 * margin);
            target = tops[topRow];
        } else {
            topRow = 0;
        }
        if (sectionScroll.getPaddingBottom() != padBottom) sectionScroll.setPadding(0, 0, 0, padBottom);
        if (sectionScroll.getScrollY() != target) sectionScroll.smoothScrollTo(0, target);
        moreMark.setVisibility(scrolls ? View.VISIBLE : View.INVISIBLE);
        moreMark.setRotation(last < n - 1 ? 0f : 180f);
    }

    // ---- building blocks -------------------------------------------------------------------------

    private void addRow(int section, int labelRes, String hint, List<? extends View> chips) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setClipChildren(false);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setClipChildren(false);
        box.setBaselineAligned(false);
        fill(box, chips, ViewGroup.LayoutParams.WRAP_CONTENT, CHIP_GAP_DP);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (labelRes == 0) {
            line.addView(box, boxParams);
        } else {
            TextView label = new TextView(this);
            label.setText(labelRes);
            label.setTextSize(16);
            label.setTextColor(getColor(R.color.theme_sand));
            label.setGravity((rtl ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dp(LABEL_DP),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (rtl) {
                labelParams.leftMargin = dp(CHIP_GAP_DP);
                line.addView(box, boxParams);
                line.addView(label, labelParams);
            } else {
                labelParams.rightMargin = dp(CHIP_GAP_DP);
                line.addView(label, labelParams);
                line.addView(box, boxParams);
            }
        }
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lineParams.bottomMargin = dp(ROW_GAP_DP);
        sectionViews.get(section).addView(line, lineParams);
        rows.add(new Row(section, hint, chips, line));
    }

    private static List<View> single(View v) {
        List<View> out = new ArrayList<>();
        out.add(v);
        return out;
    }

    /** A text chip. */
    private TextView chip(String label, View.OnClickListener click) {
        TextView v = (TextView) LayoutInflater.from(this).inflate(R.layout.item_choice, null, false);
        v.setBackgroundResource(R.drawable.settings_chip_bg);
        v.setTextSize(16);
        v.setMinHeight(dp(48));
        v.setPadding(dp(10), 0, dp(10), 0);
        v.setText(label);
        v.setOnClickListener(click);
        v.setOnKeyListener(keys);
        return v;
    }

    /** A chip with a picture, a drawing or a sample above its name. */
    private LinearLayout previewChip(View preview, int widthDp, int heightDp, String label, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(8), dp(8), dp(8), dp(6));
        c.setBackgroundResource(R.drawable.settings_chip_bg);
        c.setFocusable(true);
        c.setClickable(true);
        c.setOnClickListener(click);
        c.setOnKeyListener(keys);
        c.addView(preview, new LinearLayout.LayoutParams(
                widthDp > 0 ? dp(widthDp) : widthDp, heightDp > 0 ? dp(heightDp) : heightDp));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(14);
        name.setTextColor(getColor(R.color.theme_sand));
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(4);
        c.addView(name, nameParams);
        return c;
    }

    /**
     * Adds a row's views in reading order: right to left on an Arabic or Hebrew screen, so focus follows. The gap goes
     * between the views, never after the last one.
     */
    private void fill(LinearLayout row, List<? extends View> views, int height, int gapDp) {
        for (int i = 0; i < views.size(); i++) {
            View v = views.get(rtl ? views.size() - 1 - i : i);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, height, 1f);
            if (i < views.size() - 1) p.rightMargin = dp(gapDp);
            // A tab strip's tabs are as wide as their text.
            if (row.getId() == R.id.tabStrip) {
                p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                p.weight = 0f;
            }
            row.addView(v, p);
        }
    }

    /** A colour chip: the colour itself, a green edge when chosen and a white one under focus. */
    private StateListDrawable swatch(int colour) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(colour, 0xE6FFFFFF, 3));
        s.addState(new int[]{android.R.attr.state_selected}, rounded(colour, 0xFF57D294, 3));
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

    private int dp(int value) {
        return Math.round(value * density);
    }

    // ---- wallpaper thumbnails --------------------------------------------------------------------

    /** Decodes the first picture of each set a few times smaller than it is, off the UI thread; none is kept beyond the chip. */
    private void loadThumbnails() {
        final int width = dp(THUMB_W_DP);
        new Thread(() -> {
            for (int i = 0; i < WALL_SETS.length; i++) {
                final Bitmap bitmap = thumbnail(WALL_SETS[i], width);
                final ImageView target = thumbViews.get(i);
                if (bitmap == null) continue;
                runOnUiThread(() -> {
                    if (!isDestroyed()) target.setImageBitmap(bitmap);
                });
            }
        }, "settings-thumbnails").start();
    }

    private Bitmap thumbnail(String set, int targetWidth) {
        try {
            String dir = Wallpapers.DIR_NAME + "/" + set;
            String[] names = getAssets().list(dir);
            if (names == null) return null;
            Arrays.sort(names);
            String first = null;
            for (String name : names) {
                if (name.endsWith(".jpg")) {
                    first = name;
                    break;
                }
            }
            if (first == null) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getAssets().open(dir + "/" + first)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream in = getAssets().open(dir + "/" + first)) {
                return BitmapFactory.decodeStream(in, null, options);
            }
        } catch (IOException | RuntimeException e) {
            return null;   // the chip stays the plain one
        }
    }

    // ---- saving ----------------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == CLUBS_REQUEST || requestCode == HIDDEN_APPS_REQUEST) && resultCode == RESULT_OK) changed();
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
        for (int i = 0; i < columnViews.size(); i++) columnViews.get(i).setSelected(columns == i + 4);
        for (int i = 0; i < LANGUAGES.length; i++) languageViews.get(i).setSelected(LANGUAGES[i].equals(language));
        for (int i = 0; i < WALL_SETS.length; i++) wallViews.get(i).setSelected(WALL_SETS[i].equals(wallMode));
        for (int i = 0; i < Wallpapers.PLAIN_COLOURS.length; i++) {
            colourViews.get(i).setSelected(Wallpapers.PLAIN.equals(wallMode)
                    && (Wallpapers.PLAIN_COLOURS[i] & 0xFFFFFF) == (backgroundColor & 0xFFFFFF));
        }
        for (int i = 0; i < cards.length; i++) cardViews.get(i).setSelected(cards[i]);
        for (int i = 0; i < StripSize.ALL.length; i++) textSizeViews.get(i).setSelected(StripSize.ALL[i].equals(textSize));
        for (int i = 0; i < DockPosition.ALL.length; i++) dockPositionViews.get(i).setSelected(DockPosition.ALL[i].equals(dockPosition));
        for (int i = 0; i < DockStyle.ALL.length; i++) dockStyleViews.get(i).setSelected(DockStyle.ALL[i].equals(dockStyle));
        for (int i = 0; i < TileShape.ALL.length; i++) tileCornersViews.get(i).setSelected(TileShape.ALL[i].equals(tileCorners));
        for (int i = 0; i < ClockStyle.ALL.length; i++) clockStyleViews.get(i).setSelected(ClockStyle.ALL[i].equals(clockStyle));
        for (int i = 0; i < DigitStyle.ALL.length; i++) digitsViews.get(i).setSelected(DigitStyle.ALL[i].equals(digits));
        for (int i = 0; i < WallpaperDim.CHOICES.length; i++) dimViews.get(i).setSelected(WallpaperDim.CHOICES[i] == wallpaperDim);
        for (int i = 0; i < ALERT_CHOICES.length; i++) alertViews.get(i).setSelected(ALERT_CHOICES[i] == prayerAlertMinutes);
        for (int i = 0; i < hijriShiftViews.size(); i++) hijriShiftViews.get(i).setSelected(HijriShift.MIN + i == hijriShift);
        asrViews.get(0).setSelected(asrFactor != Prayer.ASR_HANAFI);
        asrViews.get(1).setSelected(asrFactor == Prayer.ASR_HANAFI);
        int m = methodIndex(prayerMethod);
        prayerMethodChoice.setText(getString(R.string.prayer_method_choice,
                m >= 0 ? getResources().getStringArray(R.array.prayer_method_names)[m] : prayerMethod));
        for (int i = 0; i < MOTIONS.length; i++) motionViews.get(i).setSelected(MOTIONS[i].equals(motion()));
        for (int i = 0; i < KeyFilter.ALL.length; i++) keyFilterViews.get(i).setSelected(KeyFilter.ALL[i].equals(keyFilter));
        followThemeChoice.setSelected(!hasOnTvChoices());
        Config now = Config.load(this);
        clubsChoice.setText(getString(R.string.favorite_clubs_state,
                now.interests.favoriteTeams.size(), Interests.MAX_CLUBS));
        hiddenAppsChoice.setText(getString(R.string.hidden_apps_state, now.hiddenApps.size()));
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
