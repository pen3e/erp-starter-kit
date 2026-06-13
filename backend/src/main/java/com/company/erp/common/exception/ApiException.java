package com.company.erp.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for all expected (handled) domain/application errors. Carries an HTTP
 * status and a stable, machine-readable error code so the API contract stays
 * consistent and clients can branch on {@code error} without parsing messages.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
