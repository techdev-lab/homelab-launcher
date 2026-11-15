package com.gathin.tvhome;

/**
 * What one tile of the grid shows and which picture each view must give up (docs/BATCH_F_FIXES.md, F5). A tile shows its
 * banner or its icon, never both, and the view that is hidden holds no drawable: a recycled tile that kept the hidden
 * icon carried a second bitmap it never drew. Pure Java, unit tested.
 */
final class TileFace {
    final boolean showBanner;
    final boolean showIcon;
    final boolean showLabel;

    private TileFace(boolean showBanner, boolean showIcon, boolean showLabel) {
        this.showBanner = showBanner;
        this.showIcon = showIcon;
        this.showLabel = showLabel;
    }

    /** An app with a banner shows only it; an app without one shows its icon with its name under it. */
    static TileFace of(boolean hasBanner) {
        return hasBanner ? new TileFace(true, false, false) : new TileFace(false, true, true);
    }

    /** A free dock slot shows nothing at all. */
    static TileFace empty() {
        return new TileFace(false, false, false);
    }

    /** The banner view must be cleared when it is not shown. */
    boolean clearBanner() {
        return !showBanner;
    }

    /** The icon view must be cleared when it is not shown. */
    boolean clearIcon() {
        return !showIcon;
    }
}
