package com.company.erp.roles.entity;

import com.company.erp.common.entity.BaseTenantEntity;
import com.company.erp.permissions.entity.Permission;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A tenant-scoped role grouping a set of global {@link Permission}s. Each tenant manages
 * its own roles; {@code @TenantId} (via {@link BaseTenantEntity}) ensures one tenant can
 * never see or modify another tenant's roles.
 */
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_tenant_name", columnNames = {"tenant_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class Role extends BaseTenantEntity {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    /** System roles (e.g. ADMIN) cannot be deleted and have a protected permission set. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole = false;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public void addPermission(Permission permission) {
        permissions.add(permission);
    }
}
