package com.gathin.tvhome;

/**
 * The maths of the Scenes look (docs/BATCH_S_SCENES.md): how the 1920 x 1080 picture covers a screen, where the clock box
 * is, which rows of the cut-out have to be decoded and what that costs in memory. Pure Java, unit tested on the JVM.
 */
final class SceneGeometry {
    /** Height of the clock box as a fraction of its font size. */
    static final float BOX_HEIGHT = 0.82f;
    /** The glass picture is stored at this size and stretched over the whole picture. */
    static final int GLASS_W = 480, GLASS_H = 270;
    /** Bytes per pixel: the background and glass are decoded as RGB_565, the cut-out band as ARGB_8888. */
    static final int BYTES_565 = 2, BYTES_8888 = 4;

    private SceneGeometry() {
    }

    /**
     * {scale, dx, dy} that map picture pixels to screen pixels with the same cover rule as CENTER_CROP: the picture fills
     * the screen, the longer side is cropped equally on both ends. A zero or negative screen gives the identity.
     */
    static float[] cover(int screenW, int screenH) {
        if (screenW <= 0 || screenH <= 0) return new float[]{1f, 0f, 0f};
        float scale = Math.max(screenW / (float) SceneList.PICTURE_W, screenH / (float) SceneList.PICTURE_H);
        return new float[]{scale, (screenW - SceneList.PICTURE_W * scale) / 2f, (screenH - SceneList.PICTURE_H * scale) / 2f};
    }

    /** Row below the clock box, in picture pixels, never past the bottom of the picture. */
    static int clockBottom(SceneList.Scene s) {
        return Math.min(SceneList.PICTURE_H, s.clockTop + Math.round(s.clockSize * BOX_HEIGHT));
    }

    /**
     * {first row, end row} of the cut-out the clock can touch (the end is exclusive), or null when the scene has no
     * cut-out or the clock box ends above the first opaque row, so nothing has to be decoded. Below the clock the
     * background already shows the same pixels as the cut-out.
     */
    static int[] band(SceneList.Scene s) {
        if (!s.hasCutOut()) return null;
        int start = Math.max(0, Math.max(s.cutFirstRow, s.clockTop));
        int end = clockBottom(s);
        return end > start ? new int[]{start, end} : null;
    }

    /** Decoded bytes of the cut-out band (ARGB_8888), 0 when there is none. */
    static long bandBytes(SceneList.Scene s) {
        int[] b = band(s);
        return b == null ? 0 : (long) (b[1] - b[0]) * SceneList.PICTURE_W * BYTES_8888;
    }

    /** Decoded bytes of the whole cut-out, for comparison with {@link #bandBytes}. */
    static long fullCutBytes(SceneList.Scene s) {
        if (!s.hasCutOut()) return 0;
        return (long) Math.max(0, SceneList.PICTURE_H - s.cutFirstRow) * SceneList.PICTURE_W * BYTES_8888;
    }

    /** Decoded bytes of one scene on screen: background, glass and the cut-out band. */
    static long sceneBytes(SceneList.Scene s) {
        return (long) SceneList.PICTURE_W * SceneList.PICTURE_H * BYTES_565
                + (long) GLASS_W * GLASS_H * BYTES_565 + bandBytes(s);
    }

    /** Left edge of the clock text in picture pixels for a text {@code textWidth} wide. */
    static float clockLeft(SceneList.Scene s, float textWidth) {
        return s.clockAtLeft ? SceneList.CLOCK_LEFT_MARGIN : (SceneList.PICTURE_W - textWidth) / 2f;
    }

    /** Height in picture pixels of the optional top scrim, never taller than the picture. */
    static int scrimHeight() {
        return Math.min(SceneList.PICTURE_H, SceneScrim.HEIGHT_PX);
    }
}
