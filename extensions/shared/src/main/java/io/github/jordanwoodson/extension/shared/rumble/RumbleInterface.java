package io.github.jordanwoodson.extension.shared.rumble;

import java.text.NumberFormat;
import java.util.Locale;

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
}
