package io.github.jordanwoodson.extension.shared.rumble;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.view.View;

import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Player view settings for Rumble. With a setting off, each hook returns Rumble's own value. */
@SuppressWarnings("unused")
public final class RumblePlayer {
    static final String FILL_SCREEN = "fill_screen";
    static final String KEEP_SCREEN_ON = "keep_screen_on_paused";

    /** Media3's AspectRatioFrameLayout.RESIZE_MODE_FIT and RESIZE_MODE_ZOOM. */
    private static final int FIT = 0;
    private static final int ZOOM = 4;

    private RumblePlayer() {
    }

    // ---------------------------------------------------------------------------------------------
    // Fill screen

    /**
     * Injection point: the resize mode the video frame measures with, which Rumble always sets to FIT. In
     * landscape, outside picture-in-picture, Fill screen makes it ZOOM: the video fills the screen and the
     * edges are cropped.
     */
    public static int resizeMode(View view, int mode) {
        if (mode != FIT || view == null || !Settings.isEnabled(FILL_SCREEN)) return mode;
        try {
            Configuration configuration = view.getResources().getConfiguration();
            if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return mode;
            Activity activity = activity(view.getContext());
            if (activity != null && activity.isInPictureInPictureMode()) return mode;
            return ZOOM;
        } catch (RuntimeException e) {
            return mode;
        }
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Keep screen on while paused

    /**
     * Injection point: whether the player keeps the screen on, which Rumble decides from the player state
     * (off while paused or finished), with that state. Keeps it on while paused too.
     */
    public static boolean keepScreenOn(boolean keep, Object state) {
        if (keep || state == null || !Settings.isEnabled(KEEP_SCREEN_ON)) return keep;
        try {
            return state.toString().startsWith("Paused(");
        } catch (RuntimeException e) {
            return keep;
        }
    }
}
