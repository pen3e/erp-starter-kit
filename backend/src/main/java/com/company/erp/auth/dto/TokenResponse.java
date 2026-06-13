package com.company.erp.auth.dto;

/**
 * Token pair returned on login / refresh.
 *
 * @param accessToken  short-lived bearer token for API calls
 * @param refreshToken long-lived token used only against /auth/refresh
 * @param tokenType    always "Bearer"
 * @param expiresIn    access-token lifetime in seconds
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
