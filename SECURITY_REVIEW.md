# Security Review — StarterPackERP Backend (`com.company.erp`)

**Date:** 2026-06-15
**Scope:** `Desktop/StarterPackERP/backend` — Spring Boot 3 / Spring Security 6, JWT auth, multi-tenant RBAC.
**Focus areas:** Authentication, Authorization, JWT handling, Spring Security config, SQL injection, XSS, sensitive-data exposure, hardcoded secrets.
**Method:** Manual source review of the security, auth, tenant, user/role, config, and persistence layers.

---

## Executive summary

The backend is **security-conscious and well-architected**. It gets the hard parts right: BCrypt (strength 12), short-lived access tokens with server-tracked refresh-token rotation and reuse detection, a JTI blacklist plus a per-user "revoke-before" watermark, strict response headers, stateless sessions, generic error contracts with no stack-trace leakage, anti-enumeration on login and password reset, and parameterized persistence (no SQL injection found).

The findings below are mostly **hardening / defense-in-depth** items rather than open holes. The three worth prioritizing are the **hardcoded fallback JWT secret** (token forgery if an env var is missing), the **rate-limit bypass via spoofed `X-Forwarded-For`** (defeats brute-force protection at the IP layer), and **privilege escalation through unrestricted role assignment**.

| # | Severity | Title | Area |
|---|----------|-------|------|
| 1 | **High** | Hardcoded fallback JWT signing secret (and DB password) | Hardcoded secrets |
| 2 | **Medium** | Brute-force / rate-limit bypass via spoofed `X-Forwarded-For` | Authentication |
| 3 | **Medium** | Privilege escalation through unrestricted role assignment | Authorization |
| 4 | **Low** | Anonymous access to `/actuator/metrics/**` and `/actuator/prometheus` | Sensitive data exposure |
| 5 | **Low** | Unbounded in-memory rate-limit bucket map (DoS) | Availability |
| 6 | **Low** | Password-reset token written to debug log | Sensitive data exposure |
| 7 | Info | CORS `allowCredentials=true` — guard against wildcard origin | Config |

---

## Findings

### 1. High — Hardcoded fallback JWT signing secret (and DB password)

**File:** `src/main/resources/application.yml:66` (also `:11`)

```yaml
secret: ${JWT_SECRET:Y2hhbmdlLW1lLXRoaXMtaXMtYS1kZXYtb25seS1zZWNyZXQta2V5LTEyMzQ1Njc4OTA=}
```

The default after the `:` is a **publicly known** value (base64 of `change-me-this-is-a-dev-only-secret-key-1234567890`). `JwtService` signs and verifies all tokens with this key. If `JWT_SECRET` is ever unset in a real environment, the application **boots silently** on a secret that is in the source tree — anyone can forge an access token with arbitrary `authorities` and `tenant` claims and gain full admin over any tenant. The same pattern applies to `spring.datasource.password` (`:erp`, line 11).

A missing-secret condition should *fail loudly*, not fall back to a guessable key.

**Recommendation**
- Remove the inline default; let startup fail if `JWT_SECRET` is absent. With a typed property you can enforce this (e.g. a `@PostConstruct` check rejecting null/short/known-default secrets, or fail-fast for any profile other than a local `dev`).
- Require ≥ 32 bytes of entropy (HMAC-SHA256) and reject the shipped dev value explicitly.
- Same treatment for the DB password default.

---

### 2. Medium — Brute-force / rate-limit bypass via spoofed `X-Forwarded-For`

**File:** `src/main/java/com/company/erp/security/ratelimit/RateLimitFilter.java:80-86`

```java
private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (StringUtils.hasText(forwarded)) {
        return forwarded.split(",")[0].trim();   // trusted unconditionally
    }
    return request.getRemoteAddr();
}
```

The auth rate-limit bucket (default **5/min**) is keyed on this value. Because `X-Forwarded-For` is attacker-controlled and trusted without verifying the request actually came through a trusted proxy, an attacker can send a **unique forged XFF on every request** and always land in a fresh bucket — defeating the per-IP throttle on `/api/v1/auth/login`, `/refresh`, and the password-reset endpoints.

Per-account lockout (`LoginAttemptService`, 5 failures → 15 min) still backstops brute force against *existing* accounts, but the IP limiter is the only control protecting:
- credential stuffing across *many* usernames,
- `password/reset-request` flooding,
- and it also feeds Finding #5 (unbounded bucket map).

**Recommendation**
- Only honour `X-Forwarded-For` when the immediate peer (`getRemoteAddr()`) is a known proxy. Spring Boot already has `forward-headers-strategy: framework` set — prefer deriving the client IP from `request.getRemoteAddr()` *after* a configured `ForwardedHeaderFilter` / trusted-proxy list, rather than re-parsing the raw header here.
- If you must parse XFF, validate the hop is from your ingress/CIDR allowlist before trusting it.

---

### 3. Medium — Privilege escalation through unrestricted role assignment

**Files:** `src/main/java/com/company/erp/users/UserService.java:102-104` and `:77`; gated only by `UserController` `@PreAuthorize("hasAuthority('USER_UPDATE')")` / `USER_CREATE`.

```java
if (request.roleIds() != null) {
    user.setRoles(resolveRoles(request.roleIds()));   // any role, incl. ADMIN
}
```

`resolveRoles` loads roles by id and assigns them with **no check that the caller holds the permissions being granted**, and update does **not** prevent a user from modifying **their own** roles. A user holding only `USER_UPDATE` (but not `ROLE_*`) can therefore:
- grant themselves the seeded **ADMIN** role (all permissions), or
- elevate any other user.

This converts a mid-tier "user admin" permission into full tenant takeover. (Note: `delete()` *does* guard self-deletion — the same defensive instinct is just missing for role elevation.)

**Recommendation**
- Gate role assignment behind a dedicated permission (e.g. `USER_ASSIGN_ROLE`) separate from profile edits.
- Enforce that the caller's own authority set is a **superset** of the permissions contained in the roles being granted (no granting what you don't hold).
- Forbid a user from changing their **own** roles/status; require another admin.

---

### 4. Low — Anonymous access to `/actuator/metrics/**` and `/actuator/prometheus`

**File:** `src/main/java/com/company/erp/security/SecurityConfig.java:58-62`

These endpoints are in `PUBLIC_ENDPOINTS`. `/actuator/metrics/**` exposes internal operational detail (heap, datasource pool saturation, HTTP route names and call counts) to unauthenticated callers, which aids reconnaissance. The code comment already acknowledges this and recommends an internal management port.

**Recommendation**
- In production, bind Actuator to a separate management port on an internal network and firewall it off (as the comment notes), **or** require authentication / a scrape token for these paths. Keep only `/actuator/health/**` and `/actuator/info` public if needed.

---

### 5. Low — Unbounded in-memory rate-limit bucket map (DoS)

**File:** `src/main/java/com/company/erp/security/ratelimit/RateLimitService.java:25`

```java
private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
```

Buckets are created per client key and **never evicted**. Combined with Finding #2 (spoofable key), an attacker can mint unbounded distinct keys and grow the map until the JVM OOMs.

**Recommendation**
- Use a size/time-bounded cache (e.g. Caffeine with `expireAfterAccess`) for the bucket map, or move to the Bucket4j Redis proxy-manager (also needed for multi-instance correctness, as the class comment notes).

---

### 6. Low — Password-reset token written to debug log

**File:** `src/main/java/com/company/erp/auth/AuthService.java:196-198`

```java
if (log.isDebugEnabled()) {
    log.debug("DEV-ONLY reset token for {}: {}", user.getEmail(), token);
}
```

The single-use reset token is high-entropy and short-lived, but logging the live credential means anyone with log access (or a misconfigured debug level in a shared/staging environment) can hijack the reset. It's labelled DEV-ONLY, but it's a latent sensitive-data-exposure that can ship by accident.

**Recommendation**
- Remove the line entirely; deliver the token only via the out-of-band mailer. If a dev hook is required, gate it behind an explicit non-production profile, not a log level.

---

### 7. Info — CORS `allowCredentials=true`

**File:** `src/main/java/com/company/erp/security/SecurityConfig.java:149-156`

Configuration is correct today: origins come from an explicit allowlist (`securityProperties.getCors().getAllowedOrigins()`), not a wildcard, so credentialed CORS is safe. Flagging only as a guardrail: ensure `CORS_ORIGINS` is **never** set to `*` in any environment, since `*` + credentials is both insecure and rejected by browsers. Consider validating the property at startup.

---

## What the review verified as solid (no action needed)

- **SQL injection:** No native/concatenated queries anywhere (`grep` for `@Query`/`createQuery`/`nativeQuery` returned nothing). Filtering uses the JPA Criteria API (`UserSpecifications`) with bound parameters; lookups are Spring Data derived queries. Tenant isolation is enforced by Hibernate's `@TenantId` discriminator, not string-built SQL.
- **JWT handling:** jjwt `verifyWith(SecretKey)` enforces HMAC and rejects `alg:none`/algorithm-confusion; issuer is required; expiry checked; refresh tokens carry no authorities and are server-tracked with rotation + reuse-family revocation; logout blacklists the JTI; password change/reset sets a global revoke-before watermark.
- **Authorization:** Every `UserController`/`RoleController` endpoint is `@PreAuthorize`-gated against fine-grained permissions; `@EnableMethodSecurity` is on. Authorities are rebuilt from the verified token (no DB on the hot path). (See Finding #3 for the one gap.)
- **Tenant integrity:** The verified token's `tenant` claim is authoritative and overrides the client `X-Tenant-ID` header in `JwtAuthenticationFilter`; `TenantFilter` clears context in a `finally` to prevent thread-pool leakage; `TenantResolver` validates the header against a strict slug pattern.
- **Sensitive-data exposure:** `GlobalExceptionHandler` never returns stack traces/internal messages (generic body + correlatable `traceId`); `server.error.include-stacktrace/message: never`; `UserResponse` omits the password hash and security counters.
- **XSS:** JSON-only API (no server-side HTML), strict CSP (`default-src 'none'`), `nosniff`, frame-deny; `InputSanitizer` strips control/CRLF chars as defense-in-depth. Output encoding remains the frontend's responsibility.
- **Password policy & anti-enumeration:** `StrongPasswordValidator` (≥12 chars, mixed classes); `hideUserNotFoundExceptions`; identical generic `AuthenticationFailedException` for all login failure modes; reset-request always returns 202.
- **Secrets sourcing:** All real secrets bind from env vars / Docker secrets; `SecurityProperties` holds no literals (the issue is only the *fallback defaults* in `application.yml`, Finding #1).

---

## Suggested remediation order

1. **#1** — Remove hardcoded JWT/DB fallback secrets; fail fast on missing `JWT_SECRET`. *(token forgery risk)*
2. **#2** — Stop trusting raw `X-Forwarded-For`; derive client IP via trusted-proxy config. *(brute-force control)*
3. **#3** — Separate-permission + superset check + self-elevation guard for role assignment. *(privilege escalation)*
4. **#4, #5, #6** — Lock down Actuator, bound the bucket cache, drop the reset-token debug log.
5. **#7** — Add a startup guard against wildcard CORS origins.
