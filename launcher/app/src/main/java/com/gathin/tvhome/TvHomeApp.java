package com.gathin.tvhome;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Counts this launcher's started screens from the first one on, for {@link HomeKeyService} (KeyFilter "home"). It lives
 * in the application so no screen is missed: the count starts with the process, and a screen cannot start before it.
 */
public class TvHomeApp extends Application {
    /** Touched only on the main thread: lifecycle callbacks and the key service both run there. */
    static final LauncherFront FRONT = new LauncherFront();

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                FRONT.started();
                HomeKeyService.frontChanged();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                FRONT.stopped();
                HomeKeyService.frontChanged();
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
