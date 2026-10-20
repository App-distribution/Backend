"use client";

import { forwardRef } from "react";
import { cn } from "@/lib/utils";

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
};

const variantClasses: Record<NonNullable<ButtonProps["variant"]>, string> = {
  primary:
    "bg-[var(--primary)] text-[var(--primary-contrast)] hover:bg-[var(--primary-hover)]",
  secondary: "border border-[var(--border)] bg-[var(--surface)] text-[var(--text-strong)] hover:bg-[var(--surface-muted)]",
  ghost: "text-[var(--text)] hover:bg-[var(--surface-muted)] hover:text-[var(--text-strong)]",
  danger: "bg-[var(--danger)] text-[var(--danger-contrast)] hover:bg-[var(--danger-hover)]",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = "primary", type = "button", ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      className={cn(
        "inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors sm:min-h-10 disabled:cursor-not-allowed disabled:opacity-50",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
});
