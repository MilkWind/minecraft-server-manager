package minecraft.milkwind.manager.server.dto;

import java.util.List;

public record BatchAssetActionResultDto(
        String serverId,
        String action,
        int processedCount,
        List<AssetActionResultDto> results,
        String status,
        String message
) {
}
