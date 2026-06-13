package com.company.erp.users.repository;

import com.company.erp.users.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * All queries are implicitly constrained to the current tenant by Hibernate's
 * {@code @TenantId} discriminator. {@link JpaSpecificationExecutor} backs dynamic,
 * type-safe filtering in the user listing endpoint (Specification pattern).
 */
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    /** Eagerly fetches roles + their permissions for authentication / authority mapping. */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findWithRolesById(UUID id);

    boolean existsByEmail(String email);
}
