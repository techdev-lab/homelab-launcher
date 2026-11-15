package com.gathin.tvhome;

/**
 * When the home clock needs a push to keep time (docs/BATCH_F_FIXES.md, F3). Before Android 12 a TextClock moves only
 * when the system sends the minute tick, and some TVs stop sending it after standby, so the clock freezes. From Android 12
 * the TextClock does not depend on it. Pure Java, unit tested; the refresh itself is in MainActivity.
 */
final class ClockRefresh {
    private ClockRefresh() {
    }

    /** The first Android version (12, API 31) whose TextClock keeps time without the system's minute tick. */
    static final int FIRST_SELF_UPDATING_SDK = 31;
    static final long MINUTE_MS = 60_000L;
    /** A little past the boundary, so the clock is read after the minute has really changed. */
    static final long MARGIN_MS = 300L;

    /** True where the clocks should be pushed: Android 11 and older. */
    static boolean needed(int sdkInt) {
        return sdkInt < FIRST_SELF_UPDATING_SDK;
    }

    /** Milliseconds from {@code nowMillis} to just after the next minute boundary; always above the margin. */
    static long delayToNextMinute(long nowMillis) {
        return MINUTE_MS - Math.floorMod(nowMillis, MINUTE_MS) + MARGIN_MS;
    }
}
