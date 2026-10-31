package com.gathin.tvhome;

/**
 * Where the dock is, picked in Launcher settings or as "dock_position" in config.json (docs/LAYOUT_OPTIONS.md).
 * "bottom" is today's layout and the default: the dock is the first row of the app grid. "side" puts the dock in a
 * column at the end of the reading side, under the top-bar buttons, with the other apps in a narrower grid beside it:
 * on the right on the English screen and on the left on the Arabic one, which is mirrored like the rest of that layout.
 * Pure Java, unit tested on the JVM.
 */
final class DockPosition {
    static final String BOTTOM = "bottom", SIDE = "side";
    static final String[] ALL = {BOTTOM, SIDE};

    /** Width of a dock tile in the side layout (dp); the height keeps the shape of every tile (180 by 101). */
    static final float TILE_W_DP = 108f;
    /** Room on each side of the plate for the focus zoom of a tile and the glass highlight. */
    private static final float FOCUS_ROOM_DP = 10f;
    private static final int MIN_REST_COLUMNS = 3;

    private DockPosition() {
    }

    /** The TV's choice, else config.json "dock_position", else the bottom; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : BOTTOM;
    }

    static boolean valid(String s) {
        return BOTTOM.equals(s) || SIDE.equals(s);
    }

    /** True for the column at the side. */
    static boolean side(String position) {
        return SIDE.equals(position);
    }

    /** Columns of the grid of the other apps next to the dock column: one fewer than the dock has slots, at least three. */
    static int restColumns(int columns) {
        return Math.max(MIN_REST_COLUMNS, columns - 1);
    }

    static int tileHeightDp() {
        return Math.round(TILE_W_DP * AppAdapter.DESIGN_H_DP / AppAdapter.DESIGN_W_DP);
    }

    /** The dock column: the tile, the plate's padding on both sides, and room for the zoom of the focused tile. */
    static float columnWidthDp() {
        return TILE_W_DP + 2 * DockStyle.PAD_DP + 2 * FOCUS_ROOM_DP;
    }
}
