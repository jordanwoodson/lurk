package io.github.jordanwoodson.extension.shared.twitch;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * Player controls: sleep timer, refresh, VOD skip interval, brightness and volume swipes and normal
 * latency. Everything pauses, reloads and seeks through Twitch's own paths; with a setting off, its
 * hook returns Twitch's original value or does nothing.
 */
@SuppressWarnings("unused")
public final class TwitchPlayer {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private TwitchPlayer() {
    }

    // ---------------------------------------------------------------------------------------------
    // Sleep timer

    /** IVS players that were started and not paused or released since. */
    private static final Map<Object, Boolean> STARTED = Collections.synchronizedMap(new WeakHashMap<>());
    /** Twitch's own set of players to pause, the one it pauses before a Guest Star call. */
    private static volatile WeakReference<Set<?>> pauseRegistry = new WeakReference<>(null);
    private static final Runnable SLEEP = TwitchPlayer::sleep;
    private static final Runnable SLEEP_LEFTOVERS = TwitchPlayer::pauseLeftovers;

    /**
     * Injection point: the start of IVS CorePlayerImpl.play(), which every native, portal and React
     * Native start or resume reaches. (Re)arms the timer from now. May run on any thread.
     */
    public static void onPlay(Object player) {
        if (player == null) return;
        STARTED.put(player, Boolean.TRUE);
        MAIN.post(TwitchPlayer::armSleepTimer);
    }

    /** Injection point: the start of IVS CorePlayerImpl.pause() and release(). */
    public static void onStop(Object player) {
        if (player != null) STARTED.remove(player);
    }

    /** Injection point: Twitch adding a player to its pause-everything registry. */
    public static void pauseRegistry(Set<?> registry) {
        if (registry != null && pauseRegistry.get() != registry) pauseRegistry = new WeakReference<>(registry);
    }

    private static void armSleepTimer() {
        MAIN.removeCallbacks(SLEEP);
        int minutes = Settings.getInt("sleep_timer");
        if (minutes > 0) MAIN.postDelayed(SLEEP, minutes * 60_000L);
    }

    private static void sleep() {
        if (Settings.getInt("sleep_timer") <= 0) return;
        if (STARTED.isEmpty()) {
            Logger.d("sleep timer: nothing is playing");
            return;
        }
        int registered = 0;
        Set<?> registry = pauseRegistry.get();
        if (registry != null) {
            List<Object> players;
            synchronized (registry) {
                players = new ArrayList<>(registry);
            }
            for (Object player : players) {
                try {
                    pauseRegisteredPlayer(player);
                    registered++;
                } catch (Throwable t) {
                    Logger.d("sleep timer: " + t);
                }
            }
        }
        boolean session = callRn("com.twitchrn.player.TwitchMediaSessionModule", "pausePlayer", false);
        boolean feed = callRn("com.twitchrn.player.FeedPlayerRegistry", "pauseActiveFeedPlayer", true);
        Logger.d("sleep timer: paused " + registered + " native players, React Native session " + session
                + ", feed " + feed);
        toast("Sleep timer: playback paused");
        MAIN.removeCallbacks(SLEEP_LEFTOVERS);
        MAIN.postDelayed(SLEEP_LEFTOVERS, 3000);
    }

    /** A player none of Twitch's paths reached, such as a native feed preview: pause it directly. */
    private static void pauseLeftovers() {
        List<Object> players;
        synchronized (STARTED) {
            players = new ArrayList<>(STARTED.keySet());
        }
        for (Object player : players) {
            try {
                player.getClass().getMethod("pause").invoke(player);
                Logger.d("sleep timer: paused a player Twitch's paths missed");
            } catch (Throwable t) {
                Logger.d("sleep timer: " + t);
            }
        }
    }

    /** {@code TwitchMediaSessionModule.getInstance().pausePlayer()} or {@code FeedPlayerRegistry.INSTANCE...}. */
    private static boolean callRn(String className, String method, boolean instanceField) {
        try {
            Class<?> type = Class.forName(className, false, TwitchPlayer.class.getClassLoader());
            Object target = instanceField ? type.getField("INSTANCE").get(null)
                    : type.getMethod("getInstance").invoke(null);
            if (target == null) return false;
            type.getMethod(method).invoke(target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Replaced at patch time: Twitch's pause call on one player of its pause-everything registry. */
    private static void pauseRegisteredPlayer(Object player) {
    }

    // ---------------------------------------------------------------------------------------------

    private static Window window(View view) {
        Activity activity = activity(view.getContext());
        return activity == null ? null : activity.getWindow();
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static Context application() {
        try {
            return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void toast(String text) {
        MAIN.post(() -> {
            try {
                Toast.makeText(application(), text, Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Logger.d("toast: " + e);
            }
        });
    }
}
