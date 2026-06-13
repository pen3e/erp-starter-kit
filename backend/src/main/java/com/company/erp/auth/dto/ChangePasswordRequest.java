package com.company.erp.auth.dto;

import com.company.erp.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @StrongPassword String newPassword) {
}
