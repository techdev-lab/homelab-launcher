package com.gathin.tvhome;

import android.graphics.Rect;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.TextView;

/**
 * Draws the digits of a text view as Arabic-Indic digits (the "digits" option, {@link DigitStyle}). The text itself is
 * untouched, so a clock keeps formatting itself and the cards keep building their text as before.
 */
final class DigitsTransformation implements TransformationMethod {
    static final DigitsTransformation INSTANCE = new DigitsTransformation();
    private static final char LINE_FEED = 10, CARRIAGE_RETURN = 13;

    private DigitsTransformation() {
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        if (source == null) return null;
        String text = Hijri.arabicDigits(source.toString());
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
