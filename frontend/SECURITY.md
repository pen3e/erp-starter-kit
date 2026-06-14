# Frontend security model

This document explains the security decisions baked into the frontend and what you must do
when deploying it. The backend remains the single source of truth — the UI never makes a
security decision the server doesn't independently enforce.

## Authentication & token handling

- **Access token: in memory only.** Stored in a module variable (`token-store.ts`), never in
  `localStorage`/`sessionStorage`/cookies. A page reload drops it; we silently re-mint it from
  the refresh token. This keeps it out of reach of a persisted-XSS payload running on a later
  page load.
- **Refresh token: in memory by default**, persisted to `localStorage` **only** when the user
  ticks “remember me”. Without it, the session ends when the tab is closed.
- **Rotation & reuse detection** is handled by the backend; the client persists the rotated
  refresh token after each refresh.
- **Single-flight refresh:** concurrent 401s trigger exactly one `/auth/refresh`; all pending
  requests await it and are retried once (`client.ts`).
- **Forced logout:** if a refresh ultimately fails, the API layer clears tokens and broadcasts
  `erp:auth-logout`; the `AuthProvider` tears down state and the router redirects to `/login`.

> **Strongest option (recommended for high-security deployments):** have the backend deliver
> the refresh token in a `Secure; HttpOnly; SameSite=Strict` cookie instead of the JSON body.
> JavaScript then never touches it. This requires a small backend change and a CSRF token for
> the refresh call. The current design is a pragmatic fit for the token-in-body API.

## CSRF

API calls authenticate with an `Authorization: Bearer` header (not cookies), so they are not
vulnerable to CSRF. `withCredentials` is `false`. If you migrate to cookie-based refresh, add
CSRF protection for the cookie-authenticated endpoint(s).

## Authorization in the UI

- Permissions are read from the verified access-token claims (`authorities`) for **display
  gating only** (`<Can>`, `usePermissions`, `<ProtectedRoute>`).
- Hiding a button is **not** a security control. Every users/roles/audit endpoint is
  `@PreAuthorize`-gated on the server and re-checked on each request.

## XSS

- React escapes all rendered values; the app never uses `dangerouslySetInnerHTML`.
- Serve a strict **Content-Security-Policy** as an HTTP header from your reverse proxy / host
  (a `<meta>` CSP is weaker and breaks Vite HMR). Recommended:

  ```
  Content-Security-Policy:
    default-src 'self';
    connect-src 'self' https://api.your-domain;
    img-src 'self' data:;
    style-src 'self' 'unsafe-inline';
    font-src 'self';
    object-src 'none';
    frame-ancestors 'none';
    base-uri 'self';
    form-action 'self'
  ```

  (`style-src 'unsafe-inline'` is required because Radix/Tailwind set inline styles. There is
  no inline script.)

- Also send: `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`,
  `Strict-Transport-Security` (HTTPS), and a restrictive `Permissions-Policy`.

## Transport & config

- Always serve the app over HTTPS in production.
- `VITE_*` variables are bundled into public JS — **never** put secrets there.
- Point `VITE_API_BASE_URL` at the backend origin in production; in dev the Vite proxy keeps
  the browser on a single origin (`/api` → `:8080`).
