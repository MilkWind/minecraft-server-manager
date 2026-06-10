package minecraft.milkwind.manager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Auth auth = new Auth();
    private Servers servers = new Servers();

    @Getter
    @Setter
    public static class Auth {
        private ManagerRegistration managerRegistration = new ManagerRegistration();
        private long sessionTtlHours = 8;
    }

    @Getter
    @Setter
    public static class ManagerRegistration {
        private String verificationCode = "";
        private boolean isEnable = false;
    }

    @Getter
    @Setter
    public static class Servers {
        private int pollIntervalSeconds = 8;
        private int listRefreshIntervalSeconds = 15;
        private List<ServerEntry> entries = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ServerEntry {
        private String id;
        private String displayName;
        private String rootDirectory;
        private String jvmArguments;
        private String publicAddress;
        private String gameVersion;
        private boolean chatEnabled = true;
    }
}
