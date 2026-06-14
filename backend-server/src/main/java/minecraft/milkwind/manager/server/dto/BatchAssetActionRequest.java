package minecraft.milkwind.manager.server.dto;

import java.util.List;

public record BatchAssetActionRequest(
        List<String> assetIds
) {
}
