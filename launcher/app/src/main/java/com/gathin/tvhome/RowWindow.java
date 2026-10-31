package com.gathin.tvhome;

/**
 * Which whole rows a scrolling section of the glass settings shows (docs/SETTINGS_GLASS.md). The rows are given by their top
 * and bottom edges in the section; the viewport is how much height is available. The focused row decides the first row shown
 * (the current one while the focused row is inside the window), and the window ends at the last row that fits whole, so no
 * row is ever cut. A row taller than the viewport is shown alone. Out-of-range rows are clamped. Pure Java, unit tested on
 * the JVM.
 */
final class RowWindow {
    private RowWindow() {
    }

    /** The first row to show with {@code focused} on screen; {@code currentTop} is the first row shown now. */
    static int top(int[] tops, int[] bottoms, int viewport, int currentTop, int focused) {
        check(tops, bottoms);
        int n = tops.length;
        if (n == 0) return 0;
        int f = Math.max(0, Math.min(focused, n - 1));
        int t = Math.max(0, Math.min(currentTop, n - 1));
        if (f < t) return f;
        while (t < f && bottoms[f] - tops[t] > viewport) t++;
        return t;
    }

    /** The last row that fits whole when {@code top} is the first one shown (at least {@code top} itself). */
    static int last(int[] tops, int[] bottoms, int viewport, int top) {
        check(tops, bottoms);
        int n = tops.length;
        if (n == 0) return 0;
        int t = Math.max(0, Math.min(top, n - 1));
        int last = t;
        while (last + 1 < n && bottoms[last + 1] - tops[t] <= viewport) last++;
        return last;
    }

    /** True when the rows do not all fit in the viewport. */
    static boolean scrolls(int[] tops, int[] bottoms, int viewport) {
        check(tops, bottoms);
        int n = tops.length;
        return n > 0 && bottoms[n - 1] - tops[0] > viewport;
    }

    private static void check(int[] tops, int[] bottoms) {
        if (tops == null || bottoms == null || tops.length != bottoms.length) {
            throw new IllegalArgumentException("one top and one bottom per row");
        }
    }
}
