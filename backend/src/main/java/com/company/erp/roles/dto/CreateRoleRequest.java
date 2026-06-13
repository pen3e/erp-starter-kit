package com.company.erp.roles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateRoleRequest(
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[A-Za-z0-9 _-]+$", message = "Role name contains invalid characters")
        String name,

        @Size(max = 255) String description,

        Set<UUID> permissionIds) {
}
