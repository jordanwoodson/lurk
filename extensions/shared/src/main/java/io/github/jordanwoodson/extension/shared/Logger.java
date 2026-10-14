package io.github.jordanwoodson.extension.shared;

import android.util.Log;

/**
 * Logging for patched apps. Call it from injected smali to confirm a hook fires:
 * invoke-static {v0}, Lio/github/jordanwoodson/extension/shared/Logger;->d(Ljava/lang/String;)V
 * Read it with: adb logcat -s lurk
 */
@SuppressWarnings("unused")
public final class Logger {
    public static final String TAG = "lurk";

    private Logger() {
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    /** Records that a patch stopped something, e.g. a tracker call or an analytics request. */
    public static void blocked(String what) {
        Log.i(TAG, "blocked: " + what);
    }
}
