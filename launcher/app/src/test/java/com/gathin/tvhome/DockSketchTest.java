package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/** The little drawing of the dock layouts on the settings chips (docs/SETTINGS_GLASS.md). Cells are x, y, w, h in 0..1. */
public class DockSketchTest {

    private static List<float[]> dockCells(float[][] cells) {
        List<float[]> out = new ArrayList<>();
        for (float[] c : cells) if (DockSketch.isDock(c)) out.add(c);
        return out;
    }

    private static List<float[]> gridCells(float[][] cells) {
        List<float[]> out = new ArrayList<>();
        for (float[] c : cells) if (!DockSketch.isDock(c)) out.add(c);
        return out;
    }

    @Test
    public void bottomPutsTheDockInARowBelowEveryGridCell() {
        float[][] cells = DockSketch.cells(DockPosition.BOTTOM, false);
        List<float[]> dock = dockCells(cells), grid = gridCells(cells);
        assertTrue(dock.size() >= 3);
        assertTrue(grid.size() >= 3);
        for (float[] d : dock) {
            assertEquals("one row", dock.get(0)[1], d[1], 0.0001f);
            for (float[] g : grid) assertTrue("dock below the grid", d[1] >= g[1] + g[3]);
        }
    }

    @Test
    public void sidePutsTheDockInAColumnAtTheRightOnEnglishAndTheLeftOnArabic() {
        for (boolean rtl : new boolean[]{false, true}) {
            float[][] cells = DockSketch.cells(DockPosition.SIDE, rtl);
            List<float[]> dock = dockCells(cells), grid = gridCells(cells);
            assertTrue(dock.size() >= 3);
            assertTrue(grid.size() >= 3);
            for (float[] d : dock) {
                assertEquals("one column", dock.get(0)[0], d[0], 0.0001f);
                for (float[] g : grid) {
                    if (rtl) assertTrue("dock left of the grid", d[0] + d[2] <= g[0]);
                    else assertTrue("dock right of the grid", d[0] >= g[0] + g[2]);
                }
            }
        }
    }

    @Test
    public void arabicIsTheMirrorImageOfEnglishAndBottomDoesNotMirror() {
        float[][] en = DockSketch.cells(DockPosition.SIDE, false);
        float[][] ar = DockSketch.cells(DockPosition.SIDE, true);
        assertEquals(en.length, ar.length);
        for (int i = 0; i < en.length; i++) {
            assertEquals("x mirrored", 1f - en[i][0] - en[i][2], ar[i][0], 0.0001f);
            assertEquals(en[i][1], ar[i][1], 0.0001f);
            assertEquals(en[i][2], ar[i][2], 0.0001f);
            assertEquals(en[i][3], ar[i][3], 0.0001f);
            assertEquals(en[i][4], ar[i][4], 0f);
        }
        // The bottom row is centred, so it looks the same on both screens.
        float[][] b1 = DockSketch.cells(DockPosition.BOTTOM, false), b2 = DockSketch.cells(DockPosition.BOTTOM, true);
        assertEquals(b1.length, b2.length);
        for (int i = 0; i < b1.length; i++) {
            float[] mirrored = {1f - b1[i][0] - b1[i][2], b1[i][1], b1[i][2], b1[i][3], b1[i][4]};
            boolean found = false;
            for (float[] c : b2) found |= Arrays.equals(c, mirrored) || near(c, mirrored);
            assertTrue("the bottom layout is symmetric", found);
        }
    }

    private static boolean near(float[] a, float[] b) {
        for (int i = 0; i < 5; i++) if (Math.abs(a[i] - b[i]) > 0.0001f) return false;
        return true;
    }

    @Test
    public void everyCellIsInsideTheFrameAndNoTwoOverlap() {
        for (String position : DockPosition.ALL) {
            for (boolean rtl : new boolean[]{false, true}) {
                float[][] cells = DockSketch.cells(position, rtl);
                for (float[] c : cells) {
                    assertTrue(position + " x", c[0] >= 0f && c[0] + c[2] <= 1.0001f);
                    assertTrue(position + " y", c[1] >= 0f && c[1] + c[3] <= 1.0001f);
                    assertTrue(position + " size", c[2] > 0f && c[3] > 0f);
                }
                for (int i = 0; i < cells.length; i++) {
                    for (int j = i + 1; j < cells.length; j++) {
                        float[] a = cells[i], b = cells[j];
                        boolean apart = a[0] + a[2] <= b[0] + 0.0001f || b[0] + b[2] <= a[0] + 0.0001f
                                || a[1] + a[3] <= b[1] + 0.0001f || b[1] + b[3] <= a[1] + 0.0001f;
                        assertTrue(position + " cells " + i + " and " + j + " overlap", apart);
                    }
                }
            }
        }
    }

    @Test
    public void anUnknownPositionDrawsTheDefaultBottomLayout() {
        float[][] unknown = DockSketch.cells("diagonal", false);
        float[][] bottom = DockSketch.cells(DockPosition.BOTTOM, false);
        assertEquals(bottom.length, unknown.length);
        for (int i = 0; i < bottom.length; i++) assertTrue(near(bottom[i], unknown[i]));
        assertEquals(bottom.length, DockSketch.cells(null, true).length);
    }
}
