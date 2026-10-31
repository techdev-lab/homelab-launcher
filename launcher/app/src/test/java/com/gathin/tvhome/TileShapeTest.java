package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TileShapeTest {

    private static final float DESIGN_W_DP = 180f;

    @Test
    public void tvChoiceWinsThenThemeThenNormal() {
        assertEquals("soft", TileShape.pick("soft", "normal"));
        assertEquals("soft", TileShape.pick("", "soft"));
        assertEquals("normal", TileShape.pick("", ""));
        assertEquals("normal", TileShape.pick(null, null));
        // An unknown value counts as unset: the failure case that keeps today's look.
        assertEquals("normal", TileShape.pick("round", "square"));
    }

    @Test
    public void everyListedValueIsValid() {
        for (String s : TileShape.ALL) assertTrue(s, TileShape.valid(s));
        assertFalse(TileShape.valid("round"));
        assertEquals("normal", TileShape.ALL[0]);
    }

    @Test
    public void factors() {
        assertEquals(1f, TileShape.factor("normal"), 0f);
        assertEquals(1.5f, TileShape.factor("soft"), 0f);
        assertEquals(1f, TileShape.factor("anything else"), 0f);
        assertEquals(1f, TileShape.factor(null), 0f);
    }

    @Test
    public void normalIsExactlyTodaysRadius() {
        float density = 2f;
        for (int w : new int[]{120, 240, 360, 361, 500}) {
            float today = 16f * density * Math.min(1f, w / (DESIGN_W_DP * density));
            assertEquals("tile width " + w, today, TileShape.radiusPx(w, density, DESIGN_W_DP, "normal"), 0f);
        }
        assertEquals(32f, TileShape.radiusPx(360, 2f, DESIGN_W_DP, "normal"), 0f);
    }

    @Test
    public void softIsOneAndAHalfTimesAndStillScalesDownWithSmallTiles() {
        assertEquals(48f, TileShape.radiusPx(360, 2f, DESIGN_W_DP, "soft"), 0f);
        float narrow = TileShape.radiusPx(180, 2f, DESIGN_W_DP, "soft");
        assertEquals(24f, narrow, 0f);
        assertTrue(narrow < TileShape.radiusPx(360, 2f, DESIGN_W_DP, "soft"));
    }
}
