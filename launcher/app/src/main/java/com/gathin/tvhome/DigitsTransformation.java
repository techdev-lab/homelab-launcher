package com.gathin.tvhome;

import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.TextView;

/**
 * Draws the digits of a text view in the chosen style, Latin or Arabic-Indic (the "digits" option, {@link DigitStyle}).
 * The text itself is untouched, so a clock keeps formatting itself and the cards keep building their text as before.
 */
final class DigitsTransformation implements TransformationMethod {
    private static final DigitsTransformation LATIN = new DigitsTransformation(DigitStyle.LATIN);
    private static final DigitsTransformation ARABIC = new DigitsTransformation(DigitStyle.ARABIC);
    private static final char LINE_FEED = 10, CARRIAGE_RETURN = 13;

    private final String style;

    private DigitsTransformation(String style) {
        this.style = style;
    }

    /** The transformation for a style; an unknown style is latin. */
    static DigitsTransformation of(String style) {
        return DigitStyle.arabic(style) ? ARABIC : LATIN;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        if (source == null) return null;
        String text = DigitStyle.apply(source.toString(), style);
        // A single-line view had the stock transformation, which shows a line break as a space; keep that.
        if (view instanceof TextView && ((TextView) view).getMaxLines() == 1) {
            text = text.replace(LINE_FEED, ' ').replace(CARRIAGE_RETURN, ' ');
        }
        return text;
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction,
                               Rect previouslyFocusedRect) {
    }
}
