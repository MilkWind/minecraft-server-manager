package minecraft.milkwind.manager.server.dto;

import java.util.List;

public record ServerSnapshotDto(
        String serverId,
        String displayName,
        String status,
        String publicAddress,
        String gameVersion,
        int onlinePlayerCount,
        List<PlayerDto> onlinePlayers,
        List<ManagedAssetDto> mods,
        List<ManagedAssetDto> datapacks,
        List<ManagedAssetDto> resourcePacks,
        List<LogEntryDto> chatMessages,
        ServerMetricsDto metrics,
        String rootDirectory,
        String jvmArguments,
        List<CustomCommandDto> customCommands
) {
}
