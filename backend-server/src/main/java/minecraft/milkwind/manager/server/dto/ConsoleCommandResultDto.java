package minecraft.milkwind.manager.server.dto;

public record ConsoleCommandResultDto(
        String serverId,
        String command,
        String status,
        String message
) {
}
