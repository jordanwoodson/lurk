package io.github.jordanwoodson.extension.shared.twitch;

import java.lang.reflect.Field;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Hooks for the Twitch chat patches. */
@SuppressWarnings("unused")
public final class TwitchChat {
    static final String TIMESTAMPS = "chat_timestamps";
    static final String TIMESTAMP_FORMAT = "chat_timestamp_format";

    private static volatile Field sourceField;

    private TwitchChat() { }

    // Timestamps.

    /**
     * Injection point: whether a chat line shows its time. {@code config} is the line's click configuration;
     * only lines from channel chat (its source is the {@code Chat} entry) gain a time, not whispers.
     */
    public static boolean timestamps(Object config, boolean stock) {
        if (stock || !Settings.isEnabled(TIMESTAMPS)) return stock;
        Enum<?> source = source(config);
        return source != null && "Chat".equals(source.name());
    }

    /** Injection point: the pattern of the time shown before a chat line. Read once per formatter. */
    public static String timestampPattern(String stock) {
        return Settings.getInt(TIMESTAMP_FORMAT) == 1 ? "h:mm" : stock;
    }

    private static Enum<?> source(Object config) {
        if (config == null) return null;
        try {
            Class<?> type = config.getClass();
            Field field = sourceField;
            if (field == null || field.getDeclaringClass() != type) {
                field = null;
                for (Field candidate : type.getDeclaredFields()) {
                    if (!candidate.getType().isEnum()) continue;
                    if (field != null) return null; // Ambiguous: keep Twitch's choice.
                    field = candidate;
                }
                if (field == null) return null;
                field.setAccessible(true);
                sourceField = field;
            }
            return (Enum<?>) field.get(config);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
