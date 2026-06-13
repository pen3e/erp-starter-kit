package com.company.erp.tenant;

import com.company.erp.config.TenantProperties;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bridges {@link TenantContext} into Hibernate's discriminator multi-tenancy.
 *
 * <p>Hibernate calls {@link #resolveCurrentTenantIdentifier()} when opening a session and
 * uses the returned value to (a) populate {@code @TenantId} columns on insert and
 * (b) append {@code tenant_id = ?} to every select/update/delete. Returning a non-null
 * default guarantees Hibernate always has a discriminator, even for system/bootstrap work.
 *
 * <p>Implementing {@link HibernatePropertiesCustomizer} registers this resolver with the
 * {@code EntityManagerFactory} explicitly, which is the robust, version-independent wiring.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    private final String defaultTenant;

    public TenantIdentifierResolver(TenantProperties properties) {
        this.defaultTenant = properties.getDefaultTenant();
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.get();
        return tenant != null ? tenant : defaultTenant;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // We never reuse a session across tenants, so no validation is required.
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
