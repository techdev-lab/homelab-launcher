package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class RamadanSettingsTest {

    private static Interests.Settings withPlace() {
        return new Interests.Settings("system", true, "Riyadh", "Riyadh", 24.7, 46.7, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    @Test
    public void byDefaultThereIsNoShiftTheAlertIsTenMinutesAndTheRamadanCardIsOn() {
        Interests.Settings s = withPlace();
        assertEquals(0, s.hijriShift);
        assertEquals(PrayerAlert.DEFAULT_MINUTES, s.prayerAlertMinutes);
        assertTrue(s.showRamadan);
    }

    @Test
    public void withCalendarCarriesTheThreeValuesAndLeavesTheOriginalAlone() {
        Interests.Settings base = withPlace();
        Interests.Settings s = base.withCalendar(-1, 0, false);
        assertEquals(-1, s.hijriShift);
        assertEquals(0, s.prayerAlertMinutes);
        assertFalse(s.showRamadan);
        assertEquals(0, base.hijriShift);
        assertTrue(base.showRamadan);
    }

    @Test
    public void laterWithMethodsKeepTheCalendarValues() {
        Interests.Settings s = withPlace().withCalendar(2, 15, false)
                .withCards(true, true, true, true).withPrayer("egypt", Prayer.ASR_HANAFI, new int[6]);
        assertEquals(2, s.hijriShift);
        assertEquals(15, s.prayerAlertMinutes);
        assertFalse(s.showRamadan);
    }

    @Test
    public void theRamadanCardNeedsThePrayerCardToBeAvailable() {
        assertTrue(withPlace().wantsRamadan());
        assertFalse("switched off", withPlace().withCalendar(0, 10, false).wantsRamadan());
        assertFalse("prayer card off", withPlace().withCards(false, true, true, true).wantsRamadan());
        assertFalse("no place", Interests.Settings.NONE.wantsRamadan());
    }
}
