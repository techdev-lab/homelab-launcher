package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FeedKeepTest {
    private static final long NOW = 10_000_000L;

    @Test
    public void anEmptyFetchKeepsRecentData() {
        assertTrue(FeedKeep.keepPrevious(true, true, true, NOW - 5 * 60_000L, NOW));
        assertTrue(FeedKeep.keepPrevious(true, true, true, NOW - (FeedKeep.STALE_MS - 1), NOW));
    }

    @Test
    public void oldDataIsDroppedAfterAnHour() {
        assertFalse(FeedKeep.keepPrevious(true, true, true, NOW - FeedKeep.STALE_MS, NOW));
        assertFalse(FeedKeep.keepPrevious(true, true, true, NOW - 3 * FeedKeep.STALE_MS, NOW));
    }

    @Test
    public void dataFromTheFutureIsNotTrusted() {
        assertFalse(FeedKeep.keepPrevious(true, true, true, NOW + 1, NOW));
    }

    @Test
    public void nothingIsKeptWhenTheFetchHasData() {
        assertFalse(FeedKeep.keepPrevious(true, false, true, NOW - 1000, NOW));
    }

    @Test
    public void nothingIsKeptWhenTheFeedIsSwitchedOff() {
        assertFalse(FeedKeep.keepPrevious(false, true, true, NOW - 1000, NOW));
    }

    @Test
    public void nothingIsKeptWhenThereWasNothingBefore() {
        assertFalse(FeedKeep.keepPrevious(true, true, false, 0, NOW));
    }

    // The wiring needs the Android framework, so the source is read (the same way HomeLifecycleSourceTest does).
    private static String home() throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/gathin/tvhome/MainActivity.java")),
                StandardCharsets.UTF_8);
    }

    @Test
    public void homeKeepsTheFeedDataAcrossARebuildAndAsksFeedKeep() throws IOException {
        String home = home();
        assertTrue("fetched data must outlive one Home instance (recreate() starts a new one)",
                home.contains("private static volatile List<Interests.Match> matches"));
        assertTrue(home.contains("private static volatile List<Interests.Headline> headlines"));
        assertTrue(home.contains("private static volatile Interests.Weather weather"));
        assertTrue("an empty fetch must go through FeedKeep", home.contains("FeedKeep.keepPrevious("));
    }
}
