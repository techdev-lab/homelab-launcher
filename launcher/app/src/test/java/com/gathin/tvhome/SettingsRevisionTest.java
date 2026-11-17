package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The settings revision (docs/CONFIG.md, "Settings that need Home rebuilt"): Home rebuilds in onResume when the
 * counter moved, because leaving the settings with the Home key loses the activity result. The first part tests the
 * decision logic; the second part reads the sources, because the wiring needs the Android framework.
 */
public class SettingsRevisionTest {

    // ---- the decision ----

    @Test
    public void sameRevisionDoesNotRebuild() {
        assertFalse(SettingsRevision.changed(0, 0));
        assertFalse(SettingsRevision.changed(7, 7));
    }

    @Test
    public void aNewerRevisionRebuilds() {
        assertTrue(SettingsRevision.changed(0, 1));
        assertTrue(SettingsRevision.changed(7, 8));
    }

    @Test
    public void anOlderRevisionRebuildsToo() {
        // The preferences were cleared or restored behind Home's back: show what is stored now, once.
        assertTrue(SettingsRevision.changed(7, 0));
    }

    @Test
    public void oneRebuildCannotLoop() {
        // The rebuilt Home reads the stored value in onCreate, so the next onResume compares equal.
        long stored = SettingsRevision.next(3);
        long seenByTheRebuiltHome = stored;
        assertFalse(SettingsRevision.changed(seenByTheRebuiltHome, stored));
    }

    @Test
    public void everyChangeMovesTheRevision() {
        long rev = 0;
        for (int i = 0; i < 5; i++) {
            long after = SettingsRevision.next(rev);
            assertTrue(SettingsRevision.changed(rev, after));
            rev = after;
        }
        assertEquals(5, rev);
    }

    // ---- missing, wrong type, damaged ----

    @Test
    public void aMissingValueIsZero() {
        assertEquals(0, SettingsRevision.parse(null));
    }

    @Test
    public void aWrongTypeIsZeroNotACrash() {
        assertEquals(0, SettingsRevision.parse("12"));
        assertEquals(0, SettingsRevision.parse(Boolean.TRUE));
        assertEquals(0, SettingsRevision.parse(1.5f));
        assertEquals(0, SettingsRevision.parse(new Object()));
    }

    @Test
    public void aNegativeValueIsZero() {
        assertEquals(0, SettingsRevision.parse(-5L));
        assertEquals(0, SettingsRevision.parse(-1));
    }

    @Test
    public void wholeNumbersAreKept() {
        assertEquals(42, SettingsRevision.parse(42L));
        assertEquals(42, SettingsRevision.parse(42));
    }

    @Test
    public void aDamagedValueStillGivesAUsableNextRevision() {
        // A string stored under the key reads as 0; the next bump writes 1, which differs from the 0 Home saw.
        long current = SettingsRevision.parse("junk");
        long after = SettingsRevision.next(current);
        assertTrue(SettingsRevision.changed(current, after));
    }

    @Test
    public void theEndOfTheRangeWrapsToOneNeverToZeroOrNegative() {
        assertEquals(1, SettingsRevision.next(Long.MAX_VALUE));
        assertEquals(1, SettingsRevision.next(-3));
    }

    // ---- the wiring, read from the sources (the failure cases first) ----

    private static final Pattern OK_RESULT = Pattern.compile("\\bsetResult\\(\\s*RESULT_OK\\s*\\)");
    private static final Pattern BUMP = Pattern.compile("\\bSettingsRevision\\.bump\\(");
    private static final Pattern DIRECT_RECREATE = Pattern.compile("(?<![.\\w])recreate\\(\\);");

    static int count(Pattern p, String source) {
        Matcher m = p.matcher(source);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** A screen that tells Home "something changed" must also bump the revision, once per such place at least. */
    static boolean everyOkResultBumps(String source) {
        return count(BUMP, source) >= count(OK_RESULT, source);
    }

    @Test
    public void aSettingsScreenThatSetsOkWithoutABumpIsCaught() {
        assertFalse(everyOkResultBumps("choices.edit().putInt(\"a\", 1).apply(); setResult(RESULT_OK);"));
    }

    @Test
    public void aSettingsScreenThatBumpsPasses() {
        assertTrue(everyOkResultBumps("SettingsRevision.bump(choices); setResult(RESULT_OK);"));
    }

    @Test
    public void aSecondDirectRecreateIsCaught() {
        assertEquals(2, count(DIRECT_RECREATE, "void a() { recreate(); } void b() { recreate(); }"));
        assertEquals(0, count(DIRECT_RECREATE, "void a() { recreateHome(); } // see recreate()"));
    }

    private static String source(String name) throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/gathin/tvhome/" + name)),
                StandardCharsets.UTF_8);
    }

    @Test
    public void everyScreenThatSavesASettingBumpsTheRevision() throws IOException {
        for (String name : new String[]{"LauncherSettingsActivity.java", "ClubsActivity.java", "HiddenAppsActivity.java"}) {
            String source = source(name);
            assertTrue(name + " sets RESULT_OK", count(OK_RESULT, source) > 0);
            assertTrue(name + " must call SettingsRevision.bump next to every setResult(RESULT_OK)",
                    everyOkResultBumps(source));
        }
    }

    @Test
    public void homeReadsTheRevisionWhenBuiltAndComparesItWhenResumed() throws IOException {
        String home = source("MainActivity.java");
        assertEquals("read once in onCreate", 1, count(Pattern.compile("settingsRevision = SettingsRevision\\.read\\("), home));
        assertEquals("compared once in onResume", 1, count(Pattern.compile("SettingsRevision\\.changed\\("), home));
    }

    @Test
    public void homeAsksForARebuildOnlyThroughTheOnceOnlyGuard() throws IOException {
        assertEquals("recreate() is called in one place, recreateHome()", 1,
                count(DIRECT_RECREATE, source("MainActivity.java")));
    }
}
