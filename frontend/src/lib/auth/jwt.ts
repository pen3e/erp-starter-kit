// Client-side JWT decoding for UI purposes ONLY (showing the user's name, gating menus).
// We never trust these claims for a security decision — the backend verifies the signature
// and re-authorizes every request. We deliberately do not verify the signature here.

export interface AccessTokenClaims {
  sub: string; // userId
  email: string;
  tenant: string;
  type: string; // ACCESS | REFRESH
  authorities?: string[];
  rmb?: boolean; // remember-me (refresh tokens only)
  iat: number;
  exp: number;
  jti: string;
  iss: string;
}

/** Decode a JWT payload without verifying the signature. Returns null on malformed input. */
export function decodeJwt<T = AccessTokenClaims>(token: string | null | undefined): T | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length < 2 || !parts[1]) return null;
  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    const json = new TextDecoder().decode(bytes);
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}

/** True if the token is expired (or within `skewSeconds` of expiry). */
export function isExpired(claims: Pick<AccessTokenClaims, "exp">, skewSeconds = 15): boolean {
  return Date.now() >= claims.exp * 1000 - skewSeconds * 1000;
}
