package com.company.erp.users.dto;

import com.company.erp.roles.entity.Role;
import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outward view of a user. Never exposes the password hash or security counters
 * (failed attempts / lock state) — those stay internal to the auth layer.
 */
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        List<String> roles) {

    /** Must be invoked while the persistence context is open (roles is lazy). */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getRoles().stream().map(Role::getName).sorted().toList());
    }
}
