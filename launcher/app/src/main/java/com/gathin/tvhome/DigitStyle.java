package com.gathin.tvhome;

/**
 * Digits on the home screen, picked in Launcher settings or as "digits" in config.json (docs/LAYOUT_OPTIONS.md).
 * "latin" is the default and changes nothing. "arabic" draws every number in Arabic-Indic digits: it changes how a
 * number is shown, never the number. The mapping is {@link Hijri#arabicDigits}. Pure Java, unit tested on the JVM.
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

    /** {@code text} with its digits drawn in the style; the same object for latin or an unknown style, null for null. */
    static String apply(String text, String style) {
        if (text == null || !arabic(style)) return text;
        return Hijri.arabicDigits(text);
    }
}
