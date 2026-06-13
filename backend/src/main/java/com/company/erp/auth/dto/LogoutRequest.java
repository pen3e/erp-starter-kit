package com.company.erp.auth.dto;

/** Optional refresh token to revoke alongside blacklisting the current access token. */
public record LogoutRequest(String refreshToken) {
}
