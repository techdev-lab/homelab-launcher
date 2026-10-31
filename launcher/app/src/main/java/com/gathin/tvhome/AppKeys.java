package com.gathin.tvhome;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The rules for config.json "app_keys": which remote key opens which app, and which key is ignored (docs/REMOTE_RELIABILITY.md).
 * The value {@code "ignore"} swallows the key, so a button pressed by accident does nothing. The accessibility service sees every
 * key in every app, so a short list of protected keys (Home, Back, the arrows, OK, Power) can never be mapped: a bad
 * entry is dropped, the others still work. Pure Java, unit tested on the JVM; Config hands it the JSON as a plain map.
 */
final class AppKeys {
    /** The reserved value that swallows a key instead of opening an app. */
    static final String IGNORE = "ignore";
    static final int DEFAULT_KEY = 190;
    static final String DEFAULT_APP = "com.google.android.youtube.tv";
    /** More entries than any remote has buttons: a larger object is cut, not trusted. */
    static final int MAX_KEYS = 32;

    /** Home, Back, the four arrows, OK, Enter, numpad Enter, Power, and the all-apps key some remotes send for Home. */
    private static final Set<Integer> PROTECTED = new HashSet<>(java.util.Arrays.asList(3, 4, 19, 20, 21, 22, 23, 26, 66, 160, 284));

    private AppKeys() {
    }

    /** The map when config.json has no "app_keys": the YouTube button only. */
    static Map<Integer, String> defaults() {
        return Collections.singletonMap(DEFAULT_KEY, DEFAULT_APP);
    }

    /** True for the word {@link #IGNORE}, ignoring case and surrounding spaces; a package such as "ignore.app" is not. */
    static boolean isIgnore(String value) {
        return value != null && IGNORE.equalsIgnoreCase(value.trim());
    }

    /** Keys that must keep working everywhere, whatever the config says. */
    static boolean protectedKey(int keyCode) {
        return PROTECTED.contains(keyCode);
    }

    static boolean validPackage(String s) {
        return s != null && s.matches("[A-Za-z0-9_.]{3,150}");
    }

    /**
     * The key map for an "app_keys" object given as plain text keys and values ({@code null} = the setting is absent:
     * the defaults). An empty object turns every mapping off. Invalid or protected entries are dropped.
     */
    static Map<Integer, String> parse(Map<String, ?> raw) {
        if (raw == null) return new HashMap<>(defaults());
        Map<Integer, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, ?> e : raw.entrySet()) {
            if (out.size() >= MAX_KEYS) break;
            int code;
            try {
                code = Integer.parseInt(e.getKey().trim());
            } catch (NumberFormatException | NullPointerException ex) {
                continue;
            }
            if (code <= 0 || protectedKey(code) || !(e.getValue() instanceof String)) continue;
            String value = (String) e.getValue();
            if (isIgnore(value)) out.put(code, IGNORE);
            else if (validPackage(value)) out.put(code, value);
        }
        return out;
    }
}
