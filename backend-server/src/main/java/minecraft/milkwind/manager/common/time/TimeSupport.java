package minecraft.milkwind.manager.common.time;

import java.time.Instant;

public final class TimeSupport {

    private TimeSupport() {
    }

    public static String nowIso() {
        return Instant.now().toString();
    }
}
