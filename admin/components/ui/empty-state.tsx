import type { ReactNode } from "react";
import { Card, CardContent } from "@/components/ui/card";

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <Card className="border-dashed">
      <CardContent className="flex min-h-56 flex-col items-center justify-center text-center">
        <div className="max-w-md">
          <h3 className="text-lg font-semibold">{title}</h3>
          <p className="mt-2 text-sm text-[var(--text-muted)]">{description}</p>
          {action ? <div className="mt-5">{action}</div> : null}
        </div>
      </CardContent>
    </Card>
  );
}
