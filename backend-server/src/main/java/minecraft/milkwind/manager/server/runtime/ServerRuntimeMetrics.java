package minecraft.milkwind.manager.server.runtime;

import java.time.Instant;

public record ServerRuntimeMetrics(
        double cpuUsagePercent,
        long memoryUsedMb,
        long memoryMaxMb,
        double networkInboundKbps,
        double networkOutboundKbps,
        Instant collectedAt
) {
    public static ServerRuntimeMetrics empty() {
        return new ServerRuntimeMetrics(0, 0, 0, 0, 0, Instant.now());
    }
}
