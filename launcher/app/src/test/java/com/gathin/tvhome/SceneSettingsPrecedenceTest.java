package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** The rule for the Scenes settings: the TV's choice, then config.json, then the default (docs/BATCH_S_SCENES.md). */
public class SceneSettingsPrecedenceTest {
    @Test
    public void scenesTvChoiceBeatsConfigWhichBeatsAll() {
        assertEquals(Arrays.asList("night", "dawn"), SceneList.pick("dawn,night", Arrays.asList("fog")));
        assertEquals(Arrays.asList("fog"), SceneList.pick(null, Arrays.asList("fog", "bogus")));
        assertEquals(Arrays.asList(SceneList.ALL), SceneList.pick(null, null));
    }

    @Test
    public void anEmptyOrBadChoiceStillLeavesOneScene() {
        assertEquals(Collections.singletonList("night"), SceneList.pick("", Arrays.asList("fog")));
        assertEquals(Collections.singletonList("night"), SceneList.pick(null, Arrays.asList("x")));
        assertEquals(Collections.singletonList("night"), SceneList.pick(null, Collections.emptyList()));
    }

    @Test
    public void followTheSunTvChoiceBeatsConfigInBothDirections() {
        assertTrue(SceneSun.follow(true, false));
        assertFalse(SceneSun.follow(false, true));
        assertTrue(SceneSun.follow(null, true));
        assertFalse(SceneSun.follow(null, null));
        assertFalse(SceneSun.follow(null, false));
    }

    @Test
    public void secondsTvChoiceBeatsConfigAndABadValueIsClamped() {
        assertEquals(10, SceneRotation.pick(10, 120));
        assertEquals(120, SceneRotation.pick(null, 120));
        assertEquals(600, SceneRotation.pick(null, 100000));
    }

    @Test
    public void lookAndScrimTvChoiceBeatsConfig() {
        assertEquals("scenes", HomeLook.pick("scenes", "oled"));
        assertEquals("oled", HomeLook.pick(null, "oled"));
        assertEquals("on", SceneScrim.pick("on", "off"));
        assertEquals("off", SceneScrim.pick(null, "off"));
    }
}
