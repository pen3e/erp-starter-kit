-- =============================================================================
-- V1 — Initial schema for the ERP Starter Kit.
--
-- The schema is owned by Flyway; Hibernate runs with ddl-auto=validate and must
-- find every table/column declared by the JPA entities. Column types are chosen
-- to match Hibernate 6's PostgreSQL mappings:
--   * java.util.UUID            -> uuid
--   * long  @Version            -> bigint
--   * java.time.Instant         -> timestamptz (Hibernate TIMESTAMP_UTC)
--   * enum @Enumerated(STRING)  -> varchar
--
-- Multi-tenancy: business tables carry a plain `tenant_id` discriminator column
-- (Hibernate @TenantId). It is intentionally NOT a foreign key to `tenants` — the
-- discriminator must stay writable/queryable without coupling the ORM filter to a
-- join, exactly as documented on BaseTenantEntity.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tenants (global reference data — defines the tenants themselves)
-- -----------------------------------------------------------------------------
CREATE TABLE tenants (
    id          uuid         NOT NULL,
    version     bigint       NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    created_by  varchar(255),
    updated_by  varchar(255),
    slug        varchar(64)  NOT NULL,
    name        varchar(150) NOT NULL,
    status      varchar(20)  NOT NULL,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uk_tenant_slug UNIQUE (slug)
);

-- -----------------------------------------------------------------------------
-- Permissions (global, fixed catalogue shared by every tenant)
-- -----------------------------------------------------------------------------
CREATE TABLE permissions (
    id          uuid          NOT NULL,
    version     bigint        NOT NULL,
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(255),
    updated_by  varchar(255),
    name        varchar(100)  NOT NULL,
    description varchar(255),
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permission_name UNIQUE (name)
);

-- -----------------------------------------------------------------------------
-- Roles (tenant-scoped: each tenant manages its own roles)
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id          uuid         NOT NULL,
    version     bigint       NOT NULL,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    created_by  varchar(255),
    updated_by  varchar(255),
    tenant_id   varchar(64)  NOT NULL,
    name        varchar(80)  NOT NULL,
    description varchar(255),
    system_role boolean      NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_role_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_role_tenant ON roles (tenant_id);

-- -----------------------------------------------------------------------------
-- Role <-> Permission (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id       uuid NOT NULL,
    permission_id uuid NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

-- -----------------------------------------------------------------------------
-- Users (tenant-scoped; email unique within a tenant)
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id                    uuid         NOT NULL,
    version               bigint       NOT NULL,
    created_at            timestamptz  NOT NULL,
    updated_at            timestamptz  NOT NULL,
    created_by            varchar(255),
    updated_by            varchar(255),
    tenant_id             varchar(64)  NOT NULL,
    first_name            varchar(100) NOT NULL,
    last_name             varchar(100) NOT NULL,
    email                 varchar(255) NOT NULL,
    phone                 varchar(30),
    password_hash         varchar(100) NOT NULL,
    status                varchar(20)  NOT NULL,
    failed_login_attempts integer      NOT NULL,
    locked_until          timestamptz,
    last_login_at         timestamptz,
    password_changed_at   timestamptz,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_user_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_user_tenant ON users (tenant_id);

-- -----------------------------------------------------------------------------
-- User <-> Role (many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);

-- -----------------------------------------------------------------------------
-- Audit log (tenant-scoped, append-only trail)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          uuid          NOT NULL,
    version     bigint        NOT NULL,
    created_at  timestamptz   NOT NULL,
    updated_at  timestamptz   NOT NULL,
    created_by  varchar(255),
    updated_by  varchar(255),
    tenant_id   varchar(64)   NOT NULL,
    action      varchar(40)   NOT NULL,
    outcome     varchar(20)   NOT NULL,
    actor_id    uuid,
    actor_email varchar(255),
    ip_address  varchar(45),
    user_agent  varchar(512),
    target_type varchar(100),
    target_id   varchar(64),
    old_value   text,
    new_value   text,
    message     varchar(1000),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_tenant_created ON audit_logs (tenant_id, created_at);
CREATE INDEX idx_audit_actor          ON audit_logs (actor_id);
CREATE INDEX idx_audit_action         ON audit_logs (action);
