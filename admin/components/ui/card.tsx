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
        "rounded-[var(--radius)] border border-[var(--border)] bg-[var(--surface)] shadow-[var(--shadow-soft)]",
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
    <div className={cn("flex items-start justify-between gap-4 border-b border-[var(--border)] px-5 py-4", className)}>
      <div>
        <h2 className="text-base font-semibold text-[var(--text-strong)]">{title}</h2>
        {description ? <p className="mt-1 text-sm text-[var(--text-muted)]">{description}</p> : null}
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
  return <div className={cn("px-5 py-4", className)}>{children}</div>;
}
