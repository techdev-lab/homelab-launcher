package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * The scroll area of a glass settings section. It does not scroll to a focused child on its own and has no fading edge:
 * LauncherSettingsActivity scrolls it by whole rows (RowWindow), so no row is ever cut.
 */
public final class RowScrollView extends ScrollView {
    public RowScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return 0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return 0f;
    }
}
