package minecraft.milkwind.manager.server.dto;

public record PowerActionResultDto(
        String serverId,
        String action,
        String status,
        String message
) {
}
