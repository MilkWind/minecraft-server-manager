package minecraft.milkwind.manager.server.dto;

public record PublicServerSummaryDto(
        String serverId,
        String displayName,
        String status,
        String publicAddress,
        int onlinePlayerCount,
        String gameVersion
) {
}
