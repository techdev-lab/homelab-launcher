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
    public void oledAndScenesDropTheWallpaperSkyAndVeil() {
        for (String look : new String[]{"scenes", "oled"}) {
            assertFalse(look, HomeLook.usesWallpaper(look));
            assertFalse(look, HomeLook.usesSky(look));
        }
        assertTrue(HomeLook.replacesHeaderClock("scenes"));
        assertFalse(HomeLook.replacesHeaderClock("oled"));
    }

    @Test
    public void oledIsPureBlackNotNearBlack() {
        assertEquals(0xFF000000, HomeLook.OLED_BACKGROUND);
        for (int c : Wallpapers.PLAIN_COLOURS) assertTrue(c != HomeLook.OLED_BACKGROUND);
    }
}
