package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WallpaperDimTest {

    @Test
    public void theDefaultIsTheVeilOfTwoPointNine() {
        // 2.9 drew a fixed 0x59 black veil over a wallpaper picture: 35 percent.
        assertEquals(35, WallpaperDim.DEFAULT);
        assertEquals(0x59000000, WallpaperDim.argb(WallpaperDim.DEFAULT));
    }

    @Test
    public void zeroToEightyPercentAreValid() {
        assertTrue(WallpaperDim.valid(0));
        assertTrue(WallpaperDim.valid(80));
        assertFalse(WallpaperDim.valid(-1));
        assertFalse(WallpaperDim.valid(81));
        assertFalse(WallpaperDim.valid(Integer.MIN_VALUE));
        assertFalse(WallpaperDim.valid(Integer.MAX_VALUE));
    }

    @Test
    public void theTvChoiceWinsThenTheThemeThenTheDefault() {
        assertEquals(55, WallpaperDim.pick(55, 15));
        assertEquals("a TV choice of zero is a real choice", 0, WallpaperDim.pick(0, 55));
        assertEquals(15, WallpaperDim.pick(null, 15));
        assertEquals(WallpaperDim.DEFAULT, WallpaperDim.pick(null, null));
    }

    @Test
    public void aValueOutsideTheRangeIsIgnoredNotClamped() {
        assertEquals("bad TV choice falls to the theme", 15, WallpaperDim.pick(90, 15));
        assertEquals("bad theme falls to the default", WallpaperDim.DEFAULT, WallpaperDim.pick(null, 100));
        assertEquals(WallpaperDim.DEFAULT, WallpaperDim.pick(-5, -5));
    }

    @Test
    public void thePercentBecomesTheAlphaOfABlackVeil() {
        assertEquals(0x00000000, WallpaperDim.argb(0));
        assertEquals(0x26000000, WallpaperDim.argb(15));
        assertEquals(0x8C000000, WallpaperDim.argb(55));
        assertEquals(0xBF000000, WallpaperDim.argb(75));
        assertEquals(0xCC000000, WallpaperDim.argb(80));
    }

    @Test
    public void aBadPercentGivesTheDefaultVeilNotACrashOrAnOpaqueScreen() {
        assertEquals(WallpaperDim.argb(WallpaperDim.DEFAULT), WallpaperDim.argb(-3));
        assertEquals(WallpaperDim.argb(WallpaperDim.DEFAULT), WallpaperDim.argb(255));
    }

    @Test
    public void theSettingsChipsAreAllValidAndInOrder() {
        int[] chips = WallpaperDim.CHOICES;
        assertEquals(4, chips.length);
        for (int i = 0; i < chips.length; i++) {
            assertTrue("chip " + chips[i], WallpaperDim.valid(chips[i]));
            if (i > 0) assertTrue(chips[i] > chips[i - 1]);
        }
        boolean hasDefault = false;
        for (int c : chips) hasDefault |= c == WallpaperDim.DEFAULT;
        assertTrue("the default has its own chip", hasDefault);
    }
}
