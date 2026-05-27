package minecraft.milkwind.manager.server.dto;

public record SendMessageRequest(
        String targetPlayer,
        String message
) {
}
