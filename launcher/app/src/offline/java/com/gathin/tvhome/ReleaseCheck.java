package com.gathin.tvhome;

/**
 * Offline build: no network, so there is nothing to check. The online build asks GitHub in src/online/java;
 * LauncherSettingsActivity only calls {@link #available()} and never needs to know which one it got (as with Feeds).
 */
final class ReleaseCheck {

    static boolean available() {
        return false;
    }

    static String latestTag() {
        return null;
    }

    private ReleaseCheck() {
    }
}
