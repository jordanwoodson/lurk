package io.github.jordanwoodson.extension.shared.twitch;

import android.view.View;
import android.view.ViewGroup;

import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Preference fallbacks only: Twitch continues to read and write the user's own choices. */
@SuppressWarnings("unused")
public final class TwitchPlayback {
    private TwitchPlayback() {}

    public static String autoplayDefault(String original) {
        return Settings.isEnabled("disable_autoplay") ? "never" : original;
    }

    /** Keep the native recommendation's manual Play card, without the countdown controls. */
    public static boolean showManualRecommendation(View container) {
        if (!Settings.isEnabled("autoplay_next_video_off")) return false;
        hideCountdown(container);
        container.setVisibility(View.VISIBLE);
        return true;
    }

    private static void hideCountdown(View view) {
        if (view.getId() != View.NO_ID) {
            try {
                String name = view.getResources().getResourceEntryName(view.getId());
                if (name.equals("autoplay_progress") || name.equals("autoplay_time_remaining")
                        || name.equals("cancel_autoplay")) view.setVisibility(View.GONE);
            } catch (RuntimeException ignored) {
                // Unnamed wrapper views still have children worth inspecting.
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) hideCountdown(group.getChildAt(i));
        }
    }

    public static String qualityDefault(String original) {
        String quality = selectedQuality();
        return quality == null ? original : quality;
    }

    public static String qualityPreferenceDefault(String original, String suffix) {
        String quality = selectedQuality();
        if (quality == null) return original;
        // ClipQuality.QualitySource uses "source"; live/VOD IVS manifests use "chunked".
        return "_clip".equals(suffix) && "chunked".equals(quality) ? "source" : quality;
    }

    /** Called only for absent AsyncStorage rows, before the existing result/callback is completed. */
    public static String storageQualityDefault(String key) {
        if (!"preferred-video-quality".equals(key)) return null;
        String quality = selectedQuality();
        return quality == null ? null : "\"" + quality + "\"";
    }

    private static String selectedQuality() {
        switch (Settings.getInt("video_quality")) {
            case 1: return "chunked";
            case 108060: return "1080p60";
            case 1080: return "1080p";
            case 72060: return "720p60";
            case 720: return "720p";
            case 480: return "480p";
            case 360: return "360p";
            case 160: return "160p";
            default: return null;
        }
    }
}
