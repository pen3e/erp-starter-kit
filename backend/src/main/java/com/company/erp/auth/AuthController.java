package com.company.erp.auth;

import com.company.erp.auth.dto.ChangePasswordRequest;
import com.company.erp.auth.dto.LoginRequest;
import com.company.erp.auth.dto.LogoutRequest;
import com.company.erp.auth.dto.PasswordResetConfirmRequest;
import com.company.erp.auth.dto.PasswordResetRequest;
import com.company.erp.auth.dto.RefreshRequest;
import com.company.erp.auth.dto.TokenResponse;
import com.company.erp.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token lifecycle and password management")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive an access + refresh token pair")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new token pair (rotation)")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current access token and (optionally) a refresh token")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
                                       @RequestBody(required = false) LogoutRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        String accessToken = (authHeader != null && authHeader.startsWith(BEARER_PREFIX))
                ? authHeader.substring(BEARER_PREFIX.length()).trim() : null;
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken,
                principal != null ? principal.getUserId() : null,
                principal != null ? principal.getEmail() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change the authenticated user's password (revokes existing sessions)")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal AppUserPrincipal principal) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "Request a password reset token (always returns 202, no enumeration)")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password/reset/confirm")
    @Operation(summary = "Complete a password reset using a previously issued token")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }
}
