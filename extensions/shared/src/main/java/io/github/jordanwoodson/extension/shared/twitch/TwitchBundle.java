package io.github.jordanwoodson.extension.shared.twitch;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Selects verified, equal-length Hermes edits at process startup. No bytecode relocation or eval. */
public final class TwitchBundle {
    private static Context context;
    private static String prepared;
    private static boolean embedded;

    private TwitchBundle() { }

    public static void init(Context application) {
        context = application.getApplicationContext();
    }

    /** Called only at React Native's embedded asset loader. A reload uses the same settings snapshot. */
    public static synchronized String prepare(AssetManager assets, String assetUrl) {
        if (!"assets://index.android.bundle".equals(assetUrl)) return null;
        if (prepared != null || embedded) return prepared;
        try {
            if (context == null) throw new IllegalStateException("No application context");
            JSONObject manifest = new JSONObject(new String(read(assets, "lurk-hermes.json", 1024 * 1024),
                    StandardCharsets.UTF_8));
            if (manifest.getInt("format") != 1 || manifest.getInt("version") != 98) {
                throw new IllegalArgumentException("Unknown bundle manifest");
            }
            JSONArray edits = manifest.getJSONArray("edits");
            // Each process gets a single snapshot.
            Map<String, Boolean> snapshot = new HashMap<>();
            boolean defaults = true;
            for (int i = 0; i < edits.length(); i++) {
                JSONObject edit = edits.getJSONObject(i);
                String key = edit.getString("key");
                if (!snapshot.containsKey(key)) snapshot.put(key, Settings.isEnabled(key));
                if (snapshot.get(key) != edit.getBoolean("default")) defaults = false;
            }
            File output = new File(context.getCodeCacheDir(), "lurk-twitch.hbc");
            if (defaults) {
                // The embedded asset already has the patch-time defaults: don't copy 28 MB every start.
                embedded = true;
                output.delete();
                Logger.d("Twitch Hermes: every setting is at its default, using the embedded bundle");
                return null;
            }
            byte[] bundle = read(assets, "index.android.bundle", 32 * 1024 * 1024);
            String embeddedHash = manifest.getString("embeddedSha256");
            if (!hex(MessageDigest.getInstance("SHA-256").digest(bundle)).equals(embeddedHash)) {
                throw new IllegalArgumentException("Embedded bundle hash mismatch");
            }
            int previousEnd = 0;
            for (int i = 0; i < edits.length(); i++) {
                JSONObject edit = edits.getJSONObject(i);
                int offset = edit.getInt("offset");
                byte[] original = unhex(edit.getString("original"));
                byte[] replacement = unhex(edit.getString("replacement"));
                if (original.length == 0 || original.length != replacement.length || offset < previousEnd ||
                        offset > bundle.length - 20 - original.length) {
                    throw new IllegalArgumentException("Invalid bundle edit range");
                }
                byte[] expected = edit.getBoolean("default") ? replacement : original;
                if (!Arrays.equals(expected, Arrays.copyOfRange(bundle, offset, offset + expected.length))) {
                    throw new IllegalArgumentException("Bundle edit mismatch");
                }
                byte[] selected = snapshot.get(edit.getString("key")) ? replacement : original;
                System.arraycopy(selected, 0, bundle, offset, selected.length);
                previousEnd = offset + original.length;
            }
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(bundle, 0, bundle.length - 20);
            System.arraycopy(sha1.digest(), 0, bundle, bundle.length - 20, 20);
            // Atomic replacement tolerates interrupted starts.
            AtomicFile atomic = new AtomicFile(output);
            FileOutputStream stream = null;
            try {
                stream = atomic.startWrite();
                stream.write(bundle);
                atomic.finishWrite(stream);
            } catch (Exception e) {
                if (stream != null) atomic.failWrite(stream);
                throw e;
            }
            prepared = output.getAbsolutePath();
            Logger.d("Twitch Hermes: selected " + edits.length() + " edits across " + snapshot.size() + " settings");
            return prepared;
        } catch (Exception e) {
            // The APK still has the validated patch-time defaults. Do not load a stale private copy.
            Logger.d("Twitch Hermes: using embedded defaults: " + e);
            return null;
        }
    }

    private static byte[] read(AssetManager assets, String path, int limit) throws Exception {
        try (InputStream input = assets.open(path); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() > limit - count) throw new IllegalArgumentException("Asset exceeds size limit");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static byte[] unhex(String value) {
        if (value.length() % 2 != 0) throw new IllegalArgumentException("Odd hex length");
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid hex");
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte b : value) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }
}
