package io.github.jordanwoodson.extension.shared.twitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * VOD chapters: shows a video's chapters (game/category changes) from Twitch's GQL and seeks to one
 * through Twitch's own VOD seek path. The chapter list is fetched on a background thread with the
 * app's public Client-ID; tapping a chapter seeks the native VOD presenter. With the setting off the
 * button never appears and nothing is fetched.
 */
@SuppressWarnings("unused")
public final class TwitchVod {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String GQL = "https://gql.twitch.tv/gql";
    // The Twitch Android app's public Client-ID (the same one the native GQL client sends). The VOD
    // "moments" query used here is public, so it is answered without an account token.
    private static final String CLIENT_ID = "kd1unb4b3q4t58fwlpcbzcbnm76a8fp";

    private TwitchVod() {
    }

    /** One chapter: where it starts in the VOD and what it is. */
    private static final class Chapter {
        final int positionMs;
        final String label;

        Chapter(int positionMs, String label) {
            this.positionMs = positionMs;
            this.label = label;
        }
    }

    // The VOD currently loaded in a native VOD presenter, and that presenter (to seek it).
    private static volatile WeakReference<Object> presenter = new WeakReference<>(null);
    private static volatile String vodId;
    // Chapters cached per VOD id, so the dialog opens instantly and taps don't refetch.
    private static final Map<String, List<Chapter>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    // The chapter buttons Lurk added to VOD overlays; revealed once a VOD is loaded.
    private static final Map<View, Boolean> BUTTONS = new WeakHashMap<>();
    private static int chaptersDrawable;

    /**
     * Injection point: the VOD presenter loading a video ({@code cll.q0(VodModel, int, String)}).
     * Records the presenter and the video id, prefetches its chapters and reveals the overlay button.
     */
    public static void vodLoaded(Object vodPresenter, Object vodModel) {
        if (vodPresenter == null || vodModel == null || !Settings.isEnabled("vod_chapters")) return;
        try {
            String id = (String) vodModel.getClass().getMethod("getId").invoke(vodModel);
            if (id == null || id.isEmpty()) return;
            presenter = new WeakReference<>(vodPresenter);
            vodId = id;
            fetchAsync(id);
            MAIN.post(TwitchVod::revealButtons);
        } catch (Throwable t) {
            Logger.d("vod chapters: " + t);
        }
    }

    /** Injection point: the VOD/clip overlay's settings gear, once found. Adds a Chapters button. */
    public static void bindChapters(View gear) {
        if (gear == null || !Settings.isEnabled("vod_chapters")) return;
        try {
            View rotate = gear.getRootView().findViewById(id(gear, "rotate_button"));
            if (!(rotate instanceof ImageView) || !(rotate.getParent() instanceof LinearLayout)) return;
            LinearLayout row = (LinearLayout) rotate.getParent();
            ImageView button = buildButton((ImageView) rotate);
            if (button == null) return;
            int index = row.indexOfChild(rotate);
            row.addView(button, Math.max(0, index));
            synchronized (BUTTONS) {
                BUTTONS.put(button, Boolean.TRUE);
            }
            // If a VOD is already loaded in this window, show it now.
            if (vodId != null) button.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Logger.d("vod chapters: " + t);
        }
    }

    private static ImageView buildButton(ImageView sibling) {
        Context context = sibling.getContext();
        int drawable = chaptersDrawable(sibling);
        if (drawable == 0) return null;
        ImageView button = new ImageView(context);
        button.setId(View.generateViewId());
        button.setImageResource(drawable);
        button.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setContentDescription("Chapters");
        button.setLayoutParams(new LinearLayout.LayoutParams(sibling.getLayoutParams()));
        button.setPadding(sibling.getPaddingLeft(), sibling.getPaddingTop(),
                sibling.getPaddingRight(), sibling.getPaddingBottom());
        Drawable background = sibling.getBackground();
        if (background != null && background.getConstantState() != null) {
            button.setBackground(background.getConstantState().newDrawable().mutate());
        }
        // The VOD overlay button is hidden until a VOD is loaded (clips have no chapters).
        button.setVisibility(vodId != null ? View.VISIBLE : View.GONE);
        button.setOnClickListener(TwitchVod::openChapters);
        return button;
    }

    private static void revealButtons() {
        synchronized (BUTTONS) {
            for (View button : BUTTONS.keySet()) {
                if (button.isAttachedToWindow() || button.getParent() != null) button.setVisibility(View.VISIBLE);
            }
        }
    }

    private static void openChapters(View button) {
        if (!Settings.isEnabled("vod_chapters")) return;
        final String id = vodId;
        if (id == null) {
            toast(button.getContext(), "No chapters for this video");
            return;
        }
        List<Chapter> cached = CACHE.get(id);
        if (cached != null) {
            show(button.getContext(), cached);
            return;
        }
        final Context context = button.getContext();
        toast(context, "Loading chapters…");
        new Thread(() -> {
            List<Chapter> chapters = fetch(id);
            MAIN.post(() -> {
                if (chapters == null) {
                    toast(context, "Couldn't load chapters");
                } else {
                    show(context, chapters);
                }
            });
        }, "lurk-chapters").start();
    }

    private static void show(Context context, List<Chapter> chapters) {
        Activity activity = activity(context);
        if (activity == null || activity.isFinishing()) return;
        if (chapters.isEmpty()) {
            toast(context, "This video has no chapters");
            return;
        }
        final List<Chapter> list = chapters;
        CharSequence[] items = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Chapter chapter = list.get(i);
            items[i] = time(chapter.positionMs) + "   " + chapter.label;
        }
        try {
            new AlertDialog.Builder(activity)
                    .setTitle("Chapters")
                    .setItems(items, (dialog, which) -> seekTo(list.get(which).positionMs))
                    .show();
        } catch (RuntimeException e) {
            Logger.d("vod chapters: " + e);
        }
    }

    private static void seekTo(int positionMs) {
        Object target = presenter.get();
        if (target == null) {
            Logger.d("vod chapters: no presenter to seek");
            return;
        }
        try {
            seekVod(target, positionMs);
        } catch (Throwable t) {
            Logger.d("vod chapters: seek " + t);
        }
    }

    /** Replaced at patch time: Twitch's own VOD seek ({@code cll.seekTo(int)}), position in ms. */
    private static void seekVod(Object vodPresenter, int positionMs) {
    }

    // ---------------------------------------------------------------------------------------------
    // Network

    private static void fetchAsync(String id) {
        if (CACHE.containsKey(id)) return;
        new Thread(() -> {
            List<Chapter> chapters = fetch(id);
            if (chapters != null) CACHE.put(id, chapters);
        }, "lurk-chapters-prefetch").start();
    }

    /** Fetches and parses a VOD's chapter moments, or null on failure. Caches on success. */
    private static List<Chapter> fetch(String id) {
        List<Chapter> cached = CACHE.get(id);
        if (cached != null) return cached;
        HttpURLConnection connection = null;
        try {
            JSONObject variables = new JSONObject().put("id", id);
            JSONObject payload = new JSONObject()
                    .put("query", "query($id: ID!) { video(id: $id) { moments(first: 100, "
                            + "momentRequestType: VIDEO_CHAPTER_MARKERS) { edges { node { "
                            + "positionMilliseconds description type } } } } }")
                    .put("variables", variables);
            byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);

            connection = (HttpURLConnection) new URL(GQL).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Client-ID", CLIENT_ID);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            if (connection.getResponseCode() != 200) {
                Logger.d("vod chapters: HTTP " + connection.getResponseCode());
                return null;
            }
            String response = read(connection.getInputStream());
            List<Chapter> chapters = parse(response);
            if (chapters != null) CACHE.put(id, chapters);
            return chapters;
        } catch (Throwable t) {
            Logger.d("vod chapters: fetch " + t);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static List<Chapter> parse(String response) {
        try {
            JSONObject video = new JSONObject(response).getJSONObject("data").optJSONObject("video");
            if (video == null) return new ArrayList<>();
            JSONObject moments = video.optJSONObject("moments");
            JSONArray edges = moments == null ? null : moments.optJSONArray("edges");
            List<Chapter> chapters = new ArrayList<>();
            if (edges == null) return chapters;
            for (int i = 0; i < edges.length(); i++) {
                JSONObject node = edges.getJSONObject(i).optJSONObject("node");
                if (node == null) continue;
                int position = node.optInt("positionMilliseconds", 0);
                String label = node.optString("description", "");
                if (label.isEmpty()) label = node.optString("type", "Chapter");
                chapters.add(new Chapter(position, label));
            }
            return chapters;
        } catch (Throwable t) {
            Logger.d("vod chapters: parse " + t);
            return null;
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
        return buffer.toString("UTF-8");
    }

    // ---------------------------------------------------------------------------------------------

    private static int chaptersDrawable(View sibling) {
        if (chaptersDrawable == 0) chaptersDrawable = id(sibling, "lurk_chapters", "drawable");
        return chaptersDrawable;
    }

    private static int id(View view, String name) {
        return id(view, name, "id");
    }

    /** A resource of the app by name, looked up in the app's own package (the clone keeps it). */
    private static int id(View view, String name, String type) {
        try {
            Resources resources = view.getContext().getResources();
            return resources.getIdentifier(name, type, view.getContext().getPackageName());
        } catch (RuntimeException e) {
            Logger.d("vod chapters: id " + e);
            return 0;
        }
    }

    private static String time(int ms) {
        int seconds = Math.max(0, ms / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, secs);
        return String.format("%d:%02d", minutes, secs);
    }

    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static void toast(Context context, String text) {
        MAIN.post(() -> {
            try {
                Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Logger.d("vod chapters: toast " + e);
            }
        });
    }
}
