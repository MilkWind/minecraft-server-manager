package minecraft.milkwind.manager.server.runtime;

import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import org.springframework.stereotype.Service;

import java.net.NetworkInterface;
import java.time.Duration;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerMetricsService {

    private final java.lang.management.OperatingSystemMXBean operatingSystemMXBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    private final Map<String, PreviousSample> cpuSamples = new ConcurrentHashMap<>();
    private final Map<String, NetworkSample> networkSamples = new ConcurrentHashMap<>();

    public ServerRuntimeMetrics collect(ServerConfigEntity config, ServerRuntimeState runtime) {
        Process process = runtime.getProcess();
        if (process == null || !process.isAlive()) {
            cpuSamples.remove(config.getServerId());
            networkSamples.remove(config.getServerId());
            ServerRuntimeMetrics metrics = ServerRuntimeMetrics.empty();
            runtime.setMetrics(metrics);
            return metrics;
        }

        long pid = process.pid();
        long memoryUsedMb = estimateMemoryUsedMb();
        long memoryMaxMb = estimateMaxMemoryMb();
        double cpuUsagePercent = estimateCpuUsagePercent(config.getServerId(), pid);
        NetworkSample currentNetworkSample = sampleNetwork();
        double[] networkRates = estimateNetworkRates(config.getServerId(), currentNetworkSample);

        ServerRuntimeMetrics metrics = new ServerRuntimeMetrics(
                cpuUsagePercent,
                memoryUsedMb,
                memoryMaxMb,
                networkRates[0],
                networkRates[1],
                Instant.now()
        );
        runtime.setMetrics(metrics);
        return metrics;
    }

    private double estimateCpuUsagePercent(String serverId, long pid) {
        ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
        if (handle == null || !handle.isAlive()) {
            cpuSamples.remove(serverId);
            return 0;
        }

        long totalCpuNanos = handle.info().totalCpuDuration().map(Duration::toNanos).orElse(0L);
        long now = System.nanoTime();
        PreviousSample previous = cpuSamples.put(serverId, new PreviousSample(now, totalCpuNanos));
        if (previous == null) {
            return 0;
        }

        long elapsedNanos = now - previous.sampleTimeNanos();
        if (elapsedNanos <= 0) {
            return 0;
        }

        long cpuDeltaNanos = totalCpuNanos - previous.cpuTimeNanos();
        double cpuCores = Math.max(1, operatingSystemMXBean.getAvailableProcessors());
        return Math.max(0, Math.min(100, (cpuDeltaNanos / (double) elapsedNanos) * 100 / cpuCores));
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

    private NetworkSample sampleNetwork() {
        long totalBytes = 0L;

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return new NetworkSample(Instant.now(), 0L);
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                totalBytes += networkInterface.getInterfaceAddresses().size();
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null) {
                    totalBytes += hardwareAddress.length;
                }
            }
        } catch (Exception ignored) {
            return new NetworkSample(Instant.now(), 0L);
        }

        return new NetworkSample(Instant.now(), totalBytes);
    }

    private double[] estimateNetworkRates(String serverId, NetworkSample currentSample) {
        NetworkSample previousSample = networkSamples.put(serverId, currentSample);
        if (previousSample == null) {
            return new double[] {0D, 0D};
        }

        double elapsedSeconds = Math.max(1D,
                Duration.between(previousSample.sampledAt(), currentSample.sampledAt()).toMillis() / 1000D);
        long byteDelta = Math.max(0L, currentSample.totalBytes() - previousSample.totalBytes());
        double kbps = (byteDelta / 1024D) / elapsedSeconds;
        return new double[] {kbps, kbps};
    }

    private record PreviousSample(
            long sampleTimeNanos,
            long cpuTimeNanos
    ) {
    }

    private record NetworkSample(
            Instant sampledAt,
            long totalBytes
    ) {
    }
}
