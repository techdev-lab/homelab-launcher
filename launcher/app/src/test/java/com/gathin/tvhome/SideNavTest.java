package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Arrow keys of the right-hand dock layout (docs/LAYOUT_OPTIONS.md): a dock column of {@code DOCK} slots (positions 0 to
 * DOCK - 1) and a grid of the other apps, {@code COLS} wide. Positions are absolute, as in MainActivity.
 */
public class SideNavTest {

    private static final int DOCK = 5, COLS = 4;

    private static void assertTile(String what, int expected, SideNav.Action a) {
        assertEquals(what + ": kind", SideNav.TILE, a.kind);
        assertEquals(what + ": tile", expected, a.index);
    }

    private static SideNav.Action grid(int pos, int count, boolean rtl, int dir) {
        return SideNav.grid(pos, count, DOCK, COLS, rtl, dir);
    }

    private static SideNav.Action dock(int pos, int count, boolean rtl, int dir) {
        return SideNav.dock(pos, DOCK, count, DOCK, rtl, dir);
    }

    // ---- the grid ------------------------------------------------------------------------------

    @Test
    public void inTheGridUpAndDownMoveOneRowAndTheEdgesAreKnown() {
        // 10 apps after the dock: positions 5..14, rows of 4: 5-8, 9-12, 13-14.
        int count = DOCK + 10;
        assertTile("up from row 2", 5, grid(9, count, false, SideNav.UP));
        assertTile("up from the short last row", 10, grid(14, count, false, SideNav.UP));
        assertEquals("the first row goes up to the strip", SideNav.STRIP_OR_TOP, grid(7, count, false, SideNav.UP).kind);
        assertTile("down from row 2", 13, grid(9, count, false, SideNav.DOWN));
        assertTile("a short last row is reached by its nearest tile", 14, grid(12, count, false, SideNav.DOWN));
        assertEquals("the last row stays", SideNav.STAY, grid(13, count, false, SideNav.DOWN).kind);
        assertEquals(SideNav.STAY, grid(14, count, false, SideNav.DOWN).kind);
    }

    @Test
    public void inTheGridLeftStaysAtTheFirstColumnAndRightLeavesForTheDock() {
        int count = DOCK + 10;
        assertTile("right", 6, grid(5, count, false, SideNav.RIGHT));
        assertTile("left", 5, grid(6, count, false, SideNav.LEFT));
        assertEquals("the first column stays", SideNav.STAY, grid(5, count, false, SideNav.LEFT).kind);
        assertEquals("the first column of a later row stays too", SideNav.STAY, grid(9, count, false, SideNav.LEFT).kind);
        assertEquals("the last column goes to the dock", SideNav.DOCK_NEAR, grid(8, count, false, SideNav.RIGHT).kind);
        assertEquals("the last app of a short row goes to the dock", SideNav.DOCK_NEAR,
                grid(14, count, false, SideNav.RIGHT).kind);
    }

    @Test
    public void inAMirroredGridTheDockIsAtTheFirstColumn() {
        // Arabic: the screen is mirrored. The grid runs right to left and the dock is at physical left, so the end of a
        // row is next to the dock and the key toward it is Left.
        int count = DOCK + 10;
        assertEquals("the first column is at physical right and stays", SideNav.STAY, grid(5, count, true, SideNav.RIGHT).kind);
        assertEquals(SideNav.STAY, grid(9, count, true, SideNav.RIGHT).kind);
        assertTile("physical right moves back a tile", 6, grid(7, count, true, SideNav.RIGHT));
        assertTile("physical left moves on a tile", 7, grid(6, count, true, SideNav.LEFT));
        assertEquals("the end of a row goes to the dock", SideNav.DOCK_NEAR, grid(8, count, true, SideNav.LEFT).kind);
        assertEquals("the short row's last app goes to the dock", SideNav.DOCK_NEAR, grid(14, count, true, SideNav.LEFT).kind);
    }

    @Test
    public void aGridWithOneAppIsStillNavigable() {
        int count = DOCK + 1;
        assertEquals(SideNav.STRIP_OR_TOP, grid(5, count, false, SideNav.UP).kind);
        assertEquals(SideNav.STAY, grid(5, count, false, SideNav.DOWN).kind);
        assertEquals(SideNav.STAY, grid(5, count, false, SideNav.LEFT).kind);
        assertEquals(SideNav.DOCK_NEAR, grid(5, count, false, SideNav.RIGHT).kind);
    }

    // ---- the dock ------------------------------------------------------------------------------

    @Test
    public void inTheDockUpAndDownWalkTheColumnAndStopAtItsEnds() {
        int count = DOCK + 10;
        assertEquals(SideNav.TOP_BUTTONS, dock(0, count, false, SideNav.UP).kind);
        assertTile("up", 1, dock(2, count, false, SideNav.UP));
        assertTile("down", 3, dock(2, count, false, SideNav.DOWN));
        assertEquals("the last dock app stays", SideNav.STAY, dock(4, count, false, SideNav.DOWN).kind);
    }

    @Test
    public void emptyDockSlotsTakeNoFocusSoTheLastAppIsTheEnd() {
        int count = DOCK + 10;
        // Three apps in a five-slot dock: slots 3 and 4 are empty.
        assertTile("down", 2, SideNav.dock(1, 3, count, DOCK, false, SideNav.DOWN));
        assertEquals(SideNav.STAY, SideNav.dock(2, 3, count, DOCK, false, SideNav.DOWN).kind);
    }

    @Test
    public void inTheDockLeftGoesToTheGridAndRightStays() {
        assertEquals(SideNav.GRID_NEAR, dock(2, DOCK + 10, false, SideNav.LEFT).kind);
        assertEquals(SideNav.STAY, dock(2, DOCK + 10, false, SideNav.RIGHT).kind);
        assertEquals("no grid: left stays", SideNav.STAY, dock(2, DOCK, false, SideNav.LEFT).kind);
    }

    @Test
    public void onTheMirroredScreenTheDockIsAtLeftAndRightGoesToTheGrid() {
        assertEquals(SideNav.GRID_NEAR, dock(2, DOCK + 10, true, SideNav.RIGHT).kind);
        assertEquals(SideNav.STAY, dock(2, DOCK + 10, true, SideNav.LEFT).kind);
        assertEquals("no grid: right stays", SideNav.STAY, dock(2, DOCK, true, SideNav.RIGHT).kind);
    }

    // ---- crossing between the parts ------------------------------------------------------------

    @Test
    public void leavingTheStripDownGoesToTheGridElseTheDock() {
        assertEquals(SideNav.GRID_NEAR, SideNav.fromStripDown(DOCK + 3, DOCK).kind);
        assertEquals(SideNav.DOCK_NEAR, SideNav.fromStripDown(DOCK, DOCK).kind);
        assertEquals(SideNav.DOCK_NEAR, SideNav.fromStripDown(0, DOCK).kind);
    }

    @Test
    public void fromTheTopBarDownGoesStraightToTheDockWhenAboveIt() {
        assertEquals(SideNav.DOCK_FIRST, SideNav.fromTopDown(true, true, DOCK + 3, DOCK).kind);
        assertEquals(SideNav.DOCK_FIRST, SideNav.fromTopDown(true, false, DOCK + 3, DOCK).kind);
        assertEquals(SideNav.STRIP_FIRST, SideNav.fromTopDown(false, true, DOCK + 3, DOCK).kind);
        assertEquals(SideNav.GRID_NEAR, SideNav.fromTopDown(false, false, DOCK + 3, DOCK).kind);
        assertEquals("no strip and no grid", SideNav.DOCK_FIRST, SideNav.fromTopDown(false, false, DOCK, DOCK).kind);
    }

    @Test
    public void theNearestTileIsTheOneClosestInHeight() {
        double[] centres = {100, 190, 280, 370};
        assertEquals(0, SideNav.nearest(centres, 90));
        assertEquals(1, SideNav.nearest(centres, 200));
        assertEquals(3, SideNav.nearest(centres, 900));
        assertEquals(0, SideNav.nearest(centres, -50));
        assertEquals("a tie goes to the upper tile", 1, SideNav.nearest(centres, 235));
        assertEquals(-1, SideNav.nearest(new double[0], 10));
        assertEquals(-1, SideNav.nearest(null, 10));
    }

    // ---- no dead end, in any layout ------------------------------------------------------------

    @Test
    public void fromEveryTileUpEventuallyReachesTheTopBar() {
        for (int dockFilled = 1; dockFilled <= DOCK; dockFilled++) {
            for (int count = DOCK; count <= DOCK + 14; count++) {
                for (boolean rtl : new boolean[]{false, true}) {
                    for (int pos = 0; pos < count; pos++) {
                        if (pos >= dockFilled && pos < DOCK) continue;   // an empty dock slot takes no focus
                        assertTrue("pos " + pos + " of " + count + " filled " + dockFilled + " rtl " + rtl,
                                reachesTop(pos, count, dockFilled, rtl));
                    }
                }
            }
        }
    }

    @Test
    public void everyTileHasAWayOutAndTheTopBarAlwaysLeadsBackIn() {
        for (int count = DOCK; count <= DOCK + 14; count++) {
            for (int pos = 0; pos < count; pos++) {
                boolean moved = false;
                for (int dir : new int[]{SideNav.UP, SideNav.DOWN, SideNav.LEFT, SideNav.RIGHT}) {
                    SideNav.Action a = pos < DOCK ? dock(pos, count, false, dir) : grid(pos, count, false, dir);
                    if (a.kind != SideNav.STAY) moved = true;
                }
                assertTrue("pos " + pos + " of " + count, moved);
            }
            for (boolean over : new boolean[]{true, false}) {
                for (boolean strip : new boolean[]{true, false}) {
                    assertNotEquals(SideNav.STAY, SideNav.fromTopDown(over, strip, count, DOCK).kind);
                }
            }
        }
    }

    @Test
    public void everyMoveLandsOnARealTile() {
        for (int count = DOCK; count <= DOCK + 14; count++) {
            for (boolean rtl : new boolean[]{false, true}) {
                for (int pos = 0; pos < count; pos++) {
                    for (int dir : new int[]{SideNav.UP, SideNav.DOWN, SideNav.LEFT, SideNav.RIGHT}) {
                        SideNav.Action a = pos < DOCK ? dock(pos, count, rtl, dir) : grid(pos, count, rtl, dir);
                        if (a.kind != SideNav.TILE) continue;
                        assertTrue("index " + a.index, a.index >= 0 && a.index < count);
                        assertEquals("a move inside a part stays in it", pos < DOCK, a.index < DOCK);
                    }
                }
            }
        }
    }

    /** Presses Up from a tile until the focus leaves the tiles; true when it lands on the strip or the top bar. */
    private static boolean reachesTop(int start, int count, int dockFilled, boolean rtl) {
        int pos = start;
        for (int guard = 0; guard < 100; guard++) {
            SideNav.Action a = pos < DOCK ? SideNav.dock(pos, dockFilled, count, DOCK, rtl, SideNav.UP)
                    : grid(pos, count, rtl, SideNav.UP);
            if (a.kind == SideNav.STRIP_OR_TOP || a.kind == SideNav.TOP_BUTTONS) return true;
            if (a.kind != SideNav.TILE) return false;
            pos = a.index;
        }
        return false;
    }
}
