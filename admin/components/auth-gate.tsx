"use client";

import { usePathname, useRouter } from "next/navigation";
import type { ReactNode } from "react";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";
import { useMeQuery } from "@/lib/queries";

export function AuthGate({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { isAuthenticated, isReady, logout } = useAuth();
  const meQuery = useMeQuery(isAuthenticated);

  useEffect(() => {
    if (isReady && !isAuthenticated) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [isAuthenticated, isReady, pathname, router]);

  useEffect(() => {
    if (meQuery.error) {
      logout();
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [logout, meQuery.error, pathname, router]);

  if (!isReady || !isAuthenticated || meQuery.isLoading) {
    return (
      <div className="app-gradient flex min-h-screen items-center justify-center px-6">
        <div className="rounded-xl border border-[var(--border)] bg-[var(--surface)] px-8 py-6 text-sm text-[var(--text-muted)] shadow-[var(--shadow-soft)]">
          Открываем рабочую область…
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
