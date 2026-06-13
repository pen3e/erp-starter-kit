package com.company.erp.audit;

import java.util.UUID;

/**
 * Immutable snapshot of an auditable action, captured on the request thread (so tenant,
 * actor, IP and user-agent are accurate) and persisted asynchronously by
 * {@code AuditEventListener}.
 */
public record AuditEvent(
        String tenantId,
        AuditAction action,
        String outcome,
        UUID actorId,
        String actorEmail,
        String ipAddress,
        String userAgent,
        String targetType,
        String targetId,
        String oldValue,
        String newValue,
        String message) {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
}
