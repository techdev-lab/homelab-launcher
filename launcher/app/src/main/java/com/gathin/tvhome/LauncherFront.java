package com.gathin.tvhome;

/**
 * Whether one of this launcher's screens is in front, for the "home" choice of the remote buttons switch
 * (KeyFilter, docs/KEY_FILTER.md). It counts started activities: moving from the home screen to Settings starts the new
 * screen before the old one stops, so the count touches zero only when another app takes over. Pure Java, unit tested.
 */
final class LauncherFront {
    private int started;

    void started() {
        started++;
    }

    /** A stop without a start cannot take the count below zero, or the next screen would never count as in front. */
    void stopped() {
        if (started > 0) started--;
    }

    boolean inFront() {
        return started > 0;
    }
}
