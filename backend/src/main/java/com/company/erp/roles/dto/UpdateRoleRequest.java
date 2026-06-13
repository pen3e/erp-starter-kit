package com.company.erp.roles.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/** Partial update: provided fields replace existing values; {@code null} leaves them as-is. */
public record UpdateRoleRequest(
        @Size(max = 255) String description,
        Set<UUID> permissionIds) {
}
