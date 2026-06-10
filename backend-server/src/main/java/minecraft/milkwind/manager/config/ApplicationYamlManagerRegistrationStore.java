package minecraft.milkwind.manager.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ApplicationYamlManagerRegistrationStore {

    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("src", "main", "resources", "application.yaml"),
            Path.of("backend-server", "src", "main", "resources", "application.yaml")
    );

    private final Object lock = new Object();
    private final Yaml yaml;
    private final AppProperties appProperties;
    private Path applicationYamlPath;

    public ApplicationYamlManagerRegistrationStore(AppProperties appProperties) {
        this.appProperties = appProperties;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }

    @PostConstruct
    public void initialize() {
        this.applicationYamlPath = resolveApplicationYamlPath();
        ensureRegistrationConfig();
    }

    public ManagerRegistrationConfig currentConfig() {
        synchronized (lock) {
            return readConfig(loadRoot());
        }
    }

    public ManagerRegistrationConfig ensureRegistrationConfig() {
        synchronized (lock) {
            Map<String, Object> root = loadRoot();
            ManagerRegistrationConfig config = readConfig(root);
            boolean changed = false;

            if (!isValidVerificationCode(config.verificationCode())) {
                config = new ManagerRegistrationConfig(generateVerificationCode(), config.enabled());
                writeConfig(root, config);
                changed = true;
            }

            if (!containsEnabledFlag(root)) {
                writeConfig(root, config);
                changed = true;
            }

            if (changed) {
                saveRoot(root);
            }

            return config;
        }
    }

    private boolean containsEnabledFlag(Map<String, Object> root) {
        return registrationMap(root).containsKey("is-enable");
    }

    private ManagerRegistrationConfig readConfig(Map<String, Object> root) {
        Map<String, Object> registration = registrationMap(root);
        String fallbackVerificationCode = appProperties.getAuth().getManagerRegistration().getVerificationCode();
        boolean fallbackEnabled = appProperties.getAuth().getManagerRegistration().isEnable();
        String verificationCode = normalizeVerificationCode(
                Objects.toString(registration.getOrDefault("verification-code", fallbackVerificationCode), "")
        );
        boolean enabled = parseBoolean(registration.get("is-enable"), fallbackEnabled);
        return new ManagerRegistrationConfig(verificationCode, enabled);
    }

    private void writeConfig(Map<String, Object> root, ManagerRegistrationConfig config) {
        Map<String, Object> registration = registrationMap(root);
        registration.put("verification-code", config.verificationCode());
        registration.put("is-enable", config.enabled());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> registrationMap(Map<String, Object> root) {
        Map<String, Object> app = nestedMap(root, "app");
        Map<String, Object> auth = nestedMap(app, "auth");
        return nestedMap(auth, "manager-registration");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> root, String key) {
        Object current = root.get(key);
        if (current instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        Map<String, Object> created = new LinkedHashMap<>();
        root.put(key, created);
        return created;
    }

    private Map<String, Object> loadRoot() {
        try (InputStream stream = Files.newInputStream(applicationYamlPath)) {
            Object loaded = yaml.load(stream);
            if (loaded instanceof Map<?, ?> map) {
                return castToMap(map);
            }
            return new LinkedHashMap<>();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read application.yaml", exception);
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

    private void saveRoot(Map<String, Object> root) {
        try (Writer writer = Files.newBufferedWriter(applicationYamlPath, StandardCharsets.UTF_8)) {
            yaml.dump(root, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write application.yaml", exception);
        }
    }

    private Path resolveApplicationYamlPath() {
        return CANDIDATE_PATHS.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("application.yaml was not found in the backend resources directory"));
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

    private String normalizeVerificationCode(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidVerificationCode(String value) {
        return value != null && value.matches("\\d{6}");
    }

    private String generateVerificationCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    public record ManagerRegistrationConfig(String verificationCode, boolean enabled) {
    }
}
