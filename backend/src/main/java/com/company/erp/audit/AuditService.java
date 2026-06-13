package com.company.erp.audit;

import com.company.erp.common.util.RequestUtils;
import com.company.erp.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single entry point for emitting audit records. Captures tenant / IP / user-agent on the
 * <em>request</em> thread, then hands off to an async listener for persistence so auditing
 * never adds latency to the audited operation.
 */
@Service
public class AuditService {

    private final ApplicationEventPublisher publisher;

    public AuditService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void record(AuditAction action, String outcome, UUID actorId, String actorEmail,
                       String targetType, String targetId, String oldValue, String newValue, String message) {
        AuditEvent event = new AuditEvent(
                TenantContext.get(),
                action,
                outcome,
                actorId,
                actorEmail,
                RequestUtils.clientIp(),
                RequestUtils.userAgent(),
                targetType,
                targetId,
                oldValue,
                newValue,
                message);
        publisher.publishEvent(event);
    }

    // --- Convenience methods for the common auth events ---

    public void success(AuditAction action, UUID actorId, String actorEmail, String message) {
        record(action, AuditEvent.SUCCESS, actorId, actorEmail, null, null, null, null, message);
    }

    public void failure(AuditAction action, UUID actorId, String actorEmail, String message) {
        record(action, AuditEvent.FAILURE, actorId, actorEmail, null, null, null, null, message);
    }

    public void entityChange(AuditAction action, UUID actorId, String actorEmail,
                             String targetType, String targetId, String oldValue, String newValue) {
        record(action, AuditEvent.SUCCESS, actorId, actorEmail, targetType, targetId, oldValue, newValue, null);
    }
}
