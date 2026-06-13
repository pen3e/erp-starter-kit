package com.company.erp.audit.entity;

import com.company.erp.audit.AuditAction;
import com.company.erp.common.entity.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Immutable audit trail row. Tenant-scoped so each tenant only sees its own trail.
 * Old/new value columns hold redacted JSON snapshots — secrets are masked before storage.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_tenant_created", columnList = "tenant_id, created_at"),
        @Index(name = "idx_audit_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "target_type", length = 100)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "message", length = 1000)
    private String message;
}
