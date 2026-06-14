import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { login as apiLogin, logout as apiLogout } from "@/lib/api/auth";
import { AUTH_LOGOUT_EVENT, refreshAccessToken } from "@/lib/api/client";
import { tokenStore } from "@/lib/auth/token-store";
import { decodeJwt, type AccessTokenClaims } from "@/lib/auth/jwt";
import type { LoginPayload } from "@/types/api";

export interface AuthUser {
  id: string;
  email: string;
  tenant: string;
  permissions: string[];
}

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  signIn: (payload: LoginPayload, tenant: string) => Promise<void>;
  signOut: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  hasAnyPermission: (permissions: string[]) => boolean;
  hasAllPermissions: (permissions: string[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readUserFromAccessToken(): AuthUser | null {
  const claims = decodeJwt<AccessTokenClaims>(tokenStore.getAccessToken());
  if (!claims) return null;
  return {
    id: claims.sub,
    email: claims.email,
    tenant: claims.tenant,
    permissions: claims.authorities ?? [],
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  const applySession = useCallback(() => {
    const nextUser = readUserFromAccessToken();
    setUser(nextUser);
    setStatus(nextUser ? "authenticated" : "unauthenticated");
  }, []);

  // Bootstrap exactly once: a persisted (remember-me) refresh token silently restores the
  // session; otherwise we start unauthenticated.
  const bootstrapped = useRef(false);
  useEffect(() => {
    if (bootstrapped.current) return;
    bootstrapped.current = true;

    const persisted = tokenStore.hydrate();
    if (!persisted) {
      setStatus("unauthenticated");
      return;
    }
    refreshAccessToken()
      .then(() => applySession())
      .catch(() => {
        tokenStore.clear();
        setUser(null);
        setStatus("unauthenticated");
      });
  }, [applySession]);

  // The API layer broadcasts a forced logout when a refresh ultimately fails.
  useEffect(() => {
    const handler = () => {
      setUser(null);
      setStatus("unauthenticated");
    };
    window.addEventListener(AUTH_LOGOUT_EVENT, handler);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, handler);
  }, []);

  const signIn = useCallback(
    async (payload: LoginPayload, tenant: string) => {
      await apiLogin(payload, tenant);
      applySession();
    },
    [applySession],
  );

  const signOut = useCallback(async () => {
    await apiLogout();
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  const permissionSet = useMemo(() => new Set(user?.permissions ?? []), [user]);
  const hasPermission = useCallback((p: string) => permissionSet.has(p), [permissionSet]);
  const hasAnyPermission = useCallback(
    (perms: string[]) => perms.some((p) => permissionSet.has(p)),
    [permissionSet],
  );
  const hasAllPermissions = useCallback(
    (perms: string[]) => perms.every((p) => permissionSet.has(p)),
    [permissionSet],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      signIn,
      signOut,
      hasPermission,
      hasAnyPermission,
      hasAllPermissions,
    }),
    [user, status, signIn, signOut, hasPermission, hasAnyPermission, hasAllPermissions],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an <AuthProvider>");
  }
  return ctx;
}
