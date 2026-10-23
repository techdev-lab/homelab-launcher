package com.gathin.tvhome;

/**
 * Option C of the Arabic theme, "tinted glass": the Home pills and interest cards are dark glass that takes a
 * touch of the ambient colour (the focused app's colour), so they always sit in the same hue as the glow behind
 * them instead of fighting it. Pure colour arithmetic, unit-tested; MainActivity applies it to the drawables.
 * A negative colour means no ambient glow yet: neutral glass.
 */
final class Glass {
    /**
     * Card: the app colour at 16% over black at 60%, composed into one ARGB. 60% (not 50%) keeps sand text at
     * 7.7:1 or more even over a white glow (GlassTest).
     */
    private static final float CARD_TINT = 0.16f, CARD_BLACK = 0.60f;
    /** Pill: lighter, the app colour at 18% over black at 30%. */
    private static final float PILL_TINT = 0.18f, PILL_BLACK = 0.30f;
    private static final int EDGE_ALPHA = 0x73;   // 45%
    private static final int NEUTRAL_EDGE = 0x24FFFFFF;

    private Glass() {
    }

    static int cardFill(int rgb) {
        return over(rgb, CARD_TINT, CARD_BLACK);
    }

    static int pillFill(int rgb) {
        return over(rgb, PILL_TINT, PILL_BLACK);
    }

    /** 1dp edge: the app colour at 45%, or a faint white hairline with no ambient colour. */
    static int edge(int rgb) {
        return rgb < 0 ? NEUTRAL_EDGE : (EDGE_ALPHA << 24) | (rgb & 0xFFFFFF);
    }

    /** {@code tint} of the colour over {@code black} of black, as a single colour with alpha (source-over). */
    static int over(int rgb, float tint, float black) {
        float t = rgb < 0 ? 0f : tint;
        float a = t + black * (1 - t);
        int r = rgb < 0 ? 0 : Math.round(((rgb >> 16) & 0xFF) * t / a);
        int g = rgb < 0 ? 0 : Math.round(((rgb >> 8) & 0xFF) * t / a);
        int b = rgb < 0 ? 0 : Math.round((rgb & 0xFF) * t / a);
        return (Math.round(a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /** {@code argb} painted over the opaque {@code below}; returns opaque RGB. Used by the contrast test. */
    static int composite(int argb, int below) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        int r = Math.round(((argb >> 16) & 0xFF) * a + ((below >> 16) & 0xFF) * (1 - a));
        int g = Math.round(((argb >> 8) & 0xFF) * a + ((below >> 8) & 0xFF) * (1 - a));
        int b = Math.round((argb & 0xFF) * a + (below & 0xFF) * (1 - a));
        return (r << 16) | (g << 8) | b;
    }
}
