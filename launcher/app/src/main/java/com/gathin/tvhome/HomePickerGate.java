package com.gathin.tvhome;

/**
 * When the screen behind the system's Default home picker may close (docs/BATCH_F_FIXES.md, F5). On Android 14 the picker
 * is translucent, so HomeSettingsActivity stays up with a solid backdrop behind it and finishes once the picker is gone.
 * Our first resume comes right after we start the picker; the second comes when the picker closes (or Back is pressed).
 * If the picker could not be started there is nothing to wait for. Pure Java, unit tested.
 */
final class HomePickerGate {
    private boolean tried;
    private boolean started;
    private int resumes;

    /** Called once, after the attempt to open the picker. */
    void attempted(boolean pickerStarted) {
        tried = true;
        started = pickerStarted;
    }

    /** Called at every onResume; true when the activity should finish now. */
    boolean shouldFinishOnResume() {
        if (!tried) return false;
        if (!started) return true;
        resumes++;
        return resumes >= 2;
    }
}
