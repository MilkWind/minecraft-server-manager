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
        private BootstrapManager bootstrapManager = new BootstrapManager();
        private long sessionTtlHours = 8;
    }

    @Getter
    @Setter
    public static class BootstrapManager {
        private String username = "admin";
        private String displayName = "管理员";
        private String password = "admin123456";
        private String totpSecret = "JBSWY3DPEHPK3PXP";
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
