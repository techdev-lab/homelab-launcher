package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SceneListTest {
    @Test
    public void theFixedOrderIsNightFogGoldenDawnWater() {
        assertEquals(Arrays.asList("night", "fog", "golden", "dawn", "water"), Arrays.asList(SceneList.ALL));
    }

    @Test
    public void enabledKeepsTheFixedOrderWhateverTheInputOrder() {
        assertEquals(Arrays.asList("night", "golden", "water"),
                SceneList.enabled(Arrays.asList("water", "golden", "night")));
    }

    @Test
    public void unknownRepeatedAndOddItemsAreDropped() {
        List<Object> in = Arrays.asList("fog", "FOG", " fog ", "sunset", 7, null, "", "dawn");
        assertEquals(Arrays.asList("fog", "dawn"), SceneList.enabled(in));
    }

    @Test
    public void anEmptyOrUnusableListGivesNightAlone() {
        assertEquals(Collections.singletonList("night"), SceneList.enabled(null));
        assertEquals(Collections.singletonList("night"), SceneList.enabled(Collections.emptyList()));
        assertEquals(Collections.singletonList("night"), SceneList.enabled(Arrays.asList("x", "y")));
    }

    @Test
    public void theTvChoiceIsACommaStringThatToleratesBlanksAndEmptyParts() {
        assertEquals(Arrays.asList("night", "dawn"), SceneList.fromCsv(" dawn , ,night,,nope"));
        assertEquals(Collections.singletonList("night"), SceneList.fromCsv(""));
        assertEquals(Collections.singletonList("night"), SceneList.fromCsv(null));
        assertEquals("night,golden", SceneList.toCsv(Arrays.asList("golden", "night")));
    }

    @Test
    public void aSceneThatIsOffIsReplacedByTheNearestOnInTheFixedOrder() {
        List<String> on = Arrays.asList("night", "dawn");
        assertEquals("dawn", SceneList.nearestEnabled(on, "water"));
        assertEquals("night", SceneList.nearestEnabled(on, "fog"));      // fog is 1 from night, 2 from dawn
        assertEquals("night", SceneList.nearestEnabled(Arrays.asList("night", "golden"), "fog"));   // tie: the earlier
        assertEquals("night", SceneList.nearestEnabled(Collections.emptyList(), "water"));
    }

    @Test
    public void everySceneHasDataAndOnlyWaterHasNoCutOut() {
        for (String id : SceneList.ALL) {
            SceneList.Scene s = SceneList.of(id);
            assertEquals(id, s.id);
            assertTrue(s.clockSize > 0 && s.clockTop >= 0);
            assertEquals("water".equals(id), !s.hasCutOut());
        }
        assertEquals(594, SceneList.of("water").waterLine);   // 55% of 1080
        assertEquals(0, SceneList.of("night").waterLine);
    }

    @Test
    public void anUnknownIdDrawsNightInsteadOfFailing() {
        assertEquals("night", SceneList.of("nope").id);
        assertEquals("night", SceneList.of(null).id);
        assertFalse(SceneList.known("Night"));
        assertTrue(SceneList.known("night"));
    }
}
