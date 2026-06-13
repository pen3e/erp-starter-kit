package com.company.erp.security.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Blacklist for still-valid access tokens (by {@code jti}) so that "logout" immediately
 * invalidates a token even though JWTs are otherwise stateless. Each entry self-expires
 * exactly when the token would have expired anyway, so the store never grows unbounded.
 */
@Service
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, Instant expiresAt) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return; // already expired; nothing to track
        }
        redis.opsForValue().set(PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    /** Used at password change / reset to mark a logical "revoke everything before now". */
    public void blacklistUserBefore(UUID userId, Instant cutoff, Duration maxTokenTtl) {
        redis.opsForValue().set("jwt:revoke-before:" + userId, String.valueOf(cutoff.getEpochSecond()), maxTokenTtl);
    }

    public Instant revokeBefore(UUID userId) {
        String v = redis.opsForValue().get("jwt:revoke-before:" + userId);
        return v == null ? null : Instant.ofEpochSecond(Long.parseLong(v));
    }
}
