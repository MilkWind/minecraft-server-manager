package minecraft.milkwind.manager.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationConfirmRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationQrDto;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationResultDto;
import minecraft.milkwind.manager.auth.entity.ManagerUserEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerUserMapper;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.ApplicationYamlManagerRegistrationStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ManagerRegistrationService {

    private static final String QR_ISSUER = "Minecraft Server Manager";

    private final ManagerUserMapper managerUserMapper;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;
    private final ApplicationYamlManagerRegistrationStore registrationStore;

    public ManagerRegistrationService(
            ManagerUserMapper managerUserMapper,
            TotpService totpService,
            QrCodeService qrCodeService,
            ApplicationYamlManagerRegistrationStore registrationStore
    ) {
        this.managerUserMapper = managerUserMapper;
        this.totpService = totpService;
        this.qrCodeService = qrCodeService;
        this.registrationStore = registrationStore;
    }

    public ManagerRegistrationQrDto createRegistrationQr(String username) {
        ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig account = requireEnabledAccount(username);
        ManagerUserEntity user = upsertPendingUser(account);
        String otpauthUri = totpService.buildProvisioningUri(QR_ISSUER, account.username(), user.getTotpCode());

        return new ManagerRegistrationQrDto(
                account.username(),
                account.displayName(),
                qrCodeService.generateSvgDataUri(otpauthUri),
                user.getTotpCode(),
                otpauthUri
        );
    }

    public ManagerRegistrationResultDto confirmRegistration(String username, ManagerRegistrationConfirmRequest request) {
        ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig account = requireEnabledAccount(username);
        String totpCode = requireText(request.totpCode(), "totp_code");

        ManagerUserEntity user = managerUserMapper.selectOne(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getUsername, account.username())
        );
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "registration_not_found", "管理员注册记录不存在");
        }
        if (Boolean.TRUE.equals(user.getActive())) {
            return new ManagerRegistrationResultDto(user.getUsername(), user.getDisplayName(), "管理员账号已经完成绑定。");
        }
        if (!totpService.verify(user.getTotpCode(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_totp", "验证码无效");
        }

        user.setDisplayName(account.displayName());
        user.setActive(Boolean.TRUE);
        user.setUpdatedAt(TimeSupport.nowIso());
        managerUserMapper.updateById(user);

        return new ManagerRegistrationResultDto(
                user.getUsername(),
                user.getDisplayName(),
                "管理员 2FA 注册完成，现在可以在管理台输入动态码登录。"
        );
    }

    private ManagerUserEntity upsertPendingUser(
            ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig account
    ) {
        ManagerUserEntity existing = managerUserMapper.selectOne(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getUsername, account.username())
        );
        String now = TimeSupport.nowIso();
        String secret = totpService.generateSecret();

        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new ApiException(HttpStatus.CONFLICT, "manager_exists", "管理员用户名已注册");
            }

            existing.setDisplayName(account.displayName());
            existing.setPasswordHash("");
            existing.setTotpCode(secret);
            existing.setUpdatedAt(now);
            existing.setActive(Boolean.FALSE);
            managerUserMapper.updateById(existing);
            return existing;
        }

        ManagerUserEntity user = new ManagerUserEntity();
        user.setUsername(account.username());
        user.setDisplayName(account.displayName());
        user.setPasswordHash("");
        user.setTotpCode(secret);
        user.setActive(Boolean.FALSE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        managerUserMapper.insert(user);
        return user;
    }

    private ApplicationYamlManagerRegistrationStore.ManagerRegistrationAccountConfig requireEnabledAccount(String username) {
        return registrationStore.findEnabledAccount(username)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        "registration_route_disabled",
                        "管理员注册链接已禁用或不存在"
                ));
    }

    private String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + field, "必填字段为空：" + field);
        }
        return normalized;
    }
}
