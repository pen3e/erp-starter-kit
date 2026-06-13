package com.company.erp.permissions;

/**
 * Compile-time constants for the built-in permission names, so controllers reference
 * {@code @PreAuthorize(...)} strings that are checked by the compiler and easy to refactor.
 * Keep in sync with the seeded {@code permissions} table (migration V2).
 */
public final class PermissionCatalog {

    private PermissionCatalog() {
    }

    // --- User management ---
    public static final String USER_READ = "USER_READ";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_UPDATE = "USER_UPDATE";
    public static final String USER_DELETE = "USER_DELETE";

    // --- Role management ---
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_CREATE = "ROLE_CREATE";
    public static final String ROLE_UPDATE = "ROLE_UPDATE";
    public static final String ROLE_DELETE = "ROLE_DELETE";

    // --- Permission catalogue (read-only to clients) ---
    public static final String PERMISSION_READ = "PERMISSION_READ";

    // --- Example business domain: clients ---
    public static final String CLIENT_READ = "CLIENT_READ";
    public static final String CLIENT_CREATE = "CLIENT_CREATE";
    public static final String CLIENT_UPDATE = "CLIENT_UPDATE";
    public static final String CLIENT_DELETE = "CLIENT_DELETE";

    // --- Audit ---
    public static final String AUDIT_READ = "AUDIT_READ";
}
