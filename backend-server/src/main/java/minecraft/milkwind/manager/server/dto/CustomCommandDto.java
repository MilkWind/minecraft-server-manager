package minecraft.milkwind.manager.server.dto;

import java.time.Instant;

public record CustomCommandDto(
        String id,
        String displayName,
        String commandText,
        String description,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
