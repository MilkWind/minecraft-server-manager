package minecraft.milkwind.manager.server.runtime;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerRuntimeRegistry {

    private final Map<String, ServerRuntimeState> runtimes = new ConcurrentHashMap<>();

    public ServerRuntimeState getOrCreate(String serverId, String initialStatus) {
        return runtimes.computeIfAbsent(serverId, id -> new ServerRuntimeState(id, initialStatus));
    }

    public ServerRuntimeState get(String serverId) {
        return runtimes.get(serverId);
    }

    public Collection<ServerRuntimeState> getAll() {
        return runtimes.values();
    }
}
