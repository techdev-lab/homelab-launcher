package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettingsRouteTest {
    private static final List<String> PICTURE = Arrays.asList("vendor-picture", "standard-display");

    @Test
    public void theFirstResolvableCandidateWins() {
        assertEquals("vendor-picture", SettingsRoute.firstAvailable(PICTURE, c -> true));
    }

    @Test
    public void aMissingVendorScreenFallsThroughToTheStandardAction() {
        assertEquals("standard-display",
                SettingsRoute.firstAvailable(PICTURE, c -> c.equals("standard-display")));
    }

    @Test
    public void whenNothingResolvesTheAnswerIsNullNotAGuess() {
        assertNull(SettingsRoute.firstAvailable(PICTURE, c -> false));
    }

    @Test
    public void anEmptyListIsSafe() {
        assertNull(SettingsRoute.firstAvailable(Collections.<String>emptyList(), c -> true));
    }

    @Test
    public void candidatesAfterTheWinnerAreNeverAsked() {
        int[] asked = {0};
        SettingsRoute.firstAvailable(PICTURE, c -> {
            asked[0]++;
            return true;
        });
        assertEquals(1, asked[0]);
    }
}
