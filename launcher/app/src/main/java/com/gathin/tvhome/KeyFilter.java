package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityServiceInfo;

/**
 * Whether {@link HomeKeyService} filters the remote's keys, picked in Launcher settings or as "key_filter" in config.json
 * (docs/KEY_FILTER.md). Android's key filter is all or nothing, and on some TVs a key that went through it loses its auto
 * repeat (a held Volume key steps once). "on" (the default) filters, so the YouTube button, "app_keys", the Settings
 * button and a Home that sends KEYCODE_ALL_APPS work; "off" keeps the service enabled but stops the filtering, so every
 * key reaches the system untouched. Pure Java, unit tested on the JVM.
 */
final class KeyFilter {
    static final String ON = "on", OFF = "off";
    static final String[] ALL = {ON, OFF};

    private KeyFilter() {
    }

    /** The TV's choice, else config.json "key_filter", else on; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : ON;
    }

    static boolean valid(String s) {
        return ON.equals(s) || OFF.equals(s);
    }

    /** True unless the choice is exactly "off": a value nobody understands keeps the 2.9 behaviour. */
    static boolean filtering(String choice) {
        return !OFF.equals(choice);
    }

    /** {@code current} service flags with the key-filter request set or cleared by the choice; every other flag is kept. */
    static int flags(String choice, int current) {
        return filtering(choice)
                ? current | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                : current & ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
    }
}
