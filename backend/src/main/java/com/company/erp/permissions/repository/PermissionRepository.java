package com.company.erp.permissions.repository;

import com.company.erp.permissions.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    Set<Permission> findByNameIn(Set<String> names);
}
