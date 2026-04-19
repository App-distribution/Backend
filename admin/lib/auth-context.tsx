"use client";

import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import { clearStoredTokens, getStoredTokens, setStoredTokens } from "@/lib/session";
import type { AuthTokens } from "@/lib/types";

type AuthContextValue = {
  isAuthenticated: boolean;
  isReady: boolean;
  saveTokens: (tokens: AuthTokens) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isReady, setIsReady] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    setIsAuthenticated(Boolean(getStoredTokens()?.accessToken));
    setIsReady(true);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated,
      isReady,
      saveTokens(tokens) {
        setStoredTokens(tokens);
        setIsAuthenticated(true);
      },
      logout() {
        clearStoredTokens();
        setIsAuthenticated(false);
      },
    }),
    [isAuthenticated, isReady],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
