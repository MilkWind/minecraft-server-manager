package minecraft.milkwind.manager.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlManagerRegistrationStoreTest {

    @TempDir
    Path tempDirectory;

    @Test
    void reloadAppliesUpdatedRegistrationConfigFromApplicationYaml() throws IOException {
        Path applicationYaml = tempDirectory.resolve("application.yaml");
        Files.writeString(applicationYaml, """
                app:
                  auth:
                    manager-registration:
                      is-enable: true
                      accounts:
                      - username: owner
                        display-name: 服主
                        is-enable: true
                """);

        ApplicationYamlManagerRegistrationStore store = new ApplicationYamlManagerRegistrationStore(
                new AppProperties(),
                applicationYaml
        );
        store.initialize();

        assertThat(store.findEnabledAccount("owner")).isPresent();
        assertThat(store.findEnabledAccount("admin")).isEmpty();

        Files.writeString(applicationYaml, """
                app:
                  auth:
                    manager-registration:
                      is-enable: true
                      accounts:
                      - username: admin
                        display-name: 管理员
                        is-enable: true
                """);

        assertThat(store.findEnabledAccount("admin")).isEmpty();

        ApplicationYamlManagerRegistrationStore.ManagerRegistrationConfigSnapshot snapshot = store.reload();

        assertThat(snapshot.enabled()).isTrue();
        assertThat(store.findEnabledAccount("owner")).isEmpty();
        assertThat(store.findEnabledAccount("admin"))
                .isPresent()
                .get()
                .extracting(
                        ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig::username,
                        ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig::displayName
                )
                .containsExactly("admin", "管理员");
    }
}
