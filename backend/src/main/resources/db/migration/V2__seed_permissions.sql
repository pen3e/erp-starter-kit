-- =============================================================================
-- V2 — Seed the global permission catalogue.
--
-- These names MUST stay in sync with com.company.erp.permissions.PermissionCatalog,
-- which is what @PreAuthorize("hasAuthority('...')") checks at runtime. Adding a new
-- permission is a forward migration (never edit this file once shipped).
--
-- gen_random_uuid() is built into PostgreSQL 13+. Auditing columns are filled with
-- 'system' to mirror the application's AuditorAware fallback (JpaConfig.SYSTEM_USER).
-- =============================================================================

INSERT INTO permissions (id, version, created_at, updated_at, created_by, name, description)
VALUES
    -- User management
    (gen_random_uuid(), 0, now(), now(), 'system', 'USER_READ',   'View users'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'USER_CREATE', 'Create users'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'USER_UPDATE', 'Update users'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'USER_DELETE', 'Delete users'),

    -- Role management
    (gen_random_uuid(), 0, now(), now(), 'system', 'ROLE_READ',   'View roles'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'ROLE_CREATE', 'Create roles'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'ROLE_UPDATE', 'Update roles'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'ROLE_DELETE', 'Delete roles'),

    -- Permission catalogue (read-only to clients)
    (gen_random_uuid(), 0, now(), now(), 'system', 'PERMISSION_READ', 'View the permission catalogue'),

    -- Example business domain: clients
    (gen_random_uuid(), 0, now(), now(), 'system', 'CLIENT_READ',   'View clients'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'CLIENT_CREATE', 'Create clients'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'CLIENT_UPDATE', 'Update clients'),
    (gen_random_uuid(), 0, now(), now(), 'system', 'CLIENT_DELETE', 'Delete clients'),

    -- Audit
    (gen_random_uuid(), 0, now(), now(), 'system', 'AUDIT_READ', 'View the audit trail');
