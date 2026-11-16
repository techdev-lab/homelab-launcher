package com.gathin.tvhome;

/**
 * Which scene shows and when the next one comes (docs/BATCH_S_SCENES.md). The scene is a function of the clock, like
 * {@link Wallpapers#indexAt}, so every TV on the same list shows the same scene at the same time and Home coming back
 * in the middle of a slot keeps the scene. Pure Java, unit tested on the JVM.
 */
final class SceneRotation {
    static final int MIN_SECONDS = 5, MAX_SECONDS = 600, DEFAULT_SECONDS = 20;
    /** What the TV offers in Launcher settings. */
    static final int[] CHOICES = {10, 20, 30, 60, 120};

    private SceneRotation() {
    }

    /** The TV's choice, else config.json "scene_seconds", else 20; a value outside 5 to 600 is clamped. */
    static int pick(Integer chosen, Integer theme) {
        Integer v = chosen != null ? chosen : theme;
        return v == null ? DEFAULT_SECONDS : clamp(v);
    }

    static int clamp(int seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    /** Index of the scene at {@code nowMillis}, or -1 when there are none. A negative time counts as zero. */
    static int indexAt(long nowMillis, int seconds, int count) {
        if (count <= 0) return -1;
        long slot = Math.max(0, nowMillis) / (clamp(seconds) * 1000L);
        return (int) (slot % count);
    }

    /** Milliseconds from {@code nowMillis} to the next slot boundary: always 1 to seconds x 1000. */
    static long msToNext(long nowMillis, int seconds) {
        long slot = clamp(seconds) * 1000L;
        long into = Math.max(0, nowMillis) % slot;
        return slot - into;
    }
}
