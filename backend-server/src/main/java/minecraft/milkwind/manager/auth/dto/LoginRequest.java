package minecraft.milkwind.manager.auth.dto;

public record LoginRequest(
        String totpCode
) {
}
