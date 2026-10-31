package com.gathin.tvhome;

/**
 * Digits on the home screen and the screensaver, picked in Launcher settings or as "digits" in config.json
 * (docs/LAYOUT_OPTIONS.md). The choice holds for every number on either language: "latin" (the default) draws Latin
 * digits, "arabic" draws Arabic-Indic digits. It changes how a number is shown, never the number. The clock and the
 * Gregorian date take the system locale, which on an Arabic screen gives Arabic-Indic digits, so latin has to map
 * back too. Pure Java, unit tested on the JVM.
 */
final class DigitStyle {
    static final String LATIN = "latin", ARABIC = "arabic";
    static final String[] ALL = {LATIN, ARABIC};

    private DigitStyle() {
    }

    /** The TV's choice, else config.json "digits", else latin; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : LATIN;
    }

    static boolean valid(String s) {
        return LATIN.equals(s) || ARABIC.equals(s);
    }

    static boolean arabic(String style) {
        return ARABIC.equals(style);
    }

    /**
     * {@code text} with its digits drawn in the style, null for null. An unknown style counts as latin. Letters and
     * punctuation are never touched; digits already in the style are left as they are, so applying twice is the same
     * as once.
     */
    static String apply(String text, String style) {
        if (text == null) return null;
        return arabic(style) ? Hijri.arabicDigits(text) : latinDigits(text);
    }

    /**
     * Arabic-Indic digits (U+0660 to U+0669) to Latin, with their decimal (U+066B), thousands (U+066C) and percent
     * (U+066A) signs, which belong to the same numerals; everything else unchanged.
     */
    static String latinDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0660' && c <= '\u0669') b.append((char) ('0' + (c - '\u0660')));
            else if (c == '\u066b') b.append('.');
            else if (c == '\u066c') b.append(',');
            else if (c == '\u066a') b.append('%');
            else b.append(c);
        }
        return b.toString();
    }
}
