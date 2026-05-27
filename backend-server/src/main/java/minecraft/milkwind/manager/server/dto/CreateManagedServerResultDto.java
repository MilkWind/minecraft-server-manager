package minecraft.milkwind.manager.server.dto;

public record CreateManagedServerResultDto(
        String serverId,
        String displayName,
        String rootDirectory,
        String jvmArguments,
        String publicAddress,
        String gameVersion,
        String status
) {
}
