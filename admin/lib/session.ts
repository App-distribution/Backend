import type { AuthTokens } from "@/lib/types";

const ACCESS_TOKEN_KEY = "appdist.admin.access-token";
const REFRESH_TOKEN_KEY = "appdist.admin.refresh-token";

function canUseStorage() {
  return typeof window !== "undefined";
}

export function getStoredTokens(): AuthTokens | null {
  if (!canUseStorage()) return null;
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY);
  const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!accessToken || !refreshToken) return null;
  return { accessToken, refreshToken };
}

export function setStoredTokens(tokens: AuthTokens) {
  if (!canUseStorage()) return;
  window.localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearStoredTokens() {
  if (!canUseStorage()) return;
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}
