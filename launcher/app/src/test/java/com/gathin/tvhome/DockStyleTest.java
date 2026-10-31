package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DockStyleTest {

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    @Test
    public void tvChoiceWinsThenThemeThenBand() {
        assertEquals("floating", DockStyle.pick("floating", "band"));
        assertEquals("floating", DockStyle.pick("", "floating"));
        assertEquals("band", DockStyle.pick("", ""));
        assertEquals("band", DockStyle.pick(null, null));
        // An unknown value counts as unset: the failure case that keeps today's look.
        assertEquals("band", DockStyle.pick("glass", "frosted"));
        assertEquals("floating", DockStyle.pick("glass", "floating"));
    }

    @Test
    public void everyListedValueIsValid() {
        for (String s : DockStyle.ALL) assertTrue(s, DockStyle.valid(s));
        assertFalse(DockStyle.valid(""));
        assertFalse(DockStyle.valid(null));
        assertEquals("band", DockStyle.ALL[0]);
    }

    @Test
    public void bandKeepsTodaysNumbers() {
        assertEquals(14f, DockStyle.PAD_DP, 0f);
        assertEquals(26f, DockStyle.radiusDp("band", 1f), 0f);
        assertEquals(0xFF141416, DockStyle.fillTop("band", -1));
        assertEquals(0xFF141416, DockStyle.fillBottom("band", 0x336699));
        assertEquals(0x1FFFFFFF, DockStyle.edge("band", 0x336699));
        assertFalse(DockStyle.floating("band"));
    }

    @Test
    public void unknownStyleDrawsLikeTheBand() {
        assertFalse(DockStyle.floating("weird"));
        assertEquals(26f, DockStyle.radiusDp("weird", 1f), 0f);
        assertEquals(0xFF141416, DockStyle.fillTop(null, -1));
    }

    @Test
    public void floatingIsTranslucentGlassThatTakesTheAppColour() {
        assertTrue(DockStyle.floating("floating"));
        assertEquals(34f, DockStyle.radiusDp("floating", 1f), 0f);
        int top = DockStyle.fillTop("floating", 0x336699), bottom = DockStyle.fillBottom("floating", 0x336699);
        // Translucent so the wallpaper shows through, but dark enough that the tiles keep their contrast.
        assertTrue(alpha(top) > 0x40 && alpha(top) < 0xE0);
        assertTrue(alpha(bottom) > 0x40 && alpha(bottom) < 0xE0);
        assertTrue("the glass is a little lighter at the top", alpha(top) < alpha(bottom));
        assertTrue("it carries the app colour", ((top >> 16) & 0xFF) != ((top >> 8) & 0xFF) || ((top) & 0xFF) != ((top >> 8) & 0xFF));
        assertEquals(Glass.edge(0x336699), DockStyle.edge("floating", 0x336699));
    }

    @Test
    public void noAmbientColourGivesNeutralGlass() {
        int top = DockStyle.fillTop("floating", -1);
        assertEquals("black glass, no tint", 0, top & 0xFFFFFF);
        assertEquals(0x24FFFFFF, DockStyle.edge("floating", -1));
    }

    @Test
    public void softerTilesMoveTheRadiusTogether() {
        // 1.5 x a 16 dp tile corner is 8 dp more: the plate stays concentric with the tiles.
        assertEquals(34f, DockStyle.radiusDp("band", 1.5f), 0f);
        assertEquals(42f, DockStyle.radiusDp("floating", 1.5f), 0f);
        assertEquals(DockStyle.radiusDp("floating", 1f) - DockStyle.radiusDp("band", 1f),
                DockStyle.radiusDp("floating", 1.5f) - DockStyle.radiusDp("band", 1.5f), 0f);
    }
}
