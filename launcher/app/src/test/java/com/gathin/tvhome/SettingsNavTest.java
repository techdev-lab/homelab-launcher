package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Arrow keys of the glass settings screen (docs/SETTINGS_GLASS.md): a strip of section tabs, the rows of the visible
 * section and a footer row. Chips are numbered in reading order, so on the Arabic screen the first chip is at the right
 * and the two horizontal keys swap.
 */
public class SettingsNavTest {

    /** Four sections: rows of 3, 3, 6, 2 chips; 2, 5; 1, 2; 3. A footer of 3 buttons. */
    private static final int[][] ROWS = {{3, 3, 6, 2}, {2, 5}, {1, 2}, {3}};
    private static final int FOOTER = 3;

    private static SettingsNav english() {
        return new SettingsNav(ROWS, FOOTER, false);
    }

    private static SettingsNav arabic() {
        return new SettingsNav(ROWS, FOOTER, true);
    }

    private static SettingsNav.Spot at(int section, int row, int chip) {
        return new SettingsNav.Spot(section, row, chip);
    }

    private static void assertSpot(String what, SettingsNav.Spot expected, SettingsNav.Spot actual) {
        assertEquals(what, expected, actual);
    }

    private static void assertSpot(SettingsNav.Spot expected, SettingsNav.Spot actual) {
        assertEquals(expected, actual);
    }

    // ---- inside a section ----------------------------------------------------------------------

    @Test
    public void downAndUpMoveOneRowAndKeepTheChipIndexOrClampIt() {
        SettingsNav nav = english();
        assertSpot("down keeps the chip", at(0, 1, 2), nav.move(at(0, 0, 2), SettingsNav.DOWN));
        assertSpot("down to a longer row keeps it", at(0, 2, 2), nav.move(at(0, 1, 2), SettingsNav.DOWN));
        assertSpot("down to a shorter row clamps", at(0, 3, 1), nav.move(at(0, 2, 5), SettingsNav.DOWN));
        assertSpot("up keeps the chip", at(0, 1, 2), nav.move(at(0, 2, 2), SettingsNav.UP));
        assertSpot("up to a shorter row clamps", at(0, 0, 2), nav.move(at(0, 1, 2), SettingsNav.UP));
        assertSpot("up from a short row to a long row keeps it", at(0, 2, 1), nav.move(at(0, 3, 1), SettingsNav.UP));
    }

    @Test
    public void rightAndLeftMoveOneChipInTheRow() {
        SettingsNav nav = english();
        assertSpot("right", at(0, 2, 3), nav.move(at(0, 2, 2), SettingsNav.RIGHT));
        assertSpot("left", at(0, 2, 1), nav.move(at(0, 2, 2), SettingsNav.LEFT));
    }

    // ---- section edges --------------------------------------------------------------------------

    @Test
    public void theEndOfARowGoesToTheNextSectionAtTheSameRowAndTheFirstChip() {
        SettingsNav nav = english();
        assertSpot("same row index", at(1, 1, 0), nav.move(at(0, 1, 2), SettingsNav.RIGHT));
        assertSpot("row 3 of a four-row section into a two-row one", at(1, 1, 0), nav.move(at(0, 3, 1), SettingsNav.RIGHT));
        assertSpot("a row of the next section", at(2, 1, 0), nav.move(at(1, 1, 4), SettingsNav.RIGHT));
        assertSpot("row clamped to the only row", at(3, 0, 0), nav.move(at(2, 1, 1), SettingsNav.RIGHT));
    }

    @Test
    public void theStartOfARowGoesToThePreviousSectionAtTheLastChip() {
        SettingsNav nav = english();
        assertSpot("same row index, last chip", at(0, 1, 2), nav.move(at(1, 1, 0), SettingsNav.LEFT));
        assertSpot("a shorter row of the previous section", at(1, 0, 1), nav.move(at(2, 0, 0), SettingsNav.LEFT));
        assertSpot("a one-chip last row", at(2, 0, 0), nav.move(at(3, 0, 0), SettingsNav.LEFT));
    }

    @Test
    public void theFirstAndLastSectionsStayAtTheirOuterEdges() {
        SettingsNav nav = english();
        assertSpot("left in the first section", at(0, 2, 0), nav.move(at(0, 2, 0), SettingsNav.LEFT));
        assertSpot("right in the last section", at(3, 0, 2), nav.move(at(3, 0, 2), SettingsNav.RIGHT));
    }

    @Test
    public void aRowWithOneChipCrossesToTheNextSectionBothWays() {
        SettingsNav nav = english();
        // Section 2 row 0 has one chip: it is the first and the last.
        assertSpot("right", at(3, 0, 0), nav.move(at(2, 0, 0), SettingsNav.RIGHT));
        assertSpot("left", at(1, 0, 1), nav.move(at(2, 0, 0), SettingsNav.LEFT));
        // And a model with one section and one one-chip row cannot be left sideways.
        SettingsNav lone = new SettingsNav(new int[][]{{1}}, 1, false);
        assertSpot("lone right", at(0, 0, 0), lone.move(at(0, 0, 0), SettingsNav.RIGHT));
        assertSpot("lone left", at(0, 0, 0), lone.move(at(0, 0, 0), SettingsNav.LEFT));
    }

    // ---- the Arabic order -----------------------------------------------------------------------

    @Test
    public void onTheMirroredScreenTheHorizontalKeysSwap() {
        SettingsNav ar = arabic();
        // Reading order: chip 0 is at the right, so Left goes to the next chip and Right to the previous.
        assertSpot("left is forward", at(0, 2, 3), ar.move(at(0, 2, 2), SettingsNav.LEFT));
        assertSpot("right is back", at(0, 2, 1), ar.move(at(0, 2, 2), SettingsNav.RIGHT));
        // The end of a row (physical left) goes to the next section; its start (physical right) to the previous.
        assertSpot("left at the end crosses forward", at(1, 1, 0), ar.move(at(0, 1, 2), SettingsNav.LEFT));
        assertSpot("right at the start crosses back", at(0, 1, 2), ar.move(at(1, 1, 0), SettingsNav.RIGHT));
        assertSpot("right in the first section stays", at(0, 2, 0), ar.move(at(0, 2, 0), SettingsNav.RIGHT));
        assertSpot("left in the last section stays", at(3, 0, 2), ar.move(at(3, 0, 2), SettingsNav.LEFT));
    }

    @Test
    public void theVerticalKeysAreTheSameOnBothScreens() {
        for (int dir : new int[]{SettingsNav.UP, SettingsNav.DOWN}) {
            for (int s = 0; s < ROWS.length; s++) {
                for (int r = 0; r < ROWS[s].length; r++) {
                    for (int c = 0; c < ROWS[s][r]; c++) {
                        assertEquals(english().move(at(s, r, c), dir), arabic().move(at(s, r, c), dir));
                    }
                }
            }
        }
    }

    // ---- the strip and the footer ---------------------------------------------------------------

    @Test
    public void upFromTheFirstRowGoesToItsTabAndTheTabStays() {
        SettingsNav nav = english();
        assertSpot("first row to the tab", SettingsNav.tab(2), nav.move(at(2, 0, 1), SettingsNav.UP));
        assertSpot("the tab stays on up", SettingsNav.tab(2), nav.move(SettingsNav.tab(2), SettingsNav.UP));
    }

    @Test
    public void downFromATabEntersTheSectionAtItsFirstChip() {
        SettingsNav nav = english();
        assertSpot(at(1, 0, 0), nav.move(SettingsNav.tab(1), SettingsNav.DOWN));
    }

    @Test
    public void tabsMoveAcrossTheSectionsAndStayAtTheEnds() {
        SettingsNav nav = english();
        assertSpot("next tab", SettingsNav.tab(2), nav.move(SettingsNav.tab(1), SettingsNav.RIGHT));
        assertSpot("previous tab", SettingsNav.tab(0), nav.move(SettingsNav.tab(1), SettingsNav.LEFT));
        assertSpot("first tab stays", SettingsNav.tab(0), nav.move(SettingsNav.tab(0), SettingsNav.LEFT));
        assertSpot("last tab stays", SettingsNav.tab(3), nav.move(SettingsNav.tab(3), SettingsNav.RIGHT));
        SettingsNav ar = arabic();
        assertSpot("mirrored: left is the next tab", SettingsNav.tab(2), ar.move(SettingsNav.tab(1), SettingsNav.LEFT));
        assertSpot("mirrored: right is the previous", SettingsNav.tab(0), ar.move(SettingsNav.tab(1), SettingsNav.RIGHT));
        assertSpot("mirrored: first tab stays on right", SettingsNav.tab(0), ar.move(SettingsNav.tab(0), SettingsNav.RIGHT));
    }

    @Test
    public void theLastRowGoesToTheFooterAndTheFooterComesBackToTheSameSection() {
        SettingsNav nav = english();
        assertSpot("down from the last row, chip clamped", new SettingsNav.Spot(0, SettingsNav.FOOTER, 1),
                nav.move(at(0, 3, 1), SettingsNav.DOWN));
        assertSpot("down from a wide row clamps to the footer's size", new SettingsNav.Spot(1, SettingsNav.FOOTER, 2),
                nav.move(at(1, 1, 4), SettingsNav.DOWN));
        assertSpot("the footer stays on down", new SettingsNav.Spot(1, SettingsNav.FOOTER, 2),
                nav.move(new SettingsNav.Spot(1, SettingsNav.FOOTER, 2), SettingsNav.DOWN));
        assertSpot("up from the footer is the last row of that section", at(1, 1, 1),
                nav.move(new SettingsNav.Spot(1, SettingsNav.FOOTER, 1), SettingsNav.UP));
    }

    @Test
    public void theFooterMovesAlongItsButtonsAndNeverLeavesSideways() {
        SettingsNav nav = english();
        SettingsNav.Spot first = new SettingsNav.Spot(2, SettingsNav.FOOTER, 0);
        SettingsNav.Spot last = new SettingsNav.Spot(2, SettingsNav.FOOTER, FOOTER - 1);
        assertSpot(new SettingsNav.Spot(2, SettingsNav.FOOTER, 1), nav.move(first, SettingsNav.RIGHT));
        assertSpot("left at the first button stays", first, nav.move(first, SettingsNav.LEFT));
        assertSpot("right at the last button stays", last, nav.move(last, SettingsNav.RIGHT));
        SettingsNav ar = arabic();
        assertSpot(new SettingsNav.Spot(2, SettingsNav.FOOTER, 1), ar.move(first, SettingsNav.LEFT));
        assertSpot("mirrored: right at the first button stays", first, ar.move(first, SettingsNav.RIGHT));
    }

    // ---- failure cases --------------------------------------------------------------------------

    @Test
    public void aSectionWithNoRowsIsEnteredAtItsTabAndPassedThrough() {
        SettingsNav nav = new SettingsNav(new int[][]{{2}, {}, {2}}, 2, false);
        assertSpot("a tab of an empty section goes down to the footer", new SettingsNav.Spot(1, SettingsNav.FOOTER, 0),
                nav.move(SettingsNav.tab(1), SettingsNav.DOWN));
        assertSpot("the footer of an empty section goes up to the tab", SettingsNav.tab(1),
                nav.move(new SettingsNav.Spot(1, SettingsNav.FOOTER, 0), SettingsNav.UP));
        assertSpot("crossing into it lands on its tab", SettingsNav.tab(1), nav.move(at(0, 0, 1), SettingsNav.RIGHT));
        assertSpot("crossing out of the tab strip passes through", SettingsNav.tab(2),
                nav.move(SettingsNav.tab(1), SettingsNav.RIGHT));
        assertSpot("crossing back lands on the tab again", SettingsNav.tab(1), nav.move(at(2, 0, 0), SettingsNav.LEFT));
    }

    @Test
    public void aPositionOutsideTheModelIsClampedNotAnException() {
        SettingsNav nav = english();
        assertSpot("section too high", at(3, 0, 2), nav.clamp(at(9, 0, 9)));
        assertSpot("section negative", at(0, 0, 0), nav.clamp(at(-4, 0, 0)));
        assertSpot("row too high", at(1, 1, 4), nav.clamp(at(1, 7, 99)));
        assertSpot("chip too high", at(0, 1, 2), nav.clamp(at(0, 1, 50)));
        assertSpot("chip negative", at(0, 1, 0), nav.clamp(at(0, 1, -3)));
        assertSpot("a tab with a wrong chip index is the tab of its section", SettingsNav.tab(2),
                nav.clamp(new SettingsNav.Spot(2, SettingsNav.TABS, 0)));
        assertSpot("a footer chip too high", new SettingsNav.Spot(1, SettingsNav.FOOTER, FOOTER - 1),
                nav.clamp(new SettingsNav.Spot(1, SettingsNav.FOOTER, 40)));
        // move() clamps first, so a stale position still gives a real answer in every direction.
        for (int dir = 0; dir < 4; dir++) {
            SettingsNav.Spot to = nav.move(at(12, 12, 12), dir);
            assertNotNull(to);
            assertEquals(to, nav.clamp(to));
        }
    }

    @Test
    public void badModelsAreRejected() {
        assertRejected(null, 3);
        assertRejected(new int[0][], 3);
        assertRejected(new int[][]{{2, 0}}, 3);
        assertRejected(new int[][]{{-1}}, 3);
        assertRejected(new int[][]{{2}}, 0);
        assertRejected(new int[][]{null}, 3);
    }

    private static void assertRejected(int[][] rows, int footer) {
        try {
            new SettingsNav(rows, footer, false);
            fail("should have been rejected");
        } catch (IllegalArgumentException expected) {
            // fine
        }
    }

    @Test
    public void firstIsTheFirstChipOfTheFirstRow() {
        assertSpot(at(0, 0, 0), english().first());
        assertSpot("an empty first section starts at its tab", SettingsNav.tab(0),
                new SettingsNav(new int[][]{{}, {2}}, 1, false).first());
    }

    // ---- nobody is locked out -------------------------------------------------------------------

    /** From every position the strip and the footer are reached by Up and Down alone, on both screens. */
    @Test
    public void everyPositionReachesTheStripAndTheFooterByVerticalKeys() {
        for (SettingsNav nav : new SettingsNav[]{english(), arabic()}) {
            for (SettingsNav.Spot spot : all(nav)) {
                SettingsNav.Spot p = spot;
                for (int i = 0; i < 20 && p.row != SettingsNav.FOOTER; i++) p = nav.move(p, SettingsNav.DOWN);
                assertEquals("footer from " + spot, SettingsNav.FOOTER, p.row);
                p = spot;
                for (int i = 0; i < 20 && p.row != SettingsNav.TABS; i++) p = nav.move(p, SettingsNav.UP);
                assertEquals("strip from " + spot, SettingsNav.TABS, p.row);
            }
        }
    }

    /** From the first tab, forward keys alone reach every section, and every position can be reached from there. */
    @Test
    public void everyPositionIsReachableFromTheStartOnBothScreens() {
        for (boolean rtl : new boolean[]{false, true}) {
            SettingsNav nav = new SettingsNav(ROWS, FOOTER, rtl);
            int forward = rtl ? SettingsNav.LEFT : SettingsNav.RIGHT;
            // The strip reaches every section by one forward key each.
            SettingsNav.Spot p = SettingsNav.tab(0);
            for (int s = 1; s < ROWS.length; s++) {
                p = nav.move(p, forward);
                assertEquals("tab " + s, SettingsNav.tab(s), p);
            }
            Set<SettingsNav.Spot> seen = new HashSet<>();
            ArrayDeque<SettingsNav.Spot> queue = new ArrayDeque<>();
            queue.add(SettingsNav.tab(0));
            seen.add(SettingsNav.tab(0));
            while (!queue.isEmpty()) {
                SettingsNav.Spot cur = queue.poll();
                for (int dir = 0; dir < 4; dir++) {
                    SettingsNav.Spot to = nav.move(cur, dir);
                    if (seen.add(to)) queue.add(to);
                }
            }
            // The footer's section is the one it was entered from: all footer spots of every section are reachable.
            for (SettingsNav.Spot spot : all(nav)) assertTrue("unreachable " + spot + " rtl=" + rtl, seen.contains(spot));
        }
    }

    /** A key never answers a position that the model does not have. */
    @Test
    public void everyAnswerIsARealPosition() {
        for (SettingsNav nav : new SettingsNav[]{english(), arabic()}) {
            for (SettingsNav.Spot spot : all(nav)) {
                for (int dir = 0; dir < 4; dir++) {
                    SettingsNav.Spot to = nav.move(spot, dir);
                    assertEquals(spot + " dir " + dir, to, nav.clamp(to));
                }
            }
        }
        assertFalse(all(english()).isEmpty());
    }

    private static Set<SettingsNav.Spot> all(SettingsNav nav) {
        Set<SettingsNav.Spot> out = new HashSet<>();
        for (int s = 0; s < ROWS.length; s++) {
            out.add(SettingsNav.tab(s));
            for (int r = 0; r < ROWS[s].length; r++) {
                for (int c = 0; c < ROWS[s][r]; c++) out.add(at(s, r, c));
            }
            for (int c = 0; c < FOOTER; c++) out.add(new SettingsNav.Spot(s, SettingsNav.FOOTER, c));
        }
        return out;
    }
}
