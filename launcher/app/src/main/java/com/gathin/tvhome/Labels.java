package com.gathin.tvhome;

/**
 * Names the launcher draws but does not write itself: app labels on the tiles and a theme's festive greeting.
 * Pure Java, unit tested on the JVM.
 */
final class Labels {

    private Labels() {
    }

    /**
     * An app's own label as a tile shows it: control, format (bidi overrides, zero width joiners),
     * line and paragraph separator characters become spaces, runs of spaces collapse, at most 40
     * characters (never cutting a surrogate pair), the fallback (package name) when nothing is left.
     * A label with a bidi override or a line separator must not redraw the tile name as something else (#117).
     */
    static String clean(String s, String fallback) {
        if (s == null) return fallback;
        StringBuilder b = new StringBuilder(Math.min(s.length(), 48));
        boolean space = true;   // leading spaces are dropped too
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int type = Character.getType(c);
            boolean blank = c == ' ' || Character.isISOControl(c) || Character.isWhitespace(c) || type == Character.FORMAT
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR
                    || type == Character.SPACE_SEPARATOR;
            if (blank) {
                if (!space) b.append(' ');
                space = true;
                continue;
            }
            boolean pair = Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1));
            if (b.length() + (pair ? 2 : 1) > 40) break;
            b.append(c);
            if (pair) b.append(s.charAt(++i));
            space = false;
        }
        String out = b.toString().trim();
        return out.isEmpty() ? fallback : out;
    }
}
