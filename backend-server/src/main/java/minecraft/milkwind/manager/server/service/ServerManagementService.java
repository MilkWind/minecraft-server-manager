package minecraft.milkwind.manager.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.server.dto.AssetActionRequest;
import minecraft.milkwind.manager.server.dto.AssetActionResultDto;
import minecraft.milkwind.manager.server.dto.BatchAssetActionRequest;
import minecraft.milkwind.manager.server.dto.BatchAssetActionResultDto;
import minecraft.milkwind.manager.server.dto.CreateManagedServerRequest;
import minecraft.milkwind.manager.server.dto.CreateManagedServerResultDto;
import minecraft.milkwind.manager.server.dto.CustomCommandDto;
import minecraft.milkwind.manager.server.dto.CustomCommandUpsertRequest;
import minecraft.milkwind.manager.server.dto.PlayerActionRequest;
import minecraft.milkwind.manager.server.dto.PlayerActionResultDto;
import minecraft.milkwind.manager.server.dto.SendMessageRequest;
import minecraft.milkwind.manager.server.dto.SendMessageResultDto;
import minecraft.milkwind.manager.server.dto.UpdateServerConfigRequest;
import minecraft.milkwind.manager.server.dto.UpdateServerConfigResultDto;
import minecraft.milkwind.manager.server.entity.CustomCommandEntity;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.CustomCommandMapper;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import minecraft.milkwind.manager.server.runtime.ServerProcessService;
import minecraft.milkwind.manager.server.runtime.ServerRuntimeRegistry;
import minecraft.milkwind.manager.server.runtime.ServerRuntimeState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServerManagementService {

    private final ServerConfigMapper serverConfigMapper;
    private final CustomCommandMapper customCommandMapper;
    private final ServerCatalogService serverCatalogService;
    private final ServerProcessService serverProcessService;
    private final ServerRuntimeRegistry serverRuntimeRegistry;
    private final ServerAssetService serverAssetService;

    public ServerManagementService(
            ServerConfigMapper serverConfigMapper,
            CustomCommandMapper customCommandMapper,
            ServerCatalogService serverCatalogService,
            ServerProcessService serverProcessService,
            ServerRuntimeRegistry serverRuntimeRegistry,
            ServerAssetService serverAssetService
    ) {
        this.serverConfigMapper = serverConfigMapper;
        this.customCommandMapper = customCommandMapper;
        this.serverCatalogService = serverCatalogService;
        this.serverProcessService = serverProcessService;
        this.serverRuntimeRegistry = serverRuntimeRegistry;
        this.serverAssetService = serverAssetService;
    }

    public UpdateServerConfigResultDto updateServerConfig(String serverId, UpdateServerConfigRequest request) {
        ServerConfigEntity entity = requireServerConfig(serverId);
        entity.setDisplayName(requireText(request.displayName(), "display_name"));
        entity.setRootDirectory(requireText(request.rootDirectory(), "root_directory"));
        entity.setJvmArguments(requireText(request.jvmArguments(), "jvm_arguments"));
        entity.setPublicAddress(requireText(request.publicAddress(), "public_address"));
        entity.setGameVersion(requireText(request.gameVersion(), "game_version"));
        entity.setUpdatedAt(TimeSupport.nowIso());
        serverConfigMapper.updateById(entity);
        serverCatalogService.refresh();

        return new UpdateServerConfigResultDto(
                entity.getServerId(),
                entity.getDisplayName(),
                entity.getRootDirectory(),
                entity.getJvmArguments(),
                entity.getPublicAddress(),
                entity.getGameVersion()
        );
    }

    public CreateManagedServerResultDto createManagedServer(CreateManagedServerRequest request) {
        String serverId = requireServerId(request.serverId());
        if (serverConfigMapper.selectById(serverId) != null) {
            throw new ApiException(HttpStatus.CONFLICT, "server_exists", "目标服务器 ID 已存在");
        }

        ServerConfigEntity entity = new ServerConfigEntity();
        entity.setServerId(serverId);
        entity.setDisplayName(requireText(request.displayName(), "display_name"));
        entity.setRootDirectory(requireText(request.rootDirectory(), "root_directory"));
        entity.setJvmArguments(requireText(request.jvmArguments(), "jvm_arguments"));
        entity.setPublicAddress(requireText(request.publicAddress(), "public_address"));
        entity.setGameVersion(requireText(request.gameVersion(), "game_version"));
        entity.setChatEnabled(Boolean.TRUE);
        entity.setStatus("STOPPED");
        entity.setCreatedAt(TimeSupport.nowIso());
        entity.setUpdatedAt(TimeSupport.nowIso());
        serverConfigMapper.insert(entity);
        serverCatalogService.refresh();

        return new CreateManagedServerResultDto(
                entity.getServerId(),
                entity.getDisplayName(),
                entity.getRootDirectory(),
                entity.getJvmArguments(),
                entity.getPublicAddress(),
                entity.getGameVersion(),
                entity.getStatus()
        );
    }

    public List<CustomCommandDto> listCustomCommands(String serverId) {
        requireServerConfig(serverId);
        return customCommandMapper.selectList(
                        new LambdaQueryWrapper<CustomCommandEntity>().eq(CustomCommandEntity::getServerId, serverId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CustomCommandDto createCustomCommand(String serverId, CustomCommandUpsertRequest request, String createdBy) {
        requireServerConfig(serverId);

        CustomCommandEntity entity = new CustomCommandEntity();
        entity.setServerId(serverId);
        entity.setDisplayName(requireText(request.displayName(), "display_name"));
        entity.setCommandText(requireText(request.commandText(), "command_text"));
        entity.setDescription(normalizeDescription(request.description()));
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(TimeSupport.nowIso());
        entity.setUpdatedAt(TimeSupport.nowIso());
        customCommandMapper.insert(entity);
        serverCatalogService.refresh();
        return toDto(entity);
    }

    public CustomCommandDto updateCustomCommand(
            String serverId,
            String commandId,
            CustomCommandUpsertRequest request
    ) {
        CustomCommandEntity entity = requireCommand(serverId, commandId);
        entity.setDisplayName(requireText(request.displayName(), "display_name"));
        entity.setCommandText(requireText(request.commandText(), "command_text"));
        entity.setDescription(normalizeDescription(request.description()));
        entity.setUpdatedAt(TimeSupport.nowIso());
        customCommandMapper.updateById(entity);
        serverCatalogService.refresh();
        return toDto(entity);
    }

    public void deleteCustomCommand(String serverId, String commandId) {
        CustomCommandEntity entity = requireCommand(serverId, commandId);
        customCommandMapper.deleteById(entity.getId());
        serverCatalogService.refresh();
    }

    public AssetActionResultDto suspendAsset(String serverId, AssetActionRequest request) {
        ServerConfigEntity config = requireServerConfig(serverId);
        AssetActionResultDto result = serverAssetService.suspendAsset(config, requireText(request.assetId(), "asset_id"));
        markRestartRecommended(serverId, config.getStatus());
        serverCatalogService.refresh();
        return result;
    }

    public AssetActionResultDto resumeAsset(String serverId, AssetActionRequest request) {
        ServerConfigEntity config = requireServerConfig(serverId);
        AssetActionResultDto result = serverAssetService.resumeAsset(config, requireText(request.assetId(), "asset_id"));
        markRestartRecommended(serverId, config.getStatus());
        serverCatalogService.refresh();
        return result;
    }

    public BatchAssetActionResultDto suspendAssets(String serverId, BatchAssetActionRequest request) {
        ServerConfigEntity config = requireServerConfig(serverId);
        BatchAssetActionResultDto result = serverAssetService.suspendAssets(config, requireAssetIds(request));
        markRestartRecommended(serverId, config.getStatus());
        serverCatalogService.refresh();
        return result;
    }

    public BatchAssetActionResultDto resumeAssets(String serverId, BatchAssetActionRequest request) {
        ServerConfigEntity config = requireServerConfig(serverId);
        BatchAssetActionResultDto result = serverAssetService.resumeAssets(config, requireAssetIds(request));
        markRestartRecommended(serverId, config.getStatus());
        serverCatalogService.refresh();
        return result;
    }

    public PlayerActionResultDto opPlayer(String serverId, PlayerActionRequest request) {
        return performPlayerAction(serverId, "op", request, "OP authorization command sent");
    }

    public PlayerActionResultDto deopPlayer(String serverId, PlayerActionRequest request) {
        return performPlayerAction(serverId, "deop", request, "OP removal command sent");
    }

    public PlayerActionResultDto banPlayer(String serverId, PlayerActionRequest request) {
        String playerName = requireText(request.playerName(), "player_name");
        String reason = normalizeDescription(request.reason());
        String command = reason.isBlank() ? "ban " + playerName : "ban " + playerName + " " + reason;
        dispatchConsoleCommand(serverId, command);
        return new PlayerActionResultDto(serverId, "ban", playerName, command, "DISPATCHED", "Ban command sent");
    }

    public SendMessageResultDto sendMessage(String serverId, SendMessageRequest request) {
        String message = requireText(request.message(), "message");
        String targetPlayer = normalizeDescription(request.targetPlayer());
        String command = targetPlayer.isBlank()
                ? "say " + message
                : "msg " + targetPlayer + " " + message;
        dispatchConsoleCommand(serverId, command);
        return new SendMessageResultDto(
                serverId,
                targetPlayer,
                command,
                "DISPATCHED",
                targetPlayer.isBlank() ? "Broadcast message sent" : "Private message sent"
        );
    }

    private ServerConfigEntity requireServerConfig(String serverId) {
        ServerConfigEntity entity = serverConfigMapper.selectById(serverId);
        if (entity == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "server_not_found", "目标服务器不存在");
        }
        return entity;
    }

    private CustomCommandEntity requireCommand(String serverId, String commandId) {
        CustomCommandEntity entity = customCommandMapper.selectById(commandId);
        if (entity == null || !serverId.equals(entity.getServerId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "command_not_found", "目标命令不存在");
        }
        return entity;
    }

    private String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + field, "必填字段为空：" + field);
        }
        return normalized;
    }

    private String requireServerId(String value) {
        String normalized = requireText(value, "server_id");
        if (!normalized.matches("[a-zA-Z0-9_-]+")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_server_id", "服务器 ID 只能包含字母、数字、下划线和连字符");
        }
        return normalized;
    }

    private String normalizeDescription(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> requireAssetIds(BatchAssetActionRequest request) {
        if (request == null || request.assetIds() == null || request.assetIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_asset_ids", "资源 ID 列表不能为空");
        }

        List<String> normalized = new ArrayList<>();
        for (String assetId : request.assetIds()) {
            normalized.add(requireText(assetId, "asset_id"));
        }
        return normalized;
    }

    private PlayerActionResultDto performPlayerAction(
            String serverId,
            String action,
            PlayerActionRequest request,
            String successMessage
    ) {
        String playerName = requireText(request.playerName(), "player_name");
        String command = action + " " + playerName;
        dispatchConsoleCommand(serverId, command);
        return new PlayerActionResultDto(serverId, action, playerName, command, "DISPATCHED", successMessage);
    }

    private void dispatchConsoleCommand(String serverId, String command) {
        ServerConfigEntity config = requireServerConfig(serverId);
        ServerRuntimeState runtime = serverProcessService.snapshotRuntime(config);
        serverProcessService.sendCommand(runtime, command);
    }

    private void markRestartRecommended(String serverId, String status) {
        ServerRuntimeState runtime = serverRuntimeRegistry.getOrCreate(serverId, status);
        runtime.setRestartRecommended(true);
    }

    private CustomCommandDto toDto(CustomCommandEntity entity) {
        return new CustomCommandDto(
                entity.getId(),
                entity.getDisplayName(),
                entity.getCommandText(),
                entity.getDescription(),
                entity.getCreatedBy(),
                Instant.parse(entity.getCreatedAt()),
                Instant.parse(entity.getUpdatedAt())
        );
    }
}
