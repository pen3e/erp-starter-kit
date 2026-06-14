import type { ReactNode } from "react";
import { useAuth } from "@/lib/auth/auth-provider";

interface CanProps {
  /** Require this single permission. */
  permission?: string;
  /** Require at least one of these permissions. */
  anyOf?: string[];
  /** Require all of these permissions. */
  allOf?: string[];
  /** Rendered when the check fails (defaults to nothing). */
  fallback?: ReactNode;
  children: ReactNode;
}

/**
 * Declaratively render UI only when the current user holds the required permission(s).
 * This is a convenience for hiding controls — never a security boundary. The backend
 * re-authorizes every request regardless of what the UI shows.
 */
export function Can({ permission, anyOf, allOf, fallback = null, children }: CanProps) {
  const { hasPermission, hasAnyPermission, hasAllPermissions } = useAuth();

  let allowed = true;
  if (permission) allowed = hasPermission(permission);
  if (allowed && anyOf) allowed = hasAnyPermission(anyOf);
  if (allowed && allOf) allowed = hasAllPermissions(allOf);

  return <>{allowed ? children : fallback}</>;
}
