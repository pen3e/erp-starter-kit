package com.company.erp.roles.repository;

import com.company.erp.roles.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant scoping is enforced automatically by Hibernate's {@code @TenantId} discriminator,
 * so these finders never need (and must not add) a {@code tenantId} parameter.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByName(String name);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);

    boolean existsByName(String name);
}
