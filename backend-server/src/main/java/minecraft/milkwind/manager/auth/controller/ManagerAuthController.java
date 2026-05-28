package minecraft.milkwind.manager.auth.controller;

import minecraft.milkwind.manager.auth.dto.AuthSessionDto;
import minecraft.milkwind.manager.auth.dto.LoginRequest;
import minecraft.milkwind.manager.auth.model.ManagerSession;
import minecraft.milkwind.manager.auth.service.AuthService;
import minecraft.milkwind.manager.common.api.ApiResponse;
import minecraft.milkwind.manager.server.service.ServerCatalogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/auth")
public class ManagerAuthController {

    private final AuthService authService;
    private final ServerCatalogService serverCatalogService;

    public ManagerAuthController(AuthService authService, ServerCatalogService serverCatalogService) {
        this.authService = authService;
        this.serverCatalogService = serverCatalogService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthSessionDto> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request, serverCatalogService.listServerIds()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal ManagerSession principal) {
        authService.logout(principal);
        return ApiResponse.success(null);
    }

    @GetMapping("/session")
    public ApiResponse<AuthSessionDto> session(@AuthenticationPrincipal ManagerSession principal) {
        return ApiResponse.success(authService.currentSession(principal, serverCatalogService.listServerIds()));
    }
}
