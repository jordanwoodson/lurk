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
    // Refresh stream

    private static final class Attached {
        final WeakReference<View> view;
        final long order;

        Attached(View view, long order) {
            this.view = new WeakReference<>(view);
            this.order = order;
        }
    }

    /** Native player presenters and the player view each one last attached. Main thread. */
    private static final Map<Object, Attached> PLAYER_VIEWS = new WeakHashMap<>();
    private static long attachOrder;

    /** Injection point: a native player presenter attaching its player view. */
    public static void playerView(Object presenter, View view) {
        if (presenter == null || view == null) return;
        synchronized (PLAYER_VIEWS) {
            PLAYER_VIEWS.put(presenter, new Attached(view, ++attachOrder));
        }
    }

    /** Injection point: the classic player's settings gear, once it is found. Long-press reloads. */
    public static void bindRefresh(View gear) {
        if (gear == null) return;
        gear.setOnLongClickListener(view -> {
            if (!Settings.isEnabled("refresh_stream")) return false;
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            Object presenter = presenterAt(view, location[0] + view.getWidth() / 2f,
                    location[1] + view.getHeight() / 2f, true);
            if (presenter == null) {
                Logger.d("refresh: no player for this gear");
                return false;
            }
            refresh(presenter, view);
            return true;
        });
    }

    /**
     * Injection point: the native player's long press, which Twitch emits but ignores. Reloads when it
     * is in the middle of the video, away from the edges where hands rest.
     */
    public static void videoLongPress(MotionEvent event) {
        try {
            if (event == null || !Settings.isEnabled("refresh_stream") || !Settings.isEnabled("refresh_stream_video")) {
                return;
            }
            float x = event.getRawX();
            float y = event.getRawY();
            Object presenter = presenterAt(null, x, y, false);
            if (presenter == null) return;
            View view = viewOf(presenter);
            if (view == null) return;
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            float relative = (x - location[0]) / Math.max(1, view.getWidth());
            if (relative < 0.2f || relative > 0.8f) return;
            refresh(presenter, view);
        } catch (Throwable t) {
            Logger.d("refresh: " + t);
        }
    }

    private static View viewOf(Object presenter) {
        synchronized (PLAYER_VIEWS) {
            Attached attached = PLAYER_VIEWS.get(presenter);
            return attached == null ? null : attached.view.get();
        }
    }

    /**
     * The presenter whose shown player view contains the screen point, most recently attached first.
     * With {@code sameWindow}, one in the anchor's window that doesn't contain it is the fallback.
     */
    private static Object presenterAt(View anchor, float x, float y, boolean sameWindow) {
        Object best = null;
        Object fallback = null;
        long bestOrder = -1;
        long fallbackOrder = -1;
        int[] location = new int[2];
        synchronized (PLAYER_VIEWS) {
            for (Map.Entry<Object, Attached> entry : PLAYER_VIEWS.entrySet()) {
                View view = entry.getValue().view.get();
                long order = entry.getValue().order;
                if (view == null || !view.isShown() || view.getWidth() == 0) continue;
                if (anchor != null && view.getRootView() != anchor.getRootView()) continue;
                view.getLocationOnScreen(location);
                boolean contains = x >= location[0] && x < location[0] + view.getWidth()
                        && y >= location[1] && y < location[1] + view.getHeight();
                if (contains && order > bestOrder) {
                    best = entry.getKey();
                    bestOrder = order;
                } else if (!contains && sameWindow && order > fallbackOrder) {
                    fallback = entry.getKey();
                    fallbackOrder = order;
                }
            }
        }
        return best != null ? best : fallback;
    }

    private static void refresh(Object presenter, View feedback) {
        try {
            // Exactly what the error screen's Retry does: a new access token and playlist (live), or the
            // same position with a new token (VOD).
            retryPlayer(presenter, 0, true);
            feedback.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            toast("Refreshing stream");
            Logger.d("refresh: " + presenter.getClass().getName());
        } catch (Throwable t) {
            Logger.d("refresh: " + t);
        }
    }

    /** Replaced at patch time: the player presenter's Retry, called as Twitch's error screen calls it. */
    private static void retryPlayer(Object presenter, Integer position, boolean newToken) {
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
