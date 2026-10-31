package com.gathin.tvhome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The second press that crosses a section of the glass settings (docs/SETTINGS_GLASS.md). */
public class EdgeBumpTest {

    @Test
    public void theFirstPressArmsAndTheSecondOfTheSameKeyPasses() {
        EdgeBump bump = new EdgeBump(1500);
        assertFalse(bump.pass(SettingsNav.RIGHT, 1000));
        assertTrue(bump.pass(SettingsNav.RIGHT, 1400));
    }

    @Test
    public void aPassUsesTheArmUp() {
        EdgeBump bump = new EdgeBump(1500);
        bump.pass(SettingsNav.RIGHT, 0);
        assertTrue(bump.pass(SettingsNav.RIGHT, 100));
        assertFalse("a third press arms again", bump.pass(SettingsNav.RIGHT, 200));
    }

    @Test
    public void aSecondPressAfterTheWindowOnlyArmsAgain() {
        EdgeBump bump = new EdgeBump(1500);
        assertFalse(bump.pass(SettingsNav.LEFT, 0));
        assertFalse("too late", bump.pass(SettingsNav.LEFT, 1501));
        assertTrue("but that late press armed it", bump.pass(SettingsNav.LEFT, 1600));
    }

    @Test
    public void theOtherDirectionDoesNotCountAndReArms() {
        EdgeBump bump = new EdgeBump(1500);
        assertFalse(bump.pass(SettingsNav.RIGHT, 0));
        assertFalse(bump.pass(SettingsNav.LEFT, 100));
        assertTrue(bump.pass(SettingsNav.LEFT, 200));
    }

    @Test
    public void resetDisarms() {
        EdgeBump bump = new EdgeBump(1500);
        bump.pass(SettingsNav.RIGHT, 0);
        bump.reset();
        assertFalse(bump.pass(SettingsNav.RIGHT, 100));
    }

    @Test
    public void exactlyTheWindowStillCountsAndAClockThatWentBackDoesNot() {
        EdgeBump bump = new EdgeBump(1500);
        bump.pass(SettingsNav.RIGHT, 1000);
        assertTrue(bump.pass(SettingsNav.RIGHT, 2500));
        bump.pass(SettingsNav.RIGHT, 5000);
        assertFalse("time went backwards", bump.pass(SettingsNav.RIGHT, 4000));
    }

    @Test
    public void aFreshOneDoesNotPassOnItsFirstPressEvenAtTimeZero() {
        assertFalse(new EdgeBump(1500).pass(SettingsNav.RIGHT, 0));
    }
}
