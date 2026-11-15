package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A guard for the memory leak of batch F (docs/BATCH_F_FIXES.md, F1). The leak needs the Android framework, so a JVM
 * test cannot see it; this reads the source of MainActivity instead and fails if the two patterns that caused it come
 * back: a focus listener added to the window with no matching removal, and a system service taken from the Activity.
 */
public class HomeLifecycleSourceTest {

    private static final Pattern ADDED = Pattern.compile("\\.addOnGlobalFocusChangeListener\\(");
    private static final Pattern REMOVED = Pattern.compile("\\.removeOnGlobalFocusChangeListener\\(");
    /** getSystemService(...) with no getApplicationContext() or other receiver in front of it. */
    private static final Pattern ACTIVITY_SERVICE = Pattern.compile("(?<![.\\w])getSystemService\\(\\s*"
            + "(ConnectivityManager\\.class|Context\\.CONNECTIVITY_SERVICE|CONNECTIVITY_SERVICE"
            + "|TV_INPUT_SERVICE|Context\\.TV_INPUT_SERVICE)");

    static int count(Pattern p, String source) {
        Matcher m = p.matcher(source);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    static boolean everyFocusListenerIsRemoved(String source) {
        return count(ADDED, source) <= count(REMOVED, source);
    }

    static int servicesTakenFromTheActivity(String source) {
        return count(ACTIVITY_SERVICE, source);
    }

    private static String mainActivity() throws IOException {
        Path p = Paths.get("src/main/java/com/gathin/tvhome/MainActivity.java");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    // ---- the checker itself, on small samples (the failure cases) ----

    @Test
    public void aFocusListenerWithNoRemovalIsCaught() {
        String leaky = "decor.getViewTreeObserver().addOnGlobalFocusChangeListener(l);";
        assertFalse(everyFocusListenerIsRemoved(leaky));
    }

    @Test
    public void aRemovedFocusListenerPasses() {
        String tidy = "o.addOnGlobalFocusChangeListener(l); o.removeOnGlobalFocusChangeListener(l);";
        assertTrue(everyFocusListenerIsRemoved(tidy));
    }

    @Test
    public void aServiceTakenFromTheActivityIsCaught() {
        assertEquals(1, servicesTakenFromTheActivity("cm = getSystemService(ConnectivityManager.class);"));
        assertEquals(1, servicesTakenFromTheActivity("t = (TvInputManager) getSystemService(TV_INPUT_SERVICE);"));
        assertEquals(1, servicesTakenFromTheActivity("c = getSystemService(Context.CONNECTIVITY_SERVICE);"));
    }

    @Test
    public void aServiceTakenFromTheApplicationContextPasses() {
        assertEquals(0, servicesTakenFromTheActivity(
                "cm = getApplicationContext().getSystemService(ConnectivityManager.class);"));
        assertEquals(0, servicesTakenFromTheActivity(
                "t = (TvInputManager) getApplicationContext().getSystemService(TV_INPUT_SERVICE);"));
    }

    // ---- the real file ----

    @Test
    public void homeRemovesEveryFocusListenerItAdds() throws IOException {
        String source = mainActivity();
        assertTrue("MainActivity adds a focus listener", count(ADDED, source) > 0);
        assertTrue("MainActivity must remove each global focus listener it adds (onDestroy)",
                everyFocusListenerIsRemoved(source));
    }

    @Test
    public void homeTakesConnectivityAndTvInputsFromTheApplicationContext() throws IOException {
        assertEquals("ConnectivityManager and TvInputManager must not come from the Activity", 0,
                servicesTakenFromTheActivity(mainActivity()));
    }
}
