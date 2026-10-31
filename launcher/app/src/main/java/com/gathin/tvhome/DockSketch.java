package com.gathin.tvhome;

/**
 * The little drawing of a dock layout on the settings chips (docs/SETTINGS_GLASS.md): the cells of a screen as
 * {@code {x, y, w, h, dock}} in 0..1, where dock is 1 for a cell of the dock and 0 for one of the app grid. The side
 * layout is mirrored on the Arabic screen like the real one (the column is at the left); the bottom row is centred, so it
 * looks the same on both. Pure Java, unit tested on the JVM.
 */
final class DockSketch {
    private static final float DOCK_CELL = 0.14f, GRID_CELL = 0.12f, GAP = 0.05f, MARGIN = 0.08f;

    private DockSketch() {
    }

    static boolean isDock(float[] cell) {
        return cell[4] > 0.5f;
    }

    /** An unknown position draws the default, the bottom row. */
    static float[][] cells(String position, boolean rtl) {
        return DockPosition.side(position) ? side(rtl) : bottom();
    }

    /** Two rows of four apps, and under them the dock: a centred row of four. */
    private static float[][] bottom() {
        float[][] out = new float[12][];
        int n = 0;
        float gridW = 4 * GRID_CELL + 3 * GAP;
        float x0 = (1f - gridW) / 2f;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                out[n++] = new float[]{x0 + col * (GRID_CELL + GAP), MARGIN + row * (GRID_CELL + GAP), GRID_CELL, GRID_CELL, 0f};
            }
        }
        float dockW = 4 * DOCK_CELL + 3 * GAP;
        float dx0 = (1f - dockW) / 2f;
        float dy = 1f - MARGIN - DOCK_CELL;
        for (int col = 0; col < 4; col++) out[n++] = new float[]{dx0 + col * (DOCK_CELL + GAP), dy, DOCK_CELL, DOCK_CELL, 1f};
        return out;
    }

    /** The dock as a column of four at the end of the reading side, the other apps in three columns of three beside it. */
    private static float[][] side(boolean rtl) {
        float[][] out = new float[13][];
        int n = 0;
        // English: the grid from the left margin and the dock column at the right; mirrored below for Arabic.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                out[n++] = new float[]{MARGIN + col * (GRID_CELL + GAP), MARGIN + row * (GRID_CELL + GAP) + 0.06f, GRID_CELL,
                        GRID_CELL, 0f};
            }
        }
        float dx = 1f - MARGIN - DOCK_CELL;
        for (int i = 0; i < 4; i++) out[n++] = new float[]{dx, MARGIN + i * (DOCK_CELL + GAP * 0.5f), DOCK_CELL, DOCK_CELL, 1f};
        if (rtl) {
            for (float[] c : out) c[0] = 1f - c[0] - c[2];
        }
        return out;
    }
}
