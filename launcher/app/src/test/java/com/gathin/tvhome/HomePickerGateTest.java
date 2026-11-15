package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomePickerGateTest {
    @Test
    public void aPickerThatCouldNotStartFinishesAtOnce() {
        HomePickerGate gate = new HomePickerGate();
        gate.attempted(false);
        assertTrue(gate.shouldFinishOnResume());
    }

    @Test
    public void theFirstResumeKeepsTheBackdropUnderThePicker() {
        HomePickerGate gate = new HomePickerGate();
        gate.attempted(true);
        assertFalse(gate.shouldFinishOnResume());
    }

    @Test
    public void theSecondResumeMeansThePickerClosed() {
        HomePickerGate gate = new HomePickerGate();
        gate.attempted(true);
        assertFalse(gate.shouldFinishOnResume());
        assertTrue(gate.shouldFinishOnResume());
    }

    @Test
    public void aResumeBeforeTheAttemptNeverFinishes() {
        HomePickerGate gate = new HomePickerGate();
        assertFalse(gate.shouldFinishOnResume());
        assertFalse(gate.shouldFinishOnResume());
        gate.attempted(true);
        assertFalse("the first resume after the attempt still keeps it", gate.shouldFinishOnResume());
    }
}
