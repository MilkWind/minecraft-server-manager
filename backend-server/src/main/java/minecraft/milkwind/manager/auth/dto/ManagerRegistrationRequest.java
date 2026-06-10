package minecraft.milkwind.manager.auth.dto;

public record ManagerRegistrationRequest(
        String username,
        String displayName,
        String password
) {
}
