package io.github.jordanwoodson.extension.shared.kick;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;

import io.github.jordanwoodson.extension.shared.settings.Settings;

/** AMOLED black: the window behind Kick's screens, which shows during transitions and at the edges. */
@SuppressWarnings("unused")
public final class KickTheme {
    private static final String KEY = "amoled_black";

    private KickTheme() {
    }

    /** Injection point: MainActivity.onCreate(Bundle), right after the superclass call sets up the window. */
    public static void applyWindow(Activity activity) {
        if (Settings.isRegistered(KEY) && Settings.getInt(KEY) >= 1) {
            activity.getWindow().setBackgroundDrawable(new ColorDrawable(0xFF000000));
        }
    }
}
