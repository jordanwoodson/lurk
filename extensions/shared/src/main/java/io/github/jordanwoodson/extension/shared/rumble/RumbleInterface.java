package io.github.jordanwoodson.extension.shared.rumble;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.Locale;

import io.github.jordanwoodson.extension.shared.ChatFilter;
import io.github.jordanwoodson.extension.shared.settings.Settings;

/** Interface settings for Rumble. With a setting off, each hook returns Rumble's own value. */
@SuppressWarnings("unused")
public final class RumbleInterface {
    private static Locale countLocale;
    private static NumberFormat countFormat;

    private RumbleInterface() {
    }

    // ---------------------------------------------------------------------------------------------
    // Show exact counts

    /** Injection point: a count in full, grouped the device locale's way (12,345). */
    public static synchronized String exactCount(long count) {
        Locale locale = Locale.getDefault();
        if (countFormat == null || !locale.equals(countLocale)) {
            countFormat = NumberFormat.getIntegerInstance(locale);
            countLocale = locale;
        }
        return countFormat.format(count);
    }

    // ---------------------------------------------------------------------------------------------
    // Hide Home sections

    private static volatile Field titleField;

    /**
     * Injection point: the kind of a Home section from the server (HomeSectionDto.kind). "hide" for a row of
     * categories, creators or playlists whose switch is on, or a section whose title is listed.
     */
    public static String homeSectionKind(Object section, String kind) {
        if (kind == null) return null;
        if (("categories".equals(kind) && Settings.isEnabled("home_hide_categories"))
                || ("creators".equals(kind) && Settings.isEnabled("home_hide_creators"))
                || ("playlists".equals(kind) && Settings.isEnabled("home_hide_playlists"))) {
            return "hide";
        }
        String titles = Settings.getText("home_hide_titles");
        if (titles.trim().isEmpty() || section == null) return kind;
        try {
            Field field = titleField;
            if (field == null) {
                field = section.getClass().getDeclaredField("title");
                field.setAccessible(true);
                titleField = field;
            }
            Object title = field.get(section);
            if (!(title instanceof String)) return kind;
            String wanted = ((String) title).trim();
            for (String entry : ChatFilter.entries(titles)) {
                if (entry.equalsIgnoreCase(wanted)) return "hide";
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return kind;
        }
        return kind;
    }
}
