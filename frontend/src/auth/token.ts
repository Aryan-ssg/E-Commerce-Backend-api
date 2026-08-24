const ACCESS = 'accessToken';
const REFRESH = 'refreshToken';

export const tokenStorage = {
  getAccessToken: () => localStorage.getItem(ACCESS),
  getRefreshToken: () => localStorage.getItem(REFRESH),
  setTokens: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS, access);
    localStorage.setItem(REFRESH, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
  },
};

// Decode the (unverified) JWT payload. We only read the `role` claim to decide
// client-side routing; the backend remains the authority on every request.
export function decodeJwtPayload(token?: string | null): Record<string, unknown> | null {
  if (!token) return null;
  try {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(part));
  } catch {
    return null;
  }
}
