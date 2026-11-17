package com.gathin.tvhome;

import android.content.SharedPreferences;

/**
 * A counter that says "a setting changed". The screens that save a setting bump it; Home remembers the value it was
 * built with and rebuilds itself in onResume when the value has moved.
 *
 * <p>Why not just the activity result: leaving the settings with the Home key makes Android close them with
 * RESULT_CANCELED (the Home screen is singleTask), so the result that asks Home to rebuild is lost and a setting read
 * only while Home is built (columns, text size, dock position, corners, digits, ...) shows only after a reboot. The
 * counter lives in the same preferences as the settings, so it survives however the settings screen was left.
 *
 * <p>The decision logic is plain static methods so a JVM test can cover it; only {@link #read} and {@link #bump} touch
 * the preferences.
 */
final class SettingsRevision {

    static final String KEY = "settings_rev";

    private SettingsRevision() {
    }

    /** A stored value as a usable revision: a non-negative whole number, else 0 (missing, wrong type, negative). */
    static long parse(Object stored) {
        long value;
        if (stored instanceof Long) {
            value = (Long) stored;
        } else if (stored instanceof Integer) {
            value = (Integer) stored;
        } else {
            return 0;
        }
        return value < 0 ? 0 : value;
    }

    /** The revision after one more change. Wraps to 1 (never to a negative or the "missing" 0) at the very end. */
    static long next(long current) {
        if (current < 0 || current == Long.MAX_VALUE) return 1;
        return current + 1;
    }

    /** True when Home was built with an older revision than the one stored now. */
    static boolean changed(long seen, long current) {
        return seen != current;
    }

    /** The stored revision; 0 when it is missing or was written as another type. */
    static long read(SharedPreferences prefs) {
        try {
            return parse(prefs.getAll().get(KEY));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** One more change saved. Call it right after the setting is written, before the screen can be left. */
    static void bump(SharedPreferences prefs) {
        prefs.edit().putLong(KEY, next(read(prefs))).apply();
    }
}
