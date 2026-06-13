package com.company.erp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for {@code erp.tenant.*}.
 */
@Data
@ConfigurationProperties(prefix = "erp.tenant")
public class TenantProperties {

    /** Tenant identifier used when no explicit tenant is resolved (single-tenant / bootstrap). */
    private String defaultTenant = "public";

    /** HTTP header carrying the tenant id for un-authenticated / edge resolution. */
    private String headerName = "X-Tenant-ID";
}
