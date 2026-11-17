package com.gathin.tvhome;

/**
 * What the strip does with a feed fetch that came back empty. A single failed call (a Wi-Fi blip, a source that is down
 * for a minute) used to wipe the card until the next fetch, 15 minutes later. Now the previous data stays for up to an
 * hour; after that it is dropped, so a card never shows old news as if it were current. Pure Java, unit tested.
 */
final class FeedKeep {
    /** How long data from an earlier fetch may stand in for an empty one. */
    static final long STALE_MS = 60L * 60L * 1000L;

    private FeedKeep() {
    }

    /**
     * Whether to keep the previous data of one feed.
     *
     * @param wanted        the feed is switched on (an off feed has no data and keeps nothing)
     * @param freshIsEmpty  this fetch returned nothing for it
     * @param havePrevious  there is earlier data to keep
     * @param previousAtMs  when the earlier data was fetched (0 when there is none)
     * @param nowMs         now
     */
    static boolean keepPrevious(boolean wanted, boolean freshIsEmpty, boolean havePrevious, long previousAtMs, long nowMs) {
        if (!wanted || !freshIsEmpty || !havePrevious) return false;
        long age = nowMs - previousAtMs;
        // A clock that went backwards (age < 0) is not proof of freshness: drop the old data.
        return age >= 0 && age < STALE_MS;
    }
}
