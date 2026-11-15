package com.gathin.tvhome;

import android.content.ActivityNotFoundException;

import java.util.HashSet;
import java.util.Set;

/**
 * Starts a screen from the key services without letting a refusal stop them (docs/BATCH_F_FIXES.md, F2). When the app a
 * remote button points to is gone, or the system will not let a background service start it, startActivity throws; out of
 * onKeyEvent that can stop the accessibility service and with it every remote button. Only those two failures are caught;
 * anything else is a bug and still propagates. Each target is reported once, so a button pressed again does not fill the
 * log. Pure Java apart from the exception type, unit tested.
 */
final class SafeStart {

    /** One start of one screen. */
    interface Action {
        void run();
    }

    /** Where a failure is reported once; {@code what} names the target, {@code why} is the exception. */
    interface Sink {
        void warn(String what, RuntimeException why);
    }

    private final Sink sink;
    private final Set<String> reported = new HashSet<>();

    SafeStart(Sink sink) {
        this.sink = sink;
    }

    /** Runs the start and returns whether it went through. */
    boolean attempt(String what, Action start) {
        try {
            start.run();
            return true;
        } catch (ActivityNotFoundException | SecurityException refused) {
            if (reported.add(what)) sink.warn(what, refused);
            return false;
        }
    }
}
