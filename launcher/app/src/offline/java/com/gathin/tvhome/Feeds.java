package com.gathin.tvhome;

import java.util.Collections;
import java.util.List;

/**
 * Offline build: no network, so no matches, no news and no weather. The online build has the real class in
 * src/online/java; MainActivity only asks {@link #AVAILABLE} and never needs to know which one it got.
 */
final class Feeds {
    static final boolean AVAILABLE = false;

    private Feeds() {
    }

    static List<Interests.Match> matches(Interests.Settings s) {
        return Collections.emptyList();
    }

    static List<Interests.Headline> headlines(List<String> feeds) {
        return Collections.emptyList();
    }

    static Interests.Weather weather(double lat, double lng) {
        return null;
    }
}
