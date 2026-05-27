package minecraft.milkwind.manager.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import minecraft.milkwind.manager.auth.service.ManagerBootstrapService;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.server.dto.ConsoleCommandResultDto;
import minecraft.milkwind.manager.server.dto.CustomCommandDto;
import minecraft.milkwind.manager.server.dto.LogEntryDto;
import minecraft.milkwind.manager.server.dto.PlayerDto;
import minecraft.milkwind.manager.server.dto.PowerActionResultDto;
import minecraft.milkwind.manager.server.dto.PublicServerSummaryDto;
import minecraft.milkwind.manager.server.dto.ServerMetricsDto;
import minecraft.milkwind.manager.server.dto.ServerSnapshotDto;
import minecraft.milkwind.manager.server.entity.CustomCommandEntity;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.CustomCommandMapper;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import minecraft.milkwind.manager.server.runtime.ServerProcessService;
import minecraft.milkwind.manager.server.runtime.ServerRuntimeState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ServerCatalogService {

    private final ServerConfigMapper serverConfigMapper;
    private final CustomCommandMapper customCommandMapper;
    private final DatabaseBootstrapService databaseBootstrapService;
    private final ManagerBootstrapService managerBootstrapService;
    private final ServerProcessService serverProcessService;
    private final ServerAssetService serverAssetService;

    public ServerCatalogService(
            ServerConfigMapper serverConfigMapper,
            CustomCommandMapper customCommandMapper,
            DatabaseBootstrapService databaseBootstrapService,
            ManagerBootstrapService managerBootstrapService,
            ServerProcessService serverProcessService,
            ServerAssetService serverAssetService
    ) {
        this.serverConfigMapper = serverConfigMapper;
        this.customCommandMapper = customCommandMapper;
        this.databaseBootstrapService = databaseBootstrapService;
        this.managerBootstrapService = managerBootstrapService;
        this.serverProcessService = serverProcessService;
        this.serverAssetService = serverAssetService;
    }

    @PostConstruct
    public void initialize() {
        managerBootstrapService.ensureBootstrapManager();
        databaseBootstrapService.ensureSeedData();
    }

    public void refresh() {
        // Snapshot data is assembled live from database configuration plus runtime state.
    }

    public List<String> listServerIds() {
        return listServerConfigs().stream()
                .sorted(Comparator.comparing(ServerConfigEntity::getDisplayName))
                .map(ServerConfigEntity::getServerId)
                .toList();
    }

    public List<PublicServerSummaryDto> listPublicServers() {
        return listServerConfigs().stream()
                .sorted(Comparator.comparing(ServerConfigEntity::getDisplayName))
                .map(this::toPublicSummary)
                .toList();
    }

    public ServerSnapshotDto getPublicSnapshot(String serverId) {
        return buildSnapshot(requireServerConfig(serverId), false);
    }

    public ServerSnapshotDto getManagerSnapshot(String serverId) {
        return buildSnapshot(requireServerConfig(serverId), true);
    }

    public List<LogEntryDto> getFullLogs(String serverId) {
        ServerConfigEntity config = requireServerConfig(serverId);
        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        List<LogEntryDto> logs = runtime.getRecentLines().stream()
                .map(line -> new LogEntryDto(
                        UUID.randomUUID().toString(),
                        line.timestamp(),
                        line.level(),
                        line.source(),
                        line.message(),
                        false
                ))
                .toList();

        if (!logs.isEmpty()) {
            return logs;
        }

        return List.of(new LogEntryDto(
                UUID.randomUUID().toString(),
                Instant.now(),
                "INFO",
                "manager",
                "Server logs will appear here after the managed process produces output.",
                false
        ));
    }

    public PowerActionResultDto runPowerAction(String serverId, String action) {
        ServerConfigEntity config = requireServerConfig(serverId);
        String normalizedAction = action == null ? "" : action.trim().toLowerCase();

        switch (normalizedAction) {
            case "start" -> serverProcessService.start(config);
            case "stop" -> serverProcessService.stop(config);
            case "restart" -> serverProcessService.restart(config);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_power_action", "Unsupported power action");
        }

        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        return new PowerActionResultDto(
                config.getServerId(),
                normalizedAction,
                runtime.getStatus(),
                "Process action executed against the managed server"
        );
    }

    public ConsoleCommandResultDto executeConsoleCommand(String serverId, String command) {
        ServerConfigEntity config = requireServerConfig(serverId);
        String normalizedCommand = command == null ? "" : command.trim();

        if (normalizedCommand.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "empty_command", "Console command cannot be empty");
        }

        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        serverProcessService.sendCommand(runtime, normalizedCommand);
        return new ConsoleCommandResultDto(
                config.getServerId(),
                normalizedCommand,
                "DISPATCHED",
                "Command dispatched to the server console"
        );
    }

    private List<ServerConfigEntity> listServerConfigs() {
        return serverConfigMapper.selectList(new LambdaQueryWrapper<>());
    }

    private ServerConfigEntity requireServerConfig(String serverId) {
        ServerConfigEntity entity = serverConfigMapper.selectById(serverId);
        if (entity == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "server_not_found", "Target server does not exist");
        }
        return entity;
    }

    private PublicServerSummaryDto toPublicSummary(ServerConfigEntity config) {
        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        return new PublicServerSummaryDto(
                config.getServerId(),
                config.getDisplayName(),
                runtime.getStatus(),
                config.getPublicAddress(),
                runtime.getOnlinePlayers().size(),
                config.getGameVersion()
        );
    }

    private ServerSnapshotDto buildSnapshot(ServerConfigEntity config, boolean managerView) {
        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        List<PlayerDto> players = runtime.getOnlinePlayers().stream()
                .map(playerName -> new PlayerDto(playerName, false, 0))
                .toList();

        List<LogEntryDto> publicChatMessages = runtime.getRecentChatLines().stream()
                .map(line -> new LogEntryDto(
                        UUID.randomUUID().toString(),
                        line.timestamp(),
                        line.level(),
                        line.source(),
                        line.message(),
                        true
                ))
                .limit(12)
                .toList();

        if (publicChatMessages.isEmpty()) {
            publicChatMessages = List.of(new LogEntryDto(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    "INFO",
                    "minecraft",
                    "Chat messages will appear here after the server emits chat output.",
                    true
            ));
        }

        List<CustomCommandDto> customCommands = managerView
                ? listCustomCommands(config.getServerId())
                : List.of();

        ServerMetricsDto metrics = new ServerMetricsDto(
                28.6,
                2734,
                4096,
                218.4,
                96.7
        );

        return new ServerSnapshotDto(
                config.getServerId(),
                config.getDisplayName(),
                runtime.getStatus(),
                config.getPublicAddress(),
                config.getGameVersion(),
                players.size(),
                players,
                serverAssetService.listMods(config),
                serverAssetService.listDatapacks(config),
                serverAssetService.listResourcePacks(config),
                publicChatMessages,
                metrics,
                managerView ? config.getRootDirectory() : null,
                managerView ? config.getJvmArguments() : null,
                customCommands
        );
    }

    private List<CustomCommandDto> listCustomCommands(String serverId) {
        return customCommandMapper.selectList(
                        new LambdaQueryWrapper<CustomCommandEntity>().eq(CustomCommandEntity::getServerId, serverId))
                .stream()
                .map(command -> new CustomCommandDto(
                        command.getId(),
                        command.getDisplayName(),
                        command.getCommandText(),
                        command.getDescription(),
                        command.getCreatedBy(),
                        Instant.parse(command.getCreatedAt()),
                        Instant.parse(command.getUpdatedAt())
                ))
                .toList();
    }
}
