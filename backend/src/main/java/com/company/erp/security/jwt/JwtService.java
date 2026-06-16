package com.company.erp.security.jwt;

import com.company.erp.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    /** Minimum key material for HMAC-SHA256 (RFC 7518 §3.2). */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * The throwaway secret shipped for local development (see {@code application-dev.yml}).
     * It is public knowledge, so it is rejected at startup unless a non-production profile
     * is active — preventing a deployment from silently signing tokens with a known key.
     */
    private static final String KNOWN_DEV_SECRET =
            "Y2hhbmdlLW1lLXRoaXMtaXMtYS1kZXYtb25seS1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=";

    /** Profiles under which the well-known dev secret is tolerated. */
    private static final Profiles NON_PRODUCTION = Profiles.of("dev", "local", "test");

    private final SecretKey signingKey;
    private final SecurityProperties.Jwt props;

    public JwtService(SecurityProperties securityProperties, Environment environment) {
        this.props = securityProperties.getJwt();
        this.signingKey = buildSigningKey(props.getSecret(), environment);
    }

    /**
     * Validates and builds the signing key, failing fast (so the context never starts) when the
     * secret is missing, malformed, too weak, or the public dev secret in a production profile.
     */
    private static SecretKey buildSigningKey(String secret, Environment environment) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured. Set the JWT_SECRET environment variable to a "
                            + "Base64-encoded value of at least 256 bits (run the dev profile for a local default).");
        }
        boolean nonProduction = environment.acceptsProfiles(NON_PRODUCTION);
        if (!nonProduction && KNOWN_DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "Refusing to start: the built-in development JWT secret is in use outside a dev/local/test "
                            + "profile. Provide a unique, secret JWT_SECRET for this environment.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT_SECRET must be a valid Base64 string.", ex);
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too weak: decoded length is %d bytes but at least %d (256 bits) are required."
                            .formatted(keyBytes.length, MIN_SECRET_BYTES));
        }
        return Keys.hmacShaKeyFor(keyBytes);
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
