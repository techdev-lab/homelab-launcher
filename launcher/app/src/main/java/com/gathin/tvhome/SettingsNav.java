package com.gathin.tvhome;

/**
 * Arrow keys of the glass settings screen (docs/SETTINGS_GLASS.md). The screen is a strip of section tabs, the rows of
 * the visible section, and a footer row of buttons. A {@link Spot} is a place on it; chips are numbered in reading order,
 * so on the mirrored (Arabic) screen chip 0 is at the right and the two horizontal keys swap ({@code rtl}). Each call
 * answers one key with the next Spot, or the same Spot when the key does nothing: focus never wraps and never lands on a
 * place the model does not have. The Activity maps Spots to views and moves focus; if it has no view it lets Android's own
 * focus search answer, so a mistake here cannot lock the screen. Pure Java, unit tested on the JVM.
 */
final class SettingsNav {
    static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;

    /** Row numbers of the two zones that are not rows of a section; the strip's chip number is its section. */
    static final int TABS = -1, FOOTER = -2;

    /** A place: the section (for the footer, the one that is showing), the row and the chip in reading order. */
    static final class Spot {
        final int section, row, chip;

        Spot(int section, int row, int chip) {
            this.section = section;
            this.row = row;
            this.chip = chip;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Spot && ((Spot) o).section == section && ((Spot) o).row == row && ((Spot) o).chip == chip;
        }

        @Override
        public int hashCode() {
            return (section * 31 + row) * 31 + chip;
        }

        @Override
        public String toString() {
            return "Spot(" + section + ", " + (row == TABS ? "tabs" : row == FOOTER ? "footer" : String.valueOf(row))
                    + ", " + chip + ")";
        }
    }

    static Spot tab(int section) {
        return new Spot(section, TABS, section);
    }

    private final int[][] rowChips;
    private final int footerChips;
    private final boolean rtl;

    /**
     * {@code rowChips[s][r]} is how many chips row {@code r} of section {@code s} shows (only the visible ones); a section
     * may have no rows, a row must have at least one chip, and the footer at least one button.
     */
    SettingsNav(int[][] rowChips, int footerChips, boolean rtl) {
        if (rowChips == null || rowChips.length == 0) throw new IllegalArgumentException("no sections");
        if (footerChips < 1) throw new IllegalArgumentException("the footer needs a button");
        int[][] copy = new int[rowChips.length][];
        for (int s = 0; s < rowChips.length; s++) {
            if (rowChips[s] == null) throw new IllegalArgumentException("section " + s + " is null");
            copy[s] = rowChips[s].clone();
            for (int count : copy[s]) {
                if (count < 1) throw new IllegalArgumentException("a row needs a chip");
            }
        }
        this.rowChips = copy;
        this.footerChips = footerChips;
        this.rtl = rtl;
    }

    int sections() {
        return rowChips.length;
    }

    /** Where focus starts: the first chip of the first row, or the first tab when that section has no rows. */
    Spot first() {
        return rowChips[0].length > 0 ? new Spot(0, 0, 0) : tab(0);
    }

    /** The nearest real place to {@code spot}: nothing outside the model is ever answered or accepted. */
    Spot clamp(Spot spot) {
        int s = Math.max(0, Math.min(spot.section, rowChips.length - 1));
        if (spot.row == TABS) return tab(s);
        if (spot.row == FOOTER) return new Spot(s, FOOTER, Math.max(0, Math.min(spot.chip, footerChips - 1)));
        int rows = rowChips[s].length;
        if (rows == 0) return tab(s);
        int r = Math.max(0, Math.min(spot.row, rows - 1));
        return new Spot(s, r, Math.max(0, Math.min(spot.chip, rowChips[s][r] - 1)));
    }

    /** The answer to {@code dir} (UP, DOWN, LEFT or RIGHT) at {@code from}; the same place when the key does nothing. */
    Spot move(Spot from, int dir) {
        Spot at = clamp(from);
        if (dir == UP) return up(at);
        if (dir == DOWN) return down(at);
        boolean forward = (dir == RIGHT) != rtl;   // chip 0 is at physical left unless the screen is mirrored
        return sideways(at, forward);
    }

    private Spot up(Spot at) {
        if (at.row == TABS) return at;
        if (at.row == FOOTER) {
            int rows = rowChips[at.section].length;
            return rows == 0 ? tab(at.section) : clamp(new Spot(at.section, rows - 1, at.chip));
        }
        if (at.row == 0) return tab(at.section);
        return clamp(new Spot(at.section, at.row - 1, at.chip));
    }

    private Spot down(Spot at) {
        if (at.row == FOOTER) return at;
        int rows = rowChips[at.section].length;
        if (at.row == TABS) return rows == 0 ? clamp(new Spot(at.section, FOOTER, 0)) : new Spot(at.section, 0, 0);
        if (at.row == rows - 1) return clamp(new Spot(at.section, FOOTER, at.chip));
        return clamp(new Spot(at.section, at.row + 1, at.chip));
    }

    private Spot sideways(Spot at, boolean forward) {
        if (at.row == TABS) {
            int to = at.section + (forward ? 1 : -1);
            return to < 0 || to >= rowChips.length ? at : tab(to);
        }
        if (at.row == FOOTER) {
            int to = at.chip + (forward ? 1 : -1);
            return to < 0 || to >= footerChips ? at : new Spot(at.section, FOOTER, to);
        }
        int count = rowChips[at.section][at.row];
        int to = at.chip + (forward ? 1 : -1);
        if (to >= 0 && to < count) return new Spot(at.section, at.row, to);
        // The edge of the row: the neighbouring section at the same row (or its last row), entered from its near edge.
        int section = at.section + (forward ? 1 : -1);
        if (section < 0 || section >= rowChips.length) return at;
        int rows = rowChips[section].length;
        if (rows == 0) return tab(section);
        int row = Math.min(at.row, rows - 1);
        return new Spot(section, row, forward ? 0 : rowChips[section][row] - 1);
    }
}
