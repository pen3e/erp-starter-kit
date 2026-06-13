package com.company.erp.roles;

import com.company.erp.audit.AuditAction;
import com.company.erp.audit.AuditService;
import com.company.erp.common.exception.BusinessRuleException;
import com.company.erp.common.exception.DuplicateResourceException;
import com.company.erp.common.exception.ResourceNotFoundException;
import com.company.erp.common.util.InputSanitizer;
import com.company.erp.permissions.entity.Permission;
import com.company.erp.permissions.repository.PermissionRepository;
import com.company.erp.roles.dto.CreateRoleRequest;
import com.company.erp.roles.dto.RoleResponse;
import com.company.erp.roles.dto.UpdateRoleRequest;
import com.company.erp.roles.entity.Role;
import com.company.erp.roles.repository.RoleRepository;
import com.company.erp.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService audit;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                       AuditService audit) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.audit = audit;
    }

    /**
     * Maps to DTOs inside the transaction: {@code permissions} is lazy and
     * {@code spring.jpa.open-in-view=false} closes the session before serialization.
     */
    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        return roleRepository.findAll(pageable).map(RoleResponse::from);
    }

    @Transactional(readOnly = true)
    public Role get(UUID id) {
        return roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    @Transactional
    public Role create(CreateRoleRequest request) {
        String name = InputSanitizer.clean(request.name());
        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("A role with this name already exists");
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(InputSanitizer.clean(request.description()));
        role.setPermissions(resolvePermissions(request.permissionIds()));
        Role saved = roleRepository.save(role);

        audit.entityChange(AuditAction.CREATE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "Role", saved.getId().toString(), null, saved.getName());
        return saved;
    }

    @Transactional
    public Role update(UUID id, UpdateRoleRequest request) {
        Role role = get(id);
        String before = role.getName() + " perms=" + role.getPermissions().size();

        if (request.description() != null) {
            role.setDescription(InputSanitizer.clean(request.description()));
        }
        if (request.permissionIds() != null) {
            if (role.isSystemRole()) {
                throw new BusinessRuleException("The permission set of a system role cannot be modified");
            }
            role.setPermissions(resolvePermissions(request.permissionIds()));
        }
        Role saved = roleRepository.save(role);

        audit.entityChange(AuditAction.UPDATE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "Role", saved.getId().toString(), before, saved.getName() + " perms=" + saved.getPermissions().size());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Role role = get(id);
        if (role.isSystemRole()) {
            throw new BusinessRuleException("System roles cannot be deleted");
        }
        roleRepository.delete(role);
        audit.entityChange(AuditAction.DELETE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "Role", id.toString(), role.getName(), null);
    }

    private Set<Permission> resolvePermissions(Set<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> found = permissionRepository.findAllById(permissionIds);
        if (found.size() != permissionIds.size()) {
            throw new BusinessRuleException("One or more permission ids are invalid");
        }
        return new HashSet<>(found);
    }
}
