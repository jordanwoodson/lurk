package io.github.jordanwoodson.extension.shared.settings;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;

/**
 * Remembers the app's resumed activity, so the settings screen opens on top of it, in the same task,
 * when a row inside the app is tapped.
 */
final class ResumedActivityTracker implements Application.ActivityLifecycleCallbacks {
    private static volatile WeakReference<Activity> resumed = new WeakReference<>(null);

    static Activity current() {
        Activity activity = resumed.get();
        return activity == null || activity.isFinishing() ? null : activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed = new WeakReference<>(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (resumed.get() == activity) resumed = new WeakReference<>(null);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
