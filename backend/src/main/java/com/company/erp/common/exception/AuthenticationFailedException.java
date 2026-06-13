package com.company.erp.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Generic authentication failure. The message is intentionally vague (never reveals
 * whether the account exists, is locked, or the password was wrong) to prevent
 * user enumeration. The precise reason is recorded in the audit log only.
 */
public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException() {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid credentials");
    }

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message);
    }
}
