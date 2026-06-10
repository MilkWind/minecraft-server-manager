package minecraft.milkwind.manager.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationConfirmRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationQrDto;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationResultDto;
import minecraft.milkwind.manager.auth.entity.ManagerUserEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerUserMapper;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.ApplicationYamlManagerRegistrationStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ManagerRegistrationService {

    private static final String REGISTRATION_PATH_SEGMENT = "manager-register";
    private static final Pattern REGISTRATION_PATH_PATTERN = Pattern.compile("^/" + REGISTRATION_PATH_SEGMENT + "/(\\d{6})/?$");
    private static final String QR_ISSUER = "Minecraft Server Manager";

    private final ManagerUserMapper managerUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;
    private final ApplicationYamlManagerRegistrationStore registrationStore;

    public ManagerRegistrationService(
            ManagerUserMapper managerUserMapper,
            PasswordEncoder passwordEncoder,
            TotpService totpService,
            QrCodeService qrCodeService,
            ApplicationYamlManagerRegistrationStore registrationStore
    ) {
        this.managerUserMapper = managerUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.qrCodeService = qrCodeService;
        this.registrationStore = registrationStore;
    }

    @PostConstruct
    public void initialize() {
        registrationStore.ensureRegistrationConfig();
    }

    public ManagerRegistrationQrDto createRegistrationQr(ManagerRegistrationRequest request, HttpServletRequest httpRequest) {
        validateRegistrationAccess(httpRequest);

        String username = requireText(request.username(), "username");
        String displayName = requireText(request.displayName(), "display_name");
        String password = requireText(request.password(), "password");
        if (password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "weak_password", "Password must be at least 8 characters long");
        }

        ManagerUserEntity user = upsertPendingUser(username, displayName, password);
        String otpauthUri = totpService.buildProvisioningUri(QR_ISSUER, username, user.getTotpCode());

        return new ManagerRegistrationQrDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                qrCodeService.generateSvgDataUri(otpauthUri),
                user.getTotpCode(),
                otpauthUri
        );
    }

    public ManagerRegistrationResultDto confirmRegistration(ManagerRegistrationConfirmRequest request, HttpServletRequest httpRequest) {
        validateRegistrationAccess(httpRequest);

        String registrationId = requireText(request.registrationId(), "registration_id");
        String totpCode = requireText(request.totpCode(), "totp_code");

        ManagerUserEntity user = managerUserMapper.selectById(registrationId);
        if (user == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "registration_not_found", "The manager registration record does not exist");
        }
        if (Boolean.TRUE.equals(user.getActive())) {
            return new ManagerRegistrationResultDto(user.getUsername(), user.getDisplayName(), "Manager registration is already active");
        }
        if (!totpService.verify(user.getTotpCode(), totpCode)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_totp", "Invalid verification code");
        }

        user.setActive(Boolean.TRUE);
        user.setUpdatedAt(TimeSupport.nowIso());
        managerUserMapper.updateById(user);

        return new ManagerRegistrationResultDto(
                user.getUsername(),
                user.getDisplayName(),
                "Manager registration completed. You can now sign in from the manager console."
        );
    }

    private ManagerUserEntity upsertPendingUser(String username, String displayName, String password) {
        ManagerUserEntity existing = managerUserMapper.selectOne(
                new LambdaQueryWrapper<ManagerUserEntity>().eq(ManagerUserEntity::getUsername, username)
        );
        String now = TimeSupport.nowIso();
        String secret = totpService.generateSecret();

        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new ApiException(HttpStatus.CONFLICT, "manager_exists", "The manager username is already registered");
            }

            existing.setDisplayName(displayName);
            existing.setPasswordHash(passwordEncoder.encode(password));
            existing.setTotpCode(secret);
            existing.setUpdatedAt(now);
            existing.setActive(Boolean.FALSE);
            managerUserMapper.updateById(existing);
            return existing;
        }

        ManagerUserEntity user = new ManagerUserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTotpCode(secret);
        user.setActive(Boolean.FALSE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        managerUserMapper.insert(user);
        return user;
    }

    private void validateRegistrationAccess(HttpServletRequest request) {
        ApplicationYamlManagerRegistrationStore.ManagerRegistrationConfig config = registrationStore.currentConfig();
        if (!config.enabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "registration_route_disabled", "The manager registration link is disabled");
        }

        String routeVerificationCode = extractVerificationCode(request);
        if (!routeVerificationCode.equals(config.verificationCode())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_registration_route", "The manager registration link is invalid");
        }
    }

    private String extractVerificationCode(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "missing_registration_route", "A valid manager registration page must open this request");
        }

        try {
            Matcher matcher = REGISTRATION_PATH_PATTERN.matcher(URI.create(referer.trim()).getPath());
            if (!matcher.matches()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "invalid_registration_route", "The manager registration link is invalid");
            }
            return matcher.group(1);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_registration_route", "The manager registration link is invalid");
        }
    }

    private String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + field, "Required field is blank: " + field);
        }
        return normalized;
    }
}
