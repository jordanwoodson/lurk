import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Default startup tab and Hide Home sections: runs Rumble 3.21.0's startup tab getter (with a fake
 * SharedPreferences holding My Feed) and the Home section kind getter, with each setting on and off.
 */
public final class InterfaceProbe {
    public static void main(String[] args) {
        ProbeSupport.run("startup tab choice and Home section kinds", InterfaceProbe::run);
    }

    private static void run() throws Exception {
        // Locator for 3.21.0 only: zd.d is the rumble_prefs helper, b its SharedPreferences, c the key.
        Object helper = ProbeSupport.allocate(Class.forName("zd.d"));
        SharedPreferences stored = (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
                (proxy, method, values) -> method.getName().equals("getInt") ? 3 : null);
        ProbeSupport.set(helper, "b", stored);
        ProbeSupport.set(helper, "c", "defaultTabIndexKey");
        Method tab = helper.getClass().getMethod("a");
        ProbeSupport.check((int) tab.invoke(helper) == 3, "Rumble's choice (default): the stored My Feed");
        ProbeSupport.setting("startup_tab", "0");
        ProbeSupport.check((int) tab.invoke(helper) == 0, "Home while chosen");

        Class<?> sectionType = Class.forName("com.rumble.network.dto.home.HomeSectionDto");
        Method kind = sectionType.getMethod("f"); // Locator for 3.21.0: the kind getter.
        Object creators = section(sectionType, "creators", "Top Channels");
        Object editors = section(sectionType, "videos", "Editor Picks");
        ProbeSupport.check("creators".equals(kind.invoke(creators)), "creator rows shown while off");
        ProbeSupport.check("videos".equals(kind.invoke(editors)), "titled sections shown while nothing is listed");
        ProbeSupport.setting("home_hide_creators", "true");
        ProbeSupport.setting("home_hide_titles", " editor picks , Live");
        ProbeSupport.check("hide".equals(kind.invoke(creators)), "creator rows hidden while on");
        ProbeSupport.check("hide".equals(kind.invoke(editors)), "a listed title is hidden, in any case");
        ProbeSupport.check("videos".equals(kind.invoke(section(sectionType, "videos", "News"))), "others stay");
    }

    private static Object section(Class<?> type, String kind, String title) throws Exception {
        Object section = ProbeSupport.allocate(type);
        ProbeSupport.set(section, "kind", kind);
        ProbeSupport.set(section, "title", title);
        return section;
    }
}
