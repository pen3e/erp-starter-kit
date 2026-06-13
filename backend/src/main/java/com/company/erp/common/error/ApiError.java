package com.company.erp.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Canonical error response body returned by {@link GlobalExceptionHandler} for every
 * failed request. Stable shape, never contains a stack trace or internal details.
 *
 * @param timestamp when the error was produced (UTC, ISO-8601)
 * @param status    HTTP status code
 * @param error     stable machine-readable error code (e.g. {@code RESOURCE_NOT_FOUND})
 * @param message   human-readable, safe-to-display summary
 * @param path      request URI that produced the error
 * @param traceId   correlation id — also written to server logs for support
 * @param fieldErrors per-field validation problems (only present for 400 validation errors)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path, String traceId) {
        return new ApiError(Instant.now(), status, error, message, path, traceId, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, String traceId,
                              List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, traceId, fieldErrors);
    }
}
