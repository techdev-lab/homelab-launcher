package com.gathin.tvhome;

/**
 * The optional top scrim of the Scenes look (docs/BATCH_S_SCENES.md): a thin top-down gradient behind the header and the
 * first row of tiles, so a bright scene cannot hide the date. "auto" draws it only for scenes flagged bright in
 * {@link SceneList}. Pure Java, unit tested on the JVM.
 */
final class SceneScrim {
    static final String OFF = "off", AUTO = "auto", ON = "on";
    static final String[] ALL = {OFF, AUTO, ON};
    /** Height in picture pixels (of 1080) and the opacity at the very top, in percent. */
    static final int HEIGHT_PX = 300, TOP_ALPHA_PERCENT = 55;

    private SceneScrim() {
    }

    /** The TV's choice, else config.json "scene_scrim", else auto; anything unknown counts as unset. */
    static String pick(String chosen, String theme) {
        if (valid(chosen)) return chosen;
        return valid(theme) ? theme : AUTO;
    }

    static boolean valid(String s) {
        return OFF.equals(s) || AUTO.equals(s) || ON.equals(s);
    }

    /** Whether the scrim is drawn: never outside the Scenes look; "on" always; "auto" for bright scenes; unknown acts as auto. */
    static boolean drawn(String setting, boolean brightScene, String look) {
        if (!HomeLook.SCENES.equals(look)) return false;
        if (ON.equals(setting)) return true;
        if (OFF.equals(setting)) return false;
        return brightScene;
    }
}
