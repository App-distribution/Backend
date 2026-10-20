"use client";

import { cn } from "@/lib/utils";
import { useTheme } from "@/lib/theme-provider";
import type { ThemePreference } from "@/lib/theme";
import { Icon } from "@/components/icons";

const options: Array<{ value: ThemePreference; label: string }> = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
];

export function ThemeToggle({ className, compact = false }: { className?: string; compact?: boolean }) {
  const { theme, resolvedTheme, setTheme } = useTheme();

  if (compact) {
    const nextTheme: ThemePreference = theme === "system" ? "light" : theme === "light" ? "dark" : "system";
    return (
      <button
        aria-label={`Тема: ${theme}. Переключить на ${nextTheme}`}
        className={cn("grid size-11 place-items-center rounded-lg border border-[var(--border)] bg-[var(--surface)] text-[var(--text-strong)] transition-colors hover:bg-[var(--surface-muted)] sm:size-10", className)}
        onClick={() => setTheme(nextTheme)}
        title={`Тема: ${theme} (${resolvedTheme})`}
        type="button"
      >
        <Icon className="size-[18px]" name="sun-moon" />
      </button>
    );
  }

  return (
    <div
      aria-label={`Theme switcher, current ${theme} (${resolvedTheme})`}
      className={cn(
        "inline-flex items-center gap-1 rounded-lg border border-[var(--border)] bg-[var(--surface)] p-1",
        className,
      )}
      role="group"
    >
      {options.map((option) => {
        const isActive = theme === option.value;

        return (
          <button
            key={option.value}
            aria-pressed={isActive}
            className={cn(
              "rounded-md px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.08em] transition",
              isActive
                ? "bg-[var(--primary)] text-[var(--primary-contrast)]"
                : "text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text-strong)]",
            )}
            onClick={() => setTheme(option.value)}
            type="button"
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
