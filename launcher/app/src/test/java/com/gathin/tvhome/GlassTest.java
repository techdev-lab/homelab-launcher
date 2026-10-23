package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GlassTest {
    private static final int GROUND = 0x0B0B0C, SAND = 0xF2EEE6, DIM = 0xC3B8A2;
    /** Netflix red, YouTube red, Shahid green, Jellyfin purple, and the hardest case: pure yellow and white. */
    private static final int[] APPS = {0xE50914, 0xFF0033, 0x00B48C, 0x8E5BD0, 0xFFD400, 0xFFFFFF};

    private static double lum(int rgb) {
        double[] c = {((rgb >> 16) & 0xFF) / 255.0, ((rgb >> 8) & 0xFF) / 255.0, (rgb & 0xFF) / 255.0};
        for (int i = 0; i < 3; i++) c[i] = c[i] <= 0.03928 ? c[i] / 12.92 : Math.pow((c[i] + 0.055) / 1.055, 2.4);
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    private static double contrast(int a, int b) {
        double x = lum(a), y = lum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }

    /** The ambient glow at its strongest point: the app colour at 0x5C (36%) over the ground (MainActivity.setAmbient). */
    private static int glowPeak(int app) {
        return Glass.composite((0x5C << 24) | app, GROUND);
    }

    @Test
    public void cardTextStaysReadableOverEveryGlow() {
        for (int app : APPS) {
            int behind = Glass.composite(Glass.cardFill(app), glowPeak(app));
            assertTrue(String.format("sand on card over %06X", app), contrast(SAND, behind) >= 7);
            assertTrue(String.format("dim on card over %06X", app), contrast(DIM, behind) >= 4.5);
        }
    }

    @Test
    public void pillTextStaysReadableOverEveryGlow() {
        for (int app : APPS) {
            int behind = Glass.composite(Glass.pillFill(app), glowPeak(app));
            assertTrue(String.format("sand on pill over %06X: %.2f", app, contrast(SAND, behind)),
                    contrast(SAND, behind) >= 4.5);
        }
    }

    @Test
    public void neutralWithoutAmbientColour() {
        assertEquals(0x00, (Glass.cardFill(-1) >> 16) & 0xFF);
        assertEquals(0x24FFFFFF, Glass.edge(-1));
        assertEquals(Math.round(0.6f * 255), (Glass.cardFill(-1) >>> 24) & 0xFF);
    }

    @Test
    public void cardTakesTheAppHue() {
        int c = Glass.cardFill(0xE50914);
        assertTrue(((c >> 16) & 0xFF) > ((c >> 8) & 0xFF));   // red channel leads for Netflix
        assertEquals(0x73E50914, Glass.edge(0xE50914));
    }
}
