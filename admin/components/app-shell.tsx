"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useMemo, type ReactNode } from "react";
import { useAuth } from "@/lib/auth-context";
import { useMeQuery, useWorkspaceQuery } from "@/lib/queries";
import { cn } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

const navigationItems = [
  { href: "/overview", label: "Overview" },
  { href: "/projects", label: "Projects" },
  { href: "/builds", label: "Builds" },
  { href: "/upload", label: "Upload" },
  { href: "/team", label: "Team" },
  { href: "/audit", label: "Audit" },
  { href: "/system", label: "System" },
  { href: "/profile", label: "Profile" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { logout } = useAuth();
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();

  const role = meQuery.data?.role;
  const filteredItems = useMemo(() => {
    return navigationItems.filter((item) => {
      if (item.href === "/team" || item.href === "/system") return role === "ADMIN";
      if (item.href === "/upload") return role === "ADMIN" || role === "UPLOADER";
      return true;
    });
  }, [role]);

  return (
    <div className="min-h-screen px-4 py-4 lg:px-6">
      <div className="mx-auto grid max-w-[1600px] gap-4 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="rounded-[calc(var(--radius)+0.25rem)] border border-[var(--border)] bg-[rgba(255,255,255,0.78)] p-4 shadow-[var(--shadow-soft)] backdrop-blur">
          <div className="border-b border-[var(--border)] pb-4">
            <p className="text-xs uppercase tracking-[0.24em] text-[var(--text-muted)]">AppDistribution</p>
            <h1 className="mt-3 text-xl font-semibold text-[var(--text-strong)]">Release Console</h1>
            <p className="mt-1 text-sm text-[var(--text-muted)]">
              {workspaceQuery.data?.name ?? "Workspace"} · {meQuery.data?.email ?? "signed in"}
            </p>
          </div>

          <nav className="mt-4 space-y-1">
            {filteredItems.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "flex items-center justify-between rounded-2xl px-3 py-2.5 text-sm font-medium transition",
                    isActive
                      ? "bg-[var(--primary)] text-white"
                      : "text-[var(--text)] hover:bg-[var(--surface-muted)]",
                  )}
                >
                  <span>{item.label}</span>
                  {item.href === "/audit" ? <Badge tone="neutral">soon</Badge> : null}
                </Link>
              );
            })}
          </nav>

          <div className="mt-6 rounded-3xl bg-[var(--surface-muted)] p-4">
            <p className="text-xs uppercase tracking-[0.16em] text-[var(--text-muted)]">Session</p>
            <p className="mt-3 text-sm text-[var(--text)]">
              Role: <span className="font-semibold text-[var(--text-strong)]">{meQuery.data?.role ?? "—"}</span>
            </p>
            <Button
              variant="secondary"
              className="mt-4 w-full"
              onClick={() => {
                logout();
                router.replace("/login");
              }}
            >
              Sign out
            </Button>
          </div>
        </aside>

        <main className="space-y-4">
          <header className="rounded-[calc(var(--radius)+0.25rem)] border border-[var(--border)] bg-[rgba(255,255,255,0.85)] px-5 py-4 shadow-[var(--shadow-soft)] backdrop-blur">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge tone="primary">{workspaceQuery.data?.slug ?? "workspace"}</Badge>
                  <Badge tone="neutral">DEV</Badge>
                  <Badge tone="neutral">QA</Badge>
                  <Badge tone="neutral">STAGING</Badge>
                  <Badge tone="neutral">PROD_LIKE</Badge>
                </div>
                <p className="mt-3 text-sm text-[var(--text-muted)]">
                  Observe builds, projects, team access and backend delivery flows in one place.
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <div className="rounded-2xl border border-[var(--border)] bg-white px-4 py-2.5 text-sm text-[var(--text-muted)]">
                  Search is scoped inside each page to match the current backend API.
                </div>
                {(role === "ADMIN" || role === "UPLOADER") && (
                  <Button onClick={() => router.push("/upload")}>Upload Build</Button>
                )}
              </div>
            </div>
          </header>
          {children}
        </main>
      </div>
    </div>
  );
}
