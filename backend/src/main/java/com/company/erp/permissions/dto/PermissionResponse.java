package com.company.erp.permissions.dto;

import com.company.erp.permissions.entity.Permission;

import java.util.UUID;

public record PermissionResponse(UUID id, String name, String description) {

    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getDescription());
    }
}
