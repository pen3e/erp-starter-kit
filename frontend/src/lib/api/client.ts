import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { tokenStore } from "@/lib/auth/token-store";
import { decodeJwt, type AccessTokenClaims } from "@/lib/auth/jwt";
import type { TokenResponse } from "@/types/api";

// Empty base URL in dev -> requests like "/api/v1/users" are served through the Vite proxy.
// In production set VITE_API_BASE_URL to the backend origin.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: false, // we use Bearer tokens, not cookies
});

// A bare client used only for the refresh call, so it never triggers the response interceptor
// (which would otherwise recurse on a 401 from /refresh).
const refreshClient = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

/** Broadcast a forced logout so the AuthProvider can tear down state and redirect. */
export const AUTH_LOGOUT_EVENT = "erp:auth-logout";
function emitForcedLogout(): void {
  window.dispatchEvent(new CustomEvent(AUTH_LOGOUT_EVENT));
}

// --- Single-flight refresh -------------------------------------------------
// Many requests can 401 at once; we must refresh exactly once and let them all await it.
let refreshPromise: Promise<string> | null = null;

async function doRefresh(): Promise<string> {
  const refresh = tokenStore.getRefreshToken();
  if (!refresh) throw new Error("No refresh token");

  const { data } = await refreshClient.post<TokenResponse>("/api/v1/auth/refresh", {
    refreshToken: refresh,
  });

  // The backend rotates the refresh token on every use; persist the new one iff it is a
  // remember-me token (the only case we persisted the previous one).
  const claims = decodeJwt<AccessTokenClaims>(data.refreshToken);
  tokenStore.setTokens(data.accessToken, data.refreshToken, Boolean(claims?.rmb));
  return data.accessToken;
}

export function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

// --- Interceptors ----------------------------------------------------------
api.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const url = original?.url ?? "";
    const isAuthCall = url.includes("/auth/login") || url.includes("/auth/refresh");

    if (status === 401 && original && !original._retry && !isAuthCall && tokenStore.getRefreshToken()) {
      original._retry = true;
      try {
        const newAccess = await refreshAccessToken();
        original.headers.Authorization = `Bearer ${newAccess}`;
        return api(original);
      } catch {
        tokenStore.clear();
        emitForcedLogout();
        return Promise.reject(error);
      }
    }

    if (status === 401 && !isAuthCall) {
      tokenStore.clear();
      emitForcedLogout();
    }

    return Promise.reject(error);
  },
);
