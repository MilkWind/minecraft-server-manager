package minecraft.milkwind.manager.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.auth.entity.ManagerSessionEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerSessionMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SessionCleanupService {

    private final ManagerSessionMapper managerSessionMapper;

    public SessionCleanupService(ManagerSessionMapper managerSessionMapper) {
        this.managerSessionMapper = managerSessionMapper;
    }

    @Scheduled(fixedDelay = 300_000)
    public void removeExpiredSessions() {
        List<ManagerSessionEntity> sessions = managerSessionMapper.selectList(new LambdaQueryWrapper<>());
        Instant now = Instant.now();

        for (ManagerSessionEntity session : sessions) {
            if (Instant.parse(session.getExpiresAt()).isBefore(now)) {
                managerSessionMapper.deleteById(session.getToken());
            }
        }
    }
}
