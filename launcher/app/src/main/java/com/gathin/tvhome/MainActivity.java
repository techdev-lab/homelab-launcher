package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.SoundEffectConstants;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * tvOS style home: black, a pinned dock row, an app grid, and one glass highlight that springs between tiles.
 */
public class MainActivity extends Activity implements AppAdapter.TileListener {

    private static final int LAUNCHER_SETTINGS_REQUEST = 42;
    /** Sent by QuickSettingsActivity: open Launcher settings from here so Home rebuilds when they close. */
    static final String ACTION_LAUNCHER_SETTINGS = "com.gathin.tvhome.action.LAUNCHER_SETTINGS";
    /** Tiles per row and dock slots (res/values/config.xml, columns). Set in onCreate. */
    static int COLUMNS = 4;
    private boolean rtlAppGrid;
    private static final long MINUTE_MS = 60_000L;
    private String liveTvPkg;
    private String liveTvAction;
    private String inputAction;
    private String sourcePanelService;
    private String sourcePanelAction;

    private AppAdapter adapter;
    private RecyclerView grid;
    private FrameLayout root;
    private GlassView glass;
    private View ambient;
    private TextView heroName;
    private int ambientColor = -1;
    private ValueAnimator ambientAnimator;
    private boolean heroShown;
    private boolean animations;
    private boolean advancedEffects;
    private TextView greeting;
    private View btnInputs;
    private TextView btnWifi;
    private TextView shamsiDate;
    private boolean shamsi;
    private TextView hijriDate;
    /** Language, Hijri date and the interests strip from config.json (see Interests). */
    private Interests.Settings interestsCfg = Interests.Settings.NONE;
    private LinearLayout interestsStrip;
    /** Latest online data, replaced whole by the feed thread and read on the main thread. */
    private volatile List<Interests.Match> matches = Collections.emptyList();
    private volatile List<Interests.Headline> headlines = Collections.emptyList();
    /** Online build: the last weather fetch, or null (card hidden). */
    private volatile Interests.Weather weather;
    private int headlineIndex;
    private int matchIndex;
    /** Favourite clubs' fixtures (favorite_teams), the card that shows them and which club it is on. */
    private volatile List<Interests.Match> clubMatches = Collections.emptyList();
    private View clubCard;
    private int clubIndex;
    /** Club schedules are large and change slowly: fetched every 12 hours, live scores in between. */
    private static final long CLUB_SCHEDULE_MS = 12 * 60 * MINUTE_MS;
    private long clubScheduleMs;
    private List<String> clubScheduleIds = Collections.emptyList();
    private ExecutorService feedThread;
    /** Resting backgrounds of the header pills and strip cards, re-tinted with the ambient colour (Glass). */
    private final List<GradientDrawable> glassPills = new ArrayList<>();
    private final List<GradientDrawable> glassCards = new ArrayList<>();
    private Map<Integer, String> appKeys = Collections.emptyMap();
    /** The wallpaper picture (index 0 under everything) and its dark scrim; null with the plain background. */
    private android.widget.ImageView wallView;
    /** Rotating wallpapers (Config.wallpapers) and the one on screen, or -1. */
    private List<File> wallpapers = Collections.emptyList();
    private int wallpaperMinutes = 5;
    private int wallIndex = -1;
    /** Optional time-of-day backdrop (config.xml, sky_intensity); 0 keeps it hidden. */
    private SkyView sky;
    private String vpnApp;
    private SpringAnimation springX, springY;
    private float glassTargetX, glassTargetY;
    // How far the grid has scrolled the glass. springY works without it, in grid content space, so a
    // scroll moves the glass in lockstep with its tile instead of becoming a moving spring target.
    private float glassScrollShift;
    private boolean glassShown;
    // Static so they survive the activity being destroyed while another app is open (low-memory TVs,
    // or Developer options → "Don't keep activities"); they only reset when the process dies.
    private static int lastFocused = 0;
    /** App of the last focused tile, so focus follows it when the app list changes. */
    private static String lastFocusedKey;
    private float density;
    /** Calmer motion for weak GPUs (config.xml, reduced_motion): stiff spring, short fades, no replay. */
    private boolean reducedMotion;
    /** Festive skin from the theme (see Festival); applied while it is active today. */
    private Festival festival = Festival.NONE;
    private boolean festive;
    private DiyaDrawable diya;
    private BellDrawable bell;
    private MarigoldDrawable marigold;
    private int greetingColor;
    /** The day the petal shower last played, so it greets the first Home of each day only. */
    private static LocalDate lastPetalShower;
    /** Text size of the strip (StripSize): sets the cards per row and the size of their text. */
    private String textSize = StripSize.SMALL;
    /** Look options (DockStyle, TileShape, ClockStyle, docs/LOOK_OPTIONS.md): the defaults are the 2.9 look. */
    private String dockStyle = DockStyle.BAND, tileCorners = TileShape.NORMAL, clockStyle = ClockStyle.PLAIN;
    private DockDecoration dockDecoration;
    /** Rebuild the tiles on resume: Home was fully hidden by an app, or Home was pressed. Not after a side panel such as Wi-Fi settings. */
    private boolean rebuildOnResume = true;
    /** The Inputs dropdown while it is open, so onPause can close it. */
    private PopupWindow openPopup;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Refreshes the network pill when the connection changes: at boot Wi-Fi often connects after Home is shown. */
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    /** Shows the Inputs pill once the TV registers an input: at boot HDMI inputs can appear after Home is shown. */
    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputAdded(String inputId) {
            updateInputsButton();
        }

        @Override
        public void onInputRemoved(String inputId) {
            updateInputsButton();
        }
    };
    /** Listens for any VPN coming up or going down, even one this app's traffic is excluded from. */
    private final ConnectivityManager.NetworkCallback vpnCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }

        @Override
        public void onLost(@NonNull Network network) {
            handler.post(MainActivity.this::updateWifiLabel);
        }
    };
    private final IntentFilter packageFilter = new IntentFilter();
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Updates send REMOVED followed by ADDED; keep the tile until the new APK is ready.
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
            reloadApps(false);
        }
    };
    private final Runnable minuteTick = new Runnable() {
        @Override
        public void run() {
            keepLanguage();
            updateGreeting();
            updateWifiLabel();
            sky.update();
            updateWallpaperRotation();
            handler.postDelayed(this, MINUTE_MS);
        }
    };
    /** The club card moves to the next favourite club every club_rotate_seconds (default 5) while Home shows. */
    private final Runnable clubTick = new Runnable() {
        @Override
        public void run() {
            clubIndex++;
            showClubCard();
            handler.postDelayed(this, interestsCfg.clubRotateSeconds * 1000L);
        }
    };
    /** Online build: matches, news and weather every 15 minutes, only while Home is on screen. */
    private static final long FEED_MS = 15 * MINUTE_MS;
    /** When the last fetch started; returning to Home again and again must not fetch each time. */
    private long lastFeedMs;
    private final Runnable feedTick = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long wait = lastFeedMs + FEED_MS - now;
            if (lastFeedMs == 0 || wait <= 0) {
                lastFeedMs = now;
                fetchFeeds();
                wait = FEED_MS;
            }
            handler.postDelayed(this, wait);
        }
    };

    /**
     * The launcher's language (layout-ar included) over the TV's: config.json "language" or the TV choice, else the
     * launcher's own Android language (Launcher settings or cmd locale set-app-locales). Same rule as ClockDream.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Language.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;
        // config.xml, overlaid by config.json from a pushed theme (see Config and tools/theme.sh).
        Config cfg = Config.load(this);
        COLUMNS = cfg.columns;
        animations = cfg.animations;
        advancedEffects = cfg.animations && cfg.advancedEffects;
        reducedMotion = cfg.reducedMotion;
        festival = cfg.festival;
        festive = festival.active(LocalDate.now());
        liveTvPkg = cfg.liveTvPackage;
        liveTvAction = cfg.liveTvAction;
        inputAction = cfg.inputAction;
        sourcePanelService = cfg.sourcePanelService;
        sourcePanelAction = cfg.sourcePanelAction;

        root = findViewById(R.id.root);
        root.setBackgroundColor(cfg.backgroundColor);
        wallpapers = cfg.wallpapers;
        wallpaperMinutes = cfg.wallpaperMinutes;
        if (!wallpapers.isEmpty()) updateWallpaperRotation();
        else if (cfg.wallpaper != null) showWallpaper(cfg.wallpaper);
        sky = findViewById(R.id.sky);
        sky.setIntensity(cfg.skyIntensity);
        glass = findViewById(R.id.glass);
        if (festive) glass.setAccent(festival.accentColor());
        ambient = findViewById(R.id.ambient);
        // The glow is one bitmap, so its alpha can be applied while drawing it. Otherwise every frame of
        // a fade renders it into a full-screen offscreen buffer first.
        ambient.forceHasOverlappingRendering(false);
        heroName = findViewById(R.id.heroName);
        greeting = findViewById(R.id.greeting);
        greetingColor = greeting.getCurrentTextColor();
        btnWifi = findViewById(R.id.btnWifi);
        shamsiDate = findViewById(R.id.shamsiDate);
        shamsi = cfg.shamsiDate;
        if (shamsi) {
            shamsiDate.setVisibility(View.VISIBLE);
            TextView date = findViewById(R.id.date);
            date.setTextSize(11f);
            date.setPadding(0, (int) (4 * density), 0, 0);
        }
        interestsCfg = cfg.interests;
        hijriDate = findViewById(R.id.hijriDate);
        if (interestsCfg.hijriDate) hijriDate.setVisibility(View.VISIBLE);
        interestsStrip = findViewById(R.id.interests);
        appKeys = cfg.appKeys;
        vpnApp = cfg.vpnApp;
        textSize = cfg.textSize;
        dockStyle = cfg.dockStyle;
        tileCorners = cfg.tileCorners;
        clockStyle = cfg.clockStyle;
        if (ClockStyle.capsule(clockStyle)) glassClock();

        grid = findViewById(R.id.grid);
        rtlAppGrid = getResources().getBoolean(R.bool.rtl_app_grid);
        // Mirror only the app grid; the header and other locales keep their own layouts.
        grid.setLayoutManager(new GridLayoutManager(this, COLUMNS) {
            @Override
            protected boolean isLayoutRTL() {
                return rtlAppGrid;
            }

            /**
             * Every "bring this tile on screen" scroll lands here, both RecyclerView's own on focus and
             * onTileFocused's. The stock version treats the grid's top padding as the visible top, but that
             * padding only pushes the dock down the screen: grid rows scroll up into it. With the stock rule,
             * Up into a row above the padding dragged that row down to the padding line, cut it off at the
             * bottom of the screen and could scroll the focused tile out entirely, dropping focus into the
             * top bar. Grid tiles now scroll just enough to show the rect between the grid's top edge and its
             * bottom; dock tiles are left to onTileFocused, which parks the dock row on the padding line.
             */
            @Override
            public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child,
                                                         @NonNull Rect rect, boolean immediate,
                                                         boolean focusedChildVisible) {
                if (getPosition(child) < COLUMNS) return false;
                int top = child.getTop() + rect.top;
                int bottom = top + rect.height();
                int limitBottom = getHeight() - getPaddingBottom();
                int dy;
                if (top < 0) dy = top;
                else if (bottom > limitBottom) dy = Math.min(top, bottom - limitBottom);
                else dy = 0;
                if (dy == 0) return false;
                if (immediate || reducedMotion || !animations) parent.scrollBy(0, dy);
                else parent.smoothScrollBy(0, dy);
                return true;
            }
        });
        grid.setHasFixedSize(true);
        grid.setItemAnimator(null);
        dockDecoration = new DockDecoration(COLUMNS, density, dockStyle, TileShape.factor(tileCorners));
        grid.addItemDecoration(dockDecoration);
        if (animations) {
            int enterAnim = reducedMotion ? R.anim.tile_enter_reduced : R.anim.tile_enter;
            float enterDelay = reducedMotion ? 0f : 0.06f;
            grid.setLayoutAnimation(new LayoutAnimationController(
                    AnimationUtils.loadAnimation(this, enterAnim), enterDelay));
        }
        adapter = new AppAdapter(this, this, COLUMNS, cfg);
        // Arabic screen: Arabic app names too (YouTube, Shahid… keep English labels on an Arabic system).
        if (arabic()) adapter.setLabelOverrides(NamesAr.get(this).apps);
        grid.setAdapter(adapter);

        // tvOS layout: push the dock row towards the bottom of the screen, leaving room for
        // rows_below_dock grid rows under it; the rest of the grid stays below until you press Down.
        int rowsBelowDock = cfg.rowsBelowDock;
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int h = b - t;
            // Item height follows the tile size, which follows the column count (AppAdapter.tileWidthPx).
            int inner = (r - l) - grid.getPaddingLeft() - grid.getPaddingRight();
            int itemH = AppAdapter.itemHeightPx(AppAdapter.tileHeightPx(AppAdapter.tileWidthPx(inner, COLUMNS, density)), density);
            int dockRow = itemH + (int) ((6 + 12) * density);   // item height plus DockDecoration offsets
            int gridRows = itemH * rowsBelowDock;
            int want = Math.max(0, h - dockRow - gridRows - (int) (30 * density));
            if (grid.getPaddingTop() != want) {
                grid.setPadding(grid.getPaddingLeft(), want, grid.getPaddingRight(), grid.getPaddingBottom());
            }
        });

        // Keep the glass glued to its tile while the grid scrolls.
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (!glassShown) return;
                glassTargetY -= dy;
                glassScrollShift -= dy;
                glass.setTranslationY(glass.getTranslationY() - dy);
            }
        });

        // Liquid feel: soft springs on the highlight's position.
        springX = new SpringAnimation(glass, DynamicAnimation.TRANSLATION_X);
        springY = new SpringAnimation(glass, new FloatPropertyCompat<View>("glassContentY") {
            @Override
            public float getValue(View v) {
                return v.getTranslationY() - glassScrollShift;
            }

            @Override
            public void setValue(View v, float value) {
                v.setTranslationY(value + glassScrollShift);
            }
        });
        for (SpringAnimation s : new SpringAnimation[]{springX, springY}) {
            if (reducedMotion) {
                // Stiff and almost critically damped: settles in a few frames instead of oscillating,
                // and stops once the highlight is within 1.5 px of its tile.
                s.setSpring(new SpringForce().setStiffness(1400f).setDampingRatio(0.95f));
                s.setMinimumVisibleChange(1.5f);
            } else {
                s.setSpring(new SpringForce().setStiffness(advancedEffects ? 320f : 260f)
                        .setDampingRatio(advancedEffects ? 0.76f : 0.58f));
            }
        }

        btnInputs = findViewById(R.id.btnInputs);
        View btnSettings = findViewById(R.id.btnSettings);
        for (View pill : new View[]{btnInputs, btnWifi, btnSettings}) pill.setBackground(glassPill());
        tintGlass(ambientColor);
        btnInputs.setOnClickListener(v -> showInputs());
        btnWifi.setOnClickListener(v -> openNetworkPill());
        btnWifi.setOnLongClickListener(v -> {
            open(systemIntent(Settings.ACTION_WIFI_SETTINGS));
            return true;
        });
        btnSettings.setOnClickListener(v -> open(systemIntent(Settings.ACTION_SETTINGS)));
        // Hold OK: the launcher's own settings (a plain click stays the TV's Settings).
        btnSettings.setOnLongClickListener(v -> {
            openLauncherSettings();
            return true;
        });

        // Keep focus on the top bar at its ends and at the top: Android's focus finder would otherwise
        // jump to a tile far below when there is nothing in that direction.
        boolean rtlMenuBar = getResources().getBoolean(R.bool.rtl_menu_bar);
        View.OnKeyListener pillKeys = (v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return true;
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                // The strip cards under the bar, else back to the dock tile Up came from.
                if (!focusNearestCard(v, false)) focusTile(lastFocused % COLUMNS);
                return true;
            }
            int firstButton = btnInputs.getVisibility() == View.VISIBLE ? R.id.btnInputs : R.id.btnWifi;
            int leftButton = rtlMenuBar ? R.id.btnSettings : firstButton;
            int rightButton = rtlMenuBar ? firstButton : R.id.btnSettings;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return v.getId() == leftButton;
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && v.getId() == rightButton;
        };
        btnInputs.setOnKeyListener(pillKeys);
        btnWifi.setOnKeyListener(pillKeys);
        btnSettings.setOnKeyListener(pillKeys);
        if (advancedEffects) {
            for (View button : new View[]{btnInputs, btnWifi, btnSettings}) {
                button.setOnFocusChangeListener((view, focused) -> view.animate()
                        .scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f)
                        .setDuration(160).start());
            }
        }
        if (festive) applyFestiveSkin((TextView) btnInputs, (TextView) btnSettings);

        // Hide the glass when focus leaves the tiles.
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((oldF, newF) -> {
            boolean onTile = newF != null && newF.getId() == R.id.card;
            if (!onTile) hideGlass();
        });

        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageFilter.addDataScheme("package");

        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.registerCallback(inputCallback, handler);
        if (cm != null) {
            cm.registerDefaultNetworkCallback(networkCallback);
            cm.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    .build(), vpnCallback);
        }
        // Not after recreate(): the launch intent survives it and would open the settings again.
        if (savedInstanceState == null) openLauncherSettingsIfAsked(getIntent());
    }

    private boolean openLauncherSettingsIfAsked(Intent intent) {
        if (intent == null || !ACTION_LAUNCHER_SETTINGS.equals(intent.getAction())) return false;
        setIntent(new Intent(Intent.ACTION_MAIN));
        startActivityForResult(new Intent(this, LauncherSettingsActivity.class), LAUNCHER_SETTINGS_REQUEST);
        return true;
    }

    /**
     * A theme's full-screen picture behind everything. Decoded once at the size CENTER_CROP shows, so it
     * costs a screen-sized bitmap (about 4 MB at 1080p), which is the price of a wallpaper on a 1 GB TV.
     * A picture over 4K is refused rather than decoded (#166); the plain colour stays.
     */
    private void showWallpaper(File file) {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
            int w = opts.outWidth, h = opts.outHeight;
            if (w <= 0 || h <= 0) return;
            int sample = WallpaperSize.sampleSize(w, h, dm.widthPixels, dm.heightPixels);
            // The limit applies to what is decoded, so a 12 MP photo still works once it is sampled down.
            if (!WallpaperSize.accepted(w / sample, h / sample)) return;
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(w / sample, h / sample, dm.widthPixels, dm.heightPixels);
            if (scale != null) {
                opts.inScaled = true;
                opts.inDensity = scale[0];
                opts.inTargetDensity = scale[1];
            }
            opts.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;   // no alpha needed, half the memory
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(file.getPath(), opts);
            if (bmp == null) return;
            boolean first = wallView == null;
            if (first) {
                wallView = new android.widget.ImageView(this);
                wallView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                root.addView(wallView, 0, full);
                // A dark veil over any picture, so the clock and the cards keep their contrast on a bright one.
                View scrim = new View(this);
                scrim.setBackgroundColor(0x59000000);
                root.addView(scrim, 1, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            wallView.setImageBitmap(bmp);
            if (!first && animations && !reducedMotion) {
                wallView.setAlpha(0f);
                wallView.animate().alpha(1f).setDuration(700).start();
            }
        } catch (Exception | OutOfMemoryError e) {
            // A bad or huge picture must never take the home screen down; the plain colour stays.
        }
    }

    /** Rotating wallpapers: shows the picture for the current time slot, once a minute and on return to Home. */
    private void updateWallpaperRotation() {
        if (wallpapers.isEmpty()) return;
        long minute = System.currentTimeMillis() / MINUTE_MS;
        int i = Wallpapers.indexAt(minute, wallpaperMinutes, wallpapers.size());
        if (i == wallIndex || i < 0) return;
        wallIndex = i;
        showWallpaper(wallpapers.get(i));
    }

    /**
     * Home pressed while Home is already in front: singleTask delivers it here, then onResume runs.
     * Back to the top with the first dock tile focused, like the stock launcher. Back from an app
     * never comes this way, so returning to the app you left from is unchanged (#118).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (openLauncherSettingsIfAsked(intent)) return;
        if (intent == null || !intent.hasCategory(Intent.CATEGORY_HOME)) return;
        grid.scrollToPosition(0);
        lastFocused = 0;
        lastFocusedKey = adapter.getItemCount() > 0 ? adapter.keyAt(0) : null;
        rebuildOnResume = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, packageFilter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // A festival that started or ended while Home was in the background: rebuild with or without it.
        if (festive != festival.active(LocalDate.now())) {
            recreate();
            return;
        }
        if (rebuildOnResume) reloadApps(true);
        rebuildOnResume = false;
        keepLanguage();
        updateGreeting();
        updateWifiLabel();
        updateInputsButton();
        sky.update();
        updateWallpaperRotation();
        handler.postDelayed(minuteTick, MINUTE_MS);
        if (Feeds.AVAILABLE && (interestsCfg.wantsMatches() || interestsCfg.wantsNews()
                || interestsCfg.wantsWeather() || interestsCfg.wantsClubs())) handler.post(feedTick);
        if (interestsCfg.wantsClubs()) handler.postDelayed(clubTick, interestsCfg.clubRotateSeconds * 1000L);
        handler.postDelayed(stripTick, STRIP_TURN_MS);
        if (festive && diya != null) {
            diya.start();
            LocalDate today = LocalDate.now();
            if (!reducedMotion && !today.equals(lastPetalShower)) {
                lastPetalShower = today;
                handler.postDelayed(this::playPetalShower, 450);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LAUNCHER_SETTINGS_REQUEST && resultCode == RESULT_OK) recreate();
    }

    /** Rebuilds the tiles, then puts focus back where it was. */
    private void reloadApps(boolean animate) {
        // Capture the app before rebinding can move focus and overwrite its key.
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        final boolean restoreFocus = animate || grid.hasFocus();
        if (restoreFocus) {
            glassShown = false;
            glass.setAlpha(0f);
            heroShown = false;
            heroName.setAlpha(0f);
            ambient.setAlpha(0f);
        }
        reloadTiles();
        if (animate && animations && !reducedMotion) grid.scheduleLayoutAnimation();

        if (restoreFocus) {
            // Follow the same app, or the nearest remaining slot when it was removed.
            restoreFocus();
        } else {
            // A package change must not steal focus from a top button or the Inputs popup.
            int pos = adapter.positionOf(returnKey);
            lastFocused = Math.max(0, Math.min(pos >= 0 ? pos : returnSlot, adapter.getItemCount() - 1));
            lastFocusedKey = adapter.keyAt(lastFocused);
            if (pos < 0) hideHero();
        }
    }

    /** Rebuilds the tiles, keeping the remembered focus: rebinding can move focus to the first tile. */
    private void reloadTiles() {
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        adapter.reload();
        lastFocusedKey = returnKey;
        lastFocused = returnSlot;
    }

    /**
     * Return to the app we left from, or the same slot if that app is gone. Always refocus: after
     * reload() the view that holds focus may no longer show the app it had.
     */
    private void restoreFocus() {
        final String returnKey = lastFocusedKey;
        final int returnSlot = lastFocused;
        grid.post(() -> {
            int pos = adapter.positionOf(returnKey);
            focusTile(pos >= 0 ? pos : returnSlot);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAmbientAnimation();
        heroName.animate().cancel();
        ambient.animate().cancel();
        handler.removeCallbacks(minuteTick);
        handler.removeCallbacks(feedTick);
        handler.removeCallbacks(clubTick);
        handler.removeCallbacks(stripTick);
        if (diya != null) diya.stop();
        // Nothing of ours stays on screen over another app.
        if (openPopup != null) openPopup.dismiss();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(packageReceiver);
        rebuildOnResume = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedThread != null) feedThread.shutdownNow();
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim != null) tim.unregisterCallback(inputCallback);
        if (cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
            cm.unregisterNetworkCallback(vpnCallback);
        }
    }

    @Override
    public void onBackPressed() {
        // Home screen: Back goes nowhere.
    }

    /**
     * TextClock (the clock and the Gregorian date) formats with the process default locale, and Android puts the TV's
     * language back there (ClockDream found the same): an Arabic home screen showed "WEDNESDAY 28 OCTOBER" and Latin
     * digits on the TV in 2.9.0 testing. Put the screen's language back and make the clocks format again.
     */
    private void keepLanguage() {
        Locale want = getResources().getConfiguration().getLocales().get(0);
        if (want.equals(Locale.getDefault())) return;
        Locale.setDefault(want);
        for (int id : new int[]{R.id.clock, R.id.ampm, R.id.date}) {
            View v = findViewById(id);
            if (v instanceof android.widget.TextClock) {
                android.widget.TextClock c = (android.widget.TextClock) v;
                c.setFormat12Hour(c.getFormat12Hour());
                c.setFormat24Hour(c.getFormat24Hour());
            }
        }
    }

    // ---- glass highlight -------------------------------------------------------------------------

    @Override
    public void onTileFocused(int position, View card) {
        lastFocused = position;
        lastFocusedKey = adapter.keyAt(position);
        // Tile rect in root coordinates, using layout positions (ignores the scale animation).
        Rect r = new Rect(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, r);
        float pad = 6f * density;        // matches GlassView's glow inset
        float scale = adapter.focusScale();
        float w = card.getWidth() * scale + 2 * pad;
        float h = card.getHeight() * scale + 2 * pad;
        float x = r.exactCenterX() - w / 2f;
        float y = r.exactCenterY() - h / 2f - (advancedEffects ? 4f * density : 0f);

        ViewGroup.LayoutParams lp = glass.getLayoutParams();
        if (lp.width != (int) w || lp.height != (int) h) {
            lp.width = (int) w;
            lp.height = (int) h;
            glass.setLayoutParams(lp);
            // GlassView draws its slab inset by pad, i.e. exactly over the zoomed tile: same corners as the tile.
            glass.setRadius(AppAdapter.cornerRadius(card.getWidth(), density, tileCorners) * scale);
        }
        glassTargetX = x;
        glassTargetY = y;
        if (!glassShown) {
            springX.cancel();
            springY.cancel();
            glass.setTranslationX(x);
            glass.setTranslationY(y);
            if (animations) glass.animate().alpha(1f).setDuration(reducedMotion ? 100 : 180).start();
            else glass.setAlpha(1f);
            glassShown = true;
        } else if (!animations) {
            springX.cancel();
            springY.cancel();
            glass.setTranslationX(x);
            glass.setTranslationY(y);
        } else {
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y - glassScrollShift);
        }
        View item = (View) card.getParent();
        if (position < COLUMNS) {
            // Dock: park it at the bottom of the screen and show the top shelf for this app.
            int decoratedTop = item.getTop() - (int) (6 * density);
            int dy = decoratedTop - grid.getPaddingTop();
            // A 1px mismatch used to start a scroll on every dock move. Only scroll when the row is
            // actually off; reduced motion does it in one jump so it does not fight the highlight.
            boolean scroll = Math.abs(dy) > (int) (8 * density);
            if (scroll) {
                if (!animations || reducedMotion) grid.scrollBy(0, dy);
                else grid.smoothScrollBy(0, dy);
            }
            showHero(position, scroll ? r.top - dy : r.top);
        } else {
            hideHero();
            // Scroll just enough so the whole glass (not only the tile) is visible.
            Rect want = new Rect(0, -(int) pad * 2, item.getWidth(), item.getHeight() + (int) pad);
            grid.requestChildRectangleOnScreen(item, want, reducedMotion);
        }
    }

    // ---- top shelf -------------------------------------------------------------------------------

    /** Space between the focused app's name and the dock's glass (which starts 14dp above the tiles). */
    private static final int HERO_GAP_DP = 12;

    /**
     * The name of the focused app sits between the interests strip and the dock. With the strip showing, the
     * grid used to start 10dp under it and the name ran under the prayer card, so the grid moves down by the
     * name's height whenever the strip is visible.
     */
    private void reserveHeroBand(boolean stripVisible) {
        View wrap = (View) grid.getParent();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) wrap.getLayoutParams();
        // Hero bottom = dock tiles' top - 14dp (glass) - HERO_GAP; the tiles start 10dp (grid padding) into the
        // wrapper. 6dp keeps it clear of the strip's cards.
        int want = stripVisible
                ? (int) ((6 + 14 + HERO_GAP_DP - 10) * density) + heroName.getLineHeight()
                : (int) (10 * density);
        if (lp.topMargin != want) {
            lp.topMargin = want;
            wrap.setLayoutParams(lp);
        }
    }

    private void showHero(int position, float dockTopInRoot) {
        String label = adapter.labelAt(position);
        int color = adapter.ambientColorAt(position);
        float y = dockTopInRoot - 14 * density - HERO_GAP_DP * density - heroName.getHeight();
        boolean changed = !label.contentEquals(heroName.getText());

        if (!animations || reducedMotion) {
            // Snap the shelf instead of sliding and fading it: each of those frames repaints the
            // full-screen panel, and this switch exists for the GPUs that cannot afford it.
            heroName.animate().cancel();
            ambient.animate().cancel();
            heroName.setText(label);
            heroName.setY(y);
            heroName.setTranslationX(0f);
            heroName.setAlpha(1f);
            setAmbient(color);
            if (!animations) {
                ambient.setAlpha(1f);
                heroShown = true;
            } else if (!heroShown) {
                ambient.setAlpha(0f);
                ambient.animate().alpha(1f).setDuration(160).start();
                heroShown = true;
            }
            return;
        }

        if (!heroShown) {
            stopAmbientAnimation();
            heroName.animate().cancel();
            ambient.animate().cancel();
            heroName.setText(label);
            heroName.setY(y);
            heroName.setTranslationX(0f);
            heroName.animate().alpha(1f).setDuration(260).start();
            setAmbient(color);
            ambient.animate().alpha(1f).setDuration(600).start();
            heroShown = true;
        } else if (changed) {
            if (advancedEffects) heroName.animate().cancel();
            heroName.animate().alpha(0f).translationX(-12 * density).setDuration(120).withEndAction(() -> {
                heroName.setText(label);
                heroName.setY(y);
                heroName.setTranslationX(12 * density);
                heroName.animate().alpha(1f).translationX(0f).setDuration(200).start();
            }).start();
            if (advancedEffects) {
                transitionAmbient(color);
            } else if (color != ambientColor) {
                ambient.animate().alpha(0.35f).setDuration(200).withEndAction(() -> {
                    setAmbient(color);
                    ambient.animate().alpha(1f).setDuration(500).start();
                }).start();
            }
        } else {
            heroName.setY(y);
        }
    }

    /**
     * The glow used to be a full-screen radial GradientDrawable. On Android 10 that drawable is
     * drawn on the CPU, so fading it for half a second redrew 1920×1080 every frame and the
     * highlight looked like it was running at a few frames per second. A small bitmap stretched
     * over the screen is one texture. It is painted once in white and tinted with the app's colour,
     * so changing apps neither repaints it on the CPU (about 11 ms) nor uploads it again.
     */
    private void setAmbient(int color) {
        if (color == ambientColor && ambient.getBackground() != null) return;
        ambientColor = color;
        BitmapDrawable glow = (BitmapDrawable) ambient.getBackground();
        if (glow == null) {
            int w = 480;
            int h = 270;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new RadialGradient(
                    w * 0.32f, h * 0.42f, w * 0.62f,
                    new int[]{0x5CFFFFFF, 0x26FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            glow = new BitmapDrawable(getResources(), bmp);
            glow.setGravity(Gravity.FILL);
            ambient.setBackground(glow);
        }
        // The default tint mode, SRC_IN, keeps the mask's alpha and takes the colour from the tint.
        glow.setTint(0xFF000000 | color);
        tintGlass(color);
    }

    /** Header pill: warm-white when focused, tinted glass at rest (the drawable is kept for tintGlass). */
    private Drawable glassPill() {
        float r = 21 * density;
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(getColor(R.color.theme_focus));
        focused.setCornerRadius(r);
        GradientDrawable rest = new GradientDrawable();
        rest.setCornerRadius(r);
        glassPills.add(rest);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, rest);
        return s;
    }

    /** Option C: pills and cards take a touch of the ambient colour (Glass); -1 = no glow yet, neutral. */
    private void tintGlass(int rgb) {
        int stroke = Math.max(1, Math.round(density));
        // -1 is "no glow yet" (ambientColor's start value); a colour may carry alpha bits, so mask it to RGB.
        int c = rgb == -1 ? -1 : rgb & 0xFFFFFF;
        for (GradientDrawable d : glassPills) {
            d.setColor(Glass.pillFill(c));
            d.setStroke(stroke, Glass.edge(c));
        }
        for (GradientDrawable d : glassCards) {
            d.setColor(Glass.cardFill(c));
            d.setStroke(stroke, Glass.edge(c));
        }
        // A strip card under focus keeps its white edge through every repaint (the club card swaps every 5 s).
        View focusedCard = focusedStripCard();
        if (focusedCard != null && focusedCard.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) focusedCard.getBackground()).setStroke(Math.round(2 * density), 0xFFFFFFFF);
        }
        // The floating dock plate takes the same colour; the band ignores it.
        if (dockDecoration != null && DockStyle.floating(dockStyle)) {
            dockDecoration.setAmbient(c);
            grid.invalidate();
        }
    }

    /** Clock style "glass" (ClockStyle): the time sits on a capsule, tinted with the header pills by tintGlass. */
    private void glassClock() {
        TextView clock = findViewById(R.id.clock);
        GradientDrawable capsule = new GradientDrawable();
        capsule.setCornerRadius(60 * density);   // far above half the height: Android clamps it to a capsule
        capsule.setColor(Glass.pillFill(-1));
        capsule.setStroke(Math.max(1, Math.round(density)), Glass.edge(-1));
        glassPills.add(capsule);
        int inset = Math.round(ClockStyle.insetDp(clockStyle) * density);
        clock.setBackground(new InsetDrawable(capsule, 0, inset, 0, inset));   // the capsule keeps clear of the greeting
        int pad = Math.round(ClockStyle.padDp(clockStyle) * density);
        clock.setPadding(pad, 0, pad, 0);   // sides only: the header keeps its height and the dock its place
    }

    /** Remote buttons mapped in config.json "app_keys" (default: the YouTube button, 190), while Home is in front. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        String pkg = appKeys.get(keyCode);
        if (AppKeys.isIgnore(pkg)) return true;   // "ignore": the button does nothing
        if (pkg != null) {
            if (event.getRepeatCount() == 0) {
                Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) open(i);
                else Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void transitionAmbient(int color) {
        // A rapid focus change continues from the colour actually displayed on screen.
        stopAmbientAnimation();
        ambient.animate().cancel();
        if (color == ambientColor) {
            ambient.animate().alpha(1f).setDuration(180).start();
            return;
        }
        ambient.animate().alpha(1f).setDuration(180).start();
        ambientAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), ambientColor, color);
        ambientAnimator.setDuration(360);
        ambientAnimator.addUpdateListener(animation -> setAmbient((int) animation.getAnimatedValue()));
        ambientAnimator.start();
    }

    private void stopAmbientAnimation() {
        if (ambientAnimator != null) {
            ambientAnimator.cancel();
            ambientAnimator = null;
        }
    }

    private void hideHero() {
        if (!heroShown) return;
        stopAmbientAnimation();
        heroName.animate().cancel();
        ambient.animate().cancel();
        heroShown = false;
        if (animations) {
            heroName.animate().alpha(0f).setDuration(reducedMotion ? 100 : 160).start();
            ambient.animate().alpha(0f).setDuration(reducedMotion ? 140 : 300).start();
        } else {
            heroName.setAlpha(0f);
            ambient.setAlpha(0f);
        }
    }

    private void hideGlass() {
        if (!glassShown) return;
        glassShown = false;
        glass.animate().cancel();
        if (animations) glass.animate().alpha(0f).setDuration(160).start();
        else glass.setAlpha(0f);
    }

    // ---- navigation ------------------------------------------------------------------------------

    @Override
    public boolean onTileVerticalKey(int position, int keyCode) {
        int count = adapter.getItemCount();
        int target;
        // Horizontal: swallow the key at a row's ends so focus never wraps to the next row or
        // escapes up into the top bar. Mid-row moves fall through to the default focus search.
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // In the dock, the last app is the row's end: the empty slots after it take no focus.
            int rowCount = position < COLUMNS ? Math.min(count, adapter.dockFilled()) : count;
            return GridNavigation.atHorizontalEdge(position, rowCount, COLUMNS,
                    rtlAppGrid, keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (position < COLUMNS) {
                // Dock row: the strip cards first, the top bar when the strip shows nothing.
                if (!focusNearestCard(getCurrentFocus(), true)) focusNearestButton(getCurrentFocus());
                return true;
            }
            target = position - COLUMNS;
        } else {
            int lastRowStart = ((count - 1) / COLUMNS) * COLUMNS;
            if (position >= lastRowStart) return true;     // already on the last row: stay put
            target = Math.min(position + COLUMNS, count - 1);
        }
        focusTile(target);
        // The default focus search clicks for Left/Right; Up/Down are handled here, so click for them too.
        grid.playSoundEffect(keyCode == KeyEvent.KEYCODE_DPAD_UP
                ? SoundEffectConstants.NAVIGATION_UP : SoundEffectConstants.NAVIGATION_DOWN);
        return true;
    }

    private void focusTile(int position) {
        if (adapter.getItemCount() == 0) {
            btnWifi.requestFocus();
            return;
        }
        final int pos = adapter.focusablePosition(Math.max(0, Math.min(position, adapter.getItemCount() - 1)));
        RecyclerView.ViewHolder vh = grid.findViewHolderForAdapterPosition(pos);
        if (vh != null) {
            View card = vh.itemView.findViewById(R.id.card);
            // Already focused (e.g. back from an app): no focus change fires, so restore the glass by hand.
            if (card.isFocused()) onTileFocused(pos, card);
            else card.requestFocus();
            return;
        }
        // Not laid out yet: scroll there, then focus once the scroll settles.
        if (!animations) {
            grid.scrollToPosition(pos);
            grid.post(() -> {
                RecyclerView.ViewHolder h = grid.findViewHolderForAdapterPosition(pos);
                if (h != null) h.itemView.findViewById(R.id.card).requestFocus();
            });
            return;
        }
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                rv.removeOnScrollListener(this);
                RecyclerView.ViewHolder h = rv.findViewHolderForAdapterPosition(pos);
                View target = h != null ? h.itemView.findViewById(R.id.card) : rv.getChildAt(0);
                if (target != null) target.requestFocus();
            }
        });
        grid.smoothScrollToPosition(pos);
    }

    // ---- top bar ---------------------------------------------------------------------------------

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int res;
        if (hour < 5) res = R.string.good_night;
        else if (hour < 12) res = R.string.good_morning;
        else if (hour < 17) res = R.string.good_afternoon;
        else if (hour < 22) res = R.string.good_evening;
        else res = R.string.good_night;
        String festiveGreeting = festive ? festival.greetingText() : "";
        if (!festiveGreeting.isEmpty()) {
            greeting.setText(festiveGreeting);
            greeting.setTextColor(liftTowardsWhite(festival.accentColor(), 0.25f));
        } else {
            greeting.setText(res);
            greeting.setTextColor(greetingColor);
        }
        if (shamsi) shamsiDate.setText(Shamsi.today());
        if (interestsCfg.hijriDate) hijriDate.setText(Hijri.format(this, Hijri.today(), arabic()));
        updateInterests();
    }

    // ---- interests strip (design system: PrayerCard, MatchCard, NewsCard) --------------------------

    private boolean arabic() {
        return "ar".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    /** Online build: fetch off the main thread, then redraw. One fetch at a time; a failure hides that card. */
    private void fetchFeeds() {
        if (feedThread == null) feedThread = Executors.newSingleThreadExecutor();
        final Interests.Settings s = interestsCfg;
        final List<String> feeds = s.feeds(arabic());
        feedThread.execute(() -> {
            List<Interests.Match> m = s.wantsMatches() ? Feeds.matches(s) : Collections.emptyList();
            List<Interests.Headline> h = feeds.isEmpty() ? Collections.emptyList() : Feeds.headlines(feeds);
            Interests.Weather w = s.wantsWeather() ? Feeds.weather(s.prayerLat, s.prayerLng) : null;
            if (s.wantsClubs()) {
                long now = System.currentTimeMillis();
                List<Interests.Match> c = clubMatches;
                if (c.isEmpty() || now - clubScheduleMs >= CLUB_SCHEDULE_MS || !clubScheduleIds.equals(s.favoriteTeams)) {
                    c = Feeds.clubSchedules(s.favoriteTeams);
                    clubScheduleMs = now;
                    clubScheduleIds = s.favoriteTeams;
                }
                clubMatches = Feeds.refreshLive(c, now);
            }
            matches = m;
            headlines = h;
            weather = w;
            handler.post(this::updateInterests);
        });
    }

    /**
     * Rebuilds the strip, once a minute and after each fetch. Its cards take focus between the dock and the top bar:
     * OK on the club card shows the next club match, on the rotating league or news card the next item, on the
     * others Launcher settings (where each card can be switched off). The text size (Launcher settings) sets how
     * many places the one row has: 5, 4 or 3; when there are more cards, the last place shows the rest in turn
     * (every {@link #STRIP_TURN_MS}, or OK on it).
     */
    private void updateInterests() {
        if (interestsStrip == null) return;
        boolean ar = arabic();
        glassCards.clear();
        int focusedCard = focusedCardIndex();
        long now = System.currentTimeMillis();
        // Order: prayer, the favourite clubs, the leagues, weather, news. The clubs come before the leagues so the
        // viewer's own teams are the first football card, and a large text size rotates the least wanted ones.
        List<View> cards = new ArrayList<>();
        if (interestsCfg.prayer()) cards.add(onCardClick(prayerCard(ar), this::openLauncherSettings));
        clubCard = null;
        if (interestsCfg.wantsClubs()) {
            List<Interests.Match> clubs = Interests.pickClubs(clubMatches, interestsCfg.favoriteTeams, now);
            if (!clubs.isEmpty()) cards.add(clubCard = clubCard(clubs, ar));
        }
        // One match per league (a live one, else the next), shown in turn: a Champions League game that is live
        // tonight must not hide the Saudi league's next round, and the other way round.
        if (!interestsCfg.wantsMatches()) {
            // Football switched off on the TV: no league card at all.
        } else if (!interestsCfg.sportsCards.isEmpty()) {
            // sports_cards: one card per league group, side by side (e.g. the Saudi league and the big foreign ones).
            for (Interests.Match m : Interests.pickCards(matches, interestsCfg.sportsCards, interestsCfg.sportsTeams, now)) {
                cards.add(onCardClick(matchCard(m, ar, ""), this::openLauncherSettings));
            }
        } else {
            List<Interests.Match> picks = Interests.pickMatches(matches, interestsCfg.sportsTeams, now);
            // OK on the rotating card shows the next league at once.
            if (!picks.isEmpty()) cards.add(onCardClick(matchCard(picks.get(matchIndex++ % picks.size()), ar, ""),
                    this::updateInterests));
        }
        Interests.Weather w = weather;
        if (w != null && interestsCfg.wantsWeather()) cards.add(onCardClick(weatherCard(w, ar), this::openLauncherSettings));
        List<Interests.Headline> h = headlines;
        if (!h.isEmpty()) cards.add(onCardClick(newsCard(h.get(headlineIndex++ % h.size()), ar), this::updateInterests));

        interestsStrip.removeAllViews();
        int perRow = StripSize.perRow(textSize);
        boolean rtl = getResources().getBoolean(R.bool.rtl_menu_bar);
        stripShared = StripSize.shared(cards.size(), perRow);
        cards = StripSize.visible(cards, perRow, stripTurn);
        if (stripShared) {
            // The shared last place: the same width whichever card has its turn, and OK shows the next one.
            View last = cards.get(cards.size() - 1);
            ((LinearLayout.LayoutParams) last.getLayoutParams()).weight = 1f;
            if (last != clubCard) last.setOnClickListener(v -> nextStripTurn());
        }
        List<List<View>> rows = Collections.singletonList(cards);
        tintGlass(ambientColor);
        for (int r = 0; r < rows.size(); r++) {
            List<View> row = new ArrayList<>(rows.get(r));
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setBaselineAligned(false);
            // A short last row hugs the reading side, under the first card of the row above.
            line.setGravity(rtl ? Gravity.RIGHT : Gravity.LEFT);
            float weights = 0f;
            for (View c : row) weights += ((LinearLayout.LayoutParams) c.getLayoutParams()).weight;
            // Never less than a full row of plain cards, so a lone card keeps its width instead of stretching.
            line.setWeightSum(StripSize.rowWeightSum(weights, perRow));
            if (rtl) Collections.reverse(row);
            for (int i = 0; i < row.size(); i++) {
                View c = row.get(i);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) c.getLayoutParams();
                // The gap sits between cards only, so the outer card lines up with the header's edge.
                lp.leftMargin = i > 0 ? (int) (16 * density) : 0;
                line.addView(c);
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (r > 0) rowLp.topMargin = (int) (12 * density);
            interestsStrip.addView(line, rowLp);
        }
        interestsStrip.setVisibility(cards.isEmpty() ? View.GONE : View.VISIBLE);
        reserveHeroBand(!cards.isEmpty());
        // The strip is rebuilt every minute: a card that had focus hands it to the card now in its place.
        if (focusedCard >= 0) {
            List<View> shown = stripCards();
            if (!shown.isEmpty()) shown.get(Math.min(focusedCard, shown.size() - 1)).requestFocus();
            else focusTile(lastFocused % COLUMNS);
        }
    }

    /** Seconds a card keeps the shared last place of the strip before the next one takes it. */
    private static final long STRIP_TURN_MS = 8_000L;
    /** Which of the cards that share the last place is shown; counts up. */
    private int stripTurn;
    /** True while some cards share the last place (more cards than the text size has places). */
    private boolean stripShared;
    private final Runnable stripTick = new Runnable() {
        @Override
        public void run() {
            if (stripShared) nextStripTurn();
            handler.postDelayed(this, STRIP_TURN_MS);
        }
    };

    private void nextStripTurn() {
        stripTurn++;
        updateInterests();
    }

    // ---- strip cards and the D-pad: dock <-> cards <-> top bar --------------------------------------

    /** Every card of the strip, row by row in screen order (left to right inside a row). */
    private List<View> stripCards() {
        List<View> out = new ArrayList<>();
        if (interestsStrip == null) return out;
        for (int r = 0; r < interestsStrip.getChildCount(); r++) {
            ViewGroup row = (ViewGroup) interestsStrip.getChildAt(r);
            for (int i = 0; i < row.getChildCount(); i++) out.add(row.getChildAt(i));
        }
        return out;
    }

    /** The strip card that has focus, or null. */
    private View focusedStripCard() {
        View f = getCurrentFocus();
        if (f == null || interestsStrip == null || !(f.getParent() instanceof View)) return null;
        return ((View) f.getParent()).getParent() == interestsStrip ? f : null;
    }

    private int focusedCardIndex() {
        View f = focusedStripCard();
        return f == null ? -1 : stripCards().indexOf(f);
    }

    private View onCardClick(View card, Runnable action) {
        card.setOnClickListener(v -> action.run());
        return card;
    }

    private void openLauncherSettings() {
        startActivityForResult(new Intent(this, LauncherSettingsActivity.class), LAUNCHER_SETTINGS_REQUEST);
    }

    /**
     * The strip card closest to {@code from} horizontally, focused: in the last row when coming up from the dock,
     * in the first row when coming down from the top bar. False when the strip shows nothing.
     */
    private boolean focusNearestCard(View from, boolean fromBelow) {
        if (interestsStrip == null || interestsStrip.getVisibility() != View.VISIBLE
                || interestsStrip.getChildCount() == 0) return false;
        return focusNearestIn((ViewGroup) interestsStrip.getChildAt(fromBelow ? interestsStrip.getChildCount() - 1 : 0),
                from);
    }

    private boolean focusNearestIn(ViewGroup row, View from) {
        if (row.getChildCount() == 0) return false;
        View best = row.getChildAt(0);
        for (int i = 1; i < row.getChildCount(); i++) best = nearest(from, best, row.getChildAt(i));
        return best.requestFocus();
    }

    /** The top-bar button closest to {@code from} horizontally. */
    private void focusNearestButton(View from) {
        View best = btnWifi;
        for (View b : new View[]{btnInputs, findViewById(R.id.btnSettings)}) {
            if (b != null && b.getVisibility() == View.VISIBLE) best = nearest(from, best, b);
        }
        best.requestFocus();
    }

    private static View nearest(View from, View a, View b) {
        if (from == null) return a;
        float x = centreX(from);
        return Math.abs(centreX(b) - x) < Math.abs(centreX(a) - x) ? b : a;
    }

    private static float centreX(View v) {
        int[] at = new int[2];
        v.getLocationOnScreen(at);
        return at[0] + v.getWidth() / 2f;
    }

    /**
     * Focus look and arrow keys of a strip card: Up to the row above or the top bar, Down to the row below or the
     * dock, no wrap at a row's ends.
     */
    private void makeCardFocusable(LinearLayout c) {
        c.setFocusable(true);
        c.setClickable(true);
        // A white edge only, no zoom: the outer cards sit on the strip's edge, where a zoom would be clipped.
        c.setOnFocusChangeListener((v, focused) -> tintGlass(ambientColor));
        c.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            ViewGroup row = (ViewGroup) v.getParent();
            int r = interestsStrip.indexOfChild(row);
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (r > 0) focusNearestIn((ViewGroup) interestsStrip.getChildAt(r - 1), v);
                else focusNearestButton(v);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (r >= 0 && r < interestsStrip.getChildCount() - 1) {
                    focusNearestIn((ViewGroup) interestsStrip.getChildAt(r + 1), v);
                } else {
                    focusTile(lastFocused % COLUMNS);
                }
                return true;
            }
            int at = row.indexOfChild(v);
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return at == 0;
            return keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && at == row.getChildCount() - 1;
        });
    }

    private View prayerCard(boolean ar) {
        Calendar now = Calendar.getInstance();
        boolean ramadan = Hijri.isRamadan(Hijri.today());
        double tz = TimeZone.getDefault().getOffset(now.getTimeInMillis()) / 3_600_000.0;
        double[] t = interestsCfg.prayerTimes(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), tz, ramadan);
        String city = interestsCfg.city(ar);
        String meta = getString(R.string.prayer_times) + (city.isEmpty() ? "" : " · " + city);
        if (t == null) {
            // A method name the launcher does not know: say so instead of quietly using another method.
            return card(meta, false, getString(R.string.prayer_method_unknown, interestsCfg.prayerMethodKey), null, 1.4f);
        }
        double nowH = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0
                + now.get(Calendar.SECOND) / 3600.0;
        String[] names = getResources().getStringArray(R.array.prayer_names);
        boolean twelve = !android.text.format.DateFormat.is24HourFormat(this);
        int next = Prayer.next(t, nowH);
        String title, big;
        if (next >= 0) {
            title = getString(R.string.prayer_in, names[next]);
            big = Prayer.countdown(nowH, t[next]);
        } else {
            // After Isha: tomorrow's Fajr, counted across midnight, with tomorrow's UTC offset (a clock change
            // at midnight, as in Egypt or Morocco, must not move it by an hour).
            Calendar tm = (Calendar) now.clone();
            tm.add(Calendar.DAY_OF_MONTH, 1);
            double tzTomorrow = TimeZone.getDefault().getOffset(tm.getTimeInMillis()) / 3_600_000.0;
            double fajr = Prayer.onTodaysClock(interestsCfg.prayerTimes(tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1,
                    tm.get(Calendar.DAY_OF_MONTH), tzTomorrow, ramadan)[Prayer.FAJR], tz, tzTomorrow);
            title = getString(R.string.prayer_in, names[Prayer.FAJR]);
            big = Prayer.countdown(nowH, fajr + 24);
        }
        StringBuilder all = new StringBuilder();
        for (int i : Prayer.FIVE) {
            if (all.length() > 0) all.append("  ");
            // A no-break space between name and time: a narrow card wraps between prayers, never inside one
            // (at Large, "Maghrib" ended one line and its time began the next).
            all.append(names[i]).append('\u00A0').append(Prayer.clock(t[i], twelve));
        }
        LinearLayout c = card(meta, false, title, big, 1.4f);
        // Up to two lines: the five times do not fit one line in a lone prayer card on every font and locale
        // (Mi TV, en-GB, cut off at "Isha…"), and a wrapped line beats a missing prayer.
        TextView times = cardText(all.toString(), 13, R.color.theme_dim, false);
        // Five or four cards in a row leave the prayer card narrow: a third line beats a missing prayer.
        times.setMaxLines(StripSize.perRow(textSize) > 3 ? 3 : 2);
        times.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(times);
        return c;
    }

    /**
     * Four lines: when (or LIVE and the minute), the league, the two teams, then the score once it has one. A match
     * with one of the favourite clubs gets a gold star before the league, on the league cards too.
     */
    private View matchCard(Interests.Match m, boolean ar, String prefix) {
        boolean live = Interests.Match.LIVE.equals(m.state);
        String when;
        if (live) when = getString(R.string.match_live) + " " + m.clock;
        else {
            // Day, date and time, short and with the clock isolated: the next match can be days away.
            Locale loc = getResources().getConfiguration().getLocales().get(0);
            when = Interests.whenText(m.startMillis, loc, TimeZone.getDefault(),
                    android.text.format.DateFormat.is24HourFormat(this));
        }
        String big = live || Interests.Match.POST.equals(m.state) ? m.homeScore + " – " + m.awayScore : null;
        String league = m.league, home = m.home, away = m.away;
        if (ar) {
            // ESPN has no Arabic: league by code, teams by ESPN id (assets/names_ar.json), English if unknown.
            NamesAr n = NamesAr.get(this);
            league = NamesAr.or(n.leagues, m.code, league);
            home = NamesAr.or(n.teams, m.homeId, home);
            away = NamesAr.or(n.teams, m.awayId, away);
        }
        CharSequence sub = prefix + league;
        if (Interests.involvesFavorite(m, interestsCfg.favoriteTeams)) {
            SpannableString starred = new SpannableString(STAR + sub);
            starred.setSpan(new android.text.style.ForegroundColorSpan(getColor(R.color.theme_gold)), 0, 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sub = starred;
        }
        return card(when, live, sub, home + "  ×  " + away, big, 1f);
    }

    /** Marks a match of a favourite club: a black star and a space, before the league line. */
    private static final String STAR = "★ ";

    /**
     * The favourite clubs' match whose turn it is (each club's next two, see Interests.pickClubs): the match card's
     * four lines, with the position first so a viewer knows more follow. OK skips to the next one.
     */
    private View clubCard(List<Interests.Match> clubs, boolean ar) {
        int i = Math.floorMod(clubIndex, clubs.size());
        Interests.Match m = clubs.get(i);
        // The position goes first: a long league name is cut at the end, never the "3/8" a viewer looks for.
        String count = clubs.size() > 1 ? (i + 1) + "/" + clubs.size() + "  ·  " : "";
        View c = matchCard(m, ar, count);
        // OK on the club card skips to the next match, and the timer starts again from there.
        c.setOnClickListener(v -> {
            handler.removeCallbacks(clubTick);
            clubTick.run();
        });
        return c;
    }

    /** Swaps only the club card (no strip rebuild), with a short fade, when there is more than one match to show. */
    private void showClubCard() {
        View old = clubCard;
        if (old == null || interestsStrip == null || !(old.getParent() instanceof LinearLayout)) return;
        LinearLayout row = (LinearLayout) old.getParent();
        if (row.getParent() != interestsStrip) return;
        List<Interests.Match> clubs = Interests.pickClubs(clubMatches, interestsCfg.favoriteTeams,
                System.currentTimeMillis());
        if (clubs.size() < 2) return;
        int at = row.indexOfChild(old);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) old.getLayoutParams();
        glassCards.remove(old.getBackground());
        View next = clubCard(clubs, arabic());
        next.setLayoutParams(lp);
        boolean hadFocus = old.hasFocus();
        row.removeViewAt(at);
        row.addView(next, at);
        clubCard = next;
        if (hadFocus) next.requestFocus();
        tintGlass(ambientColor);
        if (animations && !reducedMotion) {
            next.setAlpha(0f);
            next.animate().alpha(1f).setDuration(350).start();
        }
    }

    /** Narrower than the others: a condition and a number need less room than two team names or a headline. */
    private static final float WEATHER_WEIGHT = 0.8f;

    private View weatherCard(Interests.Weather w, boolean ar) {
        String city = interestsCfg.city(ar);
        String label;
        switch (Interests.sky(w.code)) {
            case Interests.SKY_CLEAR: label = getString(R.string.weather_clear); break;
            case Interests.SKY_PARTLY: label = getString(R.string.weather_partly_cloudy); break;
            case Interests.SKY_CLOUDY: label = getString(R.string.weather_cloudy); break;
            case Interests.SKY_FOG: label = getString(R.string.weather_fog); break;
            case Interests.SKY_DRIZZLE: label = getString(R.string.weather_drizzle); break;
            case Interests.SKY_RAIN: label = getString(R.string.weather_rain); break;
            case Interests.SKY_SNOW: label = getString(R.string.weather_snow); break;
            case Interests.SKY_STORM: label = getString(R.string.weather_storm); break;
            default: label = ""; break;
        }
        // The city on its own line: beside "Weather" it was cut in this narrow card ("Weather · Al-A...").
        return card(getString(R.string.weather), false, city.isEmpty() ? null : city, label, w.temperature(),
                WEATHER_WEIGHT);
    }

    private View newsCard(Interests.Headline h, boolean ar) {
        String meta = getString(R.string.news) + (h.source.isEmpty() ? "" : " · " + h.source);
        return card(meta, false, h.title, null, 1f);
    }

    /** A strip card: meta line, title (two lines at most) and an optional big number. */
    private LinearLayout card(String meta, boolean live, String title, String big, float weight) {
        return card(meta, live, null, title, big, weight);
    }

    /** As above with an optional second small line under the meta (the league on a match card). */
    private LinearLayout card(String meta, boolean live, CharSequence sub, String title, String big, float weight) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16 * density);
        glassCards.add(bg);
        c.setBackground(bg);
        int pad = (int) (16 * density);
        c.setPadding(pad, (int) (12 * density), pad, (int) (12 * density));
        // A share of the strip, not a fixed width: the TV draws 1080p at density 2 (960dp wide), where three
        // fixed 360-440dp cards ran off the edge. STRIP_WEIGHTS keeps a lone card from stretching across it.
        // MATCH_PARENT inside the wrap_content strip: LinearLayout measures every card to the tallest one, so the
        // row reads as one band instead of boxes of four different heights.
        c.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight));
        makeCardFocusable(c);
        c.addView(cardText(meta, 13, live ? R.color.theme_red : R.color.theme_dim, true));
        if (sub != null) c.addView(cardText(sub, 13, R.color.theme_dim, true));
        TextView t = cardText(title, 16, R.color.theme_sand, false);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(t);
        if (big != null) c.addView(cardText(big, 26, R.color.theme_clock, true));
        return c;
    }

    /**
     * IBM Plex Sans Arabic (OFL, google/fonts 9862abc0) for the cards: Arabic and Latin drawn as one family, so an
     * Arabic card and an English team name read as the same design. Chosen in 2.9 over Readex Pro, Noto Kufi, Noto
     * Sans Arabic, Tajawal, Cairo and Almarai by a side-by-side render: it is the narrowest of them at the same
     * size, so the five prayer times fit a fifth of the screen. Light for the big numbers, Regular for the rest.
     */
    private android.graphics.Typeface cardFont, cardFontLight;

    private TextView cardText(CharSequence s, float sp, int colorRes, boolean singleLine) {
        TextView t = new TextView(this);
        if (cardFont == null) {
            cardFont = getResources().getFont(R.font.plex_arabic);
            cardFontLight = getResources().getFont(R.font.plex_arabic_light);
        }
        // Big numbers light like the clock, everything else regular.
        t.setTypeface(sp >= 24 ? cardFontLight : cardFont);
        t.setText(s);
        // Small keeps the 2.8 sizes; medium and large scale every line of every card alike.
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sp * StripSize.scale(textSize));
        t.setTextColor(getColor(colorRes));
        t.setGravity(getResources().getBoolean(R.bool.rtl_menu_bar) ? Gravity.RIGHT : Gravity.LEFT);
        // Each line takes its own direction from its first letter: an Arabic headline reads right to left
        // even in the English layout, and an English team name stays left to right in the Arabic one.
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setIncludeFontPadding(false);
        t.setPadding(0, (int) (3 * density), 0, (int) (3 * density));
        if (singleLine) {
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
        }
        return t;
    }

    /** Shows the Wi-Fi network name on the button when we can read it, otherwise the connection type. */
    private void updateWifiLabel() {
        String text = getString(R.string.wifi);
        boolean vpn = false, online = false;
        int icon = R.drawable.ic_wifi;
        // The diya's flame: 0 is out (offline), 1 to 4 follow the Wi-Fi signal, Ethernet burns steady at 4.
        int flame = 0;
        boolean steady = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean wifi = false, ethernet = false;
            // All networks, not just the active one: with a VPN up the active network is the VPN itself.
            for (Network net : cm != null ? cm.getAllNetworks() : new Network[0]) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                if (caps == null) continue;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    vpn = true;
                    continue;
                }
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue;
                online = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) ethernet = true;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true;
            }
            if (!online) {
                text = getString(R.string.offline);
                vpn = false;
            } else if (ethernet) {
                text = getString(R.string.ethernet);
                icon = R.drawable.ic_ethernet;
                flame = 4;
                steady = true;
            } else if (wifi) {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
                String ssid = info != null ? info.getSSID() : null;
                flame = info != null ? Math.max(1, Math.min(4, WifiManager.calculateSignalLevel(info.getRssi(), 5))) : 3;
                if (ssid != null) {
                    ssid = ssid.replace("\"", "").trim();
                    if (!ssid.isEmpty() && !ssid.contains("unknown") && !ssid.equals("0x")) text = ssid;
                }
            }
        } catch (Exception ignored) {
        }
        // With a VPN app configured, a VPN that is down matters too: show a red "VPN off" capsule then.
        boolean vpnDown = !vpn && online && !vpnApp.isEmpty();
        if (vpn || vpnDown) {
            // Network name, then the green lock-and-VPN (or red "VPN off") capsule drawn in place of the word.
            String badge = getString(vpn ? R.string.vpn : R.string.vpn_off);
            VpnBadge span = new VpnBadge(vpn ? getDrawable(R.drawable.ic_lock) : null, density);
            // The pill has a maximum width and ellipsizes at the end, which would cut the badge off after
            // a long network name, so shorten the name to whatever room the badge leaves. The total paddings
            // include the Wi-Fi icon.
            TextPaint paint = btnWifi.getPaint();
            float room = btnWifi.getMaxWidth() - btnWifi.getTotalPaddingStart() - btnWifi.getTotalPaddingEnd()
                    - span.getSize(paint, badge, 0, badge.length(), null) - paint.measureText(" ") - 2 * density;
            text = TextUtils.ellipsize(text, paint, Math.max(0, room), TextUtils.TruncateAt.END).toString();
            SpannableString label = new SpannableString(text + " " + badge);
            label.setSpan(span, text.length() + 1, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            btnWifi.setText(label);
        } else {
            btnWifi.setText(text);
        }
        if (festive && diya != null) {
            if (online && flame == 0) flame = 3;   // online some other way (cellular, USB tethering)
            diya.setSteady(steady);
            diya.setFlame(flame);
        } else {
            btnWifi.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        }
    }

    // ---- festive skin ------------------------------------------------------------------------------

    /**
     * Swaps the pill icons for the festival's: a bell for Inputs that swings when focused, a diya for the
     * network whose flame follows the connection, and a marigold for Settings that turns when focused.
     */
    private void applyFestiveSkin(TextView inputs, TextView settings) {
        bell = new BellDrawable(density);
        marigold = new MarigoldDrawable(density);
        diya = new DiyaDrawable(density);
        bell.setReducedMotion(reducedMotion);
        marigold.setReducedMotion(reducedMotion);
        diya.setReducedMotion(reducedMotion);
        setFestiveIcon(inputs, bell);
        setFestiveIcon(btnWifi, diya);
        setFestiveIcon(settings, marigold);
        addFocusAction(inputs, bell::ring);
        addFocusAction(settings, marigold::turn);
    }

    private static void setFestiveIcon(TextView pill, Drawable icon) {
        // The pills tint their icons to follow focus; the festive ones keep their own colours.
        pill.setCompoundDrawableTintList(null);
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    /** Runs {@code action} when the view gains focus, keeping any focus listener it already had. */
    private static void addFocusAction(View view, Runnable action) {
        View.OnFocusChangeListener previous = view.getOnFocusChangeListener();
        view.setOnFocusChangeListener((v, focused) -> {
            if (previous != null) previous.onFocusChange(v, focused);
            if (focused) action.run();
        });
    }

    private void playPetalShower() {
        if (isFinishing() || isDestroyed()) return;
        PetalShowerView shower = new PetalShowerView(this);
        root.addView(shower, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shower.play(60, 2200, () -> root.removeView(shower));
    }

    private static int liftTowardsWhite(int argb, float amount) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** The network pill opens the VPN app when one is configured and installed, otherwise Wi-Fi settings. */
    private void openNetworkPill() {
        Intent i = vpnApp.isEmpty() ? null : getPackageManager().getLeanbackLaunchIntentForPackage(vpnApp);
        if (i == null && !vpnApp.isEmpty()) i = getPackageManager().getLaunchIntentForPackage(vpnApp);
        open(i != null ? i : systemIntent(Settings.ACTION_WIFI_SETTINGS));
    }


    /**
     * China-market MIUI TV firmware answers no standard Settings intents at all; open Xiaomi's own
     * settings app (com.xiaomi.mitv.settings) there, so the Settings and Wi-Fi pills keep working.
     * On firmware with a standard settings handler the behaviour is unchanged.
     */
    private Intent systemIntent(String standardAction) {
        Intent i = new Intent(standardAction);
        if (i.resolveActivity(getPackageManager()) != null) return i;
        i = new Intent().setComponent(new ComponentName(
                "com.xiaomi.mitv.settings", "com.xiaomi.mitv.settings.entry.MainActivity"));
        if (i.resolveActivity(getPackageManager()) != null) return i;
        return new Intent(standardAction);   // nothing found: let open() show the can't-open toast
    }

    /** Hides the Inputs pill when it would have nothing to list: streaming boxes have no HDMI-in, AV or tuner. */
    private void updateInputsButton() {
        boolean any = liveTvIntent() != null || !sourcePanelService.isEmpty();
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (!any && tim != null) {
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.isPassthroughInput()) {
                    any = true;
                    break;
                }
            }
        }
        btnInputs.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    /** The Live TV app's launch intent: its leanback entry point, or the configured action if it has none. */
    private Intent liveTvIntent() {
        if (liveTvPkg.isEmpty()) return null;
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(liveTvPkg);
        if (i != null || liveTvAction.isEmpty()) return i;
        Intent byAction = new Intent(liveTvAction).setPackage(liveTvPkg);
        return getPackageManager().resolveActivity(byAction, 0) != null ? byAction : null;
    }

    /** The TV's own source-selection panel, for TVs that have no TvInputManager (TCL Android TV 9). */
    private void openSourcePanel() {
        Intent i = new Intent(sourcePanelAction);
        ComponentName c = ComponentName.unflattenFromString(sourcePanelService);
        if (c != null) i.setComponent(c);
        try {
            startService(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void open(Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    /** Switches to a passthrough input (HDMI, AV). The standard way is ACTION_VIEW on its channel URI,
     *  which the Live TV app handles; where nothing takes that, the configured `input_action` is sent
     *  with the same URI instead (XGIMI's HDMI player only answers com.xgimi.action.hdmiPlayer). */
    private void openInput(String inputId) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
        Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
            return;
        } catch (ActivityNotFoundException e) {
            if (inputAction.isEmpty()) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        open(new Intent(inputAction).setDataAndType(uri, "vnd.android.cursor.item/channel"));
    }

    /** Dropdown menu under the Inputs pill: HDMI ports (with connection state), AV and Live TV. */
    private void showInputs() {
        View anchor = findViewById(R.id.btnInputs);
        View panel = getLayoutInflater().inflate(R.layout.popup_inputs, root, false);
        LinearLayout rows = panel.findViewById(R.id.rows);

        // Compact: as wide as the Inputs pill (a touch wider so "HDMI 1" fits), hanging right below it.
        int width = Math.max(anchor.getWidth(), (int) (150 * density));
        final PopupWindow popup = glassPopup(panel, width);

        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        boolean sawNonHdmi = false;
        String tunerId = null;
        boolean tunerIsHw = false;
        if (tim != null) {
            List<TvInputInfo> inputs = new ArrayList<>(tim.getTvInputList());
            // HDMI ports first in port order, then AV and the rest.
            Collections.sort(inputs, (a, b) -> {
                boolean ha = a.getType() == TvInputInfo.TYPE_HDMI, hb = b.getType() == TvInputInfo.TYPE_HDMI;
                if (ha != hb) return ha ? -1 : 1;
                return String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this)));
            });
            // Some TVs (Sony) report every HDMI port as connected whether or not anything is plugged in,
            // so drop the HDMI status then. A TV with every port really in use loses it too.
            int hdmiPorts = 0, hdmiConnected = 0;
            for (TvInputInfo info : inputs) {
                if (!info.isPassthroughInput() || info.getType() != TvInputInfo.TYPE_HDMI) continue;
                hdmiPorts++;
                if (tim.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED) hdmiConnected++;
            }
            boolean hdmiStateBogus = hdmiPorts > 1 && hdmiConnected == hdmiPorts;
            for (TvInputInfo info : inputs) {
                if (info.getType() == TvInputInfo.TYPE_TUNER
                        && (info.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    // Prefer a hardware tuner: the framework names those ".../HW<n>". System apps such as
                    // AirPlay also register software inputs of type TUNER and may sort first (seen on TCL G05).
                    boolean hw = info.getId().matches(".*/HW\\d+$");
                    if (tunerId == null || (hw && !tunerIsHw) || (hw == tunerIsHw && info.getId().compareTo(tunerId) < 0)) {
                        tunerId = info.getId();
                        tunerIsHw = hw;
                    }
                }
                if (!info.isPassthroughInput()) continue;
                CharSequence l = info.loadLabel(this);
                String label = l == null ? info.getId() : l.toString();
                boolean hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
                if (hdmi && !label.toUpperCase(Locale.ROOT).contains("HDMI")) label = "HDMI " + label;
                final String id = info.getId();
                int state = hdmi && hdmiStateBogus ? -1 : tim.getInputState(id);
                if (!hdmi && rows.getChildCount() > 0 && !sawNonHdmi) addSeparator(rows);
                if (!hdmi) sawNonHdmi = true;
                addInputRow(rows, popup, hdmi ? R.drawable.ic_input : R.drawable.ic_av, label, state,
                        () -> openInput(id));
            }
        }
        // Nothing to list where there is no TvInputManager: the TV's own source panel switches inputs.
        if (rows.getChildCount() == 0 && !sourcePanelService.isEmpty()) {
            addInputRow(rows, popup, R.drawable.ic_input, getString(R.string.inputs), -1, this::openSourcePanel);
        }
        Intent liveTv = liveTvIntent();
        if (liveTv != null && tunerId != null) {
            liveTv = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(TvContract.buildChannelUriForPassthroughInput(tunerId), "vnd.android.cursor.item/channel")
                    .setPackage(liveTvPkg);
        }
        if (liveTv != null) {
            if (rows.getChildCount() > 0) addSeparator(rows);
            final Intent liveTvIntent = liveTv;
            addInputRow(rows, popup, R.drawable.ic_livetv, getString(R.string.live_tv), -1, () -> open(liveTvIntent));
        }
        if (rows.getChildCount() == 0) {
            Toast.makeText(this, R.string.no_inputs, Toast.LENGTH_SHORT).show();
            return;
        }

        // Fit the widest row: TVs that report a status on every port (Sony) would otherwise squeeze the names out.
        panel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popup.setWidth(Math.max(width, panel.getMeasuredWidth()));

        int alignment = getResources().getBoolean(R.bool.rtl_menu_bar) ? Gravity.END : Gravity.START;
        dropIn(popup, panel, anchor, alignment);
        rows.getChildAt(0).requestFocus();
    }

    private PopupWindow glassPopup(View panel, int width) {
        final PopupWindow popup = new PopupWindow(panel, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));   // needed for Back to dismiss
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        popup.setOnDismissListener(() -> {
            if (openPopup == popup) openPopup = null;
        });
        openPopup = popup;
        return popup;
    }

    /** Drop in: fade and unfold from the top edge, hanging under the anchor pill. */
    private void dropIn(PopupWindow popup, View panel, View anchor, int gravity) {
        panel.setAlpha(0f);
        panel.setPivotY(0f);
        panel.setScaleY(0.92f);
        panel.setTranslationY(-8 * density);
        popup.showAsDropDown(anchor, 0, (int) (10 * density), gravity);
        panel.animate().alpha(1f).scaleY(1f).translationY(0f).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();
    }

    private void addSeparator(LinearLayout rows) {
        View line = new View(this);
        line.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) density);
        int m = (int) (6 * density), h = (int) (12 * density);
        lp.setMargins(h, m, h, m);
        rows.addView(line, lp);
    }

    private void addInputRow(LinearLayout rows, PopupWindow popup, int iconRes, String title, int state, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        TextView status = row.findViewById(R.id.status);
        View dot = row.findViewById(R.id.dot);
        // This TV reports "standby" for every port whether or not something is plugged in, so only
        // definite states are shown.
        if (state == TvInputManager.INPUT_STATE_CONNECTED) {
            status.setText(R.string.connected);
            dot.setVisibility(View.VISIBLE);
        } else if (state == TvInputManager.INPUT_STATE_DISCONNECTED) {
            status.setText(R.string.not_connected);
            dot.setVisibility(View.GONE);
        } else {
            status.setVisibility(View.GONE);
            dot.setVisibility(View.GONE);
        }
        row.setOnClickListener(v -> {
            popup.dismiss();
            action.run();
        });
        rows.addView(row);
    }
}
