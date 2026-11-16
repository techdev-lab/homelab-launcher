package com.gathin.tvhome;

/**
 * Whether a scene cross-fades into the next one (docs/BATCH_S_SCENES.md). A fade keeps two scenes in memory at once, so
 * a small-memory TV (batch L, {@link SmallMemory}) never fades: the old scene is released, then the next is drawn.
 * Pure Java, unit tested on the JVM.
 */
final class SceneFade {
    static final long FADE_MS = 1200;

    private SceneFade() {
    }

    static boolean useFade(boolean smallMemory, boolean animations, boolean reducedMotion) {
        return !smallMemory && animations && !reducedMotion;
    }
}
