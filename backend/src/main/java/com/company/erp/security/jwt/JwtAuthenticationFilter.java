package com.company.erp.security.jwt;

import com.company.erp.security.AppUserPrincipal;
import com.company.erp.security.token.TokenBlacklistService;
import com.company.erp.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Authenticates requests bearing a valid {@code Authorization: Bearer <access-token>} header.
 *
 * <p>The token is verified, checked against the blacklist and the per-user "revoke before"
 * watermark, and—crucially—its {@code tenant} claim becomes the authoritative
 * {@link TenantContext} value (overriding any client-supplied {@code X-Tenant-ID} header).
 * Authorities are taken straight from the token, so no database access happens here.
 * Any failure leaves the context unauthenticated; the entry point then returns 401.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService blacklist) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            JwtService.JwtPayload payload = jwtService.parse(token);

            if (payload.type() != TokenType.ACCESS) {
                return; // refresh tokens are not accepted for API access
            }
            if (blacklist.isBlacklisted(payload.jti())) {
                return;
            }
            Instant revokeBefore = blacklist.revokeBefore(payload.userId());
            if (revokeBefore != null && payload.issuedAt().isBefore(revokeBefore)) {
                // Token issued before a global revocation (e.g. password change) — reject.
                return;
            }

            // Tenant from the verified token is authoritative for the rest of the request.
            TenantContext.set(payload.tenantId());

            AppUserPrincipal principal = new AppUserPrincipal(
                    payload.userId(), payload.email(), payload.tenantId(), payload.authorities());
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            // Malformed / expired / tampered token: stay anonymous, let entry point answer 401.
            log.debug("JWT authentication skipped: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
