package minecraft.milkwind.manager.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import minecraft.milkwind.manager.auth.entity.ManagerUserEntity;
import minecraft.milkwind.manager.auth.mapper.ManagerUserMapper;
import minecraft.milkwind.manager.common.time.TimeSupport;
import minecraft.milkwind.manager.config.AppProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ManagerBootstrapService {

    private final ManagerUserMapper managerUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public ManagerBootstrapService(
            ManagerUserMapper managerUserMapper,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties
    ) {
        this.managerUserMapper = managerUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    public void ensureBootstrapManager() {
        long count = managerUserMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            return;
        }

        AppProperties.BootstrapManager bootstrap = appProperties.getAuth().getBootstrapManager();
        String now = TimeSupport.nowIso();

        ManagerUserEntity user = new ManagerUserEntity();
        user.setUsername(bootstrap.getUsername());
        user.setDisplayName(bootstrap.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(bootstrap.getPassword()));
        user.setTotpCode(bootstrap.getTotpCode());
        user.setActive(Boolean.TRUE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        managerUserMapper.insert(user);
    }
}
