package io.github.jordanwoodson.extension.shared;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * Sleep timer, shared by the apps: pauses playback the chosen number of minutes (the {@code sleep_timer}
 * choice) after it last started or resumed. The app reports its players starting and stopping and supplies
 * the pause action.
 */
public final class SleepTimer {
    public static final String KEY = "sleep_timer";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    /** Players that were started and not paused or released since. */
    private static final Map<Object, Boolean> STARTED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Runnable SLEEP = SleepTimer::sleep;
    private static volatile Runnable pauseAction;

    private SleepTimer() {
    }

    /** The app's way to pause everything that plays. Called on the main thread when the timer runs out. */
    public static void setPauseAction(Runnable action) {
        pauseAction = action;
    }

    /** A player started or resumed: (re)arms the timer from now. May run on any thread. */
    public static void onPlay(Object player) {
        if (player == null) return;
        STARTED.put(player, Boolean.TRUE);
        MAIN.post(SleepTimer::arm);
    }

    /** A player paused, stopped or was released. */
    public static void onStop(Object player) {
        if (player != null) STARTED.remove(player);
    }

    /** The players started and not stopped since, for pause actions that pause them directly. */
    public static List<Object> started() {
        synchronized (STARTED) {
            return new ArrayList<>(STARTED.keySet());
        }
    }

    private static void arm() {
        MAIN.removeCallbacks(SLEEP);
        int minutes = Settings.getInt(KEY);
        if (minutes > 0) MAIN.postDelayed(SLEEP, minutes * 60_000L);
    }

    private static void sleep() {
        if (Settings.getInt(KEY) <= 0) return;
        if (STARTED.isEmpty()) {
            Logger.d("sleep timer: nothing is playing");
            return;
        }
        Runnable action = pauseAction;
        if (action == null) {
            Logger.d("sleep timer: no pause action");
            return;
        }
        action.run();
        toast("Sleep timer: playback paused");
    }

    private static void toast(String text) {
        MAIN.post(() -> {
            try {
                Toast.makeText(Settings.appContext(), text, Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Logger.d("toast: " + e);
            }
        });
    }
}
