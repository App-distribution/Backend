"use client";

import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";
import { useMeQuery } from "@/lib/queries";

export function AuthGate({ children }: { children: ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, isReady, logout } = useAuth();
  const meQuery = useMeQuery(isAuthenticated);

  useEffect(() => {
    if (isReady && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isReady, router]);

  useEffect(() => {
    if (meQuery.error) {
      logout();
      router.replace("/login");
    }
  }, [logout, meQuery.error, router]);

  if (!isReady || !isAuthenticated || meQuery.isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="rounded-3xl border border-[var(--border)] bg-[var(--surface)] px-8 py-6 text-sm text-[var(--text-muted)] shadow-[var(--shadow-soft)]">
          Loading admin console…
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
