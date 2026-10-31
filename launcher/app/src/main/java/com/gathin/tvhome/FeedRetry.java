package com.gathin.tvhome;

/**
 * When the online build fetches again (docs/REMOTE_RELIABILITY.md). Matches, news and weather come every 15 minutes while
 * Home is on screen. After a boot the Wi-Fi often connects after Home is shown, the first fetch finds nothing, and the cards
 * used to stay empty for up to 15 minutes. While there is no data the next fetch now comes after 1, 2, 4 and 8 minutes, and a
 * network that comes back triggers one fetch after a short wait. With data nothing changes. Pure Java, unit tested on the JVM.
 */
final class FeedRetry {
    /** The first wait after a fetch that found nothing. */
    static final long FIRST_RETRY_MS = 60_000L;
    /** A network that comes back refetches only if the last fetch started at least this long ago (a flapping network is ignored). */
    static final long MIN_GAP_ON_NETWORK_MS = 15_000L;
    /** The connection needs a moment after it is reported before DNS and routes work. */
    static final long NETWORK_DELAY_MS = 3_000L;
    private static final int MAX_DOUBLINGS = 4;

    private FeedRetry() {
    }

    /**
     * How long to wait for the next fetch. {@code failures} counts the fetches in a row that found nothing. With data it is
     * {@code normalMs}; without, 1, 1, 2, 4, 8 minutes for 0 to 4 failures, and never more than {@code normalMs}.
     */
    static long waitMs(boolean haveData, int failures, long normalMs) {
        if (haveData) return normalMs;
        // Clamp first: failures - 1 would wrap around for the smallest int.
        int tries = Math.max(0, Math.min(failures, MAX_DOUBLINGS + 2));
        long wait = FIRST_RETRY_MS << Math.max(0, tries - 1);
        return Math.min(wait, normalMs);
    }

    /**
     * True when a network that just came back should start a fetch: some card needs data ({@code wanted}), there is none
     * yet, and the last fetch started at least {@link #MIN_GAP_ON_NETWORK_MS} ago (or never).
     */
    static boolean refetchOnNetwork(boolean wanted, boolean haveData, long lastFetchMs, long nowMs) {
        if (!wanted || haveData) return false;
        if (lastFetchMs == 0L) return true;
        return nowMs - lastFetchMs >= MIN_GAP_ON_NETWORK_MS;
    }
}
