package minecraft.milkwind.manager.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.auth.dto.AuthSessionDto;
import minecraft.milkwind.manager.auth.dto.LoginRequest;
import minecraft.milkwind.manager.auth.entity.ManagerSessionEntity;
import minecraft.milkwind.manager.auth.entity.ManagerUserEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerSessionMapper;
import minecraft.milkwind.manager.auth.mapper.ManagerUserMapper;
import minecraft.milkwind.manager.auth.model.ManagerSession;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final ManagerUserMapper managerUserMapper;
    private final ManagerSessionMapper managerSessionMapper;
    private final AppProperties appProperties;
    private final TotpService totpService;

    public AuthService(
            ManagerUserMapper managerUserMapper,
            ManagerSessionMapper managerSessionMapper,
            AppProperties appProperties,
            TotpService totpService
    ) {
        this.managerUserMapper = managerUserMapper;
        this.managerSessionMapper = managerSessionMapper;
        this.appProperties = appProperties;
        this.totpService = totpService;
    }

    public AuthSessionDto login(LoginRequest request, List<String> allowedServerIds) {
        String totpCode = requireText(request.totpCode(), "totp_code");

        ManagerUserEntity user = resolveActiveManagerByTotp(totpCode);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_totp", "验证码无效");
        }

        revokeExistingSessions(user.getUsername());

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(appProperties.getAuth().getSessionTtlHours(), ChronoUnit.HOURS);

        ManagerSessionEntity entity = new ManagerSessionEntity();
        entity.setToken(token);
        entity.setUsername(user.getUsername());
        entity.setDisplayName(user.getDisplayName());
        entity.setCreatedAt(TimeSupport.nowIso());
        entity.setLastSeenAt(TimeSupport.nowIso());
        entity.setExpiresAt(expiresAt.toString());
        managerSessionMapper.insert(entity);

        return new AuthSessionDto(
                token,
                user.getUsername(),
                user.getDisplayName(),
                expiresAt,
                allowedServerIds
        );
    }

    private ManagerUserEntity resolveActiveManagerByTotp(String totpCode) {
        List<ManagerUserEntity> activeUsers = managerUserMapper.selectList(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getActive, Boolean.TRUE)
        );
        List<ManagerUserEntity> matches = new ArrayList<>();
        for (ManagerUserEntity user : activeUsers) {
            if (totpService.verify(user.getTotpCode(), totpCode)) {
                matches.add(user);
            }
        }

        if (matches.size() > 1) {
            throw new ApiException(HttpStatus.CONFLICT, "ambiguous_totp", "验证码匹配到多个管理员");
        }

        return matches.isEmpty() ? null : matches.get(0);
    }

    public AuthSessionDto currentSession(ManagerSession principal, List<String> allowedServerIds) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthenticated", "需要管理员会话");
        }

        ManagerSessionEntity entity = managerSessionMapper.selectById(principal.token());
        if (entity == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "session_expired", "管理员会话已过期");
        }

        Instant expiresAt = Instant.parse(entity.getExpiresAt());
        if (expiresAt.isBefore(Instant.now())) {
            managerSessionMapper.deleteById(entity.getToken());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "session_expired", "管理员会话已过期");
        }

        entity.setLastSeenAt(TimeSupport.nowIso());
        managerSessionMapper.updateById(entity);

        return new AuthSessionDto(
                entity.getToken(),
                entity.getUsername(),
                entity.getDisplayName(),
                expiresAt,
                allowedServerIds
        );
    }

    public ManagerSession authenticate(String token) {
        String normalizedToken = requireText(token, "token");
        ManagerSessionEntity entity = managerSessionMapper.selectById(normalizedToken);
        if (entity == null) {
            return null;
        }

        Instant expiresAt = Instant.parse(entity.getExpiresAt());
        if (expiresAt.isBefore(Instant.now())) {
            managerSessionMapper.deleteById(entity.getToken());
            return null;
        }

        Instant createdAt = Instant.parse(entity.getCreatedAt());
        Instant lastSeenAt = Instant.parse(entity.getLastSeenAt());
        return new ManagerSession(
                entity.getToken(),
                entity.getUsername(),
                entity.getDisplayName(),
                createdAt,
                lastSeenAt,
                expiresAt
        );
    }

    public void logout(ManagerSession principal) {
        if (principal == null) {
            return;
        }

        managerSessionMapper.deleteById(principal.token());
    }

    private void revokeExistingSessions(String username) {
        managerSessionMapper.delete(
                new LambdaQueryWrapper<ManagerSessionEntity>().eq(ManagerSessionEntity::getUsername, username)
        );
    }

    private String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + field, "必填字段为空：" + field);
        }
        return normalized;
    }
}
