package io.github.jordanwoodson.extension.shared.twitch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.github.jordanwoodson.extension.shared.Logger;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/**
 * Chat keywords: highlights native chat lines that mention you or contain a highlight word, and hides lines with
 * a blocked word or from a blocked user. The line builder marks each line's sender span with its verdict; the row
 * binders read the mark and tint or collapse the row. Live messages are also checked as they arrive, so blocked
 * ones never reach the chat list and highlighted ones can vibrate.
 */
@SuppressWarnings("unused")
public final class TwitchChatKeywords {
    static final String KEY = "chat_keywords";
    static final String MENTIONS = "chat_highlight_mentions";
    static final String HIGHLIGHT_WORDS = "chat_highlight_words";
    static final String VIBRATE = "chat_highlight_vibrate";
    static final String BLOCKED_WORDS = "chat_blocked_words";
    static final String BLOCKED_USERS = "chat_blocked_users";

    static final int NONE = 0;
    static final int HIGHLIGHT = 1;
    static final int HIDE = 2;

    /** Twitch purple at 30%: readable over both the dark and the light chat background. */
    static final int HIGHLIGHT_COLOR = 0x4D9147FF;

    private static final long VIBRATE_GAP_MS = 3000;

    /** The verdict of a line, set on its sender span. Spans of any class survive Twitch's copies of the line. */
    private static final class Mark {
        final int verdict;

        Mark(int verdict) {
            this.verdict = verdict;
        }
    }

    private static final Mark HIGHLIGHTED = new Mark(HIGHLIGHT);
    private static final Mark HIDDEN = new Mark(HIDE);

    /** The sender's login while a line is being built; null when the line isn't checked. */
    private static final ThreadLocal<String> LINE = new ThreadLocal<>();
    /** The height each collapsed row had. */
    private static final Map<View, Integer> COLLAPSED = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Class<?>, Field> TEXT_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> LOGIN_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> SOURCE_FIELDS = new ConcurrentHashMap<>();
    private static final Field NO_FIELD;
    private static final Set<String> vibrated = Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > 64;
        }
    });

    private static volatile Lists lists;
    private static volatile SharedPreferences userPreferences;
    private static long lastVibration;
    private static boolean loggedFailure;

    static {
        Field none = null;
        try {
            none = TwitchChatKeywords.class.getDeclaredField("loggedFailure");
        } catch (NoSuchFieldException ignored) {
            // Never: the field above exists.
        }
        NO_FIELD = none;
    }

    private TwitchChatKeywords() {
    }

    // ---------------------------------------------------------------------------------------------
    // Injection points

    /**
     * Injection point: the start of the chat line builder, with the line's sender (a model with a
     * {@code username=}) and its click configuration, whose source tells channel chat from whispers.
     */
    public static void beginLine(Object sender, Object config) {
        String login = null;
        try {
            if (enabled() && !isWhisper(config)) login = login(sender);
        } catch (RuntimeException e) {
            fail(e);
        }
        LINE.set(login);
    }

    /**
     * Injection point: the end of the chat line builder, with the line's sender, inline header and message
     * spans. Returns the sender spans, marked when the line is highlighted or hidden.
     */
    public static CharSequence markLine(CharSequence sender, CharSequence header, CharSequence message) {
        // Kept until the next line begins: Chat appearance asks hidden() for the same line.
        String login = LINE.get();
        if (login == null || sender == null || sender.length() == 0) return sender;
        try {
            int verdict = classify(login, message == null ? "" : message.toString());
            if (verdict == NONE) return sender;
            Spannable marked = sender instanceof Spannable ? (Spannable) sender : new SpannableString(sender);
            marked.setSpan(verdict == HIDE ? HIDDEN : HIGHLIGHTED, 0, marked.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return marked;
        } catch (RuntimeException e) {
            fail(e);
            return sender;
        }
    }

    /**
     * Injection point: a live message arriving at the native chat list, with its ID, sender and tokens. True drops
     * it (blocked); a highlighted message vibrates when that is switched on.
     */
    public static boolean onLiveMessage(String id, Object sender, List<?> tokens) {
        try {
            if (!enabled()) return false;
            String login = login(sender);
            int verdict = classify(login, text(tokens));
            if (verdict == HIDE) return true;
            if (verdict == HIGHLIGHT && Settings.isEnabled(VIBRATE)
                    && !login.toLowerCase(Locale.ROOT).equals(lists().self)) {
                vibrate(id);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
        }
        return false;
    }

    /**
     * Injection point: a chat row once bound, in native chat and VOD chat replay. Collapses a hidden line's row and
     * tints a highlighted one's, unless Twitch shows it selected or pressed.
     */
    public static void bindRow(View row, TextView text) {
        try {
            if (row == null || text == null) return;
            int verdict = enabled() ? verdict(text.getText()) : NONE;
            collapse(row, verdict == HIDE);
            if (verdict == HIGHLIGHT && TwitchChatStyle.isStockBackground(row)) row.setBackgroundColor(HIGHLIGHT_COLOR);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /**
     * Whether the line being built, or a live message, is hidden, so Chat appearance leaves it out of the
     * alternating rows. Works in either order of the two patches' hooks.
     */
    static boolean hidden(CharSequence sender, CharSequence message) {
        try {
            if (verdict(sender) == HIDE) return true;
            String login = LINE.get();
            return login != null && classify(login, message == null ? "" : message.toString()) == HIDE;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Whether a live or history message (sender and tokens) is hidden. */
    static boolean hiddenLive(Object sender, List<?> tokens) {
        try {
            return enabled() && classify(login(sender), text(tokens)) == HIDE;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Verdicts

    static boolean enabled() {
        return Settings.isEnabled(KEY);
    }

    /** The verdict marked on a bound line, NONE when it has none. */
    static int verdict(CharSequence line) {
        if (!(line instanceof Spanned)) return NONE;
        Mark[] marks = ((Spanned) line).getSpans(0, line.length(), Mark.class);
        int verdict = NONE;
        for (Mark mark : marks) verdict = Math.max(verdict, mark.verdict);
        return verdict;
    }

    /** Hidden for a blocked user or word, highlighted for a mention of you or a highlight word. */
    static int classify(String login, String text) {
        Lists current = lists();
        String sender = login == null ? "" : login.toLowerCase(Locale.ROOT);
        if (!sender.isEmpty() && current.blockedUsers.contains(sender)) return HIDE;
        if (current.blockedWords != null && current.blockedWords.matcher(text).find()) return HIDE;
        if (current.mention != null && !sender.equals(current.self) && current.mention.matcher(text).find()) {
            return HIGHLIGHT;
        }
        if (current.highlightWords != null && current.highlightWords.matcher(text).find()) return HIGHLIGHT;
        return NONE;
    }

    /** The settings as patterns, rebuilt when one of them changes. */
    private static final class Lists {
        final String rawBlockedWords;
        final String rawBlockedUsers;
        final String rawHighlightWords;
        final boolean mentions;
        final String self;
        final String selfName;
        final Pattern blockedWords;
        final Pattern highlightWords;
        final Pattern mention;
        final Set<String> blockedUsers = new HashSet<>();

        Lists(String blockedWords, String blockedUsers, String highlightWords, boolean mentions, String self,
              String selfName) {
            rawBlockedWords = blockedWords;
            rawBlockedUsers = blockedUsers;
            rawHighlightWords = highlightWords;
            this.mentions = mentions;
            this.self = self;
            this.selfName = selfName;
            this.blockedWords = wordsPattern(entries(blockedWords));
            this.highlightWords = wordsPattern(entries(highlightWords));
            for (String user : entries(blockedUsers)) {
                while (user.startsWith("@")) user = user.substring(1);
                if (!user.isEmpty()) this.blockedUsers.add(user.toLowerCase(Locale.ROOT));
            }
            List<String> names = new ArrayList<>();
            if (mentions && !self.isEmpty()) {
                names.add(self);
                if (!selfName.isEmpty() && !selfName.equalsIgnoreCase(self)) names.add(selfName);
            }
            this.mention = wordsPattern(names);
        }

        boolean matches(String blockedWords, String blockedUsers, String highlightWords, boolean mentions,
                        String self, String selfName) {
            return rawBlockedWords.equals(blockedWords) && rawBlockedUsers.equals(blockedUsers)
                    && rawHighlightWords.equals(highlightWords) && this.mentions == mentions
                    && this.self.equals(self) && this.selfName.equals(selfName);
        }
    }

    static Lists lists() {
        String blockedWords = Settings.getText(BLOCKED_WORDS);
        String blockedUsers = Settings.getText(BLOCKED_USERS);
        String highlightWords = Settings.getText(HIGHLIGHT_WORDS);
        boolean mentions = Settings.isEnabled(MENTIONS);
        String self = signedIn("name");
        String selfName = mentions ? signedIn("DisplayName") : "";
        Lists current = lists;
        if (current == null || !current.matches(blockedWords, blockedUsers, highlightWords, mentions, self, selfName)) {
            lists = current = new Lists(blockedWords, blockedUsers, highlightWords, mentions, self, selfName);
        }
        return current;
    }

    /** The entries of a comma- (or line-) separated list, trimmed, without empty ones. */
    static List<String> entries(String list) {
        List<String> entries = new ArrayList<>();
        for (String entry : list.split("[,\n]")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) entries.add(trimmed);
        }
        return entries;
    }

    /** Any of {@code words} as a whole word (not inside a longer word), in any case. Null for no words. */
    static Pattern wordsPattern(List<String> words) {
        if (words.isEmpty()) return null;
        StringBuilder alternatives = new StringBuilder();
        for (String word : words) {
            if (alternatives.length() > 0) alternatives.append('|');
            alternatives.append(Pattern.quote(word));
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}_])(?:" + alternatives + ")(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** A value of the signed-in account from Twitch's own user preferences, lowercase; "" when signed out. */
    private static String signedIn(String key) {
        try {
            SharedPreferences preferences = userPreferences;
            if (preferences == null) {
                Context context = application();
                if (context == null) return "";
                userPreferences = preferences = context.getSharedPreferences("twitch_user_prefs", Context.MODE_PRIVATE);
            }
            String value = preferences.getString(key, null);
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        } catch (RuntimeException e) {
            return "";
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rows

    /** Gives a hidden line's row no height, and gives a reused row its height back. */
    private static void collapse(View row, boolean hide) {
        ViewGroup.LayoutParams params = row.getLayoutParams();
        if (hide) {
            if (!COLLAPSED.containsKey(row)) {
                COLLAPSED.put(row, params == null ? ViewGroup.LayoutParams.WRAP_CONTENT : params.height);
            }
            if (params == null) {
                params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
            } else {
                params.height = 0;
            }
            row.setLayoutParams(params);
            row.setVisibility(View.GONE);
        } else {
            Integer height = COLLAPSED.remove(row);
            if (height == null) return;
            if (params != null) {
                params.height = height;
                row.setLayoutParams(params);
            }
            row.setVisibility(View.VISIBLE);
        }
    }

    private static void vibrate(String id) {
        synchronized (vibrated) {
            if (id != null && !vibrated.add(id)) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastVibration < VIBRATE_GAP_MS) return;
            lastVibration = now;
        }
        Context context = application();
        if (context == null) return;
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = context.getSystemService(VibratorManager.class);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else {
            vibrator = context.getSystemService(Vibrator.class);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    // ---------------------------------------------------------------------------------------------
    // Models, read through their own toString labels (their field names are obfuscated)

    /** The sender's login ("username=" in its toString), or "" if there is none. */
    static String login(Object sender) {
        if (sender == null) return "";
        try {
            Field field = LOGIN_FIELDS.get(sender.getClass());
            if (field == null) {
                Field[] found = labelled(sender, "username=");
                if (found == null) return "";
                field = found[0];
                // Two fields can print the same value (login and display name alike): use it, but don't keep it.
                if (found.length == 1) LOGIN_FIELDS.put(sender.getClass(), field);
            }
            Object login = field.get(sender);
            return login instanceof String ? (String) login : "";
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    /** A message's tokens as plain text: text, mentions, links and emote codes. */
    static String text(List<?> tokens) throws ReflectiveOperationException {
        if (tokens == null) return "";
        StringBuilder text = new StringBuilder();
        for (Object token : tokens) {
            if (token == null) continue;
            Field field = TEXT_FIELDS.get(token.getClass());
            if (field == null) {
                Field[] found = labelled(token, "text=");
                if (found == null) found = labelled(token, "url=");
                field = found == null ? NO_FIELD : found[0];
                TEXT_FIELDS.put(token.getClass(), field);
            }
            if (field == NO_FIELD) continue;
            Object value = field.get(token);
            if (value instanceof String) text.append((String) value);
        }
        return text.toString();
    }

    /**
     * The String fields of {@code model} whose value its toString prints right after {@code label}, or null if
     * none does.
     */
    private static Field[] labelled(Object model, String label) throws ReflectiveOperationException {
        String described = model.toString();
        List<Field> found = new ArrayList<>();
        for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
                field.setAccessible(true);
                Object value = field.get(model);
                if (!(value instanceof String)) continue;
                String printed = label + value;
                int at = described.indexOf(printed);
                while (at >= 0) {
                    int end = at + printed.length();
                    if (end == described.length() || described.charAt(end) == ',' || described.charAt(end) == ')') {
                        found.add(field);
                        break;
                    }
                    at = described.indexOf(printed, at + 1);
                }
            }
        }
        return found.isEmpty() ? null : found.toArray(new Field[0]);
    }

    /** Whether a line's click configuration says it is shown in a whisper thread. */
    private static boolean isWhisper(Object config) {
        return "Whisper".equals(source(config));
    }

    /**
     * Where a line is shown, from its click configuration's one enum field: "Chat" (live chat and history),
     * "Whisper", "Other" (VOD chat replay), or null when it can't tell.
     */
    static String source(Object config) {
        if (config == null) return null;
        Class<?> type = config.getClass();
        Field field = SOURCE_FIELDS.get(type);
        if (field == null) {
            for (Field candidate : type.getDeclaredFields()) {
                if (!candidate.getType().isEnum() || Modifier.isStatic(candidate.getModifiers())) continue;
                if (field != null) {
                    field = NO_FIELD; // Ambiguous.
                    break;
                }
                field = candidate;
            }
            if (field == null) field = NO_FIELD;
            else if (field != NO_FIELD) field.setAccessible(true);
            SOURCE_FIELDS.put(type, field);
        }
        if (field == NO_FIELD) return null;
        try {
            Object source = field.get(config);
            return source instanceof Enum ? ((Enum<?>) source).name() : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static volatile Context application;

    static Context application() {
        Context current = application;
        if (current == null) {
            try {
                current = (Context) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication").invoke(null);
                application = current;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return current;
    }

    private static void fail(Exception e) {
        if (loggedFailure) return;
        loggedFailure = true;
        Logger.d("chat keywords: failed: " + e);
    }
}
