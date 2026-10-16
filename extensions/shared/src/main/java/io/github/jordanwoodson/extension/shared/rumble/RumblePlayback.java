package io.github.jordanwoodson.extension.shared.rumble;

import java.util.List;

import io.github.jordanwoodson.extension.shared.Network;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * Playback settings for Rumble: the quality a video starts at. With a setting off, each hook returns
 * Rumble's own value.
 */
@SuppressWarnings("unused")
public final class RumblePlayback {
    static final String QUALITY = "video_quality";
    static final String LIVE = "video_quality_live";
    static final String MOBILE = "video_quality_mobile";

    private RumblePlayback() {
    }

    // ---------------------------------------------------------------------------------------------
    // Default video quality

    /** The default quality's height, 0 for Rumble's default or when Default video quality isn't included. */
    static int wifiHeight() {
        return Settings.isRegistered(QUALITY) ? Settings.getInt(QUALITY) : 0;
    }

    /** The highest height to start at on mobile data, 0 for no cap. */
    static int capHeight() {
        return Settings.isRegistered(MOBILE) ? Settings.getInt(MOBILE) : 0;
    }

    /** Injection point: whether the mobile-data cap applies to the video starting now. */
    public static boolean capActive() {
        return capHeight() > 0 && Network.isMetered(Settings.appContext());
    }

    /**
     * Injection point: the default of Rumble's "live streams use Auto" preference, which is true. False
     * while Live streams too is on and a default quality is chosen, so live streams start at that quality.
     */
    public static boolean liveUsesAutoByDefault() {
        return !(Settings.isRegistered(LIVE) && Settings.isEnabled(LIVE) && wifiHeight() > 0);
    }

    /**
     * Injection point: the start of Rumble's start-source chooser, with its sources, the height and bitrate
     * last picked in the player (or nulls) and the server's preferred height. Returns the height to start
     * at. The same instance as {@code saved} keeps the picked height and bitrate; another value is used
     * with the highest bitrate; null leaves Rumble's preferred height.
     */
    public static Integer startHeight(List<?> sources, Integer saved, Integer savedBitrate, int preferred) {
        int cap = capActive() ? capHeight() : 0;
        if (cap <= 0) {
            if (saved != null) return saved;
            int wifi = wifiHeight();
            return wifi > 0 ? Integer.valueOf(wifi) : null;
        }
        int[] heights = new int[sources == null ? 0 : sources.size()];
        for (int i = 0; i < heights.length; i++) {
            try {
                heights[i] = sourceHeight(sources.get(i));
            } catch (RuntimeException e) {
                heights[i] = 0;
            }
        }
        int pick = cappedHeight(saved, preferred, wifiHeight(), cap, heights);
        if (saved != null && savedBitrate != null && saved == pick) return saved;
        return pick;
    }

    /**
     * The height to start at under a cap: what Rumble or the default quality would start at, lowered to the
     * highest available height at or below the cap. Rumble picks the nearest height by the difference of
     * reciprocals, which can land above the cap when no source matches; an available height can't.
     */
    static int cappedHeight(Integer saved, int preferred, int wifi, int cap, int[] heights) {
        int base;
        if (saved != null && saved > 0) base = saved;
        else if (saved == null && wifi > 0) base = wifi;
        else if (saved == null && preferred > 0) base = preferred;
        else base = cap; // Auto (height 0) on mobile data starts at the cap.
        int target = Math.min(base, cap);
        int best = 0;
        int lowest = Integer.MAX_VALUE;
        for (int height : heights) {
            if (height <= 0) continue;
            if (height <= target && height > best) best = height;
            if (height < lowest) lowest = height;
        }
        if (best > 0) return best;
        return lowest != Integer.MAX_VALUE ? lowest : target;
    }

    // ---------------------------------------------------------------------------------------------
    // Resume videos when signed out

    /**
     * Injection point: where a video starts when you aren't signed in, which is 0 in Rumble. Returns the
     * position Rumble kept on the phone (milliseconds) while the switch is on.
     */
    public static long signedOutPosition(long local) {
        return Settings.isEnabled("resume_signed_out") && local > 0 ? local : 0;
    }

    /** Replaced at patch time: a player video source's height (0 for Auto). */
    private static int sourceHeight(Object source) {
        return 0;
    }
}
