package com.company.erp.tenant.entity;

import com.company.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant (customer organisation). This is a <em>global</em> entity — it is NOT tenant
 * scoped (it defines the tenants themselves), hence it extends {@link BaseEntity} rather
 * than {@code BaseTenantEntity}. The {@link #slug} is the discriminator value stored in
 * every business row's {@code tenant_id} column.
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    /** Stable, URL-safe identifier used as the multi-tenancy discriminator. */
    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;
}
