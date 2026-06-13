package com.company.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code erp.bootstrap.*}.
 *
 * <p>Controls optional, idempotent demo seeding (a tenant + ADMIN role + admin user) so the
 * starter kit is usable immediately in development. It is <strong>disabled by default</strong>
 * and must never be enabled in production — the seeded credentials are well-known.
 */
@Data
@ConfigurationProperties(prefix = "erp.bootstrap")
public class BootstrapProperties {

    /** Master switch — when false (default) nothing is seeded. */
    private boolean demoData = false;

    private String tenantSlug = "demo";
    private String tenantName = "Demo Company";

    private String adminEmail = "admin@demo.local";
    /** Must satisfy the StrongPassword policy; change it immediately after first login. */
    private String adminPassword = "ChangeMe!2024";
    private String adminFirstName = "Demo";
    private String adminLastName = "Admin";
    private String adminRoleName = "ADMIN";
}
