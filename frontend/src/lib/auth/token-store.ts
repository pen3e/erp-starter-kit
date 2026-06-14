// Token storage strategy (security-critical):
//
//  * ACCESS token  -> in memory only, never persisted. A reload drops it (we silently
//                     re-derive it from the refresh token). This keeps it out of reach of
//                     persisted-XSS payloads that run on a later page load.
//  * REFRESH token -> in memory by default. Persisted to localStorage ONLY when the user
//                     opts into "remember me", so the session survives a reload.
//
// This is a pragmatic trade-off for a token-in-body backend. The strongest option is for the
// backend to set the refresh token in a Secure; HttpOnly; SameSite=Strict cookie — see
// frontend/SECURITY.md. Because we authenticate with a Bearer header (not cookies), CSRF does
// not apply to API calls.

const REFRESH_KEY = "erp.refresh";
const TENANT_KEY = "erp.tenant";

let accessToken: string | null = null;
let refreshToken: string | null = null;

export const tokenStore = {
  getAccessToken: (): string | null => accessToken,
  getRefreshToken: (): string | null => refreshToken,
  getTenant: (): string | null => safeGet(TENANT_KEY),

  setTokens(access: string, refresh: string, persist: boolean): void {
    accessToken = access;
    refreshToken = refresh;
    if (persist) safeSet(REFRESH_KEY, refresh);
    else safeRemove(REFRESH_KEY);
  },

  setAccessToken(access: string): void {
    accessToken = access;
  },

  setTenant(tenant: string): void {
    safeSet(TENANT_KEY, tenant);
  },

  /** On app start, promote a persisted refresh token (remember-me) into memory. */
  hydrate(): string | null {
    const persisted = safeGet(REFRESH_KEY);
    if (persisted) refreshToken = persisted;
    return persisted;
  },

  /** Wipe all session state. Tenant is kept to pre-fill the next login. */
  clear(): void {
    accessToken = null;
    refreshToken = null;
    safeRemove(REFRESH_KEY);
  },
};

function safeGet(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}
function safeSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* storage unavailable (private mode quota) — degrade to in-memory only */
  }
}
function safeRemove(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}
