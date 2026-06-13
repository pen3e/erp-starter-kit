package com.company.erp.auth;

import com.company.erp.audit.AuditAction;
import com.company.erp.audit.AuditService;
import com.company.erp.auth.dto.ChangePasswordRequest;
import com.company.erp.auth.dto.LoginRequest;
import com.company.erp.auth.dto.PasswordResetConfirmRequest;
import com.company.erp.auth.dto.PasswordResetRequest;
import com.company.erp.auth.dto.RefreshRequest;
import com.company.erp.auth.dto.TokenResponse;
import com.company.erp.common.exception.AuthenticationFailedException;
import com.company.erp.common.exception.ResourceNotFoundException;
import com.company.erp.common.util.InputSanitizer;
import com.company.erp.config.SecurityProperties;
import com.company.erp.security.AppUserPrincipal;
import com.company.erp.security.jwt.JwtService;
import com.company.erp.security.jwt.JwtService.IssuedToken;
import com.company.erp.security.jwt.JwtService.JwtPayload;
import com.company.erp.security.jwt.TokenType;
import com.company.erp.security.token.RefreshTokenStore;
import com.company.erp.security.token.TokenBlacklistService;
import com.company.erp.tenant.TenantContext;
import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;
import com.company.erp.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates authentication: login, token refresh (with rotation + reuse detection),
 * logout, password change and password reset. Every outcome is audited; client-facing
 * errors are deliberately generic to avoid user enumeration.
 */
@Slf4j
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklist;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final AuditService audit;
    private final SecurityProperties securityProperties;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       JwtService jwtService, RefreshTokenStore refreshTokenStore,
                       TokenBlacklistService tokenBlacklist, PasswordEncoder passwordEncoder,
                       LoginAttemptService loginAttemptService, PasswordResetTokenStore passwordResetTokenStore,
                       AuditService audit, SecurityProperties securityProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenBlacklist = tokenBlacklist;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetTokenStore = passwordResetTokenStore;
        this.audit = audit;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = InputSanitizer.normalizeEmail(request.email());
        Optional<User> userOpt = userRepository.findByEmail(email);
        UUID userId = userOpt.map(User::getId).orElse(null);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();

            userOpt.ifPresent(loginAttemptService::onSuccess);
            TokenResponse tokens = issueTokens(principal, request.rememberMe());
            audit.success(AuditAction.LOGIN_SUCCESS, principal.getUserId(), principal.getEmail(), null);
            return tokens;

        } catch (LockedException ex) {
            audit.failure(AuditAction.LOGIN_BLOCKED, userId, email, "account temporarily locked");
            throw new AuthenticationFailedException();
        } catch (DisabledException ex) {
            audit.failure(AuditAction.LOGIN_FAILED, userId, email, "account disabled");
            throw new AuthenticationFailedException();
        } catch (BadCredentialsException ex) {
            userOpt.ifPresent(loginAttemptService::onFailure);
            audit.failure(AuditAction.LOGIN_FAILED, userId, email, "invalid credentials");
            throw new AuthenticationFailedException();
        }
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        JwtPayload payload;
        try {
            payload = jwtService.parse(request.refreshToken());
        } catch (Exception ex) {
            throw new AuthenticationFailedException("Invalid refresh token");
        }
        if (payload.type() != TokenType.REFRESH) {
            throw new AuthenticationFailedException();
        }

        // Scope all subsequent lookups to the tenant named in the verified token.
        TenantContext.set(payload.tenantId());

        if (!refreshTokenStore.isValid(payload.userId(), payload.jti())) {
            // Unknown / already-rotated token => probable theft. Revoke the whole family.
            refreshTokenStore.revokeAll(payload.userId());
            audit.failure(AuditAction.TOKEN_REFRESH, payload.userId(), payload.email(),
                    "invalid or reused refresh token");
            throw new AuthenticationFailedException();
        }

        User user = userRepository.findWithRolesById(payload.userId())
                .orElseThrow(AuthenticationFailedException::new);
        if (user.getStatus() != UserStatus.ACTIVE || user.isLocked()) {
            refreshTokenStore.revoke(payload.userId(), payload.jti());
            throw new AuthenticationFailedException();
        }

        // Rotation: invalidate the presented token, issue a fresh pair.
        refreshTokenStore.revoke(payload.userId(), payload.jti());
        TokenResponse tokens = issueTokens(new AppUserPrincipal(user), payload.rememberMe());
        audit.success(AuditAction.TOKEN_REFRESH, user.getId(), user.getEmail(), null);
        return tokens;
    }

    /**
     * Logout: blacklist the still-valid access token (so it stops working immediately) and
     * revoke the supplied refresh token. {@code accessToken} is the raw bearer credential.
     */
    public void logout(String accessToken, String refreshToken, UUID actorId, String actorEmail) {
        if (accessToken != null) {
            try {
                JwtPayload access = jwtService.parse(accessToken);
                tokenBlacklist.blacklist(access.jti(), access.expiresAt());
            } catch (Exception ignored) {
                // already invalid; nothing to blacklist
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                JwtPayload refresh = jwtService.parse(refreshToken);
                refreshTokenStore.revoke(refresh.userId(), refresh.jti());
            } catch (Exception ignored) {
                // ignore malformed refresh token on logout
            }
        }
        audit.success(AuditAction.LOGOUT, actorId, actorEmail, null);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            audit.failure(AuditAction.PASSWORD_CHANGE, userId, user.getEmail(), "current password mismatch");
            throw new AuthenticationFailedException("Current password is incorrect");
        }
        applyNewPassword(user, request.newPassword());
        audit.success(AuditAction.PASSWORD_CHANGE, userId, user.getEmail(), null);
    }

    /** Always responds the same way whether or not the email exists (anti-enumeration). */
    @Transactional(readOnly = true)
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = InputSanitizer.normalizeEmail(request.email());
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = passwordResetTokenStore.issue(user.getId(), user.getTenantId());
            // Delivery is out of scope of the starter: wire this to your mailer.
            log.info("Password reset requested for {} (tenant={}). Reset token generated.",
                    user.getEmail(), user.getTenantId());
            audit.record(AuditAction.PASSWORD_RESET_REQUEST, "SUCCESS", user.getId(), user.getEmail(),
                    null, null, null, null, "reset token issued");
            // NOTE: never return the token in the API response.
            if (log.isDebugEnabled()) {
                log.debug("DEV-ONLY reset token for {}: {}", user.getEmail(), token);
            }
        });
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetTokenStore.ResetTarget target = passwordResetTokenStore.consume(request.token())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired reset token"));

        TenantContext.set(target.tenantId());
        User user = userRepository.findById(target.userId())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid or expired reset token"));

        applyNewPassword(user, request.newPassword());
        audit.success(AuditAction.PASSWORD_RESET, user.getId(), user.getEmail(), null);
    }

    // ----------------------------------------------------------------------

    private void applyNewPassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Invalidate all existing sessions: drop refresh tokens and reject older access tokens.
        refreshTokenStore.revokeAll(user.getId());
        Duration maxTtl = securityProperties.getJwt().getRememberMeTtl();
        tokenBlacklist.blacklistUserBefore(user.getId(), Instant.now(), maxTtl);
    }

    private TokenResponse issueTokens(AppUserPrincipal principal, boolean rememberMe) {
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        IssuedToken access = jwtService.generateAccessToken(
                principal.getUserId(), principal.getEmail(), principal.getTenantId(), authorities);
        IssuedToken refresh = jwtService.generateRefreshToken(
                principal.getUserId(), principal.getEmail(), principal.getTenantId(), rememberMe);
        refreshTokenStore.store(principal.getUserId(), refresh.jti(), refresh.expiresAt());

        long expiresIn = securityProperties.getJwt().getAccessTokenTtl().toSeconds();
        return TokenResponse.bearer(access.token(), refresh.token(), expiresIn);
    }
}
