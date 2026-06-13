package com.company.erp.security.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Server-side registry of valid refresh tokens, enabling rotation and revocation.
 *
 * <p>Per user we keep a Redis hash {@code refresh:user:{userId}} mapping {@code jti ->
 * expiryEpochSeconds}. A refresh token is only accepted if its {@code jti} is present and
 * unexpired. Rotation atomically removes the presented {@code jti} and registers the new
 * one, so a stolen-and-replayed refresh token is rejected (and signals possible theft).
 */
@Service
public class RefreshTokenStore {

    private static final String KEY = "refresh:user:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void store(UUID userId, String jti, Instant expiresAt) {
        String key = KEY + userId;
        redis.opsForHash().put(key, jti, String.valueOf(expiresAt.getEpochSecond()));
        // Keep the hash alive at least as long as the longest-lived token within it.
        redis.expire(key, Duration.between(Instant.now(), expiresAt).plusDays(1));
    }

    public boolean isValid(UUID userId, String jti) {
        Object raw = redis.opsForHash().get(KEY + userId, jti);
        if (raw == null) {
            return false;
        }
        long expiry = Long.parseLong(raw.toString());
        if (Instant.now().getEpochSecond() > expiry) {
            redis.opsForHash().delete(KEY + userId, jti); // lazy cleanup
            return false;
        }
        return true;
    }

    /** Atomically invalidate one refresh token (used during rotation and logout). */
    public void revoke(UUID userId, String jti) {
        redis.opsForHash().delete(KEY + userId, jti);
    }

    /** Invalidate every refresh token for a user (password change / reset / forced logout). */
    public void revokeAll(UUID userId) {
        redis.delete(KEY + userId);
    }
}
