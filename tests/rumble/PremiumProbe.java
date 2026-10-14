import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Runs the real Rumble 3.21.0 profile code on Android, without an account or network requests. */
public final class PremiumProbe {
    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        Class<?> profileClass = Class.forName("com.rumble.network.dto.profile.UserProfile");
        Object profile = allocate(profileClass);
        set(profile, "email", "test@example.invalid");
        set(profile, "apiKey", "test-only");

        Object age = allocate(Class.forName("com.rumble.network.dto.profile.AgeVerification"));
        set(age, "required", true);
        set(age, "minEligible", 18);
        set(profile, "ageVerification", age);
        Object onboarding = allocate(Class.forName("com.rumble.network.dto.profile.Onboarding"));
        set(onboarding, "isPending", true);
        set(profile, "onboarding", onboarding);

        // These obfuscated names are deliberately pinned in the version-specific integration test,
        // never in the patch. They are independently identified from the original 3.21.0 APK.
        Method premiumGetter = profileClass.getMethod("l");
        Method mapper = Class.forName("we.a").getMethod("t", profileClass,
                String.class, String.class, String.class);
        for (boolean serverPremium : new boolean[] {false, true}) {
            set(profile, "isPremium", serverPremium);
            check(Boolean.TRUE.equals(premiumGetter.invoke(profile)),
                    "Profile must report premium when server is_premium=" + serverPremium);
            Object entity = mapper.invoke(null, profile, "test-user", "test-airship", "test-eid");
            check(Boolean.TRUE.equals(entity.getClass().getMethod("x").invoke(entity)),
                    "Profile mapper must propagate premium to the entity used by the session");
            check(profileClass.getMethod("i").invoke(profile) == null,
                    "Missing subscription metadata must remain null");
            check(Boolean.FALSE.equals(entity.getClass().getMethod("w").invoke(entity)),
                    "Account validation must not change");
            check(Boolean.TRUE.equals(age.getClass().getMethod("b").invoke(age)),
                    "Age verification must not change");
            check(Boolean.TRUE.equals(onboarding.getClass().getMethod("e").invoke(onboarding)),
                    "Onboarding must not change");
        }
        System.out.println("PASS: free/paid profile flags and entity mapping; unrelated state preserved");
    }
}
