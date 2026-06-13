package com.company.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Supplies the current principal name for JPA auditing ({@code @CreatedBy} /
 * {@code @LastModifiedBy}). Falls back to "system" for unauthenticated work
 * (migrations, scheduled jobs, bootstrap).
 */
@Configuration
public class JpaConfig {

    public static final String SYSTEM_USER = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of(SYSTEM_USER);
            }
            return Optional.ofNullable(authentication.getName());
        };
    }
}
