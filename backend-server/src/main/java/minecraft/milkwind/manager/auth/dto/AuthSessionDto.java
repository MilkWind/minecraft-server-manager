package minecraft.milkwind.manager.auth.dto;

import java.time.Instant;
import java.util.List;

public record AuthSessionDto(
        String token,
        String username,
        String displayName,
        Instant createdAt,
        Instant expiresAt,
        List<String> accessibleServerIds
) {
}
