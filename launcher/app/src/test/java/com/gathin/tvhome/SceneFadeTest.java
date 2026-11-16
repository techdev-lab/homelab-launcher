package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SceneFadeTest {
    @Test
    public void aSmallMemoryTvNeverFadesWhateverTheMotionSettingsSay() {
        assertFalse(SceneFade.useFade(true, true, false));
        assertFalse(SceneFade.useFade(true, true, true));
        assertFalse(SceneFade.useFade(true, false, false));
    }

    @Test
    public void anyOtherTvFadesOnlyWithAnimationsOnAndMotionNotReduced() {
        assertTrue(SceneFade.useFade(false, true, false));
        assertFalse(SceneFade.useFade(false, false, false));
        assertFalse(SceneFade.useFade(false, true, true));
    }
}
