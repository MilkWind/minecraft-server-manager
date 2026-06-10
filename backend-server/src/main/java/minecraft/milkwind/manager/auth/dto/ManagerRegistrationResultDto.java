package minecraft.milkwind.manager.auth.dto;

public record ManagerRegistrationResultDto(
        String username,
        String displayName,
        String message
) {
}
