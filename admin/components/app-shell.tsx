"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useMemo, useState, type ReactNode } from "react";
import { BrandMark } from "@/components/brand-mark";
import { Icon, type IconName } from "@/components/icons";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/lib/auth-context";
import { useMeQuery, useWorkspaceQuery } from "@/lib/queries";
import { cn } from "@/lib/utils";

type NavigationItem = {
  href: string;
  label: string;
  icon: IconName;
  group: "workspace" | "manage";
  roles?: Array<"ADMIN" | "UPLOADER" | "TESTER" | "VIEWER">;
};

const navigationItems: NavigationItem[] = [
  { href: "/overview", label: "Обзор", icon: "layout-dashboard", group: "workspace" },
  { href: "/projects", label: "Проекты", icon: "boxes", group: "workspace" },
  { href: "/builds", label: "Сборки", icon: "package", group: "workspace" },
  { href: "/upload", label: "Загрузить APK", icon: "cloud-upload", group: "workspace", roles: ["ADMIN", "UPLOADER"] },
  { href: "/team", label: "Команда", icon: "users", group: "workspace", roles: ["ADMIN"] },
  { href: "/audit", label: "Аудит", icon: "shield-check", group: "manage" },
  { href: "/system", label: "Система", icon: "settings", group: "manage", roles: ["ADMIN"] },
  { href: "/profile", label: "Профиль", icon: "user", group: "manage" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { logout } = useAuth();
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const [mobileOpen, setMobileOpen] = useState(false);

  const role = meQuery.data?.role;
  const filteredItems = useMemo(
    () => navigationItems.filter((item) => !item.roles || (role ? item.roles.includes(role) : false)),
    [role],
  );

  const activeItem = filteredItems.find((item) => pathname === item.href || pathname.startsWith(`${item.href}/`));
  const initials = (meQuery.data?.email ?? "AD")
    .split(/[@.\s_-]/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  const signOut = () => {
    logout();
    router.replace("/login");
  };

  return (
    <div className="min-h-screen bg-[var(--background)] lg:grid lg:grid-cols-[244px_minmax(0,1fr)]">
      {mobileOpen ? (
        <button
          aria-label="Закрыть навигацию"
          className="fixed inset-0 z-40 bg-black/30 lg:hidden"
          onClick={() => setMobileOpen(false)}
          type="button"
        />
      ) : null}

      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-50 flex w-[244px] flex-col border-r border-[var(--border-soft)] bg-[var(--surface)] transition-transform duration-200 lg:sticky lg:top-0 lg:h-screen lg:translate-x-0",
          mobileOpen ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-[68px] items-center justify-between px-5">
          <BrandMark href="/overview" />
          <button
            aria-label="Закрыть меню"
            className="grid size-11 place-items-center rounded-lg text-[var(--text-muted)] hover:bg-[var(--surface-muted)] lg:hidden"
            onClick={() => setMobileOpen(false)}
            type="button"
          >
            <Icon className="size-5" name="x" />
          </button>
        </div>

        <div className="mx-4 flex min-h-[58px] items-center gap-3 rounded-lg bg-[var(--surface-muted)] px-3">
          <span className="grid size-8 shrink-0 place-items-center rounded-full bg-[var(--accent)] text-[11px] font-bold text-[#2b2a28]">AD</span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold text-[var(--text-strong)]">{workspaceQuery.data?.name ?? "App Distribution"}</p>
            <p className="mt-0.5 truncate text-[10px] text-[var(--text-muted)]">Release workspace</p>
          </div>
          <Icon className="size-4 text-[var(--text-muted)]" name="chevrons-up-down" />
        </div>

        <div className="app-scrollbar flex-1 overflow-y-auto px-3 pb-5 pt-6">
          <NavigationGroup
            activePath={pathname}
            items={filteredItems.filter((item) => item.group === "workspace")}
            label="Рабочая область"
            onNavigate={() => setMobileOpen(false)}
          />
          <NavigationGroup
            activePath={pathname}
            className="mt-7"
            items={filteredItems.filter((item) => item.group === "manage")}
            label="Управление"
            onNavigate={() => setMobileOpen(false)}
          />

          <p className="mb-2 mt-7 px-2 font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.09em] text-[var(--text-muted)]">
            Публичный доступ
          </p>
          <Link
            className="flex min-h-11 items-center gap-3 rounded-lg px-3 text-[13px] font-medium text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-muted)] hover:text-[var(--text-strong)] lg:min-h-10"
            href="/"
            onClick={() => setMobileOpen(false)}
          >
            <Icon className="size-[18px]" name="external-link" />
            Каталог тестировщиков
          </Link>
        </div>

        <div className="border-t border-[var(--border-soft)] p-4">
          <div className="flex items-center gap-3">
            <span className="grid size-9 shrink-0 place-items-center rounded-full bg-[var(--text-strong)] text-[11px] font-bold text-[var(--surface)]">
              {initials || "AD"}
            </span>
            <Link className="min-w-0 flex-1" href="/profile">
              <p className="truncate text-xs font-semibold text-[var(--text-strong)]">{meQuery.data?.email ?? "Профиль"}</p>
              <p className="mt-0.5 font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.06em] text-[var(--text-muted)]">
                {meQuery.data?.role ?? "—"}
              </p>
            </Link>
            <button
              aria-label="Выйти"
              className="grid size-11 place-items-center rounded-lg text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-muted)] hover:text-[var(--text-strong)] lg:size-9"
              onClick={signOut}
              title="Выйти"
              type="button"
            >
              <Icon className="size-4" name="log-out" />
            </button>
          </div>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-[68px] items-center border-b border-[var(--border-soft)] bg-[var(--background)]/95 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <button
            aria-label="Открыть навигацию"
            className="mr-3 grid size-11 place-items-center rounded-lg border border-[var(--border)] bg-[var(--surface)] lg:hidden"
            onClick={() => setMobileOpen(true)}
            type="button"
          >
            <Icon className="size-5" name="menu" />
          </button>
          <p className="min-w-0 flex-1 truncate text-xs text-[var(--text-muted)]">
            AppDist <span className="px-2 text-[var(--border)]">/</span> {activeItem?.label ?? "Рабочая область"}
          </p>
          <Link
            className="mr-3 hidden h-10 w-[300px] items-center gap-3 rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 text-[13px] text-[var(--text-muted)] transition-colors hover:bg-[var(--surface-muted)] xl:flex"
            href="/builds"
          >
            <Icon className="size-[18px]" name="search" />
            <span className="flex-1">Поиск по сборкам</span>
            <kbd className="font-[var(--font-mono)] text-[10px]">⌘ K</kbd>
          </Link>
          <ThemeToggle compact />
          <span className="ml-3 grid size-9 place-items-center rounded-full bg-[var(--accent)] text-[11px] font-bold text-[#2b2a28]">
            {initials || "AD"}
          </span>
        </header>

        <main className="app-page-enter mx-auto max-w-[1500px] space-y-5 px-4 py-6 sm:px-6 lg:px-8 lg:py-7">{children}</main>
      </div>
    </div>
  );
}

function NavigationGroup({
  activePath,
  className,
  items,
  label,
  onNavigate,
}: {
  activePath: string;
  className?: string;
  items: NavigationItem[];
  label: string;
  onNavigate: () => void;
}) {
  if (!items.length) return null;

  return (
    <nav aria-label={label} className={className}>
      <p className="mb-2 px-2 font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.09em] text-[var(--text-muted)]">
        {label}
      </p>
      <div className="space-y-1">
        {items.map((item) => {
          const isActive = activePath === item.href || activePath.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              className={cn(
                "relative flex min-h-11 items-center gap-3 rounded-lg px-3 text-[13px] font-medium transition-colors lg:min-h-10",
                isActive
                  ? "bg-[var(--surface-muted)] font-semibold text-[var(--text-strong)]"
                  : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text-strong)]",
              )}
              href={item.href}
              onClick={onNavigate}
            >
              <Icon className="size-[18px]" name={item.icon} />
              <span>{item.label}</span>
              {isActive ? <span className="absolute right-2 h-4 w-1 rounded-sm bg-[var(--accent-strong)]" /> : null}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
