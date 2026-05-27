package minecraft.milkwind.manager.auth.controller;

import minecraft.milkwind.manager.auth.dto.AuthSessionDto;
import minecraft.milkwind.manager.auth.dto.LoginRequest;
import minecraft.milkwind.manager.auth.service.AuthService;
import minecraft.milkwind.manager.common.api.ApiResponse;
import minecraft.milkwind.manager.common.exception.ApiException;
import minecraft.milkwind.manager.security.ManagerPrincipal;
import minecraft.milkwind.manager.server.service.ServerCatalogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ApiResponse<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return ApiResponse.success(null);
    }

    @GetMapping("/session")
    public ApiResponse<AuthSessionDto> session(@AuthenticationPrincipal ManagerPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Current session is not authenticated");
        }
        return ApiResponse.success(authService.currentSession(principal, serverCatalogService.listServerIds()));
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
