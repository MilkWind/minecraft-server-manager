package minecraft.milkwind.manager.auth.model;

import java.time.Instant;

public record ManagerSession(
        String token,
        String username,
        String displayName,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt
) {
    public ManagerSession touch(Instant seenAt) {
        return new ManagerSession(token, username, displayName, createdAt, seenAt, expiresAt);
    }
}
