// Mirror of backend com.company.erp.permissions.PermissionCatalog.
// These gate the UI only — the server is always the source of truth and re-checks every call.

export const Permission = {
  USER_READ: "USER_READ",
  USER_CREATE: "USER_CREATE",
  USER_UPDATE: "USER_UPDATE",
  USER_DELETE: "USER_DELETE",

  ROLE_READ: "ROLE_READ",
  ROLE_CREATE: "ROLE_CREATE",
  ROLE_UPDATE: "ROLE_UPDATE",
  ROLE_DELETE: "ROLE_DELETE",

  PERMISSION_READ: "PERMISSION_READ",

  CLIENT_READ: "CLIENT_READ",
  CLIENT_CREATE: "CLIENT_CREATE",
  CLIENT_UPDATE: "CLIENT_UPDATE",
  CLIENT_DELETE: "CLIENT_DELETE",

  AUDIT_READ: "AUDIT_READ",
} as const;

export type PermissionName = (typeof Permission)[keyof typeof Permission];
