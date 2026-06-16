package com.company.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Strongly-typed binding for {@code erp.security.*}.
 *
 * <p>No secret value is ever hard-coded here — defaults live in {@code application.yml}
 * and are sourced from environment variables / Docker secrets at runtime.
 */
@Data
@ConfigurationProperties(prefix = "erp.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final RateLimit rateLimit = new RateLimit();

    @Data
    public static class Jwt {
        /** Base64-encoded signing secret (HMAC-SHA, min 256 bits). */
        private String secret;
        private String issuer = "erp-starter-kit";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(7);
        private Duration rememberMeTtl = Duration.ofDays(30);
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int apiCapacity = 100;
        private Duration apiRefillPeriod = Duration.ofMinutes(1);
        private int authCapacity = 5;
        private Duration authRefillPeriod = Duration.ofMinutes(1);
        private int maxFailedLogins = 5;
        private Duration lockoutDuration = Duration.ofMinutes(15);

        /**
         * Reverse-proxy / load-balancer addresses (single IPs or CIDR ranges, IPv4 or IPv6)
         * that are permitted to set {@code X-Forwarded-For}. When empty (the default) the
         * header is ignored entirely and the direct socket address is used — the safe choice
         * for a directly-exposed service, and the only way to stop clients spoofing their IP
         * to evade rate limiting.
         */
        private List<String> trustedProxies = List.of();
    }
}
