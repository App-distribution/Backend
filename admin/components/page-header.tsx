import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
  className,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  actions?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-col gap-4 rounded-[var(--radius)] border border-[var(--border)] bg-white px-5 py-5 shadow-[var(--shadow-soft)] lg:flex-row lg:items-start lg:justify-between", className)}>
      <div>
        {eyebrow ? <p className="text-xs uppercase tracking-[0.2em] text-[var(--text-muted)]">{eyebrow}</p> : null}
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">{title}</h1>
        <p className="mt-2 max-w-3xl text-sm leading-6 text-[var(--text-muted)]">{description}</p>
      </div>
      {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
    </div>
  );
}
