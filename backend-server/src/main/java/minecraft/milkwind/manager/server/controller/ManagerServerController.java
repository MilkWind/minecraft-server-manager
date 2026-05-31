package minecraft.milkwind.manager.server.controller;

import minecraft.milkwind.manager.common.api.ApiResponse;
import minecraft.milkwind.manager.server.dto.AssetActionRequest;
import minecraft.milkwind.manager.server.dto.AssetActionResultDto;
import minecraft.milkwind.manager.server.dto.CreateManagedServerRequest;
import minecraft.milkwind.manager.server.dto.CreateManagedServerResultDto;
import minecraft.milkwind.manager.server.dto.ConsoleCommandResultDto;
import minecraft.milkwind.manager.server.dto.CustomCommandDto;
import minecraft.milkwind.manager.server.dto.CustomCommandUpsertRequest;
import minecraft.milkwind.manager.server.dto.ExecuteCommandRequest;
import minecraft.milkwind.manager.server.dto.LogEntryDto;
import minecraft.milkwind.manager.server.dto.PublicServerSummaryDto;
import minecraft.milkwind.manager.server.dto.PlayerActionRequest;
import minecraft.milkwind.manager.server.dto.PlayerActionResultDto;
import minecraft.milkwind.manager.server.dto.PowerActionResultDto;
import minecraft.milkwind.manager.server.dto.SendMessageRequest;
import minecraft.milkwind.manager.server.dto.SendMessageResultDto;
import minecraft.milkwind.manager.server.dto.ServerSnapshotDto;
import minecraft.milkwind.manager.server.dto.UpdateServerConfigRequest;
import minecraft.milkwind.manager.server.dto.UpdateServerConfigResultDto;
import minecraft.milkwind.manager.server.service.ServerCatalogService;
import minecraft.milkwind.manager.server.service.ServerManagementService;
import minecraft.milkwind.manager.auth.model.ManagerSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/servers")
public class ManagerServerController {

    private final ServerCatalogService serverCatalogService;
    private final ServerManagementService serverManagementService;

    public ManagerServerController(
            ServerCatalogService serverCatalogService,
            ServerManagementService serverManagementService
    ) {
        this.serverCatalogService = serverCatalogService;
        this.serverManagementService = serverManagementService;
    }

    @GetMapping("/{serverId}/snapshot")
    public ApiResponse<ServerSnapshotDto> snapshot(@PathVariable String serverId) {
        return ApiResponse.success(serverCatalogService.getManagerSnapshot(serverId));
    }

    @GetMapping
    public ApiResponse<List<PublicServerSummaryDto>> listServers() {
        return ApiResponse.success(serverCatalogService.listPublicServers());
    }

    @GetMapping("/{serverId}/logs")
    public ApiResponse<List<LogEntryDto>> logs(@PathVariable String serverId) {
        return ApiResponse.success(serverCatalogService.getFullLogs(serverId));
    }

    @PostMapping("/{serverId}/power/{action}")
    public ApiResponse<PowerActionResultDto> powerAction(@PathVariable String serverId, @PathVariable String action) {
        return ApiResponse.success(serverCatalogService.runPowerAction(serverId, action));
    }

    @PostMapping("/{serverId}/commands/execute")
    public ApiResponse<ConsoleCommandResultDto> executeCommand(
            @PathVariable String serverId,
            @RequestBody ExecuteCommandRequest request
    ) {
        return ApiResponse.success(serverCatalogService.executeConsoleCommand(serverId, request.command()));
    }

    @PostMapping("/{serverId}/assets/suspend")
    public ApiResponse<AssetActionResultDto> suspendAsset(
            @PathVariable String serverId,
            @RequestBody AssetActionRequest request
    ) {
        return ApiResponse.success(serverManagementService.suspendAsset(serverId, request));
    }

    @PostMapping("/{serverId}/assets/resume")
    public ApiResponse<AssetActionResultDto> resumeAsset(
            @PathVariable String serverId,
            @RequestBody AssetActionRequest request
    ) {
        return ApiResponse.success(serverManagementService.resumeAsset(serverId, request));
    }

    @PostMapping("/{serverId}/players/op")
    public ApiResponse<PlayerActionResultDto> opPlayer(
            @PathVariable String serverId,
            @RequestBody PlayerActionRequest request
    ) {
        return ApiResponse.success(serverManagementService.opPlayer(serverId, request));
    }

    @PostMapping("/{serverId}/players/deop")
    public ApiResponse<PlayerActionResultDto> deopPlayer(
            @PathVariable String serverId,
            @RequestBody PlayerActionRequest request
    ) {
        return ApiResponse.success(serverManagementService.deopPlayer(serverId, request));
    }

    @PostMapping("/{serverId}/players/ban")
    public ApiResponse<PlayerActionResultDto> banPlayer(
            @PathVariable String serverId,
            @RequestBody PlayerActionRequest request
    ) {
        return ApiResponse.success(serverManagementService.banPlayer(serverId, request));
    }

    @PostMapping("/{serverId}/messages")
    public ApiResponse<SendMessageResultDto> sendMessage(
            @PathVariable String serverId,
            @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.success(serverManagementService.sendMessage(serverId, request));
    }

    @PostMapping
    public ApiResponse<CreateManagedServerResultDto> createManagedServer(
            @RequestBody CreateManagedServerRequest request
    ) {
        return ApiResponse.success(serverManagementService.createManagedServer(request));
    }

    @PutMapping("/{serverId}/config")
    public ApiResponse<UpdateServerConfigResultDto> updateConfig(
            @PathVariable String serverId,
            @RequestBody UpdateServerConfigRequest request
    ) {
        return ApiResponse.success(serverManagementService.updateServerConfig(serverId, request));
    }

    @GetMapping("/{serverId}/commands")
    public ApiResponse<List<CustomCommandDto>> listCommands(@PathVariable String serverId) {
        return ApiResponse.success(serverManagementService.listCustomCommands(serverId));
    }

    @PostMapping("/{serverId}/commands")
    public ApiResponse<CustomCommandDto> createCommand(
            @PathVariable String serverId,
            @RequestBody CustomCommandUpsertRequest request,
            @AuthenticationPrincipal ManagerSession principal
    ) {
        return ApiResponse.success(serverManagementService.createCustomCommand(serverId, request, principal.username()));
    }

    @PutMapping("/{serverId}/commands/{commandId}")
    public ApiResponse<CustomCommandDto> updateCommand(
            @PathVariable String serverId,
            @PathVariable String commandId,
            @RequestBody CustomCommandUpsertRequest request
    ) {
        return ApiResponse.success(serverManagementService.updateCustomCommand(serverId, commandId, request));
    }

    @DeleteMapping("/{serverId}/commands/{commandId}")
    public ApiResponse<Void> deleteCommand(
            @PathVariable String serverId,
            @PathVariable String commandId
    ) {
        serverManagementService.deleteCustomCommand(serverId, commandId);
        return ApiResponse.success(null);
    }
}
