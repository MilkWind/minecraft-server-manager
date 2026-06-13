package minecraft.milkwind.manager.config;

import java.time.Instant;
import java.util.List;

public record ManagerRegistrationConfigReloadDto(
        boolean enabled,
        int accountCount,
        int enabledAccountCount,
        List<String> enabledUsernames,
        String source,
        Instant loadedAt
) {
    public static ManagerRegistrationConfigReloadDto from(
            ApplicationYamlManagerRegistrationStore.ManagerRegistrationConfigSnapshot snapshot
    ) {
        List<String> enabledUsernames = snapshot.accounts().stream()
                .filter(ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig::enabled)
                .map(ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig::username)
                .toList();
        return new ManagerRegistrationConfigReloadDto(
                snapshot.enabled(),
                snapshot.accounts().size(),
                enabledUsernames.size(),
                enabledUsernames,
                snapshot.source(),
                snapshot.loadedAt()
        );
    }
}
