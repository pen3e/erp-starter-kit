package com.company.erp.permissions.entity;

import com.company.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A fine-grained, system-wide permission such as {@code USER_READ} or {@code CLIENT_CREATE}.
 *
 * <p>Permissions form a fixed catalogue shared by all tenants (global reference data), so
 * this entity is NOT tenant scoped. Tenants compose them into tenant-specific {@code Role}s.
 * The string {@link #name} is exactly what {@code @PreAuthorize("hasAuthority('...')")} checks.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
