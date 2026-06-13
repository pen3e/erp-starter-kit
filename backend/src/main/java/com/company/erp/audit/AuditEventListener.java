package com.company.erp.audit;

import com.company.erp.audit.entity.AuditLog;
import com.company.erp.audit.repository.AuditLogRepository;
import com.company.erp.common.util.SensitiveDataMasker;
import com.company.erp.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists {@link AuditEvent}s asynchronously. The tenant captured in the event is restored
 * onto the worker thread so Hibernate's {@code @TenantId} stamps the row with the correct
 * tenant, then cleared in a {@code finally} block. Secret-bearing snapshots are masked.
 */
@Slf4j
@Component
public class AuditEventListener {

    private final AuditLogRepository repository;

    public AuditEventListener(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Async("auditExecutor")
    @EventListener
    @Transactional
    public void on(AuditEvent event) {
        try {
            if (event.tenantId() != null) {
                TenantContext.set(event.tenantId());
            }
            AuditLog entry = new AuditLog();
            entry.setAction(event.action());
            entry.setOutcome(event.outcome());
            entry.setActorId(event.actorId());
            entry.setActorEmail(event.actorEmail());
            entry.setIpAddress(event.ipAddress());
            entry.setUserAgent(event.userAgent());
            entry.setTargetType(event.targetType());
            entry.setTargetId(event.targetId());
            entry.setOldValue(SensitiveDataMasker.mask(event.oldValue()));
            entry.setNewValue(SensitiveDataMasker.mask(event.newValue()));
            entry.setMessage(event.message());
            repository.save(entry);
        } catch (Exception ex) {
            // Never let an audit failure break (or be visible to) the audited operation.
            log.error("Failed to persist audit event {}", event.action(), ex);
        } finally {
            TenantContext.clear();
        }
    }
}
