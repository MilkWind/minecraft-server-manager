package minecraft.milkwind.manager.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.AppProperties;
import minecraft.milkwind.manager.server.entity.CustomCommandEntity;
import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.CustomCommandMapper;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

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
        for (AppProperties.ServerEntry entry : appProperties.getServers().getEntries()) {
            ensureServerSeed(entry);
        }

        for (AppProperties.ServerEntry entry : appProperties.getServers().getEntries()) {
            ensureCommandSeed(entry.getId());
        }
    }

    private void ensureServerSeed(AppProperties.ServerEntry entry) {
        ServerConfigEntity entity = serverConfigMapper.selectById(entry.getId());
        if (entity == null) {
            entity = new ServerConfigEntity();
            entity.setServerId(entry.getId());
            entity.setStatus("STOPPED");
            entity.setCreatedAt(TimeSupport.nowIso());
        }

        entity.setDisplayName(entry.getDisplayName());
        entity.setRootDirectory(entry.getRootDirectory());
        entity.setJvmArguments(entry.getJvmArguments());
        entity.setPublicAddress(entry.getPublicAddress());
        entity.setGameVersion(entry.getGameVersion());
        entity.setChatEnabled(entry.isChatEnabled());
        entity.setUpdatedAt(TimeSupport.nowIso());

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(TimeSupport.nowIso());
        }

        if (serverConfigMapper.selectById(entry.getId()) == null) {
            serverConfigMapper.insert(entity);
        } else {
            serverConfigMapper.updateById(entity);
        }
    }

    private void ensureCommandSeed(String serverId) {
        long commandCount = customCommandMapper.selectCount(
                new LambdaQueryWrapper<CustomCommandEntity>().eq(CustomCommandEntity::getServerId, serverId)
        );
        if (commandCount > 0) {
            return;
        }

        insertCommand(serverId, "保存世界", "save-all", "立即触发一次世界保存", "admin");
        insertCommand(serverId, "查询玩家", "list", "查询当前在线玩家列表", "admin");
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
