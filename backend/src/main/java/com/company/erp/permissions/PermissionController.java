package com.company.erp.permissions;

import com.company.erp.permissions.dto.PermissionResponse;
import com.company.erp.permissions.repository.PermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only catalogue of the permissions available to compose into roles. */
@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions", description = "Permission catalogue (read-only)")
public class PermissionController {

    private final PermissionRepository repository;

    public PermissionController(PermissionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.PERMISSION_READ + "')")
    @Operation(summary = "List all permissions")
    public List<PermissionResponse> list() {
        return repository.findAll(Sort.by("name")).stream()
                .map(PermissionResponse::from)
                .toList();
    }
}
