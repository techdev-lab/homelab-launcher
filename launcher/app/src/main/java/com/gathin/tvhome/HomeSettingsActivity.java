package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Lets the user change HOME through the system UI, without shell or privileged permissions. The system's picker is
 * translucent on Android 14, so this screen stays up behind it with a solid backdrop (Theme.HomePickerBackdrop) and
 * finishes when the picker closes, instead of letting the home screen's tiles show through the picker's text.
 */
public class HomeSettingsActivity extends Activity {
    private final HomePickerGate gate = new HomePickerGate();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean started = false;
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            started = true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
        gate.attempted(started);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gate.shouldFinishOnResume()) finish();
    }
}
