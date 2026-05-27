package minecraft.milkwind.manager.server.controller;

import minecraft.milkwind.manager.common.api.ApiResponse;
import minecraft.milkwind.manager.server.dto.PublicServerSummaryDto;
import minecraft.milkwind.manager.server.dto.ServerSnapshotDto;
import minecraft.milkwind.manager.server.service.ServerCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/servers")
public class PublicServerController {

    private final ServerCatalogService serverCatalogService;

    public PublicServerController(ServerCatalogService serverCatalogService) {
        this.serverCatalogService = serverCatalogService;
    }

    @GetMapping
    public ApiResponse<List<PublicServerSummaryDto>> listServers() {
        return ApiResponse.success(serverCatalogService.listPublicServers());
    }

    @GetMapping("/{serverId}/snapshot")
    public ApiResponse<ServerSnapshotDto> snapshot(@PathVariable String serverId) {
        return ApiResponse.success(serverCatalogService.getPublicSnapshot(serverId));
    }
}
