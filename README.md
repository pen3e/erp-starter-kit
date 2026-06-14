<div align="center">

# SaaS multi-tenant Spring Boot

**A secure, multi-tenant foundation for building SMB business & SaaS applications.**

It ships the cross-cutting concerns that are tedious and risky to get right —
authentication, RBAC, multi-tenancy, auditing, rate limiting and hardened HTTP —
so you can focus on your own domain from day one.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

---

## Table of contents

- [Why this starter](#why-this-starter)
- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [First login](#first-login-demo-data)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Project structure](#project-structure)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Why this starter

Most business apps re-implement the same security and tenancy plumbing — usually
incompletely. This project gives you a **production-grade baseline** instead of a blank
`main()`:

- Schema is owned by **Flyway** migrations; Hibernate runs `ddl-auto=validate`, so the
  database and the code can never silently drift.
- Multi-tenancy is enforced **at the ORM layer**, not by remembering to add `WHERE tenant_id = ?`
  to every query — there is no code path a developer can forget.
- Security defaults are strict out of the box (token rotation, lockout, CSP/HSTS, generic
  errors to prevent user enumeration).

> **Status:** backend foundation. The example business domain (`clients`) and a React
> frontend are intentionally left as extension points — see the [Roadmap](#roadmap).

## Features

| Area | What you get |
|------|--------------|
| **Authentication** | JWT access (15 min) + refresh (7 d, 30 d "remember me") with **rotation & reuse detection**; logout blacklists the live access token. |
| **Passwords** | BCrypt (strength 12); strong-password policy (12+ chars, mixed classes); self-service change + reset flows. |
| **Brute-force defence** | Account lockout after repeated failures; strict per-IP rate limit on auth endpoints; generic responses (no user enumeration). |
| **Authorization (RBAC)** | Users → roles → fine-grained permissions, enforced with `@PreAuthorize`. |
| **Multi-tenancy** | Hibernate `@TenantId` discriminator — every business query is transparently scoped to the current tenant. |
| **Auditing** | Async, per-tenant audit trail with secret masking; never adds latency to the audited operation. |
| **Hardened HTTP** | CSP, HSTS, nosniff, anti-clickjacking, Referrer-Policy, Permissions-Policy, strict CORS. |
| **Observability** | Actuator health/info + Prometheus metrics; OpenAPI/Swagger UI. |
| **DX** | Typed config properties, consistent error contract with trace IDs, Testcontainers wired, Docker Compose dev stack. |

## Architecture

```
                 ┌─────────────────────── Spring Security filter chain ───────────────────────┐
HTTP request ──▶ │ RateLimitFilter ──▶ TenantFilter ──▶ JwtAuthenticationFilter ──▶ @PreAuthorize │ ──▶ Controller
                 └────────────────────────────────────────────────────────────────────────────┘
                          │                  │                       │
                     Bucket4j/Redis   TenantContext           validates JWT,
                                      (ThreadLocal)           sets authoritative tenant
                                            │
                                            ▼
                            Hibernate @TenantId discriminator
                         (auto-appends tenant_id = ? to every query)
                                            │
                                            ▼
                                  PostgreSQL  ·  Redis
```

- **Tenant resolution:** unauthenticated requests resolve the tenant from the `X-Tenant-ID`
  header (falling back to a default); authenticated requests use the verified tenant claim in
  the JWT, so a client cannot reach another tenant's data by spoofing the header.
- **Token state in Redis:** refresh-token store, access-token blacklist and rate-limit buckets.

## Tech stack

- **Java 21**, **Spring Boot 3.4** (Web, Data JPA, Security, Validation, Actuator)
- **PostgreSQL 16** + **Flyway**
- **Redis 7**
- **jjwt** (JWT), **Bucket4j** (rate limiting), **springdoc** (OpenAPI), **Micrometer/Prometheus**
- **MapStruct** + **Lombok**; **Testcontainers** for integration tests

## Getting started

Prerequisites: **Docker** (for Postgres + Redis) and a **JDK 21** — or run everything in Docker.

```bash
git clone https://github.com/pen3e/erp-starter-kit.git
cd erp-starter-kit
cp .env.example .env

# Option A — infra in Docker, app from your IDE / Maven
docker compose up -d                # starts Postgres + Redis
cd backend && mvn spring-boot:run   # or run ErpApplication from your IDE

# Option B — everything in Docker
docker compose --profile app up --build
```

Flyway applies the schema (`V1`) and seeds the permission catalogue (`V2`) on first boot.

- API base: <http://localhost:8080/api/v1>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

## First login (demo data)

When `BOOTSTRAP_DEMO_DATA=true` (the default in `.env.example`, **never enable in production**),
a demo tenant + `ADMIN` role + admin user are created on startup. The default tenant is
`public`, so send the demo tenant id as a header:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' -H 'X-Tenant-ID: demo' \
  -d '{"email":"admin@demo.local","password":"ChangeMe!2024"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` on subsequent calls.

## Configuration

All settings have safe local defaults and are overridable via environment variables
(see `.env.example`). Secrets must come from the environment / a secret manager — never commit them.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `…localhost:5432/erp` / `erp` / `erp` | PostgreSQL connection |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / _(empty)_ | Redis connection |
| `JWT_SECRET` | dev-only sample | Base64 HMAC secret, **min 256 bits** — override everywhere (`openssl rand -base64 48`) |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` / `JWT_REMEMBER_TTL` | `PT15M` / `P7D` / `P30D` | Token lifetimes (ISO-8601 durations) |
| `CORS_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `RATE_LIMIT_ENABLED` | `true` | Toggle the rate-limit filter |
| `SERVER_PORT` | `8080` | HTTP port |
| `BOOTSTRAP_DEMO_DATA` | `false` | Seed demo tenant/admin (dev only) |

## API overview

| Area | Endpoints |
|------|-----------|
| **Auth** | `POST /auth/login` · `/refresh` · `/logout` · `/password/change` · `/password/reset-request` · `/password/reset/confirm` |
| **Users** | `GET/POST /users` · `GET/PUT/DELETE /users/{id}` (filters: `?search=&status=`) |
| **Roles** | `GET/POST /roles` · `GET/PUT/DELETE /roles/{id}` |
| **Permissions** | `GET /permissions` (read-only catalogue) |
| **Audit** | `GET /audit-logs` |

Every non-auth endpoint is gated by a permission from `PermissionCatalog` (e.g. `USER_CREATE`).
Full, interactive docs are available in Swagger UI.

## Project structure

```
backend/src/main/java/com/company/erp
  auth/         authentication, token lifecycle, password reset
  users/        user administration (entity, repo, service, controller, dto)
  roles/        role administration (RBAC)
  permissions/  permission catalogue
  tenant/       multi-tenancy (context, resolver, Hibernate @TenantId wiring)
  security/     JWT filter, rate limiting, security config, principal
  audit/        async audit trail
  bootstrap/    optional demo-data seeding (dev only)
  common/       base entities, error contract, validation, utilities
  config/       typed @ConfigurationProperties + JPA/Redis/Async/OpenAPI config
backend/src/main/resources/db/migration   Flyway migrations (V1 schema, V2 permission seed)
```

## Roadmap

Planned extension points — what is intentionally left for you to build on top of the foundation:

- [ ] Example business domain (`clients`) wiring the `CLIENT_*` permissions into a CRUD module — a template for your own entities.
- [ ] React (Vite) frontend consuming this API.
- [ ] Email delivery for password reset (the service already issues tokens and logs them in dev).
- [ ] Automated test suite (Testcontainers is already on the classpath).
- [ ] CI pipeline (build + test on push).

## Contributing

Issues and pull requests are welcome. Please keep changes focused, follow the existing module
layout and security conventions, and describe the motivation in the PR.

## License


