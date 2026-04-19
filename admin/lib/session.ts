import type { AuthTokens } from "@/lib/types";

const ACCESS_TOKEN_KEY = "appdist.admin.access-token";
const REFRESH_TOKEN_KEY = "appdist.admin.refresh-token";
const INVALID_TOKEN_VALUES = new Set(["undefined", "null"]);

function canUseStorage() {
  return typeof window !== "undefined";
}

function normalizeToken(value: string | null | undefined) {
  if (!value) return null;
  const normalized = value.trim();
  if (!normalized || INVALID_TOKEN_VALUES.has(normalized.toLowerCase())) {
    return null;
  }
  return normalized;
}

export function getStoredTokens(): AuthTokens | null {
  if (!canUseStorage()) return null;
  const accessToken = normalizeToken(window.localStorage.getItem(ACCESS_TOKEN_KEY));
  const refreshToken = normalizeToken(window.localStorage.getItem(REFRESH_TOKEN_KEY));
  if (!accessToken || !refreshToken) {
    clearStoredTokens();
    return null;
  }
  return { accessToken, refreshToken };
}

export function setStoredTokens(tokens: AuthTokens) {
  if (!canUseStorage()) return;
  const accessToken = normalizeToken(tokens.accessToken);
  const refreshToken = normalizeToken(tokens.refreshToken);
  if (!accessToken || !refreshToken) {
    clearStoredTokens();
    return;
  }
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearStoredTokens() {
  if (!canUseStorage()) return;
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  window.localStorage.removeItem(REFRESH_TOKEN_KEY);
}
