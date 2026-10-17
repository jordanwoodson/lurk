import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Refresh stream: the error panel's modifier gets Compose's clickable through the bridged stub while the
 * switch is on (its default), and the reload's quality callback is a Kotlin Function2 that does nothing.
 */
public final class RefreshProbe {
    private static final String PLAYER = "io.github.jordanwoodson.extension.shared.rumble.RumblePlayer";

    public static void main(String[] args) {
        ProbeSupport.run("clickable error panel and silent quality callback", () -> {
            Object modifier = modifierCompanion();
            Method refreshable = Class.forName(PLAYER).getMethod("refreshable", Object.class, Object.class);
            Object clickable = refreshable.invoke(null, modifier, null);
            ProbeSupport.check(clickable != modifier && Class.forName("t1.o").isInstance(clickable),
                    "switch on: the panel's modifier becomes clickable");
            Object callback = Class.forName(PLAYER).getMethod("noQualityCallback").invoke(null);
            ProbeSupport.check(Class.forName("kotlin.jvm.functions.Function2").isInstance(callback),
                    "the reload's quality callback is a Function2");
            ProbeSupport.setting("refresh_stream", "false");
            ProbeSupport.check(refreshable.invoke(null, modifier, null) == modifier, "switch off: Rumble's modifier");
        });
    }

    /** Compose's Modifier (locator t1.o): the companion object every modifier chain starts from. */
    private static Object modifierCompanion() throws Exception {
        Class<?> modifier = Class.forName("t1.o");
        for (Field field : modifier.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && modifier.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field.get(null);
            }
        }
        throw new AssertionError("no Modifier companion");
    }
}
