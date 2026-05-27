package minecraft.milkwind.manager.server.dto;

public record PlayerActionResultDto(
        String serverId,
        String action,
        String playerName,
        String command,
        String status,
        String message
) {
}
