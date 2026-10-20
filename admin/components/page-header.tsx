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
    <div className={cn("app-gradient flex min-h-[154px] flex-col gap-6 overflow-hidden rounded-[14px] border border-[#dad7e8] px-7 py-7 lg:flex-row lg:items-start lg:justify-between dark:border-[var(--border)]", className)}>
      <div>
        {eyebrow ? <span className="sr-only">{eyebrow}</span> : null}
        <h1 className="font-[var(--font-display)] text-[clamp(2rem,3vw,2.5rem)] font-bold leading-none tracking-[-0.025em]">{title}</h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-[var(--text-muted)]">{description}</p>
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap gap-3">{actions}</div> : null}
    </div>
  );
}
