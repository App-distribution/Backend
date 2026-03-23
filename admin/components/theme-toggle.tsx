"use client";

import { cn } from "@/lib/utils";
import { useTheme } from "@/lib/theme-provider";
import type { ThemePreference } from "@/lib/theme";

const options: Array<{ value: ThemePreference; label: string }> = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
];

export function ThemeToggle({ className }: { className?: string }) {
  const { theme, resolvedTheme, setTheme } = useTheme();

  return (
    <div
      aria-label={`Theme switcher, current ${theme} (${resolvedTheme})`}
      className={cn(
        "inline-flex items-center gap-1 rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-1 shadow-[var(--shadow-soft)]",
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
              "rounded-[calc(var(--radius)-0.5rem)] px-3 py-2 text-xs font-semibold uppercase tracking-[0.14em] transition",
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
