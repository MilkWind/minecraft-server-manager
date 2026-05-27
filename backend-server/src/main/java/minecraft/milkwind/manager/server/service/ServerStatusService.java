package minecraft.milkwind.manager.server.service;

import minecraft.milkwind.manager.server.entity.ServerConfigEntity;
import minecraft.milkwind.manager.server.mapper.ServerConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class ServerStatusService {

    private final ServerConfigMapper serverConfigMapper;

    public ServerStatusService(ServerConfigMapper serverConfigMapper) {
        this.serverConfigMapper = serverConfigMapper;
    }

    public void updateStatus(String serverId, String status) {
        ServerConfigEntity entity = serverConfigMapper.selectById(serverId);
        if (entity == null) {
            return;
        }

        entity.setStatus(status);
        serverConfigMapper.updateById(entity);
    }
}
