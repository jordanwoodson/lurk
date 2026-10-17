package io.github.jordanwoodson.extension.shared.kick;

import java.io.IOException;
import java.util.regex.Pattern;

import io.github.jordanwoodson.extension.shared.Logger;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Reads which channel a chat belongs to, for 7TV emotes: 7TV looks emotes up by the channel owner's Kick user ID,
 * and Kick's chat page only knows the channel ID. Kick's own channel responses give the channel's slug for its ID,
 * which leads to the owner. They are only read, never changed. The only extension class that names OkHttp, which
 * Kick ships unrenamed; Twitch and Rumble never load it.
 */
@SuppressWarnings("unused")
public final class KickHttp implements Interceptor {
    private static final long PEEK_LIMIT = 1024 * 1024;
    /** A channel by slug, and the channel info screen's lighter version of it. */
    private static final Pattern CHANNEL = Pattern.compile("/api/v[12]/channels/[^/]+(?:/info)?");

    /**
     * Injection point: the end of React Native's OkHttpClientProvider.createClientBuilder(), which every client
     * Kick makes starts from. Clients built while the switch is off stay as they are.
     */
    public static OkHttpClient.Builder withInterceptor(OkHttpClient.Builder builder) {
        if (KickChat.on(KickChat.SEVEN_TV)) builder.addInterceptor(new KickHttp());
        return builder;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        HttpUrl url = request.url();
        String host = url.host();
        if (response.isSuccessful() && ("kick.com".equals(host) || host.endsWith(".kick.com"))
                && CHANNEL.matcher(url.encodedPath()).matches()) {
            try {
                KickChat.onChannel(response.peekBody(PEEK_LIMIT).string());
            } catch (IOException | RuntimeException e) {
                Logger.d("7tv: channel response: " + e);
            }
        }
        return response;
    }
}
