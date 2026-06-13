package com.company.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the ERP Starter Kit.
 *
 * <p>Architecture: Clean Architecture + DDD-light. Each feature module owns its
 * controllers / services / repositories / entities / dto / mapper, while cross-cutting
 * concerns (security, tenant, audit, common) live in their own packages.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("com.company.erp.config")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
