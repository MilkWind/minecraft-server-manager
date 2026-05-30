package minecraft.milkwind.manager.server.dto;

public record ServerMetricsDto(
        double cpuUsagePercent,
        long memoryUsedMb,
        long memoryMaxMb,
        double networkInboundKbps,
        double networkOutboundKbps
) {
}
