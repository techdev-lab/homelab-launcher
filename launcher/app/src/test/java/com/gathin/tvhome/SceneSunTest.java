package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SceneSunTest {
    private static final List<String> ALL = Arrays.asList(SceneList.ALL);
    /** fajr, sunrise, dhuhr, asr, maghrib, isha for a made-up day. */
    private static final double[] T = {4.5, 6.0, 12.0, 15.5, 18.0, 19.5};

    @Test
    public void withAPlaceTheWindowsFollowTheSun() {
        assertEquals("night", SceneSun.pick(T, 3.0, ALL));
        assertEquals("dawn", SceneSun.pick(T, 5.0, ALL));
        assertEquals("dawn", SceneSun.pick(T, 6.5, ALL));
        assertEquals("water", SceneSun.pick(T, 10.0, ALL));
        assertEquals("golden", SceneSun.pick(T, 16.0, ALL));
        assertEquals("golden", SceneSun.pick(T, 18.2, ALL));
        assertEquals("night", SceneSun.pick(T, 21.0, ALL));
    }

    @Test
    public void atAnExactEdgeTheLaterWindowWins() {
        assertEquals("dawn", SceneSun.pick(T, 4.5, ALL));      // fajr
        assertEquals("water", SceneSun.pick(T, 6.75, ALL));    // sunrise + 45 min
        assertEquals("golden", SceneSun.pick(T, 15.5, ALL));   // asr
        assertEquals("night", SceneSun.pick(T, 18.25, ALL));   // maghrib + 15 min
    }

    @Test
    public void withoutAPlaceTheClockRuleIsUsed() {
        assertEquals("dawn", SceneSun.pick((double[]) null, 6.0, ALL));
        assertEquals("water", SceneSun.pick((double[]) null, 12.0, ALL));
        assertEquals("golden", SceneSun.pick((double[]) null, 17.0, ALL));
        assertEquals("night", SceneSun.pick((double[]) null, 23.0, ALL));
        assertEquals("night", SceneSun.byClock(4.99));
        assertEquals("dawn", SceneSun.byClock(5.0));
        assertEquals("water", SceneSun.byClock(8.0));
    }

    @Test
    public void badPlacesAndBadTimesFallBackToTheClockAndNeverThrow() {
        assertEquals("water", SceneSun.pick(Double.NaN, 46.0, 2026, 6, 21, 3, 12.0, ALL));
        assertEquals("water", SceneSun.pick(0, 0, 2026, 6, 21, 3, 12.0, ALL));
        assertEquals("water", SceneSun.pick(80, 20, 2026, 6, 21, 3, 12.0, ALL));   // above the usable latitude
        assertEquals("water", SceneSun.pick(new double[]{Double.NaN, 6, 12, 15, 18, 19}, 12.0, ALL));
        assertEquals("water", SceneSun.pick(new double[]{1, 2}, 12.0, ALL));
    }

    @Test
    public void aRealPlaceGivesNightAtNightAndDayAtNoon() {
        assertEquals("night", SceneSun.pick(24.7, 46.7, 2026, 6, 21, 3, 1.0, ALL));
        assertEquals("water", SceneSun.pick(24.7, 46.7, 2026, 6, 21, 3, 11.0, ALL));
        assertEquals("golden", SceneSun.pick(24.7, 46.7, 2026, 6, 21, 3, 17.0, ALL));
    }

    @Test
    public void aSceneThatIsOffIsReplaced() {
        List<String> noWater = Arrays.asList("night", "fog", "golden", "dawn");
        assertEquals("fog", SceneSun.pick(T, 10.0, noWater));                       // water off: fog by day
        assertEquals("night", SceneSun.pick(T, 10.0, Collections.singletonList("night")));   // only night: always night
        assertEquals("night", SceneSun.pick(T, 16.0, Collections.singletonList("night")));
        assertEquals("night", SceneSun.pick(T, 10.0, Collections.emptyList()));     // empty list acts as night alone
        assertEquals("golden", SceneSun.pick(T, 10.0, Arrays.asList("golden", "dawn")));   // water and fog off: golden is nearer fog
    }
}
