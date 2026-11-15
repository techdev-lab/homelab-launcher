package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The panel the remote's Settings button opens when {@code settings_key_panel} is on (HomeKeyService):
 * a few glass rows under the Settings pill, over Home or whatever app is playing. Translucent and in a
 * task of its own, so the app behind keeps running and Back returns to it. Pressing Settings again
 * closes the panel.
 */
public final class QuickSettingsActivity extends Activity {

    /** The settings app's own picture screen, where the TV has one (see pictureIntent). */
    private static final String PICTURE_PACKAGE = "com.android.tv.settings";
    private static final String PICTURE_ACTIVITY = "com.xiaomi.mitv.settings.picture.PictureActivity";
    private static final String TAG = "QuickSettings";
    /** A target the system refuses (or that is gone) is logged once; the next candidate is tried. */
    private final SafeStart safeStart = new SafeStart((what, why) ->
            Log.w(TAG, "could not open " + what + ": " + why.getClass().getSimpleName()));

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_settings);
        findViewById(R.id.scrim).setOnClickListener(v -> finish());
        LinearLayout rows = findViewById(R.id.rows);
        addRow(rows, R.drawable.ic_settings, R.string.tv_settings, 0, new Intent(Settings.ACTION_SETTINGS));
        addRow(rows, R.drawable.ic_picture, R.string.quick_picture, 0, pictureIntents());
        addRow(rows, R.drawable.ic_wifi, R.string.quick_network, 0, new Intent(Settings.ACTION_WIFI_SETTINGS));
        addRow(rows, R.drawable.ic_apps, R.string.quick_apps, R.string.quick_apps_hint,
                new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
        // Through Home, so it rebuilds with the new choices when Launcher settings closes.
        addRow(rows, R.drawable.ic_home_settings, R.string.launcher_settings, 0,
                new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS));
        rows.getChildAt(0).requestFocus();
    }

    /**
     * The TV's picture settings, in the order to try. On some TVs the standard display action opens an empty stub while
     * the settings app has its own picture screen, so that screen goes first when this TV has it and lets us start it
     * (it resolves and is exported); the standard action is always the last candidate, so a screen that then refuses to
     * start falls back to it once. Any other TV behaves as before (docs/BATCH_F_FIXES.md, F4).
     */
    private List<Intent> pictureIntents() {
        List<Intent> order = new ArrayList<>();
        Intent own = new Intent().setComponent(new ComponentName(PICTURE_PACKAGE, PICTURE_ACTIVITY));
        ResolveInfo found = getPackageManager().resolveActivity(own, 0);
        if (found != null && found.activityInfo != null
                && SettingsRoute.usable(true, found.activityInfo.exported)) {
            order.add(own);
        }
        order.add(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        return order;
    }

    /** A short name for a target in the log: its component, else its action. */
    private static String describe(Intent target) {
        return target.getComponent() != null
                ? target.getComponent().flattenToShortString() : String.valueOf(target.getAction());
    }

    /** Settings pressed again while the panel is up. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    /** Anything else taking the screen (Home, an app, a screensaver) closes the panel. */
    @Override
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }

    private void addRow(LinearLayout rows, int icon, int title, int status, Intent target) {
        addRow(rows, icon, title, status, Collections.singletonList(target));
    }

    private void addRow(LinearLayout rows, int icon, int title, int status, List<Intent> targets) {
        View row = getLayoutInflater().inflate(R.layout.item_input, rows, false);
        ((ImageView) row.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.title)).setText(title);
        row.findViewById(R.id.dot).setVisibility(View.GONE);
        TextView hint = row.findViewById(R.id.status);
        if (status != 0) hint.setText(status);
        else hint.setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            boolean started = SettingsRoute.startInOrder(targets, t -> safeStart.attempt(describe(t),
                    () -> startActivity(t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));
            if (!started) Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            finish();
        });
        rows.addView(row);
    }
}
