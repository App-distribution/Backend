import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Card({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return (
    <div
      className={cn(
        "rounded-[var(--radius)] border border-[var(--border-soft)] bg-[var(--surface)] shadow-[var(--shadow-soft)]",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function CardHeader({
  className,
  title,
  description,
  action,
}: {
  className?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className={cn("flex items-start justify-between gap-4 border-b border-[var(--border-soft)] px-6 py-5", className)}>
      <div>
        <h2 className="font-[var(--font-display)] text-lg font-bold tracking-[-0.015em] text-[var(--text-strong)]">{title}</h2>
        {description ? <p className="mt-1 text-[13px] leading-5 text-[var(--text-muted)]">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function CardContent({
  className,
  children,
}: {
  className?: string;
  children: ReactNode;
}) {
  return <div className={cn("px-6 py-5", className)}>{children}</div>;
}
