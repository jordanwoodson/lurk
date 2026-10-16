package io.github.jordanwoodson.extension.shared.rumble;

import android.content.Context;
import android.text.format.DateFormat;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * Live chat for Rumble: hiding notices before Rumble builds its messages, deleted message text and
 * message times. With every setting off, each hook returns Rumble's own value.
 */
@SuppressWarnings("unused")
public final class RumbleChat {
    static final String HIDE_RAIDS = "chat_hide_raid_notices";
    static final String HIDE_GIFTS = "chat_hide_gift_notices";
    static final String SHOW_DELETED = "show_deleted_messages";
    static final String TIMESTAMPS = "chat_timestamps";

    /** The last lists handed out, so a getter called again on the same DTO returns the same list. */
    private static final int REMEMBERED = 8;
    private static final Filtered[] FILTERED = new Filtered[REMEMBERED];
    private static int nextFiltered;

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Field MISSING;
    private static boolean loggedFailure;

    private static boolean timeFormat24;
    private static DateTimeFormatter timeFormatter;

    static {
        Field missing = null;
        try {
            missing = RumbleChat.class.getDeclaredField("loggedFailure");
        } catch (NoSuchFieldException ignored) {
            // Never: the field above exists.
        }
        MISSING = missing;
    }

    private RumbleChat() {
    }

    private static final class Filtered {
        final WeakReference<Object> dto;
        final List<?> source;
        final String settings;
        final List<?> result;

        Filtered(Object dto, List<?> source, String settings, List<?> result) {
            this.dto = new WeakReference<>(dto);
            this.source = source;
            this.settings = settings;
            this.result = result;
        }
    }

    /** Whether a switch of an included patch is on. Keys of patches left out count as off here. */
    static boolean on(String key) {
        return Settings.isRegistered(key) && Settings.isEnabled(key);
    }

    // ---------------------------------------------------------------------------------------------
    // Live chat messages (Hide chat clutter)

    /**
     * Injection point: the messages getter of a live chat DTO (live messages, or the history sent on joining),
     * with the list it read. Returns it without the messages a switch hides, before Rumble turns them into
     * chat rows. The same list instance while nothing is hidden.
     */
    public static List<?> messages(List<?> messages, Object dto) {
        if (messages == null || messages.isEmpty() || dto == null) return messages;
        try {
            boolean raids = on(HIDE_RAIDS);
            boolean gifts = on(HIDE_GIFTS);
            if (!raids && !gifts) return messages;
            String settings = raids + "," + gifts;
            synchronized (FILTERED) {
                for (Filtered filtered : FILTERED) {
                    if (filtered != null && filtered.dto.get() == dto && filtered.source == messages
                            && filtered.settings.equals(settings)) {
                        return filtered.result;
                    }
                }
            }
            List<Object> kept = null;
            for (int i = 0; i < messages.size(); i++) {
                Object message = messages.get(i);
                boolean hide = message != null
                        && ((raids && read(message, "raidNotification") != null)
                        || (gifts && read(message, "giftPurchaseNotification") != null));
                if (hide && kept == null) {
                    kept = new ArrayList<>(messages.size());
                    for (int j = 0; j < i; j++) kept.add(messages.get(j));
                } else if (!hide && kept != null) {
                    kept.add(message);
                }
            }
            List<?> result = kept == null ? messages : kept;
            synchronized (FILTERED) {
                FILTERED[nextFiltered] = new Filtered(dto, messages, settings, result);
                nextFiltered = (nextFiltered + 1) % REMEMBERED;
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return messages;
        }
    }

    /** A field of one of Rumble's Gson DTOs, which keep their field names; null when it has none. */
    static Object read(Object dto, String name) throws ReflectiveOperationException {
        Class<?> type = dto.getClass();
        String key = type.getName() + '.' + name;
        Field field = FIELDS.get(key);
        if (field == null) {
            try {
                field = type.getDeclaredField(name);
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                field = MISSING;
            }
            FIELDS.put(key, field);
        }
        return field == MISSING ? null : field.get(dto);
    }

    // ---------------------------------------------------------------------------------------------
    // Show deleted messages

    /**
     * Injection point: the "removed by moderator" label a deleted (or muted sender's) message shows, with the
     * message's entity. Adds the original text after the label while Show deleted messages is on.
     */
    public static String deletedLabel(String label, Object entity) {
        if (entity == null || !on(SHOW_DELETED)) return label;
        try {
            String text = messageText(entity);
            if (text == null || text.trim().isEmpty()) return label;
            return label + ": " + text;
        } catch (RuntimeException e) {
            fail(e);
            return label;
        }
    }

    /** Replaced at patch time: a chat message entity's text. */
    private static String messageText(Object entity) {
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    // Chat timestamps

    /** Injection point: the sender's name a chat row shows, with the message's entity. Puts the time first. */
    public static String nameWithTime(String name, Object entity) {
        if (name == null || entity == null || !on(TIMESTAMPS)) return name;
        try {
            Object time = timeReceived(entity);
            if (!(time instanceof LocalDateTime)) return name;
            return formatter().format((LocalDateTime) time) + "  " + name;
        } catch (RuntimeException e) {
            fail(e);
            return name;
        }
    }

    /** H:mm or h:mm, following the device's 24-hour setting. */
    private static synchronized DateTimeFormatter formatter() {
        Context context = Settings.appContext();
        boolean is24 = context != null && DateFormat.is24HourFormat(context);
        if (timeFormatter == null || is24 != timeFormat24) {
            timeFormatter = DateTimeFormatter.ofPattern(is24 ? "H:mm" : "h:mm");
            timeFormat24 = is24;
        }
        return timeFormatter;
    }

    /** Replaced at patch time: when a chat message entity was sent, as a local date-time. */
    private static Object timeReceived(Object entity) {
        return null;
    }

    private static void fail(Exception e) {
        if (loggedFailure) return;
        loggedFailure = true;
        Logger.d("rumble chat: failed: " + e);
    }
}
