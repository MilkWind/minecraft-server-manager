package minecraft.milkwind.manager.auth.controller;

import minecraft.milkwind.manager.auth.dto.ManagerRegistrationConfirmRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationQrDto;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationResultDto;
import minecraft.milkwind.manager.auth.service.ManagerRegistrationService;
import minecraft.milkwind.manager.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/manager-registration")
public class ManagerRegistrationController {

    private final ManagerRegistrationService managerRegistrationService;

    public ManagerRegistrationController(ManagerRegistrationService managerRegistrationService) {
        this.managerRegistrationService = managerRegistrationService;
    }

    @PostMapping("/{username}/qr")
    public ApiResponse<ManagerRegistrationQrDto> createQr(
            @PathVariable String username
    ) {
        return ApiResponse.success(managerRegistrationService.createRegistrationQr(username));
    }

    @PostMapping("/{username}/confirm")
    public ApiResponse<ManagerRegistrationResultDto> confirm(
            @PathVariable String username,
            @RequestBody ManagerRegistrationConfirmRequest request
    ) {
        return ApiResponse.success(managerRegistrationService.confirmRegistration(username, request));
    }
}
