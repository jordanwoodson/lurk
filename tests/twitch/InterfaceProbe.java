import io.github.jordanwoodson.extension.shared.twitch.TwitchInterface;
import io.github.jordanwoodson.extension.shared.settings.Settings;
import java.util.Arrays;
import java.util.AbstractList;
import kotlin.enums.EnumEntries;
import android.os.Bundle;

public final class InterfaceProbe {
    enum Tab { HOME, CLIPS, VIDEOS, CHAT }
    static final class Tabs extends AbstractList<Tab> implements EnumEntries<Tab> {
        public Tab get(int index) { return Tab.values()[index]; }
        public int size() { return Tab.values().length; }
    }
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (boolean enabled : new boolean[] {false, true}) {
            Settings.enabled = enabled;
            check(!TwitchInterface.bitsVisible(false), "Stock hidden Bits stays hidden");
            check(TwitchInterface.bitsVisible(true) == !enabled, "Bits setting applies");
            check(TwitchInterface.clipTabs(null) == null, "Missing UI data stays missing");
            java.util.List<String> unrelated = Arrays.asList("CLIPS", "normal chat");
            check(TwitchInterface.clipTabs(unrelated) == unrelated, "Unrelated strings are not discovery enum tabs");
            Tabs source = new Tabs();
            EnumEntries<?> filtered = (EnumEntries<?>) TwitchInterface.clipTabs(source);
            check(source.size() == 4, "Original enum entries are unchanged");
            check(filtered.size() == (enabled ? 3 : 4), "Only Clips removed");
            check(filtered.get(0) == Tab.HOME, "Home order preserved");
            check(filtered.contains(Tab.CHAT), "Chat kept");
            check(filtered.contains(Tab.CLIPS) == !enabled, "Clip discovery setting applies");
            check(enabled || filtered == source, "Disabled preserves original identity");
            Bundle plain = new Bundle();
            check(((java.util.List<?>) TwitchInterface.clipTabs(source, plain)).size() == (enabled ? 3 : 4), "Plain profile filtering");
            for (String key : new String[]{"clipId", "contentType", "profileScope", "profileScopeLower"}) {
                Bundle explicit = new Bundle();
                explicit.putString(key.equals("profileScopeLower") ? "profileScope" : key, key.equals("clipId") ? "shared-clip" : key.equals("contentType") ? "clips_content" : key.equals("profileScopeLower") ? "clips" : "CLIPS");
                check(TwitchInterface.clipTabs(source, explicit) == source, "Explicit clip context must retain full profile: " + key);
            }
            if (enabled) {
                check(filtered.indexOf(Tab.VIDEOS) == 1, "Pager indexes match filtered entries");
                check(filtered.equals(Arrays.asList(Tab.HOME, Tab.VIDEOS, Tab.CHAT)), "List equality preserved");
                check(filtered.toArray().length == 3, "List bridge array conversion preserved");
                try { ((java.util.List) filtered).clear(); throw new AssertionError("Mutable result"); }
                catch (UnsupportedOperationException expected) { }
            }
        }
        System.out.println("Twitch interface helper checks passed");
    }
}
