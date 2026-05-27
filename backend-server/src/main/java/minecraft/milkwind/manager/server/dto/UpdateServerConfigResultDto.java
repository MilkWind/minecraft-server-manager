package minecraft.milkwind.manager.server.dto;

public record UpdateServerConfigResultDto(
        String serverId,
        String displayName,
        String rootDirectory,
        String jvmArguments,
        String publicAddress,
        String gameVersion
) {
}
