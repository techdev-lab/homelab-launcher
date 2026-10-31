package com.gathin.tvhome;

/**
 * How round the app tiles are, picked in Launcher settings (docs/LOOK_OPTIONS.md). "normal" is the 16 dp corner of
 * 2.9 and stays the default. "soft" is one and a half times that. The glass highlight and the dock plate follow the
 * tile corner, so all three stay concentric. Pure Java, unit tested on the JVM.
 */
final class TileShape {
    static final String NORMAL = "normal", SOFT = "soft";
    static final String[] ALL = {NORMAL, SOFT};

    private static final float CORNER_DP = 16f, SOFT_FACTOR = 1.5f;

    private TileShape() {
    }

    /** The TV's choice, else config.json "tile_corners", else normal; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : NORMAL;
    }

    static boolean valid(String s) {
        return NORMAL.equals(s) || SOFT.equals(s);
    }

    /** Factor on the normal tile corner: 1 or 1.5. */
    static float factor(String shape) {
        return SOFT.equals(shape) ? SOFT_FACTOR : 1f;
    }

    /**
     * Corner radius for a tile of this width: 16 dp at the design width (times the shape's factor), proportionally
     * less when the tile is narrower, so small tiles keep the same shape.
     */
    static float radiusPx(int tileWidthPx, float density, float designWidthDp, String shape) {
        return CORNER_DP * density * Math.min(1f, tileWidthPx / (designWidthDp * density)) * factor(shape);
    }
}
