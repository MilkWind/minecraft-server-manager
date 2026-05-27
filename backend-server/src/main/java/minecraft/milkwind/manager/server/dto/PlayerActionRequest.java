package minecraft.milkwind.manager.server.dto;

public record PlayerActionRequest(
        String playerName,
        String reason
) {
}
