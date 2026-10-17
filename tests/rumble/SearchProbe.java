import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Don't save searches: calls Rumble 3.21.0's save, update and recent-searches use cases with no repository.
 * With the switch on (its default) they return before reaching it; off, Rumble's code runs and fails on the
 * missing repository.
 */
public final class SearchProbe {
    public static void main(String[] args) {
        ProbeSupport.run("search history use cases skip the database while on", () -> {
            // Locators for 3.21.0 only: Mf.j save, Mf.m update, Mf.h recent searches; Lf.d a query row.
            Class<?> continuation = Class.forName("Qh.c");
            Object save = ProbeSupport.allocate(Class.forName("Mf.j"));
            Object update = ProbeSupport.allocate(Class.forName("Mf.m"));
            Object recent = ProbeSupport.allocate(Class.forName("Mf.h"));
            Method saveCall = save.getClass().getMethod("a", String.class, continuation);
            Method updateCall = update.getClass().getMethod("a", Class.forName("Lf.d"), continuation);
            Method recentCall = recent.getClass().getMethod("a", continuation);

            Object unit = Class.forName("kotlin.Unit").getField("a").get(null);
            ProbeSupport.check(saveCall.invoke(save, "a search", null) == unit, "saving returns Unit while on");
            ProbeSupport.check(updateCall.invoke(update, null, null) == unit, "updating returns Unit while on");
            Object list = recentCall.invoke(recent, (Object) null);
            ProbeSupport.check(list instanceof List && ((List<?>) list).isEmpty(), "no recent searches while on");

            ProbeSupport.setting("no_search_history", "false");
            ProbeSupport.check(throwsOriginal(saveCall, save, "a search", null), "saving reaches the repository while off");
            ProbeSupport.check(throwsOriginal(recentCall, recent, (Object) null), "recent searches read the repository while off");
        });
    }

    private static boolean throwsOriginal(Method method, Object target, Object... args) throws Exception {
        try {
            method.invoke(target, args);
            return false;
        } catch (InvocationTargetException e) {
            return e.getCause() instanceof NullPointerException;
        }
    }
}
