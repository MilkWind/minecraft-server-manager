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
import minecraft.milkwind.manager.security.ManagerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
public class AuthService {

    private static final long SESSION_TTL_SECONDS = 8L * 60L * 60L;

    private final ManagerUserMapper managerUserMapper;
    private final ManagerSessionMapper managerSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            ManagerUserMapper managerUserMapper,
            ManagerSessionMapper managerSessionMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.managerUserMapper = managerUserMapper;
        this.managerSessionMapper = managerSessionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthSessionDto login(LoginRequest request, Collection<String> accessibleServerIds) {
        String username = normalize(request.username());
        String password = request.password();
        String totpCode = normalize(request.totpCode());

        ManagerUserEntity user = managerUserMapper.selectOne(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getUsername, username)
        );

        if (user == null
                || !Boolean.TRUE.equals(user.getActive())
                || password == null
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "auth_failed", "Username or password is incorrect");
        }

        if (!user.getTotpCode().equals(totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_totp", "Two-factor verification code is incorrect");
        }

        managerSessionMapper.delete(
                new LambdaQueryWrapper<ManagerSessionEntity>().eq(ManagerSessionEntity::getUsername, username)
        );

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(SESSION_TTL_SECONDS);
        String token = issueToken();

        ManagerSessionEntity entity = new ManagerSessionEntity();
        entity.setToken(token);
        entity.setUsername(user.getUsername());
        entity.setDisplayName(user.getDisplayName());
        entity.setCreatedAt(now.toString());
        entity.setLastSeenAt(now.toString());
        entity.setExpiresAt(expiresAt.toString());
        managerSessionMapper.insert(entity);

        return toSessionDto(
                new ManagerSession(token, user.getUsername(), user.getDisplayName(), now, now, expiresAt),
                accessibleServerIds
        );
    }

    public ManagerPrincipal authenticate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        ManagerSessionEntity entity = managerSessionMapper.selectById(token);
        if (entity == null) {
            return null;
        }

        Instant now = Instant.now();
        if (Instant.parse(entity.getExpiresAt()).isBefore(now)) {
            managerSessionMapper.deleteById(token);
            return null;
        }

        entity.setLastSeenAt(now.toString());
        managerSessionMapper.updateById(entity);

        return new ManagerPrincipal(entity.getUsername(), entity.getDisplayName(), entity.getToken());
    }

    public AuthSessionDto currentSession(ManagerPrincipal principal, Collection<String> accessibleServerIds) {
        ManagerSessionEntity entity = managerSessionMapper.selectById(principal.token());
        if (entity == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "session_missing", "Current session is no longer valid");
        }

        return toSessionDto(
                new ManagerSession(
                        entity.getToken(),
                        entity.getUsername(),
                        entity.getDisplayName(),
                        Instant.parse(entity.getCreatedAt()),
                        Instant.parse(entity.getLastSeenAt()),
                        Instant.parse(entity.getExpiresAt())
                ),
                accessibleServerIds
        );
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        managerSessionMapper.deleteById(token);
    }

    private AuthSessionDto toSessionDto(ManagerSession session, Collection<String> accessibleServerIds) {
        return new AuthSessionDto(
                session.token(),
                session.username(),
                session.displayName(),
                session.createdAt(),
                session.expiresAt(),
                List.copyOf(accessibleServerIds)
        );
    }

    private String issueToken() {
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
