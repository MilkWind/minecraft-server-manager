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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final ManagerUserMapper managerUserMapper;
    private final ManagerSessionMapper managerSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final TotpService totpService;

    public AuthService(
            ManagerUserMapper managerUserMapper,
            ManagerSessionMapper managerSessionMapper,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties,
            TotpService totpService
    ) {
        this.managerUserMapper = managerUserMapper;
        this.managerSessionMapper = managerSessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
        this.totpService = totpService;
    }

    public AuthSessionDto login(LoginRequest request, List<String> allowedServerIds) {
        String username = requireText(request.username(), "username");
        String password = requireText(request.password(), "password");
        String totpCode = requireText(request.totpCode(), "totp_code");
        String targetServerId = requireText(request.serverId(), "server_id");

        if (!allowedServerIds.contains(targetServerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "server_not_allowed", "The selected server is not available");
        }

        ManagerUserEntity user = managerUserMapper.selectOne(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getUsername, username)
        );
        if (user == null || Boolean.FALSE.equals(user.getActive())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid manager credentials");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid manager credentials");
        }

        if (!totpService.verify(user.getTotpCode(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_totp", "Invalid verification code");
        }

        revokeExistingSessions(username);

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(appProperties.getAuth().getSessionTtlHours(), ChronoUnit.HOURS);

        ManagerSessionEntity entity = new ManagerSessionEntity();
        entity.setToken(token);
        entity.setUsername(username);
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

    public AuthSessionDto currentSession(ManagerSession principal, List<String> allowedServerIds) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthenticated", "Manager session is required");
        }

        ManagerSessionEntity entity = managerSessionMapper.selectById(principal.token());
        if (entity == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "session_expired", "Manager session has expired");
        }

        Instant expiresAt = Instant.parse(entity.getExpiresAt());
        if (expiresAt.isBefore(Instant.now())) {
            managerSessionMapper.deleteById(entity.getToken());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "session_expired", "Manager session has expired");
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + field, "Required field is blank: " + field);
        }
        return normalized;
    }
}
