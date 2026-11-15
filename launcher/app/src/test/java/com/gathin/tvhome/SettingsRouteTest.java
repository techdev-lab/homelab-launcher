package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettingsRouteTest {
    private static final List<String> PICTURE = Arrays.asList("vendor-picture", "standard-display");

    @Test
    public void aScreenThatResolvesButIsNotExportedIsNotUsable() {
        assertFalse(SettingsRoute.usable(true, false));
        assertFalse(SettingsRoute.usable(false, true));
        assertFalse(SettingsRoute.usable(false, false));
        assertTrue(SettingsRoute.usable(true, true));
    }

    @Test
    public void aRefusedFirstScreenFallsBackToTheStandardActionOnce() {
        List<String> tried = new ArrayList<>();
        boolean started = SettingsRoute.startInOrder(PICTURE, c -> {
            tried.add(c);
            return c.equals("standard-display");   // the vendor screen refuses to start
        });
        assertTrue(started);
        assertEquals(PICTURE, tried);
    }

    @Test
    public void aScreenThatStartsIsNotFollowedByTheNext() {
        List<String> tried = new ArrayList<>();
        assertTrue(SettingsRoute.startInOrder(PICTURE, c -> {
            tried.add(c);
            return true;
        }));
        assertEquals(Collections.singletonList("vendor-picture"), tried);
    }

    @Test
    public void whenEveryScreenRefusesNothingStartsAndTheCallerShowsTheToast() {
        assertFalse(SettingsRoute.startInOrder(PICTURE, c -> false));
        assertFalse(SettingsRoute.startInOrder(Collections.<String>emptyList(), c -> true));
    }
}
