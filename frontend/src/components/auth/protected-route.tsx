import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/lib/auth/auth-provider";
import { FullScreenLoader } from "@/components/common/full-screen-loader";

interface ProtectedRouteProps {
  children: ReactNode;
  /** If set, the user must hold at least one of these permissions. */
  anyOf?: string[];
  /** If set, the user must hold all of these permissions. */
  allOf?: string[];
}

/**
 * Route guard: blocks unauthenticated users (redirect to /login, remembering the target),
 * and authenticated-but-unauthorized users (redirect to /403). It is a UX gate layered on
 * top of the server's authorization, not a replacement for it.
 */
export function ProtectedRoute({ children, anyOf, allOf }: ProtectedRouteProps) {
  const { status, hasAnyPermission, hasAllPermissions } = useAuth();
  const location = useLocation();

  if (status === "loading") {
    return <FullScreenLoader />;
  }
  if (status === "unauthenticated") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (anyOf && anyOf.length > 0 && !hasAnyPermission(anyOf)) {
    return <Navigate to="/403" replace />;
  }
  if (allOf && allOf.length > 0 && !hasAllPermissions(allOf)) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}
