package com.company.erp.roles;

import com.company.erp.common.dto.PageResponse;
import com.company.erp.permissions.PermissionCatalog;
import com.company.erp.roles.dto.CreateRoleRequest;
import com.company.erp.roles.dto.RoleResponse;
import com.company.erp.roles.dto.UpdateRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.function.Function;

/** Tenant-scoped role administration (RBAC). All endpoints are permission-gated. */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Tenant role administration (RBAC)")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.ROLE_READ + "')")
    @Operation(summary = "List roles for the current tenant")
    public PageResponse<RoleResponse> list(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(roleService.list(pageable), Function.identity());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.ROLE_READ + "')")
    @Operation(summary = "Get a single role by id")
    public RoleResponse get(@PathVariable UUID id) {
        return RoleResponse.from(roleService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCatalog.ROLE_CREATE + "')")
    @Operation(summary = "Create a role")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request,
                                               UriComponentsBuilder uriBuilder) {
        RoleResponse body = RoleResponse.from(roleService.create(request));
        URI location = uriBuilder.path("/api/v1/roles/{id}").buildAndExpand(body.id()).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCatalog.ROLE_UPDATE + "')")
    @Operation(summary = "Update a role's description and/or permission set")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return RoleResponse.from(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + PermissionCatalog.ROLE_DELETE + "')")
    @Operation(summary = "Delete a non-system role")
    public void delete(@PathVariable UUID id) {
        roleService.delete(id);
    }
}
