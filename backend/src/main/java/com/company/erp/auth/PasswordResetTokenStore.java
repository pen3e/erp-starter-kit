package com.company.erp.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Single-use, time-boxed password-reset tokens kept in Redis. The token maps to the target
 * user + tenant and is deleted the moment it is consumed (so it cannot be replayed).
 */
@Service
public class PasswordResetTokenStore {

    private static final String PREFIX = "pwreset:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public PasswordResetTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return an opaque, high-entropy token to be delivered to the user out-of-band (email). */
    public String issue(UUID userId, String tenantId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(PREFIX + token, tenantId + "::" + userId, TTL);
        return token;
    }

    /** Atomically validates and consumes a token. */
    public Optional<ResetTarget> consume(String token) {
        String key = PREFIX + token;
        String value = redis.opsForValue().getAndDelete(key);
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split("::", 2);
        return Optional.of(new ResetTarget(parts[0], UUID.fromString(parts[1])));
    }

    public record ResetTarget(String tenantId, UUID userId) {
    }
}
