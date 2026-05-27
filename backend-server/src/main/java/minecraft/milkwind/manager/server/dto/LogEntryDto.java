package minecraft.milkwind.manager.server.dto;

import java.time.Instant;

public record LogEntryDto(
        String id,
        Instant timestamp,
        String level,
        String source,
        String message,
        boolean publicVisible
) {
}
