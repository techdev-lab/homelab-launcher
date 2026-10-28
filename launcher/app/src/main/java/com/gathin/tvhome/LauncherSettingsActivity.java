package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView clubsChoice;
    /** Wallpaper choices in screen order and the mode each one saves. */
    private final TextView[] wallChoices = new TextView[4];
    private static final String[] WALL_MODES = {Wallpapers.HOMELAB, Wallpapers.SERENE, Wallpapers.ROTATE,
            Wallpapers.PLAIN};
    private String wallMode;
    private static final int CLUBS_REQUEST = 1;
    private int columns;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        columns = config.columns;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        // Keep the numeric choices increasing in the Hebrew reading direction. Reorder the
        // actual views so D-pad focus follows their positions without changing the saved values.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            LinearLayout row = (LinearLayout) columnChoices[0].getParent();
            row.removeAllViews();
            for (int i = columnChoices.length - 1; i >= 0; i--) row.addView(columnChoices[i]);
        }
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            setResult(RESULT_OK);
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        wallMode = config.wallpaperMode;
        int[] wallIds = {R.id.wallHomelab, R.id.wallSerene, R.id.wallMine, R.id.wallPlain};
        for (int i = 0; i < wallIds.length; i++) {
            final String mode = WALL_MODES[i];
            wallChoices[i] = findViewById(wallIds[i]);
            wallChoices[i].setOnClickListener(v -> {
                if (mode.equals(wallMode)) return;
                wallMode = mode;
                choices.edit().putString(Config.WALLPAPER_MODE_KEY, mode).apply();
                setResult(RESULT_OK);
                refresh();
            });
        }
        // Same as the columns row: read the choices right to left on an RTL screen, focus follows the positions.
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            LinearLayout row = (LinearLayout) wallChoices[0].getParent();
            row.removeAllViews();
            for (int i = wallChoices.length - 1; i >= 0; i--) row.addView(wallChoices[i]);
        }
        // "My pictures" needs pictures pushed to files/wallpapers; without any it would show a bare colour.
        boolean mine = !Wallpapers.list(Config.dir(this) == null ? null
                : new java.io.File(Config.dir(this), Wallpapers.DIR_NAME)).isEmpty();
        wallChoices[2].setVisibility(mine || Wallpapers.ROTATE.equals(wallMode) ? View.VISIBLE : View.GONE);
        clubsChoice = findViewById(R.id.clubsChoice);
        // Matches need the network: the offline build has no INTERNET, so the row would promise nothing.
        clubsChoice.setVisibility(Feeds.AVAILABLE ? View.VISIBLE : View.GONE);
        clubsChoice.setOnClickListener(v ->
                startActivityForResult(new Intent(this, ClubsActivity.class), CLUBS_REQUEST));
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove("animations").remove("advanced_effects")
                    .remove(Config.WALLPAPER_MODE_KEY).commit();
            Config current = Config.load(this);
            wallMode = current.wallpaperMode;
            columns = current.columns;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            setResult(RESULT_OK);
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(new Intent(Settings.ACTION_SETTINGS)));
        refresh();
        columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CLUBS_REQUEST && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            refresh();
        }
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        setResult(RESULT_OK);
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        for (int i = 0; i < wallChoices.length; i++) wallChoices[i].setSelected(WALL_MODES[i].equals(wallMode));
        followThemeChoice.setSelected(!hasOnTvChoices());
        clubsChoice.setText(getString(R.string.favorite_clubs_state,
                Config.load(this).interests.favoriteTeams.size(), Interests.MAX_CLUBS));
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains("animations")
                || choices.contains("advanced_effects") || choices.contains(Config.WALLPAPER_MODE_KEY);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
