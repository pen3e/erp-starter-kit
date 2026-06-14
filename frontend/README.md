# ERP Starter Kit — Frontend

A secure, permission-aware admin console for the ERP Starter Kit API, built with React,
TypeScript, Vite and shadcn/ui.

## Stack

- **React 18 + TypeScript + Vite**
- **shadcn/ui** (Radix UI + Tailwind CSS) — hand-vendored components in `src/components/ui`
- **TanStack Query** for server state, caching and pagination
- **React Router** with permission-gated route guards
- **React Hook Form + Zod** for typed, validated forms
- **Axios** with auth/refresh interceptors

## Features

- JWT login with “remember me”, silent single-flight token refresh, and clean forced-logout.
- Permission-driven UI: navigation, routes and action buttons are gated by the user's
  permissions (`<Can>`, `<ProtectedRoute>`), mirroring the backend `PermissionCatalog`.
- Users module: search/filter, paginated table, create/edit (profile, status, roles), delete.
- Roles module: create/edit with a permission picker; system roles are protected.
- Permissions catalogue (read-only) and a paginated audit-log viewer.
- Accessible dialogs, toasts, empty/loading states.

See **[SECURITY.md](./SECURITY.md)** for the full security model (token storage, CSP, CSRF).

## Getting started

Requires Node 20+ and the backend running (see the root README).

```bash
cp .env.example .env.local   # optional
npm install
npm run dev                  # http://localhost:5173 (proxies /api -> :8080)
```

Other scripts:

```bash
npm run build       # typecheck (tsc) + production build to dist/
npm run typecheck   # types only
npm run preview     # serve the production build locally
```

Log in with the demo tenant seeded by the backend (`BOOTSTRAP_DEMO_DATA=true`):
tenant `demo`, email `admin@demo.local`, password `ChangeMe!2024`.

## Project structure

```
src/
  lib/
    api/        axios client (+ refresh) and one module per resource
    auth/       token store, JWT decode, permission catalog, AuthProvider
    utils.ts    cn() + formatters · validation.ts (Zod schemas)
  components/
    ui/         shadcn primitives (button, dialog, table, select, …)
    auth/       Can, ProtectedRoute, ChangePasswordDialog
    layout/     AppLayout, Sidebar, Topbar, UserMenu
    common/     PageHeader, ConfirmDialog, CheckboxList, FieldError
  pages/        login, dashboard, users/, roles/, permissions, audit, 403, 404
  router.tsx    route tree with guards   ·   App.tsx / main.tsx   entry
```

## Security at a glance

- Access token in memory only; refresh token persisted solely for “remember me”.
- UI permission checks are convenience only — the server authorizes every request.
- Serve a strict CSP and HTTPS in production (see SECURITY.md).
