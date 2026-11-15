package com.gathin.tvhome;

import java.util.List;
import java.util.function.Predicate;

/**
 * Picks the first way of opening a system settings screen that this TV can resolve (docs/BATCH_F_FIXES.md, F4). The
 * candidates come most specific first, so a TV that has its own screen for the thing uses it and any other TV falls
 * through to the standard action. This is a capability check, not a test of the maker or the model. Pure Java, unit tested.
 */
final class SettingsRoute {
    private SettingsRoute() {
    }

    /** A screen of another app is worth trying only if the system resolves it and the app lets us start it (exported). */
    static boolean usable(boolean resolved, boolean exported) {
        return resolved && exported;
    }

    /**
     * Tries the candidates in order and stops at the first one that starts; false when none does. Used so a screen that
     * resolves but refuses to start falls back to the next one instead of ending in an error.
     */
    static <T> boolean startInOrder(List<T> candidates, Predicate<T> start) {
        for (T candidate : candidates) {
            if (start.test(candidate)) return true;
        }
        return false;
    }

    /** The first candidate for which {@code resolves} is true, or null when none is (or the list is empty). */
    static <T> T firstAvailable(List<T> candidates, Predicate<T> resolves) {
        for (T candidate : candidates) {
            if (resolves.test(candidate)) return candidate;
        }
        return null;
    }
}
