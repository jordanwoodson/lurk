package io.github.jordanwoodson.extension.shared.twitch;

import android.util.JsonReader;
import android.util.JsonToken;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.jordanwoodson.extension.shared.Logger;

/**
 * Network access for the opt-in third-party chat add-ons (emotes and badges). Every request runs on a
 * small pool of background threads with short timeouts, sends no cookies or Twitch credentials, and
 * identifies itself only as Lurk (not the device model). Callers never wait for it on a UI or chat thread.
 */
final class TwitchThirdParty {
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final String USER_AGENT = "Lurk (+https://github.com/jordanwoodson/lurk-morphe-patches)";

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(3, 3, 30, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), runnable -> {
                Thread thread = new Thread(runnable, "lurk-third-party");
                thread.setDaemon(true);
                return thread;
            });

    static {
        POOL.allowCoreThreadTimeOut(true);
    }

    private TwitchThirdParty() {
    }

    interface JsonParser<T> {
        T parse(JsonReader reader) throws IOException;
    }

    /** Runs {@code task} in the background. A failing task only logs. */
    static void run(String what, Runnable task) {
        try {
            POOL.execute(() -> {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    Logger.d("third-party: " + what + " failed: " + e);
                }
            });
        } catch (RejectedExecutionException e) {
            Logger.d("third-party: " + what + " rejected");
        }
    }

    /**
     * GETs {@code url} and streams its JSON body through {@code parser}. Returns {@code notFound} on HTTP
     * 404 (a channel the service doesn't know), and throws on any other failure.
     */
    static <T> T getJson(String url, JsonParser<T> parser, T notFound) throws IOException {
        return getJson(url, parser, notFound, READ_TIMEOUT_MS);
    }

    /** {@link #getJson(String, JsonParser, Object)} with its own read timeout, for small, frequent requests. */
    static <T> T getJson(String url, JsonParser<T> parser, T notFound, int readTimeoutMs) throws IOException {
        HttpURLConnection connection = open(url, "application/json");
        connection.setReadTimeout(readTimeoutMs);
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return notFound;
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);
            try (JsonReader reader = new JsonReader(new InputStreamReader(
                    new BufferedInputStream(connection.getInputStream()), StandardCharsets.UTF_8))) {
                reader.setLenient(true);
                return parser.parse(reader);
            }
        } finally {
            connection.disconnect();
        }
    }

    /** GETs {@code url} as bytes, at most {@code max} of them. */
    static byte[] getBytes(String url, int max) throws IOException {
        HttpURLConnection connection = open(url, "image/png,image/*");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (out.size() > max) throw new IOException("too large");
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", accept);
        return connection;
    }

    // JsonReader helpers. Every value of an unexpected type is skipped, so a changed field can't fail a whole list.

    static String string(JsonReader reader) throws IOException {
        JsonToken token = reader.peek();
        if (token == JsonToken.STRING || token == JsonToken.NUMBER) return reader.nextString();
        reader.skipValue();
        return null;
    }

    static boolean bool(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.BOOLEAN) return reader.nextBoolean();
        reader.skipValue();
        return false;
    }

    static long number(JsonReader reader, long fallback) throws IOException {
        if (reader.peek() == JsonToken.NUMBER) {
            try {
                return reader.nextLong();
            } catch (NumberFormatException e) {
                reader.skipValue();
                return fallback;
            }
        }
        reader.skipValue();
        return fallback;
    }

    /** True and inside the object when the next value is an object; otherwise skips it. */
    static boolean beginObject(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();
            return true;
        }
        reader.skipValue();
        return false;
    }

    /** True and inside the array when the next value is an array; otherwise skips it. */
    static boolean beginArray(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            return true;
        }
        reader.skipValue();
        return false;
    }
}
