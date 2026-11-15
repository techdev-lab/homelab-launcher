package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TileFaceTest {
    @Test
    public void aBannerTileGivesUpItsIcon() {
        TileFace face = TileFace.of(true);
        assertTrue(face.showBanner);
        assertFalse(face.showIcon);
        assertFalse(face.showLabel);
        assertTrue("a hidden icon must not keep its drawable", face.clearIcon());
        assertFalse(face.clearBanner());
    }

    @Test
    public void anIconTileGivesUpItsBanner() {
        TileFace face = TileFace.of(false);
        assertTrue(face.showIcon);
        assertTrue(face.showLabel);
        assertFalse(face.showBanner);
        assertTrue(face.clearBanner());
        assertFalse(face.clearIcon());
    }

    @Test
    public void noTileEverShowsBothPictures() {
        for (boolean hasBanner : new boolean[]{true, false}) {
            TileFace face = TileFace.of(hasBanner);
            assertFalse(face.showBanner && face.showIcon);
        }
    }
}
