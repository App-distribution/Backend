import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

const tones = {
  neutral: "bg-[var(--badge-neutral-bg)] text-[var(--badge-neutral-text)]",
  primary: "bg-[var(--badge-primary-bg)] text-[var(--badge-primary-text)]",
  success: "bg-[var(--badge-success-bg)] text-[var(--badge-success-text)]",
  warning: "bg-[var(--badge-warning-bg)] text-[var(--badge-warning-text)]",
  danger: "bg-[var(--badge-danger-bg)] text-[var(--badge-danger-text)]",
};

export function Badge({
  children,
  tone = "neutral",
  className,
}: {
  children: ReactNode;
  tone?: keyof typeof tones;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.12em]",
        tones[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
