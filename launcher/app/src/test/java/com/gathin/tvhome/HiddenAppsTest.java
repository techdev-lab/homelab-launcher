package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class HiddenAppsTest {

    @Test
    public void packageNamesAreCheckedAndTheLauncherItselfCanNeverBeHidden() {
        assertTrue(HiddenApps.valid("com.example.app"));
        assertFalse(HiddenApps.valid("ab"));
        assertFalse(HiddenApps.valid("has space.pkg"));
        assertFalse(HiddenApps.valid(null));
        assertFalse("hiding the launcher would leave no Home", HiddenApps.valid("com.gathin.tvhome"));
    }

    @Test
    public void parseKeepsTheOrderDropsBadNamesAndDuplicates() {
        assertEquals(Arrays.asList("com.a.one", "com.b.two"), HiddenApps.parse("com.a.one, com.b.two ,com.a.one"));
        assertEquals(Collections.emptyList(), HiddenApps.parse(""));
        assertEquals(Collections.emptyList(), HiddenApps.parse(null));
        assertEquals(Arrays.asList("com.fine.app"), HiddenApps.parse("x,, has space.pkg,com.gathin.tvhome,com.fine.app,"));
    }

    @Test
    public void aHugeStoredValueIsCut() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("com.example.app").append(i).append(',');
        assertEquals(HiddenApps.MAX, HiddenApps.parse(sb.toString()).size());
        // A value far larger than any real list is not even scanned to the end.
        StringBuilder junk = new StringBuilder();
        for (int i = 0; i < 100_000; i++) junk.append("com.junk.a,");
        assertEquals(1, HiddenApps.parse(junk.toString()).size());
    }

    @Test
    public void toggleHidesAnAppThenShowsItAgain() {
        List<String> none = new ArrayList<>();
        List<String> one = HiddenApps.toggle(none, "com.a.one");
        assertEquals(Arrays.asList("com.a.one"), one);
        assertEquals("the input list is not changed", 0, none.size());
        List<String> two = HiddenApps.toggle(one, "com.b.two");
        assertEquals(Arrays.asList("com.a.one", "com.b.two"), two);
        assertEquals(Arrays.asList("com.b.two"), HiddenApps.toggle(two, "com.a.one"));
    }

    @Test
    public void toggleIgnoresAnInvalidPackageAndStopsAtTheCap() {
        List<String> start = Arrays.asList("com.a.one");
        assertEquals(start, HiddenApps.toggle(start, "x"));
        assertEquals(start, HiddenApps.toggle(start, "com.gathin.tvhome"));
        List<String> full = new ArrayList<>();
        for (int i = 0; i < HiddenApps.MAX; i++) full.add("com.example.app" + i);
        assertEquals("a full list does not grow", HiddenApps.MAX, HiddenApps.toggle(full, "com.new.app").size());
        assertEquals("but an app can still be shown again", HiddenApps.MAX - 1, HiddenApps.toggle(full, "com.example.app3").size());
    }

    @Test
    public void csvRoundTrips() {
        List<String> l = Arrays.asList("com.a.one", "com.b.two");
        assertEquals("com.a.one,com.b.two", HiddenApps.toCsv(l));
        assertEquals(l, HiddenApps.parse(HiddenApps.toCsv(l)));
        assertEquals("", HiddenApps.toCsv(Collections.emptyList()));
    }

    @Test
    public void theTvChoiceWinsEvenWhenItIsEmptyAndResetGoesBackToTheTheme() {
        List<String> theme = Arrays.asList("com.theme.one", "bad name", "com.theme.two");
        assertEquals("no TV choice: the theme list, cleaned", Arrays.asList("com.theme.one", "com.theme.two"), HiddenApps.effective(null, theme));
        assertEquals("a TV choice replaces it", Arrays.asList("com.tv.pick"), HiddenApps.effective("com.tv.pick", theme));
        assertEquals("an empty TV choice means nothing is hidden", Collections.emptyList(), HiddenApps.effective("", theme));
        assertEquals(Collections.emptyList(), HiddenApps.effective(null, null));
    }
}
