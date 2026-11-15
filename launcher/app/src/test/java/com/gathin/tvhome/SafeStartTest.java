package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SafeStartTest {

    private final List<String> reports = new ArrayList<>();
    private final SafeStart safe = new SafeStart((what, why) -> reports.add(what));

    private static SafeStart.Action refusing(RuntimeException e) {
        return () -> {
            throw e;
        };
    }

    @Test
    public void aStartThatGoesThroughIsTrueAndNotReported() {
        assertTrue(safe.attempt("home", () -> { }));
        assertTrue(reports.isEmpty());
    }

    @Test
    public void aSecurityExceptionDoesNotEscapeAndIsReported() {
        assertFalse(safe.attempt("settings", refusing(new SecurityException("not allowed"))));
        assertEquals(1, reports.size());
        assertEquals("settings", reports.get(0));
    }

    @Test
    public void aMissingActivityDoesNotEscapeEither() {
        // ActivityNotFoundException is built through the Android stub jar, which may refuse to run its constructor.
        RuntimeException missing;
        try {
            missing = new android.content.ActivityNotFoundException("no such app");
        } catch (RuntimeException stubbed) {
            return;   // not constructible in a plain JVM; SecurityException above covers the same code path
        }
        assertFalse(safe.attempt("app key", refusing(missing)));
        assertEquals(1, reports.size());
    }

    @Test
    public void theSameTargetIsReportedOnlyOnce() {
        for (int i = 0; i < 3; i++) {
            assertFalse(safe.attempt("app key", refusing(new SecurityException())));
        }
        assertEquals(1, reports.size());
    }

    @Test
    public void aDifferentTargetIsReportedAgain() {
        safe.attempt("settings", refusing(new SecurityException()));
        safe.attempt("app key", refusing(new SecurityException()));
        assertEquals(2, reports.size());
    }

    @Test
    public void anyOtherExceptionStillPropagates() {
        try {
            safe.attempt("home", refusing(new IllegalStateException("a real bug")));
            fail("an unrelated exception must not be swallowed");
        } catch (IllegalStateException expected) {
            assertTrue(reports.isEmpty());
        }
    }

    @Test
    public void aTargetThatFailedCanStillStartLater() {
        assertFalse(safe.attempt("app key", refusing(new SecurityException())));
        assertTrue(safe.attempt("app key", () -> { }));
    }
}
