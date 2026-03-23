"use client";

import { createColumnHelper } from "@tanstack/react-table";
import { RoleBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { formatDateTime } from "@/lib/format";
import { useMeQuery, useTeamQuery, useWorkspaceQuery } from "@/lib/queries";
import type { User } from "@/lib/types";

const columnHelper = createColumnHelper<User>();

const columns = [
  columnHelper.accessor("name", {
    header: "Name",
    cell: (info) => (
      <div>
        <p className="font-semibold">{info.getValue()}</p>
        <p className="text-xs text-[var(--text-muted)]">{info.row.original.email}</p>
      </div>
    ),
  }),
  columnHelper.accessor("role", {
    header: "Role",
    cell: (info) => <RoleBadge role={info.getValue()} />,
  }),
  columnHelper.accessor("createdAt", {
    header: "Joined",
    cell: (info) => formatDateTime(info.getValue()),
  }),
  columnHelper.display({
    id: "fcm",
    header: "FCM",
    cell: () => <Badge tone="neutral">not exposed</Badge>,
  }),
];

export default function TeamPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const isAdmin = meQuery.data?.role === "ADMIN";
  const teamQuery = useTeamQuery(workspaceQuery.data?.id, isAdmin);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Team"
        title="Workspace members"
        description="The backend currently exposes a read-only team list for admins. Role edits and invite flows can be layered in when member-management endpoints are added."
      />
      {!isAdmin ? (
        <EmptyState
          title="Admins only"
          description="The current backend protects workspace user listing behind ADMIN role."
        />
      ) : (
        <Card>
          <CardHeader title="People with access" description="Current DTO does not expose raw FCM state, so the column is kept intentionally neutral." />
          <CardContent>
            {teamQuery.data?.length ? (
              <DataTable columns={columns} data={teamQuery.data} />
            ) : (
              <EmptyState title="No members" description="Workspace users will appear here after OTP sign in creates or joins the workspace." />
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
