package minecraft.milkwind.manager.auth.dto;

public record ManagerRegistrationConfirmRequest(
        String totpCode
) {
}
