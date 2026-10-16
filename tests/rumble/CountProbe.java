import java.lang.reflect.Method;
import java.text.NumberFormat;

/** Show exact counts: Rumble 3.21.0's count formatter abbreviates with the switch off and doesn't with it on. */
public final class CountProbe {
    public static void main(String[] args) {
        ProbeSupport.run("count formatter abbreviates while off and prints full numbers while on", () -> {
            // Locator for 3.21.0 only: the formatter (count, keepDecimal).
            Method format = Class.forName("Eg.C").getMethod("g", long.class, boolean.class);
            String off = (String) format.invoke(null, 1234567L, true);
            ProbeSupport.check(off.endsWith("M"), "switch off (default) must keep Rumble's 1.2M, got " + off);
            ProbeSupport.check("999".equals(format.invoke(null, 999L, false)), "small numbers are unchanged");

            ProbeSupport.setting("exact_counts", "true");
            String expected = NumberFormat.getIntegerInstance().format(1234567L);
            String on = (String) format.invoke(null, 1234567L, true);
            ProbeSupport.check(expected.equals(on), "switch on must print " + expected + ", got " + on);
            ProbeSupport.check("12".equals(format.invoke(null, 12L, false)), "12 stays 12");
        });
    }
}
