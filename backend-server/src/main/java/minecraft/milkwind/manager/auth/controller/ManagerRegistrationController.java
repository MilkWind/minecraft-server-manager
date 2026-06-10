package minecraft.milkwind.manager.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationConfirmRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationQrDto;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationRequest;
import minecraft.milkwind.manager.auth.dto.ManagerRegistrationResultDto;
import minecraft.milkwind.manager.auth.service.ManagerRegistrationService;
import minecraft.milkwind.manager.common.api.ApiResponse;
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

    @PostMapping("/qr")
    public ApiResponse<ManagerRegistrationQrDto> createQr(
            @RequestBody ManagerRegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(managerRegistrationService.createRegistrationQr(request, httpRequest));
    }

    @PostMapping("/confirm")
    public ApiResponse<ManagerRegistrationResultDto> confirm(
            @RequestBody ManagerRegistrationConfirmRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(managerRegistrationService.confirmRegistration(request, httpRequest));
    }
}
