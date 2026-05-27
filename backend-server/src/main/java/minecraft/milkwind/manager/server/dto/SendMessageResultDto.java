package minecraft.milkwind.manager.server.dto;

public record SendMessageResultDto(
        String serverId,
        String targetPlayer,
        String command,
        String status,
        String message
) {
}
