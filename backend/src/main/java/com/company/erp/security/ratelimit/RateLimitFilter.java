package com.company.erp.security.ratelimit;

import com.company.erp.common.error.ApiError;
import com.company.erp.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * Applies {@link RateLimitService} buckets per client IP. Auth endpoints get the strict
 * bucket; other API endpoints get the general bucket. On exhaustion returns 429 with a
 * {@code Retry-After} header and the standard {@link ApiError} body.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String API_PATH_PREFIX = "/api/";

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper,
                           SecurityProperties properties) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.enabled = properties.getRateLimit().isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!enabled || !path.startsWith(API_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = clientIp(request);
        Bucket bucket = path.startsWith(AUTH_PATH_PREFIX)
                ? rateLimitService.resolveAuthBucket(clientKey)
                : rateLimitService.resolveApiBucket(clientKey);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
            writeTooManyRequests(request, response, retryAfterSeconds);
        }
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please retry later.", request.getRequestURI(),
                UUID.randomUUID().toString());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** Honour the first hop of X-Forwarded-For when behind a trusted proxy, else remote addr. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
