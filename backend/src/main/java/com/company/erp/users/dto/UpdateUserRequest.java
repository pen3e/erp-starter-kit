package com.company.erp.users.dto;

import com.company.erp.users.entity.UserStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * Partial update of a user's profile, status and role assignment. {@code null} fields are
 * left untouched. Email and password are deliberately not editable here — email is the login
 * identity, and passwords go through the dedicated {@code /auth/password/*} endpoints.
 */
public record UpdateUserRequest(
        @Size(max = 100) String firstName,

        @Size(max = 100) String lastName,

        @Size(max = 30)
        @Pattern(regexp = "^[+0-9 ().-]*$", message = "Phone contains invalid characters")
        String phone,

        UserStatus status,

        Set<UUID> roleIds) {
}
