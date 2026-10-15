package io.github.jordanwoodson.extension.shared.twitch;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import java.lang.reflect.Field;
import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Hooks for the Twitch chat patches. */
@SuppressWarnings("unused")
public final class TwitchChat {
    static final String SHOW_DELETED = "chat_show_deleted";
    static final String IGNORE_CLEARS = "chat_ignore_clears";
    static final String TIMESTAMPS = "chat_timestamps";
    static final String TIMESTAMP_FORMAT = "chat_timestamp_format";

    private static volatile Field sourceField;

    private TwitchChat() { }

    // Deleted messages.

    /** Injection point: the text of a message that was deleted before it was shown, or null for the placeholder. */
    public static SpannedString deletedBody(SpannedString body) {
        if (body == null || !Settings.isEnabled(SHOW_DELETED)) return null;
        SpannableStringBuilder text = new SpannableStringBuilder(body);
        text.setSpan(new DeletedSpan(), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Logger.d("chat: kept a message deleted before it was shown");
        return new SpannedString(text);
    }

    /**
     * Injection point: a shown line whose message was deleted, with the message from {@code bodyStart} on
     * struck through, or null for Twitch's placeholder.
     */
    public static Spanned deletedLine(SpannedString line, int bodyStart) {
        if (line == null || bodyStart < 0 || bodyStart > line.length() || !Settings.isEnabled(SHOW_DELETED)) return null;
        SpannableStringBuilder text = new SpannableStringBuilder(line);
        text.setSpan(new DeletedSpan(), bodyStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Logger.d("chat: kept a deleted message");
        return text;
    }

    /** Injection point: whether a moderator's clear of the whole chat is ignored. Timeouts and bans still apply. */
    public static boolean ignoreChatClears() {
        boolean ignore = Settings.isEnabled(IGNORE_CLEARS);
        if (ignore) Logger.d("chat: kept the messages of a cleared chat");
        return ignore;
    }

    /** Struck through, with the text's own color at half strength, so it works on light and dark themes. */
    static final class DeletedSpan extends CharacterStyle implements UpdateAppearance {
        @Override
        public void updateDrawState(TextPaint paint) {
            paint.setStrikeThruText(true);
            paint.setAlpha(paint.getAlpha() / 2);
        }
    }

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
