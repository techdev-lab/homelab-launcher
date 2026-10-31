package com.gathin.tvhome;

/**
 * Look of the plate behind the dock row, picked in Launcher settings (docs/LOOK_OPTIONS.md). "band" is the flat plate
 * of 2.9 and stays the default, so an upgrade changes nothing until the viewer picks another look. "floating" is a
 * translucent glass slab that takes the colour of the focused app. The padding around the tiles is the same in both,
 * because the name of the focused app is placed from it. Pure Java, unit tested on the JVM.
 */
final class DockStyle {
    static final String BAND = "band", FLOATING = "floating";
    static final String[] ALL = {BAND, FLOATING};

    /** Space between the dock tiles and the edge of the plate. */
    static final float PAD_DP = 14f;

    private static final float BAND_RADIUS_DP = 26f, FLOATING_RADIUS_DP = 34f;
    /** One normal tile corner; a softer corner (TileShape) moves the plate by the same amount, so it stays concentric. */
    private static final float TILE_CORNER_DP = 16f;
    private static final int BAND_FILL = 0xFF141416, BAND_EDGE = 0x1FFFFFFF;
    /** Floating glass: the app colour at 22% over black at 40% at the top, 14% over 62% at the bottom. */
    private static final float TOP_TINT = 0.22f, TOP_BLACK = 0.40f, BOTTOM_TINT = 0.14f, BOTTOM_BLACK = 0.62f;

    private DockStyle() {
    }

    /** The TV's choice, else config.json "dock_style", else the band; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : BAND;
    }

    static boolean valid(String s) {
        return BAND.equals(s) || FLOATING.equals(s);
    }

    static boolean floating(String style) {
        return FLOATING.equals(style);
    }

    /** Corner radius of the plate for a tile corner {@code tileCornerFactor} times the normal one (TileShape.factor). */
    static float radiusDp(String style, float tileCornerFactor) {
        float base = floating(style) ? FLOATING_RADIUS_DP : BAND_RADIUS_DP;
        return base + TILE_CORNER_DP * (tileCornerFactor - 1f);
    }

    /** Fill at the top of the plate; {@code rgb} is the ambient colour (0xRRGGBB), or negative for none yet. */
    static int fillTop(String style, int rgb) {
        return floating(style) ? Glass.over(rgb, TOP_TINT, TOP_BLACK) : BAND_FILL;
    }

    /** Fill at the bottom of the plate. */
    static int fillBottom(String style, int rgb) {
        return floating(style) ? Glass.over(rgb, BOTTOM_TINT, BOTTOM_BLACK) : BAND_FILL;
    }

    /** The 1 dp edge of the plate. */
    static int edge(String style, int rgb) {
        return floating(style) ? Glass.edge(rgb) : BAND_EDGE;
    }
}
