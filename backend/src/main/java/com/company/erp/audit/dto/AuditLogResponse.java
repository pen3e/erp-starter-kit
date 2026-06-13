package com.company.erp.audit.dto;

import com.company.erp.audit.AuditAction;
import com.company.erp.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditAction action,
        String outcome,
        UUID actorId,
        String actorEmail,
        String ipAddress,
        String userAgent,
        String targetType,
        String targetId,
        String message,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getAction(), a.getOutcome(), a.getActorId(),
                a.getActorEmail(), a.getIpAddress(), a.getUserAgent(), a.getTargetType(),
                a.getTargetId(), a.getMessage(), a.getCreatedAt());
    }
}
