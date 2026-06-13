package com.company.erp.security.ratelimit;

import com.company.erp.config.SecurityProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiting (Bucket4j). Buckets are keyed per client + scope and held
 * in memory; for a single-instance starter this is sufficient. For a clustered deployment
 * swap the {@link java.util.Map} for a Bucket4j Redis/Hazelcast proxy-manager.
 *
 * <p>Two scopes:
 * <ul>
 *   <li><b>auth</b> — very tight (default 5/min/IP) to blunt brute-force and credential
 *       stuffing against login/refresh/reset endpoints;</li>
 *   <li><b>api</b> — generous (default 100/min) for general traffic.</li>
 * </ul>
 */
@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bandwidth authLimit;
    private final Bandwidth apiLimit;

    public RateLimitService(SecurityProperties properties) {
        SecurityProperties.RateLimit cfg = properties.getRateLimit();
        this.authLimit = Bandwidth.builder()
                .capacity(cfg.getAuthCapacity())
                .refillGreedy(cfg.getAuthCapacity(), cfg.getAuthRefillPeriod())
                .build();
        this.apiLimit = Bandwidth.builder()
                .capacity(cfg.getApiCapacity())
                .refillGreedy(cfg.getApiCapacity(), cfg.getApiRefillPeriod())
                .build();
    }

    public Bucket resolveAuthBucket(String clientKey) {
        return buckets.computeIfAbsent("auth:" + clientKey,
                k -> Bucket.builder().addLimit(authLimit).build());
    }

    public Bucket resolveApiBucket(String clientKey) {
        return buckets.computeIfAbsent("api:" + clientKey,
                k -> Bucket.builder().addLimit(apiLimit).build());
    }
}
