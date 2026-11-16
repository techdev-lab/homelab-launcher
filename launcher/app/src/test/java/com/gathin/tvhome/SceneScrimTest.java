package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SceneScrimTest {
    @Test
    public void autoIsOnOnlyForBrightScenes() {
        assertTrue(SceneScrim.drawn("auto", SceneList.of("golden").bright, "scenes"));
        assertTrue(SceneScrim.drawn("auto", SceneList.of("dawn").bright, "scenes"));
        assertFalse(SceneScrim.drawn("auto", SceneList.of("night").bright, "scenes"));
        assertFalse(SceneScrim.drawn("auto", SceneList.of("water").bright, "scenes"));
    }

    @Test
    public void onAndOffIgnoreTheBrightFlag() {
        assertTrue(SceneScrim.drawn("on", false, "scenes"));
        assertFalse(SceneScrim.drawn("off", true, "scenes"));
    }

    @Test
    public void anUnknownValueActsAsAuto() {
        assertTrue(SceneScrim.drawn("maybe", true, "scenes"));
        assertFalse(SceneScrim.drawn(null, false, "scenes"));
    }

    @Test
    public void neverDrawnOutsideTheScenesLook() {
        assertFalse(SceneScrim.drawn("on", true, "oled"));
        assertFalse(SceneScrim.drawn("on", true, "classic"));
        assertFalse(SceneScrim.drawn("on", true, null));
    }

    @Test
    public void theTvChoiceBeatsConfigWhichBeatsAuto() {
        assertEquals("on", SceneScrim.pick("on", "off"));
        assertEquals("off", SceneScrim.pick("", "off"));
        assertEquals("auto", SceneScrim.pick("loud", "bad"));
        assertEquals("auto", SceneScrim.pick(null, null));
    }
}
