package minecraft.milkwind.manager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ApplicationYamlManagerRegistrationStore {

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("src", "main", "resources", "application.yaml"),
            Path.of("backend-server", "src", "main", "resources", "application.yaml"),
            Path.of("resources", "application.yaml")
    );

    private final Object lock = new Object();
    private final AppProperties appProperties;
    private final Yaml yaml = new Yaml();
    private volatile ManagerRegistrationConfigSnapshot snapshot;
    private Path applicationYamlPath;

    @Autowired
    public ApplicationYamlManagerRegistrationStore(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    ApplicationYamlManagerRegistrationStore(AppProperties appProperties, Path applicationYamlPath) {
        this.appProperties = appProperties;
        this.applicationYamlPath = applicationYamlPath.toAbsolutePath();
    }

    @PostConstruct
    public void initialize() {
        if (applicationYamlPath != null) {
            reload();
            return;
        }

        this.applicationYamlPath = resolveApplicationYamlPath();
        reload();
    }

    public ManagerRegistrationConfigSnapshot reload() {
        synchronized (lock) {
            snapshot = readSnapshot();
            return snapshot;
        }
    }

    public ManagerRegistrationConfigSnapshot currentSnapshot() {
        ManagerRegistrationConfigSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        return reload();
    }

    public Optional<ManagerRegistrationAccountConfig> findEnabledAccount(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            return Optional.empty();
        }

        ManagerRegistrationConfigSnapshot current = currentSnapshot();
        if (!current.enabled()) {
            return Optional.empty();
        }

        return current.accounts().stream()
                .filter(ManagerRegistrationAccountConfig::enabled)
                .filter(account -> normalizedUsername.equals(account.username()))
                .findFirst();
    }

    public List<ManagerRegistrationAccountConfig> enabledAccounts() {
        ManagerRegistrationConfigSnapshot current = currentSnapshot();
        if (!current.enabled()) {
            return List.of();
        }

        return current.accounts().stream()
                .filter(ManagerRegistrationAccountConfig::enabled)
                .toList();
    }

    private ManagerRegistrationConfigSnapshot readSnapshot() {
        Map<String, Object> root = loadRoot();
        Map<String, Object> registration = registrationMap(root);
        boolean fallbackEnabled = appProperties.getAuth().getManagerRegistration().isEnable();
        boolean enabled = parseBoolean(registration.get("is-enable"), fallbackEnabled);
        List<ManagerRegistrationAccountConfig> accounts = readAccounts(registration);
        return new ManagerRegistrationConfigSnapshot(enabled, accounts, applicationYamlPath.toString(), Instant.now());
    }

    private List<ManagerRegistrationAccountConfig> readAccounts(Map<String, Object> registration) {
        Object rawAccounts = registration.get("accounts");
        if (!(rawAccounts instanceof List<?> accountList)) {
            return appProperties.getAuth().getManagerRegistration().getAccounts().stream()
                    .map(account -> new ManagerRegistrationAccountConfig(
                            normalizeUsername(account.getUsername()),
                            normalizeDisplayName(account.getDisplayName(), normalizeUsername(account.getUsername())),
                            account.isEnable()
                    ))
                    .filter(account -> !account.username().isBlank())
                    .toList();
        }

        return accountList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(account -> new ManagerRegistrationAccountConfig(
                        normalizeUsername(Objects.toString(account.get("username"), "")),
                        normalizeDisplayName(
                                Objects.toString(account.get("display-name"), ""),
                                normalizeUsername(Objects.toString(account.get("username"), ""))
                        ),
                        parseBoolean(account.get("is-enable"), false)
                ))
                .filter(account -> !account.username().isBlank())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> registrationMap(Map<String, Object> root) {
        Object app = root.get("app");
        if (!(app instanceof Map<?, ?> appMap)) {
            return Map.of();
        }

        Object auth = appMap.get("auth");
        if (!(auth instanceof Map<?, ?> authMap)) {
            return Map.of();
        }

        Object registration = authMap.get("manager-registration");
        if (!(registration instanceof Map<?, ?> registrationMap)) {
            return Map.of();
        }

        return (Map<String, Object>) registrationMap;
    }

    private Map<String, Object> loadRoot() {
        try (InputStream stream = Files.newInputStream(applicationYamlPath)) {
            Object loaded = yaml.load(stream);
            if (loaded instanceof Map<?, ?> map) {
                return castToMap(map);
            }
            return new LinkedHashMap<>();
        } catch (IOException exception) {
            throw new IllegalStateException("读取 application.yaml 失败", exception);
        }
    }

    private Map<String, Object> castToMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                result.put(String.valueOf(entry.getKey()), castToMap(nested));
            } else {
                result.put(String.valueOf(entry.getKey()), value);
            }
        }
        return result;
    }

    private Path resolveApplicationYamlPath() {
        return CANDIDATE_PATHS.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未在后端资源目录中找到 application.yaml"));
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue.trim());
        }
        return fallback;
    }

    private String normalizeUsername(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeDisplayName(String value, String fallbackUsername) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallbackUsername : normalized;
    }

    public record ManagerRegistrationConfigSnapshot(
            boolean enabled,
            List<ManagerRegistrationAccountConfig> accounts,
            String source,
            Instant loadedAt
    ) {
    }

    public record ManagerRegistrationAccountConfig(String username, String displayName, boolean enabled) {
    }
}
