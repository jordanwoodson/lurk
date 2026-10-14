package io.github.jordanwoodson.extension.shared.twitch;

import android.content.Context;

import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Local presentation decisions; never writes or submits the user's consent. */
@SuppressWarnings("unused")
public final class TwitchPrivacy {
    private static boolean initialized;

    private TwitchPrivacy() { }

    /** Providers run before Application.onCreate; reuse one early preference initialization. */
    public static synchronized void initSettingsOnce(Context context) {
        if (!initialized && context != null) {
            Context application = context.getApplicationContext();
            Settings.init(application != null ? application : context);
            initialized = true;
        }
    }

    public static boolean showCookieNotice(boolean requested) {
        return requested && !Settings.isEnabled("block_cookie_notice");
    }

    public static boolean allowAnalyticsCollection(boolean requested) {
        return requested && !Settings.isEnabled("disable_analytics");
    }

    /** Null restores the SDK default; suppression must override that request too. */
    public static Boolean allowAnalyticsCollectionBoxed(Boolean requested) {
        return Settings.isEnabled("disable_analytics") ? Boolean.FALSE : requested;
    }

    public static boolean disableBranchTracking(boolean requested) {
        return requested || Settings.isEnabled("disable_analytics");
    }
}
