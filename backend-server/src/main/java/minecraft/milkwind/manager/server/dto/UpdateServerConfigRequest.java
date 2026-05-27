package minecraft.milkwind.manager.server.dto;

public record UpdateServerConfigRequest(
        String displayName,
        String rootDirectory,
        String jvmArguments,
        String publicAddress,
        String gameVersion
) {
}
