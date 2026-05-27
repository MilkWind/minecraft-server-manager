package minecraft.milkwind.manager.server.dto;

public record CustomCommandUpsertRequest(
        String displayName,
        String commandText,
        String description
) {
}
