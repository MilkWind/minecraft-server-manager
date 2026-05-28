package minecraft.milkwind.manager.auth.dto;

import java.time.Instant;
import java.util.List;

public record AuthSessionDto(
        String token,
        String username,
        String displayName,
        Instant expiresAt,
        List<String> allowedServerIds
) {
}
