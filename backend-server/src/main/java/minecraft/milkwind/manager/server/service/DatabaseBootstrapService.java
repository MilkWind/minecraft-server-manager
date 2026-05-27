package minecraft.milkwind.manager.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.AppProperties;
import minecraft.milkwind.manager.server.entity.CustomCommandEntity;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.CustomCommandMapper;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBootstrapService {

    private final ServerConfigMapper serverConfigMapper;
    private final CustomCommandMapper customCommandMapper;
    private final AppProperties appProperties;

    public DatabaseBootstrapService(
            ServerConfigMapper serverConfigMapper,
            CustomCommandMapper customCommandMapper,
            AppProperties appProperties
    ) {
        this.serverConfigMapper = serverConfigMapper;
        this.customCommandMapper = customCommandMapper;
        this.appProperties = appProperties;
    }

    public void ensureSeedData() {
        long serverCount = serverConfigMapper.selectCount(new LambdaQueryWrapper<>());
        if (serverCount == 0) {
            for (AppProperties.ServerEntry entry : appProperties.getServers().getEntries()) {
                ServerConfigEntity entity = new ServerConfigEntity();
                entity.setServerId(entry.getId());
                entity.setDisplayName(entry.getDisplayName());
                entity.setRootDirectory(entry.getRootDirectory());
                entity.setJvmArguments(entry.getJvmArguments());
                entity.setPublicAddress(entry.getPublicAddress());
                entity.setGameVersion(entry.getGameVersion());
                entity.setChatEnabled(entry.isChatEnabled());
                entity.setStatus("STOPPED");
                entity.setCreatedAt(TimeSupport.nowIso());
                entity.setUpdatedAt(TimeSupport.nowIso());
                serverConfigMapper.insert(entity);
            }
        }

        long commandCount = customCommandMapper.selectCount(new LambdaQueryWrapper<>());
        if (commandCount == 0) {
            for (ServerConfigEntity server : serverConfigMapper.selectList(new LambdaQueryWrapper<>())) {
                insertCommand(server.getServerId(), "Save World", "save-all", "Trigger an immediate world save", "admin");
                insertCommand(server.getServerId(), "List Players", "list", "Query the current online player list", "admin");
            }
        }
    }

    private void insertCommand(String serverId, String displayName, String commandText, String description, String createdBy) {
        CustomCommandEntity entity = new CustomCommandEntity();
        entity.setServerId(serverId);
        entity.setDisplayName(displayName);
        entity.setCommandText(commandText);
        entity.setDescription(description);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(TimeSupport.nowIso());
        entity.setUpdatedAt(TimeSupport.nowIso());
        customCommandMapper.insert(entity);
    }
}
