package com.gathin.tvhome;

/**
 * Arrow keys of the side dock layout (docs/LAYOUT_OPTIONS.md). The dock column holds positions 0 to
 * {@code dockSize - 1}; the grid holds the other apps, {@code cols} wide. The dock is at the end of the reading side: at
 * physical right on the English screen and at physical left on the Arabic one ({@code rtl}), where the grid is mirrored
 * too, so in both the last column of a row is the one next to the dock. Positions are absolute, as in MainActivity. Each
 * function answers one key with an {@link Action}; MainActivity carries it out, because "the tile nearest in height" needs
 * the real views. Pure Java, unit tested on the JVM, including that no tile is a dead end.
 */
final class SideNav {
    static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;

    /** The key does nothing: focus stays where it is, and never wraps or escapes to a tile far away. */
    static final int STAY = 0;
    /** Focus the app at {@link Action#index}. */
    static final int TILE = 1;
    /** The strip card nearest across, else the top-bar button nearest across. */
    static final int STRIP_OR_TOP = 2;
    static final int TOP_BUTTONS = 3;
    /** The dock tile nearest in height; the first dock tile; the grid tile nearest in height; the strip's first row. */
    static final int DOCK_NEAR = 4, DOCK_FIRST = 5, GRID_NEAR = 6, STRIP_FIRST = 7;

    private SideNav() {
    }

    static final class Action {
        final int kind;
        final int index;

        Action(int kind, int index) {
            this.kind = kind;
            this.index = index;
        }
    }

    private static final Action STAY_PUT = new Action(STAY, -1);

    static Action of(int kind) {
        return kind == STAY ? STAY_PUT : new Action(kind, -1);
    }

    private static Action tile(int index) {
        return new Action(TILE, index);
    }

    /**
     * A key on a grid tile ({@code pos >= dockSize}). {@code rtl} is the mirrored screen (Arabic): its first column is at
     * physical right and the dock at physical left, so the key toward the dock is Left instead of Right.
     */
    static Action grid(int pos, int count, int dockSize, int cols, boolean rtl, int dir) {
        int rel = pos - dockSize;
        int restCount = count - dockSize;
        int row = rel / cols, col = rel % cols;
        if (dir == UP) return row == 0 ? of(STRIP_OR_TOP) : tile(pos - cols);
        if (dir == DOWN) {
            int lastRowStart = ((restCount - 1) / cols) * cols;
            return rel >= lastRowStart ? of(STAY) : tile(Math.min(pos + cols, count - 1));
        }
        boolean towardStart = (dir == LEFT) != rtl;   // the first column is at physical left unless the screen is mirrored
        if (towardStart) return col == 0 ? of(STAY) : tile(pos - 1);
        if (col == cols - 1 || pos == count - 1) return of(DOCK_NEAR);   // the end of a row is next to the dock
        return tile(pos + 1);
    }

    /**
     * A key on a dock tile ({@code pos < dockSize}); {@code dockFilled} slots hold an app, the rest take no focus. The key
     * toward the grid is Left on the English screen and Right on the mirrored (Arabic) one.
     */
    static Action dock(int pos, int dockFilled, int count, int dockSize, boolean rtl, int dir) {
        if (dir == UP) return pos == 0 ? of(TOP_BUTTONS) : tile(pos - 1);
        if (dir == DOWN) return pos >= dockFilled - 1 ? of(STAY) : tile(pos + 1);
        boolean towardGrid = (dir == LEFT) != rtl;
        return towardGrid && count > dockSize ? of(GRID_NEAR) : of(STAY);
    }

    /** Down from the last strip row: the grid, or the dock when there are no other apps. */
    static Action fromStripDown(int count, int dockSize) {
        return count > dockSize ? of(GRID_NEAR) : of(DOCK_NEAR);
    }

    /**
     * Down from a top-bar button. A button above the dock column goes straight to the dock; the others go to the strip,
     * else the grid, else the dock.
     */
    static Action fromTopDown(boolean aboveDock, boolean stripVisible, int count, int dockSize) {
        if (aboveDock) return of(DOCK_FIRST);
        if (stripVisible) return of(STRIP_FIRST);
        return count > dockSize ? of(GRID_NEAR) : of(DOCK_FIRST);
    }

    /** Index of the centre closest to {@code target}, the upper one on a tie; -1 when there are none. */
    static int nearest(double[] centres, double target) {
        if (centres == null || centres.length == 0) return -1;
        int best = 0;
        for (int i = 1; i < centres.length; i++) {
            if (Math.abs(centres[i] - target) < Math.abs(centres[best] - target)) best = i;
        }
        return best;
    }
}
