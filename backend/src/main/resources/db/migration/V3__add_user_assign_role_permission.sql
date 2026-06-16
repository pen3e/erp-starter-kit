-- =============================================================================
-- V3 — Add the USER_ASSIGN_ROLE permission.
--
-- Role assignment is split out of the generic USER_UPDATE/USER_CREATE permissions so a
-- user-administrator cannot escalate privileges by granting themselves (or others) a
-- higher-privileged role. The service layer additionally enforces that a caller may only
-- grant permissions they already hold (see UserService#guardRoleAssignment).
--
-- Keep in sync with com.company.erp.permissions.PermissionCatalog.USER_ASSIGN_ROLE.
-- =============================================================================

INSERT INTO permissions (id, version, created_at, updated_at, created_by, name, description)
VALUES (gen_random_uuid(), 0, now(), now(), 'system', 'USER_ASSIGN_ROLE', 'Assign roles to users');

-- Grant the new permission to every existing system role (e.g. the seeded ADMIN role) so
-- current administrators retain the ability to assign roles after this migration.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system_role = true
  AND p.name = 'USER_ASSIGN_ROLE'
  AND NOT EXISTS (
        SELECT 1
        FROM role_permissions rp
        WHERE rp.role_id = r.id
          AND rp.permission_id = p.id
  );
