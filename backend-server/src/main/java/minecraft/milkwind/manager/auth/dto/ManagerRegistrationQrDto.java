package minecraft.milkwind.manager.auth.dto;

public record ManagerRegistrationQrDto(
        String registrationId,
        String username,
        String displayName,
        String qrCodeImage,
        String manualEntryKey,
        String otpauthUri
) {
}
