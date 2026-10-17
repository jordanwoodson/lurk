package io.github.jordanwoodson.extension.shared.kick;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Default startup tab: a launcher start opens Browse or Following through Kick's own link handling. */
@SuppressWarnings("unused")
public final class KickStartup {
    private static final String KEY = "default_tab";

    private KickStartup() {
    }

    /**
     * Injection point: the start of MainActivity.onCreate(Bundle). A fresh start from the launcher (no saved
     * state, no link) becomes a kick.com link to the chosen screen, which Kick's link mapper turns into that
     * tab. Links, notifications and restored activities keep their own intent.
     */
    public static void rewriteLaunch(Activity activity, Bundle savedInstanceState) {
        if (savedInstanceState != null || !Settings.isRegistered(KEY)) return;
        String path;
        switch (Settings.getInt(KEY)) {
            case 1:
                path = "browse";
                break;
            case 2:
                path = "following";
                break;
            default:
                return;
        }
        Intent intent = activity.getIntent();
        if (intent == null || intent.getData() != null) return;
        String action = intent.getAction();
        boolean launcher = action == null ||
                (Intent.ACTION_MAIN.equals(action) && intent.hasCategory(Intent.CATEGORY_LAUNCHER));
        if (!launcher) return;
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://kick.com/" + path));
        Logger.d("startup tab: " + path);
    }
}
