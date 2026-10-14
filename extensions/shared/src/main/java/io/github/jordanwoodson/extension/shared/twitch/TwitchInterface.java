package io.github.jordanwoodson.extension.shared.twitch;

import android.os.Bundle;
import io.github.jordanwoodson.extension.shared.settings.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** Small UI-only transforms. No account, playback token or chat-message state is changed. */
public final class TwitchInterface {
    private TwitchInterface() { }

    public static boolean bitsVisible(boolean stock) {
        return stock && !Settings.isEnabled("hide_tipping");
    }

    public static Object clipTabs(Object stock) {
        return clipTabs(stock, null);
    }

    public static Object clipTabs(Object stock, Bundle arguments) {
        if (!Settings.isEnabled("hide_clips") || !(stock instanceof List)) return stock;
        // The profile pager also hosts explicitly opened clips. Its caller selects CLIPS for
        // these argument values, so retain the full tab list in that context.
        if (arguments != null && (arguments.getString("clipId") != null ||
                "clips_content".equals(arguments.getString("contentType")) ||
                "CLIPS".equalsIgnoreCase(arguments.getString("profileScope")))) return stock;
        List<?> tabs = (List<?>) stock;
        List<Object> kept = new ArrayList<>();
        for (Object tab : tabs) {
            if (!(tab instanceof Enum)) return stock;
            String name = ((Enum<?>) tab).name();
            if (!"CLIPS".equals(name) && !"Clips".equals(name)) kept.add(tab);
        }
        if (kept.size() == tabs.size() || kept.isEmpty()) return stock;
        try {
            // Kotlin's marker interface is supplied by Twitch, so the extension carries no copy
            // of its stdlib. Both the pager and tab bar receive this same immutable filtered list.
            Class<?> entries = Class.forName("kotlin.enums.EnumEntries", false, stock.getClass().getClassLoader());
            if (!entries.isInstance(stock)) return stock;
            List<?> immutable = Collections.unmodifiableList(kept);
            return Proxy.newProxyInstance(entries.getClassLoader(), new Class<?>[]{entries}, (proxy, method, args) -> {
                try { return method.invoke(immutable, args); }
                catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return stock;
        }
    }
}
