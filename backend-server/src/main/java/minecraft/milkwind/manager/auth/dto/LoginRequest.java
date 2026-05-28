package minecraft.milkwind.manager.auth.dto;

public record LoginRequest(
        String username,
        String password,
        String totpCode,
        String serverId
) {
}
