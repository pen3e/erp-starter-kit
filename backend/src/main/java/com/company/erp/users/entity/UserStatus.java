package com.company.erp.users.entity;

public enum UserStatus {
    /** Normal, can authenticate. */
    ACTIVE,
    /** Disabled by an administrator; cannot authenticate. */
    INACTIVE,
    /** Created but not yet activated (e.g. awaiting email confirmation). */
    PENDING
}
