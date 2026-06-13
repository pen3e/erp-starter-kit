package com.company.erp.roles.dto;

import com.company.erp.roles.entity.Role;

import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean systemRole,
        List<String> permissions) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                role.getPermissions().stream().map(p -> p.getName()).sorted().toList());
    }
}
