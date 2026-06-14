import { api } from "@/lib/api/client";
import { tokenStore } from "@/lib/auth/token-store";
import type { LoginPayload, TokenResponse } from "@/types/api";

export async function login(payload: LoginPayload, tenant: string): Promise<TokenResponse> {
  const { data } = await api.post<TokenResponse>("/api/v1/auth/login", payload, {
    headers: { "X-Tenant-ID": tenant },
  });
  tokenStore.setTenant(tenant);
  tokenStore.setTokens(data.accessToken, data.refreshToken, payload.rememberMe);
  return data;
}

export async function logout(): Promise<void> {
  const refreshToken = tokenStore.getRefreshToken();
  try {
    await api.post("/api/v1/auth/logout", { refreshToken });
  } catch {
    // Best effort — we always clear local state below.
  } finally {
    tokenStore.clear();
  }
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.post("/api/v1/auth/password/change", { currentPassword, newPassword });
}
