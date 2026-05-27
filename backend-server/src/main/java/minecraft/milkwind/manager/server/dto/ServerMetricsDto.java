package minecraft.milkwind.manager.server.dto;

public record ServerMetricsDto(
        double cpuUsagePercent,
        long usedMemoryMb,
        long maxMemoryMb,
        double networkDownKbps,
        double networkUpKbps
) {
}
