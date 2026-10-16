import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helpers for the probes. Under app_process the app has no Context, so Lurk settings read their
 * patch-time defaults from the registry; {@link #setting} rewrites a default there to test the other value.
 */
final class ProbeSupport {
    private static final String SETTINGS = "io.github.jordanwoodson.extension.shared.settings.Settings";
    private static final String REGISTRY = "io.github.jordanwoodson.extension.shared.settings.Registry";

    private ProbeSupport() {
    }

    static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
    }

    static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Try the superclass.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    static void set(Object target, String name, Object value) throws Exception {
        field(target.getClass(), name).set(target, value);
    }

    static Object get(Object target, String name) throws Exception {
        return field(target.getClass(), name).get(target);
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static Object call(String className, String method, Class<?>[] types, Object... args) throws Exception {
        Method m = Class.forName(className).getDeclaredMethod(method, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private static JSONObject current;

    /** The registry as this process sees it: what the patches wrote, with the probe's changes. */
    static JSONObject registry() throws Exception {
        if (current == null) {
            Method json = Class.forName(SETTINGS).getDeclaredMethod("registryJson");
            json.setAccessible(true);
            current = new JSONObject((String) json.invoke(null));
        }
        return new JSONObject(current.toString());
    }

    private static void install(JSONObject root) throws Exception {
        current = root;
        Method parse = Class.forName(REGISTRY).getDeclaredMethod("parse", String.class);
        parse.setAccessible(true);
        Field registry = Class.forName(SETTINGS).getDeclaredField("registry");
        registry.setAccessible(true);
        registry.set(null, parse.invoke(null, root.toString()));
    }

    /** Whether the patches registered {@code key}. */
    static boolean registered(String key) throws Exception {
        JSONArray settings = registry().getJSONArray("settings");
        for (int i = 0; i < settings.length(); i++) {
            if (key.equals(settings.getJSONObject(i).getString("key"))) return true;
        }
        return false;
    }

    /** Makes {@code value} the value of setting {@code key} for this process. */
    static void setting(String key, String value) throws Exception {
        JSONObject root = registry();
        JSONArray settings = root.getJSONArray("settings");
        boolean found = false;
        for (int i = 0; i < settings.length(); i++) {
            JSONObject entry = settings.getJSONObject(i);
            if (key.equals(entry.getString("key"))) {
                entry.put("default", value);
                found = true;
            }
        }
        check(found, "setting " + key + " is not registered");
        install(root);
    }

    /** Leaves {@code key} out of the registry for this process, as if its patch weren't included. */
    static void unregister(String key) throws Exception {
        JSONObject root = registry();
        JSONArray settings = root.getJSONArray("settings");
        JSONArray kept = new JSONArray();
        for (int i = 0; i < settings.length(); i++) {
            if (!key.equals(settings.getJSONObject(i).getString("key"))) kept.put(settings.getJSONObject(i));
        }
        root.put("settings", kept);
        install(root);
    }

    /** Runs a probe body, printing its failure and exiting 1. */
    static void run(String name, ThrowingRunnable body) {
        try {
            body.run();
            System.out.println("PASS: " + name);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
