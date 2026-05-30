package minecraft.milkwind.manager.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.config.AppProperties;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import minecraft.milkwind.manager.server.runtime.ServerRuntimeState;
import minecraft.milkwind.manager.server.runtime.ServerProcessService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ServerRuntimeRefreshService {

    private static final long MIN_REFRESH_DELAY_MS = 1000L;

    private final ServerConfigMapper serverConfigMapper;
    private final ServerProcessService serverProcessService;
    private final AppProperties appProperties;

    public ServerRuntimeRefreshService(
            ServerConfigMapper serverConfigMapper,
            ServerProcessService serverProcessService,
            AppProperties appProperties
    ) {
        this.serverConfigMapper = serverConfigMapper;
        this.serverProcessService = serverProcessService;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedDelayString = "${app.servers.list-refresh-interval-seconds:15}000")
    public void refreshOnlineServerPlayerLists() {
        List<ServerConfigEntity> servers = serverConfigMapper.selectList(new LambdaQueryWrapper<>());
        Duration refreshInterval = Duration.ofSeconds(Math.max(1, appProperties.getServers().getListRefreshIntervalSeconds()));
        Instant now = Instant.now();

        for (ServerConfigEntity server : servers) {
            ServerRuntimeState runtime = serverProcessService.snapshotRuntime(server);
            if (runtime.getProcess() == null || !"ONLINE".equals(runtime.getStatus())) {
                continue;
            }

            Instant lastRefreshAt = runtime.getLastListRefreshAt();
            if (lastRefreshAt != null && Duration.between(lastRefreshAt, now).compareTo(refreshInterval) < 0) {
                continue;
            }

            serverProcessService.refreshPlayerList(server);
        }
    }
}
