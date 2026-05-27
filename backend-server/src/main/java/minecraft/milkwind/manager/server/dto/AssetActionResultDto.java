package minecraft.milkwind.manager.server.dto;

public record AssetActionResultDto(
        String serverId,
        String assetId,
        String assetType,
        String action,
        String status,
        String message
) {
}
