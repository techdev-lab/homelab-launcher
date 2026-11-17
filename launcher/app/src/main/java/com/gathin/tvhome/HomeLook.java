package com.gathin.tvhome;

/**
 * The look of the whole home screen, picked in Launcher settings or as "home_look" in config.json
 * (docs/BATCH_S_SCENES.md): "classic" is the home as it has always been (the wallpaper mode decides the picture),
 * "scenes" draws the Scenes look, "oled" is plain black. Classic is the default. Pure Java, unit tested on the JVM.
 */
final class HomeLook {
    static final String CLASSIC = "classic", SCENES = "scenes", OLED = "oled";
    static final String[] ALL = {CLASSIC, SCENES, OLED};
    /** Pure black, so an OLED panel switches its pixels off. */
    static final int OLED_BACKGROUND = 0xFF000000;

    private HomeLook() {
    }

    /** The TV's choice, else config.json "home_look", else classic; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : CLASSIC;
    }

    static boolean valid(String s) {
        return CLASSIC.equals(s) || SCENES.equals(s) || OLED.equals(s);
    }

    /** Whether a wallpaper picture and its dark veil are used: only in classic. */
    static boolean usesWallpaper(String look) {
        return !SCENES.equals(look) && !OLED.equals(look);
    }

    /** Whether the time-of-day sky and the ambient layer may show: only in classic. */
    static boolean usesSky(String look) {
        return usesWallpaper(look);
    }

    /**
     * Whether the strip cards (prayer, football, weather, news) are drawn. Not in Scenes: the strip fills the height the
     * big clock uses, so the cards would cover the digits (docs/BATCH_S_SCENES.md, section 13).
     */
    static boolean showsCards(String look) {
        return !SCENES.equals(look);
    }

    /** Whether the header pills (Wi-Fi, Settings, Inputs) have a glass fill and edge at rest. Not in Scenes. */
    static boolean framesHeaderPills(String look) {
        return !SCENES.equals(look);
    }

    /** Whether the small header clock is replaced by the big scene clock. */
    static boolean replacesHeaderClock(String look) {
        return SCENES.equals(look);
    }
}
