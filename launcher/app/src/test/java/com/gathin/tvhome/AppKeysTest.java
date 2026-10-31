package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class AppKeysTest {

    private static Map<String, Object> raw(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
        return m;
    }

    @Test
    public void withoutAnAppKeysSettingOnlyTheYouTubeButtonIsMapped() {
        Map<Integer, String> d = AppKeys.parse(null);
        assertEquals(1, d.size());
        assertEquals("com.google.android.youtube.tv", d.get(190));
        assertEquals(d, AppKeys.defaults());
    }

    @Test
    public void anEmptyObjectTurnsEveryMappingOff() {
        assertTrue(AppKeys.parse(new HashMap<>()).isEmpty());
    }

    @Test
    public void validAppsAndTheIgnoreWordAreKept() {
        Map<Integer, String> m = AppKeys.parse(raw("190", "ignore", "191", "com.example.app"));
        assertEquals(2, m.size());
        assertEquals(AppKeys.IGNORE, m.get(190));
        assertEquals("com.example.app", m.get(191));
        assertTrue(AppKeys.isIgnore(m.get(190)));
        assertFalse(AppKeys.isIgnore(m.get(191)));
    }

    @Test
    public void theIgnoreWordIgnoresCaseAndSpacesButNotAPackageThatStartsWithIt() {
        assertTrue(AppKeys.isIgnore("Ignore"));
        assertTrue(AppKeys.isIgnore("  IGNORE "));
        assertFalse(AppKeys.isIgnore("ignore.app"));
        assertFalse(AppKeys.isIgnore(null));
        assertEquals(AppKeys.IGNORE, AppKeys.parse(raw("190", "  Ignore ")).get(190));
        assertEquals("ignore.app", AppKeys.parse(raw("190", "ignore.app")).get(190));
    }

    @Test
    public void protectedKeysCanNeverBeMappedSoTheOwnerIsNeverLockedOut() {
        // Home, Back, the arrows, OK (centre and Enter), Power and the all-apps key some remotes use for Home.
        int[] protectedCodes = {3, 4, 19, 20, 21, 22, 23, 26, 66, 160, 284};
        for (int code : protectedCodes) assertTrue("key " + code, AppKeys.protectedKey(code));
        Map<String, Object> every = new LinkedHashMap<>();
        for (int code : protectedCodes) every.put(String.valueOf(code), "ignore");
        every.put("190", "com.example.app");
        Map<Integer, String> m = AppKeys.parse(every);
        assertEquals("only the unprotected entry is left", 1, m.size());
        assertEquals("com.example.app", m.get(190));
        assertFalse(AppKeys.protectedKey(190));
        assertFalse(AppKeys.protectedKey(24));   // volume up
    }

    @Test
    public void badEntriesAreDroppedAndTheRestStillWorks() {
        Map<String, Object> bad = raw(
                "abc", "com.example.app",            // not a key code
                "-5", "com.example.app",             // negative
                "0", "com.example.app",              // zero
                "99999999999", "com.example.app",    // does not fit an int
                "191", 123,                          // not a string
                "192", null,                         // null value
                "193", "xy",                         // too short for a package name
                "194", "has space.pkg",              // illegal character
                "195", "com.example.fine");
        Map<Integer, String> m = AppKeys.parse(bad);
        assertEquals(1, m.size());
        assertEquals("com.example.fine", m.get(195));
    }

    @Test
    public void aHugeObjectIsCut() {
        Map<String, Object> many = new LinkedHashMap<>();
        for (int i = 100; i < 400; i++) many.put(String.valueOf(i), "ignore");
        assertEquals(AppKeys.MAX_KEYS, AppKeys.parse(many).size());
    }

    @Test
    public void packageNamesNeedThreeToOneHundredFiftyAllowedCharacters() {
        assertTrue(AppKeys.validPackage("com.a.b"));
        assertFalse(AppKeys.validPackage("ab"));
        assertFalse(AppKeys.validPackage(null));
        assertFalse(AppKeys.validPackage("a/b.c"));
        assertFalse(AppKeys.validPackage(new String(new char[151]).replace('\0', 'a')));
        assertTrue(AppKeys.validPackage(new String(new char[150]).replace('\0', 'a')));
    }
}
