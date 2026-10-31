package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DockPositionTest {

    @Test
    public void theTvChoiceWinsThenTheThemeThenTheBottom() {
        assertEquals(DockPosition.SIDE, DockPosition.pick("side", "bottom"));
        assertEquals(DockPosition.BOTTOM, DockPosition.pick("bottom", "side"));
        assertEquals(DockPosition.SIDE, DockPosition.pick("", "side"));
        assertEquals(DockPosition.SIDE, DockPosition.pick(null, "side"));
        assertEquals(DockPosition.BOTTOM, DockPosition.pick(null, null));
        assertEquals(DockPosition.BOTTOM, DockPosition.pick("", ""));
    }

    @Test
    public void anUnknownValueCountsAsUnset() {
        assertEquals("unknown TV choice falls to the theme", DockPosition.SIDE, DockPosition.pick("left", "side"));
        assertEquals("unknown theme falls to the bottom", DockPosition.BOTTOM, DockPosition.pick(null, "top"));
        assertEquals("case matters, like every other look option", DockPosition.BOTTOM, DockPosition.pick("Side", null));
        assertFalse(DockPosition.valid(null));
        assertFalse(DockPosition.valid(""));
        assertTrue(DockPosition.valid("side"));
        assertTrue(DockPosition.valid("bottom"));
    }

    @Test
    public void onlyTheSideValueIsASideDock() {
        assertTrue(DockPosition.side(DockPosition.SIDE));
        assertFalse("right was the first draft's word and is not a value", DockPosition.side("right"));
        assertFalse(DockPosition.side(DockPosition.BOTTOM));
        assertFalse(DockPosition.side(null));
        assertFalse(DockPosition.side("left"));
    }

    @Test
    public void theBottomIsTheFirstChipAndTheDefault() {
        assertEquals(DockPosition.BOTTOM, DockPosition.ALL[0]);
        assertEquals(2, DockPosition.ALL.length);
    }

    @Test
    public void theSideLayoutHasOneStripPlaceFewerAndAtLeastTwo() {
        assertEquals(4, StripSize.perRowSide(StripSize.SMALL));
        assertEquals(3, StripSize.perRowSide(StripSize.MEDIUM));
        assertEquals(2, StripSize.perRowSide(StripSize.LARGE));
        assertEquals("an unknown size is small", 4, StripSize.perRowSide("huge"));
        for (String size : StripSize.ALL) assertTrue(StripSize.perRowSide(size) >= 2);
    }

    @Test
    public void theSideGridHasOneColumnFewerAndAtLeastThree() {
        assertEquals(3, DockPosition.restColumns(4));
        assertEquals(4, DockPosition.restColumns(5));
        assertEquals(5, DockPosition.restColumns(6));
        assertEquals(6, DockPosition.restColumns(7));
        assertEquals("never fewer than three", 3, DockPosition.restColumns(3));
        assertEquals(3, DockPosition.restColumns(1));
        assertEquals(3, DockPosition.restColumns(0));
        assertEquals(3, DockPosition.restColumns(-4));
    }

    @Test
    public void theColumnIsAsWideAsTheTileThePlateAndRoomForTheFocusZoom() {
        float w = DockPosition.columnWidthDp();
        assertTrue(w > DockPosition.TILE_W_DP + 2 * DockStyle.PAD_DP);
        assertTrue("it must not eat the screen", w < 200f);
        // Same shape as every other tile (180 by 101).
        assertEquals(Math.round(DockPosition.TILE_W_DP * AppAdapter.DESIGN_H_DP / AppAdapter.DESIGN_W_DP),
                DockPosition.tileHeightDp());
    }
}
