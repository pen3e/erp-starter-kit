package com.company.erp.audit;

import com.company.erp.audit.dto.AuditLogResponse;
import com.company.erp.audit.repository.AuditLogRepository;
import com.company.erp.common.dto.PageResponse;
import com.company.erp.permissions.PermissionCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only access to the (tenant-scoped) audit trail. */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit", description = "Tenant audit trail")
public class AuditController {

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.AUDIT_READ + "')")
    @Operation(summary = "List audit log entries for the current tenant")
    public PageResponse<AuditLogResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), AuditLogResponse::from);
    }
}
