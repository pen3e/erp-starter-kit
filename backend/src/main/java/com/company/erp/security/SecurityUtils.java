package com.company.erp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Convenience accessors for the authenticated principal, for use in the service layer. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AppUserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UUID currentUserId() {
        return currentPrincipal().map(AppUserPrincipal::getUserId).orElse(null);
    }

    public static String currentUserEmail() {
        return currentPrincipal().map(AppUserPrincipal::getEmail).orElse(null);
    }
}
