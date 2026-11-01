package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityServiceInfo;

/**
 * Whether {@link HomeKeyService} filters the remote's keys, picked in Launcher settings or as "key_filter" in config.json
 * (docs/KEY_FILTER.md). Android's key filter is all or nothing, and on some TVs a key that went through it loses its auto
 * repeat (a held Volume key steps once). "on" (the default) filters, so the YouTube button, "app_keys", the Settings
 * button and a Home that sends KEYCODE_ALL_APPS work; "off" keeps the service enabled but stops the filtering, so every
 * key reaches the system untouched; "home" filters only while a screen of this launcher is in front, so a held Volume
 * key repeats inside other apps while the buttons still work on the home screen. Pure Java, unit tested on the JVM.
 */
final class KeyFilter {
    static final String ON = "on", HOME = "home", OFF = "off";
    static final String[] ALL = {ON, HOME, OFF};

    private KeyFilter() {
    }

    /** The TV's choice, else config.json "key_filter", else on; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : ON;
    }

    static boolean valid(String s) {
        return ON.equals(s) || HOME.equals(s) || OFF.equals(s);
    }

    /**
     * Whether the key service should filter now. "off" never, "home" only while a launcher screen is in front, and
     * anything else (including a value nobody understands) always, which is the 2.9 behaviour.
     */
    static boolean filtering(String choice, boolean launcherInFront) {
        if (OFF.equals(choice)) return false;
        return !HOME.equals(choice) || launcherInFront;
    }

    /** {@code current} service flags with the key-filter request set or cleared by the choice; every other flag is kept. */
    static int flags(String choice, int current, boolean launcherInFront) {
        return filtering(choice, launcherInFront)
                ? current | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                : current & ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
    }
}
