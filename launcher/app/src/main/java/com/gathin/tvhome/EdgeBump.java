package com.gathin.tvhome;

/**
 * The second press that crosses a section of the glass settings (docs/SETTINGS_GLASS.md): the first press of a key at the edge
 * of a row only arms the crossing, a second press of the same key within the window passes. Any other key, or a press after
 * the window, arms again. Pure Java, unit tested on the JVM; the clock is passed in.
 */
final class EdgeBump {
    private final long windowMs;
    private int armedDirection = -1;
    private long armedAt;

    EdgeBump(long windowMs) {
        this.windowMs = windowMs;
    }

    /** True when {@code direction} is pressed a second time in time; otherwise arms it and returns false. */
    boolean pass(int direction, long nowMs) {
        boolean again = armedDirection == direction && nowMs >= armedAt && nowMs - armedAt <= windowMs;
        if (again) {
            armedDirection = -1;
            return true;
        }
        armedDirection = direction;
        armedAt = nowMs;
        return false;
    }

    void reset() {
        armedDirection = -1;
    }
}
