package com.company.erp.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * Base class for every <strong>business</strong> entity that must be isolated per tenant.
 *
 * <p>The {@link TenantId} field activates Hibernate's discriminator multi-tenancy:
 * <ul>
 *   <li>on INSERT, Hibernate fills {@code tenant_id} from the
 *       {@code CurrentTenantIdentifierResolver};</li>
 *   <li>on SELECT/UPDATE/DELETE, Hibernate transparently appends
 *       {@code AND tenant_id = ?} to <em>every</em> query.</li>
 * </ul>
 * This makes cross-tenant access structurally impossible at the ORM layer — there is
 * no code path a developer can forget. Never expose a setter for {@code tenantId}.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseTenantEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64)
    private String tenantId;
}
