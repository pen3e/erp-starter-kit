package com.company.erp.security.jwt;

import com.company.erp.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies signed JWTs (HMAC-SHA256).
 *
 * <p>Access tokens are short-lived (15 min) and self-contained: they embed the user's
 * authorities so request authorization needs no DB round-trip. Refresh tokens are long-lived
 * (7d, or 30d for "remember me"), carry no authorities, and are tracked server-side in Redis
 * for rotation and revocation. Every token has a {@code jti} so it can be individually
 * blacklisted on logout.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TENANT = "tenant";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_REMEMBER = "rmb";

    private final SecretKey signingKey;
    private final SecurityProperties.Jwt props;

    public JwtService(SecurityProperties securityProperties) {
        this.props = securityProperties.getJwt();
        byte[] keyBytes = Decoders.BASE64.decode(props.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public IssuedToken generateAccessToken(UUID userId, String email, String tenantId,
                                           List<String> authorities) {
        return build(userId, email, tenantId, TokenType.ACCESS, authorities, props.getAccessTokenTtl(), false);
    }

    public IssuedToken generateRefreshToken(UUID userId, String email, String tenantId, boolean rememberMe) {
        Duration ttl = rememberMe ? props.getRememberMeTtl() : props.getRefreshTokenTtl();
        return build(userId, email, tenantId, TokenType.REFRESH, List.of(), ttl, rememberMe);
    }

    private IssuedToken build(UUID userId, String email, String tenantId, TokenType type,
                              List<String> authorities, Duration ttl, boolean rememberMe) {
        Instant now = Instant.now();
        Instant expiry = now.plus(ttl);
        String jti = UUID.randomUUID().toString();

        var builder = Jwts.builder()
                .id(jti)
                .issuer(props.getIssuer())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TENANT, tenantId)
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (type == TokenType.ACCESS) {
            builder.claim(CLAIM_AUTHORITIES, authorities);
        } else {
            builder.claim(CLAIM_REMEMBER, rememberMe);
        }

        String token = builder.signWith(signingKey).compact();
        return new IssuedToken(token, jti, expiry);
    }

    /**
     * Verifies signature + expiry and returns the parsed claims, or throws
     * {@link JwtException} on any problem (tampered, expired, malformed).
     */
    public JwtPayload parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token);
        Claims c = jws.getPayload();

        @SuppressWarnings("unchecked")
        List<String> authorities = c.get(CLAIM_AUTHORITIES, List.class);
        Boolean rememberMe = c.get(CLAIM_REMEMBER, Boolean.class);
        return new JwtPayload(
                c.getId(),
                UUID.fromString(c.getSubject()),
                c.get(CLAIM_EMAIL, String.class),
                c.get(CLAIM_TENANT, String.class),
                TokenType.valueOf(c.get(CLAIM_TYPE, String.class)),
                authorities == null ? List.of() : authorities,
                Boolean.TRUE.equals(rememberMe),
                c.getIssuedAt().toInstant(),
                c.getExpiration().toInstant());
    }

    /** Issued token plus the metadata callers need (jti for blacklist, expiry for cookie/ttl). */
    public record IssuedToken(String token, String jti, Instant expiresAt) {
    }

    public record JwtPayload(
            String jti,
            UUID userId,
            String email,
            String tenantId,
            TokenType type,
            List<String> authorities,
            boolean rememberMe,
            Instant issuedAt,
            Instant expiresAt) {
    }
}
