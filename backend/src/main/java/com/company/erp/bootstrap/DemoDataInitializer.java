package com.company.erp.bootstrap;

import com.company.erp.config.BootstrapProperties;
import com.company.erp.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds optional demo data on startup when {@code erp.bootstrap.demo-data=true}.
 *
 * <p>The whole bean is conditional, so in the default (production) configuration it is not
 * even instantiated. It orchestrates the idempotent {@link DemoDataSeeder} steps and binds
 * the demo tenant to {@link TenantContext} around the tenant-scoped seeding.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "erp.bootstrap", name = "demo-data", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    private final BootstrapProperties properties;
    private final DemoDataSeeder seeder;

    public DemoDataInitializer(BootstrapProperties properties, DemoDataSeeder seeder) {
        this.properties = properties;
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seeder.ensureTenant(properties);

        boolean created;
        try {
            TenantContext.set(properties.getTenantSlug());
            created = seeder.ensureTenantData(properties);
        } finally {
            TenantContext.clear();
        }

        if (created) {
            log.warn("""
                    ====================================================================
                    DEMO DATA SEEDED. Tenant '{}' now has an admin user:
                      email    : {}
                      password : (the configured erp.bootstrap.admin-password)
                    Log in by sending the header 'X-Tenant-ID: {}' to /api/v1/auth/login.
                    CHANGE THIS PASSWORD IMMEDIATELY — do NOT enable demo data in production.
                    ====================================================================""",
                    properties.getTenantSlug(), properties.getAdminEmail(), properties.getTenantSlug());
        } else {
            log.info("Demo data already present for tenant '{}'; nothing to seed.", properties.getTenantSlug());
        }
    }
}
