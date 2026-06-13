package minecraft.milkwind.manager.auth.service;

import minecraft.milkwind.manager.auth.dto.AuthSessionDto;
import minecraft.milkwind.manager.auth.dto.LoginRequest;
import minecraft.milkwind.manager.auth.entity.ManagerSessionEntity;
import minecraft.milkwind.manager.auth.entity.ManagerUserEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerSessionMapper;
import minecraft.milkwind.manager.auth.mapper.ManagerUserMapper;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final ManagerUserMapper managerUserMapper = mock(ManagerUserMapper.class);
    private final ManagerSessionMapper managerSessionMapper = mock(ManagerSessionMapper.class);
    private final TotpService totpService = mock(TotpService.class);
    private final AppProperties appProperties = new AppProperties();
    private final AuthService authService = new AuthService(
            managerUserMapper,
            managerSessionMapper,
            appProperties,
            totpService
    );

    @Test
    void loginUsesOnlyTotpCodeForActiveManager() {
        ManagerUserEntity user = manager("owner", "服主", "secret-owner");
        when(managerUserMapper.selectList(any())).thenReturn(List.of(user));
        when(totpService.verify("secret-owner", "123456")).thenReturn(true);

        AuthSessionDto session = authService.login(new LoginRequest("123456"), List.of("MilkWind"));

        assertThat(session.username()).isEqualTo("owner");
        assertThat(session.displayName()).isEqualTo("服主");
        assertThat(session.allowedServerIds()).containsExactly("MilkWind");
        verify(managerSessionMapper).insert(any(ManagerSessionEntity.class));
    }

    @Test
    void loginRejectsAmbiguousTotpCode() {
        ManagerUserEntity owner = manager("owner", "服主", "secret-owner");
        ManagerUserEntity admin = manager("admin", "管理员", "secret-admin");
        when(managerUserMapper.selectList(any())).thenReturn(List.of(owner, admin));
        when(totpService.verify("secret-owner", "123456")).thenReturn(true);
        when(totpService.verify("secret-admin", "123456")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("123456"), List.of("MilkWind")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("ambiguous_totp");
    }

    private ManagerUserEntity manager(String username, String displayName, String secret) {
        ManagerUserEntity user = new ManagerUserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setTotpCode(secret);
        user.setActive(Boolean.TRUE);
        return user;
    }
}
