package minecraft.milkwind.manager.server.runtime;

import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerMetricsService {

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final Map<String, PreviousSample> previousSamples = new ConcurrentHashMap<>();

    public ServerRuntimeMetrics collect(ServerConfigEntity config, ServerRuntimeState runtime) {
        Process process = runtime.getProcess();
        if (process == null || !process.isAlive()) {
            previousSamples.remove(config.getServerId());
            ServerRuntimeMetrics metrics = ServerRuntimeMetrics.empty();
            runtime.setMetrics(metrics);
            return metrics;
        }

        long pid = process.pid();
        long memoryUsedMb = estimateMemoryUsedMb();
        long memoryMaxMb = estimateMaxMemoryMb();
        double cpuUsagePercent = estimateCpuUsagePercent(config.getServerId(), pid);

        ServerRuntimeMetrics metrics = new ServerRuntimeMetrics(
                cpuUsagePercent,
                memoryUsedMb,
                memoryMaxMb,
                0,
                0,
                Instant.now()
        );
        runtime.setMetrics(metrics);
        return metrics;
    }

    private double estimateCpuUsagePercent(String serverId, long pid) {
        ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
        if (handle == null || !handle.isAlive()) {
            previousSamples.remove(serverId);
            return 0;
        }

        long totalCpuNanos = handle.info().totalCpuDuration().mapToLong(duration -> duration.toNanos()).orElse(0L);
        long now = System.nanoTime();
        PreviousSample previous = previousSamples.put(serverId, new PreviousSample(now, totalCpuNanos));
        if (previous == null) {
            return 0;
        }

        long elapsedNanos = now - previous.sampleTimeNanos();
        if (elapsedNanos <= 0) {
            return 0;
        }

        long cpuDeltaNanos = totalCpuNanos - previous.cpuTimeNanos();
        double cpuCores = Math.max(1, operatingSystemMXBean.getAvailableProcessors());
        return Math.max(0, Math.min(100, (cpuDeltaNanos / (double) elapsedNanos) * 100 * cpuCores));
    }

    private long estimateMemoryUsedMb() {
        Runtime runtime = Runtime.getRuntime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        return bytesToMb(usedBytes);
    }

    private long estimateMaxMemoryMb() {
        return bytesToMb(Runtime.getRuntime().maxMemory());
    }

    private long bytesToMb(long bytes) {
        return Math.max(0, bytes / 1024 / 1024);
    }

    private record PreviousSample(
            long sampleTimeNanos,
            long cpuTimeNanos
    ) {
    }
}
