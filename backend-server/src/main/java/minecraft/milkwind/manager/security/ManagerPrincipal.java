package minecraft.milkwind.manager.security;

public record ManagerPrincipal(
        String username,
        String displayName,
        String token
) {
}
