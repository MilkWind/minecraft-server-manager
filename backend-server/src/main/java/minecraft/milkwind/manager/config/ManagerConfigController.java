package minecraft.milkwind.manager.config;

import minecraft.milkwind.manager.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/config")
public class ManagerConfigController {

    private final ApplicationYamlManagerRegistrationStore registrationStore;

    public ManagerConfigController(ApplicationYamlManagerRegistrationStore registrationStore) {
        this.registrationStore = registrationStore;
    }

    @PostMapping("/registration/reload")
    public ApiResponse<ManagerRegistrationConfigReloadDto> reloadRegistrationConfig() {
        return ApiResponse.success(ManagerRegistrationConfigReloadDto.from(registrationStore.reload()));
    }
}
