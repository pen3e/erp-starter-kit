package com.company.erp.users.dto;

import com.company.erp.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/** Payload to provision a new user within the current tenant. */
public record CreateUserRequest(
        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotBlank @Email @Size(max = 255) String email,

        @Size(max = 30)
        @Pattern(regexp = "^[+0-9 ().-]*$", message = "Phone contains invalid characters")
        String phone,

        @StrongPassword String password,

        Set<UUID> roleIds) {
}
