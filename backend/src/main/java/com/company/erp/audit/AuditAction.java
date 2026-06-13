package com.company.erp.audit;

/** Canonical, auditable actions. CRUD actions carry the target type in the log row. */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGIN_BLOCKED,
    LOGOUT,
    TOKEN_REFRESH,
    PASSWORD_CHANGE,
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET,
    CREATE,
    UPDATE,
    DELETE,
    ROLE_ASSIGNMENT
}
