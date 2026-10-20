import Link from "next/link";
import { Icon } from "@/components/icons";
import { cn } from "@/lib/utils";

export function BrandMark({ className, href = "/" }: { className?: string; href?: string }) {
  return (
    <Link className={cn("inline-flex items-center gap-3", className)} href={href}>
      <span className="grid size-9 place-items-center rounded-[9px] bg-[var(--text-strong)] text-white">
        <Icon className="size-[18px]" name="package-check" />
      </span>
      <span className="font-[var(--font-display)] text-xl font-bold tracking-[-0.02em]">AppDist</span>
    </Link>
  );
}
