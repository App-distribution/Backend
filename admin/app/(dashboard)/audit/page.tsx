import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";

export default function AuditPage() {
  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Audit"
        title="Audit trail"
        description="Audit events are already written by the backend service layer, but the current HTTP API does not expose them yet. This route is kept in place so the IA stays stable when the endpoint arrives."
      />
      <EmptyState
        title="Audit API is not exposed yet"
        description="Recommended backend addition: GET /api/v1/audit-logs with filters for resource_type, action and pagination."
        action={<Button variant="secondary">Waiting for backend endpoint</Button>}
      />
    </div>
  );
}
