package minecraft.milkwind.manager.server.dto;

public record ManagedAssetDto(
        String id,
        String name,
        String type,
        boolean enabled
) {
}
