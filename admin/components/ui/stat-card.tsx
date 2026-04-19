import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";

export function StatCard({
  label,
  value,
  hint,
  delta,
}: {
  label: string;
  value: string;
  hint: string;
  delta?: string;
}) {
  return (
    <Card>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-[var(--text-muted)]">{label}</p>
          {delta ? <Badge tone="primary">{delta}</Badge> : null}
        </div>
        <div>
          <p className="text-3xl font-semibold tracking-tight text-[var(--text-strong)]">{value}</p>
          <p className="mt-1 text-sm text-[var(--text-muted)]">{hint}</p>
        </div>
      </CardContent>
    </Card>
  );
}
