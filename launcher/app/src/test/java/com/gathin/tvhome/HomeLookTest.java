package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeLookTest {
    @Test
    public void classicIsTheDefaultAndChangesNothing() {
        assertEquals("classic", HomeLook.pick(null, null));
        assertEquals("classic", HomeLook.pick("", ""));
        assertTrue(HomeLook.usesWallpaper("classic"));
        assertTrue(HomeLook.usesSky("classic"));
        assertFalse(HomeLook.replacesHeaderClock("classic"));
    }

    @Test
    public void theTvChoiceBeatsConfigInBothDirections() {
        assertEquals("scenes", HomeLook.pick("scenes", "oled"));
        assertEquals("classic", HomeLook.pick("classic", "scenes"));
        assertEquals("oled", HomeLook.pick("", "oled"));
    }

    @Test
    public void anUnknownLookCountsAsUnset() {
        assertEquals("oled", HomeLook.pick("neon", "oled"));
        assertEquals("classic", HomeLook.pick("neon", "disco"));
        assertEquals("classic", HomeLook.pick("Scenes", "OLED"));   // values are lower case
    }

    @Test
    public void onlyScenesHidesTheCardsAndFramesNoPills() {
        for (String look : new String[]{"classic", "oled"}) {
            assertTrue(look, HomeLook.showsCards(look));
            assertTrue(look, HomeLook.framesHeaderPills(look));
        }
        assertFalse(HomeLook.showsCards("scenes"));
        assertFalse(HomeLook.framesHeaderPills("scenes"));
    }

    @Test
    public void oledAndScenesDropTheWallpaperSkyAndVeil() {
        for (String look : new String[]{"scenes", "oled"}) {
            assertFalse(look, HomeLook.usesWallpaper(look));
            assertFalse(look, HomeLook.usesSky(look));
        }
        assertTrue(HomeLook.replacesHeaderClock("scenes"));
        assertFalse(HomeLook.replacesHeaderClock("oled"));
    }

    @Test
    public void anUnknownOrMissingLookKeepsTheCardsAndFrames() {
        // pick() turns an unknown look into classic, but the rules must also be safe on raw input.
        for (String look : new String[]{null, "", "neon", "Scenes"}) {
            assertTrue(String.valueOf(look), HomeLook.showsCards(look));
            assertTrue(String.valueOf(look), HomeLook.framesHeaderPills(look));
        }
    }

    @Test
    public void homeAsksTheLookBeforeDrawingTheStripAndThePills() throws java.io.IOException {
        // The wiring needs the Android framework, so the source is read: if the calls go, the Scenes look covers the clock again.
        String home = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/java/com/gathin/tvhome/MainActivity.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue("MainActivity must hide the strip through HomeLook.showsCards", home.contains("HomeLook.showsCards("));
        assertTrue("MainActivity must frame the pills through HomeLook.framesHeaderPills",
                home.contains("HomeLook.framesHeaderPills("));
        assertFalse("glassPill() without the framed flag would frame the pills in Scenes", home.contains("glassPill()"));
    }

    @Test
    public void oledIsPureBlackNotNearBlack() {
        assertEquals(0xFF000000, HomeLook.OLED_BACKGROUND);
        for (int c : Wallpapers.PLAIN_COLOURS) assertTrue(c != HomeLook.OLED_BACKGROUND);
    }
}
