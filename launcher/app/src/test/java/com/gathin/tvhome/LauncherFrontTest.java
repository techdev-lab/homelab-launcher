package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LauncherFrontTest {
    @Test
    public void aFreshProcessHasNoLauncherScreenInFront() {
        assertFalse(new LauncherFront().inFront());
    }

    @Test
    public void aStartedScreenIsInFrontUntilItStops() {
        LauncherFront front = new LauncherFront();
        front.started();
        assertTrue(front.inFront());
        front.stopped();
        assertFalse(front.inFront());
    }

    @Test
    public void movingBetweenLauncherScreensNeverLeavesTheFront() {
        LauncherFront front = new LauncherFront();
        front.started();                 // home
        front.started();                 // settings starts before home stops
        assertTrue(front.inFront());
        front.stopped();                 // home stops
        assertTrue("settings is still showing", front.inFront());
        front.stopped();                 // settings stops: another app took over
        assertFalse(front.inFront());
    }

    @Test
    public void aStopWithoutAStartCannotLeaveTheCounterBelowZero() {
        LauncherFront front = new LauncherFront();
        front.stopped();
        front.stopped();
        assertFalse(front.inFront());
        front.started();
        assertTrue("a screen that starts after stray stops is still seen", front.inFront());
        front.stopped();
        assertFalse(front.inFront());
    }

    @Test
    public void everyStartNeedsItsOwnStop() {
        LauncherFront front = new LauncherFront();
        front.started();
        front.started();
        front.stopped();
        assertTrue(front.inFront());
    }
}
