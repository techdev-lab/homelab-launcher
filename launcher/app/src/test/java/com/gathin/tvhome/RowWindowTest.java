package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Which whole rows a scrolling section shows (docs/SETTINGS_GLASS.md). Rows are 100 high with a 10 gap unless said. */
public class RowWindowTest {

    private static int[] tops(int n) {
        int[] t = new int[n];
        for (int i = 0; i < n; i++) t[i] = i * 110;
        return t;
    }

    private static int[] bottoms(int n) {
        int[] b = new int[n];
        for (int i = 0; i < n; i++) b[i] = i * 110 + 100;
        return b;
    }

    @Test
    public void whenEverythingFitsTheFirstRowStaysOnTopWhateverIsFocused() {
        for (int focused = 0; focused < 5; focused++) {
            assertEquals(0, RowWindow.top(tops(5), bottoms(5), 600, 0, focused));
        }
        assertEquals(4, RowWindow.last(tops(5), bottoms(5), 600, 0));
        assertFalse(RowWindow.scrolls(tops(5), bottoms(5), 600));
    }

    @Test
    public void movingDownScrollsJustEnoughToShowTheFocusedRowInWhole() {
        int[] t = tops(6), b = bottoms(6);
        // 250 shows two whole rows (210) but not three (320).
        assertEquals(0, RowWindow.top(t, b, 250, 0, 1));
        assertEquals(1, RowWindow.top(t, b, 250, 0, 2));
        assertEquals(2, RowWindow.top(t, b, 250, 1, 3));
        assertEquals(1, RowWindow.last(t, b, 250, 0));
        assertTrue(RowWindow.scrolls(t, b, 250));
    }

    @Test
    public void movingUpPutsTheFocusedRowOnTopAndNeverCutsIt() {
        int[] t = tops(6), b = bottoms(6);
        assertEquals(2, RowWindow.top(t, b, 250, 4, 2));
        assertEquals(0, RowWindow.top(t, b, 250, 4, 0));
    }

    @Test
    public void aFocusedRowInsideTheWindowKeepsTheWindowWhereItIs() {
        int[] t = tops(6), b = bottoms(6);
        assertEquals(2, RowWindow.top(t, b, 250, 2, 2));
        assertEquals(2, RowWindow.top(t, b, 250, 2, 3));
    }

    @Test
    public void theLastWholeRowNeverShowsAHalfRowBelowIt() {
        int[] t = tops(6), b = bottoms(6);
        // From row 1 the window reaches 110 + 250 = 360: rows 1 and 2 (bottom 320) fit, row 3 (bottom 430) does not.
        assertEquals(2, RowWindow.last(t, b, 250, 1));
        // From the last window the end of the list is reached.
        assertEquals(5, RowWindow.last(t, b, 250, 4));
    }

    @Test
    public void aRowTallerThanTheViewportIsShownAloneAndStillCounts() {
        int[] t = {0, 410};
        int[] b = {400, 460};
        assertEquals(0, RowWindow.top(t, b, 300, 0, 0));
        assertEquals(0, RowWindow.last(t, b, 300, 0));
        assertEquals(1, RowWindow.top(t, b, 300, 0, 1));
    }

    @Test
    public void rowsOfDifferentHeightsAreMeasuredNotCounted() {
        // A tall row (wallpapers, 200) then three short ones (60) with 10 gaps.
        int[] t = {0, 210, 280, 350};
        int[] b = {200, 270, 340, 410};
        assertEquals(1, RowWindow.last(t, b, 300, 0));
        assertEquals(1, RowWindow.top(t, b, 300, 0, 2));
        assertEquals(3, RowWindow.last(t, b, 300, 1));
    }

    @Test
    public void inputsOutsideTheListAreClampedAndNoRowsIsNotAnError() {
        int[] t = tops(3), b = bottoms(3);
        assertEquals(0, RowWindow.top(t, b, 250, -5, -2));
        int far = RowWindow.top(t, b, 250, 99, 99);
        assertTrue("a real row", far >= 0 && far <= 2);
        assertEquals(0, RowWindow.top(new int[0], new int[0], 250, 3, 3));
        assertEquals(0, RowWindow.last(new int[0], new int[0], 250, 0));
        assertFalse(RowWindow.scrolls(new int[0], new int[0], 250));
        assertEquals("a negative top counts as the first row", 1, RowWindow.last(t, b, 250, -4));
    }

    @Test
    public void aViewportThatIsZeroShowsOneRowAndMismatchedListsAreRejected() {
        int[] t = tops(3), b = bottoms(3);
        assertEquals(1, RowWindow.top(t, b, 0, 0, 1));
        assertEquals(1, RowWindow.last(t, b, 0, 1));
        try {
            RowWindow.top(new int[2], new int[3], 100, 0, 0);
            fail("lists of different length");
        } catch (IllegalArgumentException expected) {
            // fine
        }
    }
}
