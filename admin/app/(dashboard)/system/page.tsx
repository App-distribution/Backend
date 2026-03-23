"use client";

import { PageHeader } from "@/components/page-header";
import { EmptyState } from "@/components/ui/empty-state";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { StatCard } from "@/components/ui/stat-card";
import { formatDateTime } from "@/lib/format";
import { useMeQuery, useProjectsQuery, useRecentBuildsQuery, useTeamQuery, useWorkspaceQuery } from "@/lib/queries";

export default function SystemPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const projectsQuery = useProjectsQuery();
  const recentBuildsQuery = useRecentBuildsQuery(20);
  const isAdmin = meQuery.data?.role === "ADMIN";
  const teamQuery = useTeamQuery(workspaceQuery.data?.id, isAdmin);

  if (!isAdmin) {
    return (
      <div className="space-y-4">
        <PageHeader
          eyebrow="System"
          title="Operational diagnostics"
          description="This route is reserved for workspace admins because it concentrates system-oriented context."
        />
        <EmptyState title="Admins only" description="Use an admin account to inspect system-level diagnostics." />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="System"
        title="Operational diagnostics"
        description="This is a product health screen, not a DevOps dashboard. It shows what can be inferred from the current API and lists the next backend additions needed for true system observability."
      />

      <section className="grid gap-4 xl:grid-cols-4">
        <StatCard label="Workspace" value={workspaceQuery.data?.slug ?? "—"} hint="Resolved from authenticated context" />
        <StatCard label="Projects" value={String(projectsQuery.data?.length ?? 0)} hint="Configured applications in this workspace" />
        <StatCard label="Members" value={String(teamQuery.data?.length ?? 0)} hint="Users tied to the workspace" />
        <StatCard label="Recent builds" value={String(recentBuildsQuery.data?.length ?? 0)} hint="Current recent feed sample size" />
      </section>

      <section className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader title="Available now" description="What the current API lets the admin console infer safely." />
          <CardContent className="space-y-3 text-sm text-[var(--text)]">
            <p>Authenticated user: {meQuery.data?.email ?? "—"}</p>
            <p>Workspace created: {workspaceQuery.data ? formatDateTime(workspaceQuery.data.createdAt) : "—"}</p>
            <p>Latest build seen: {recentBuildsQuery.data?.[0] ? formatDateTime(recentBuildsQuery.data[0].uploadDate) : "—"}</p>
            <p>Backend health endpoint: not exposed</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="Recommended backend additions" description="High-leverage endpoints for making System page truly useful." />
          <CardContent className="space-y-3 text-sm text-[var(--text)]">
            <p>`GET /api/v1/system/health` for DB, MinIO and Firebase status.</p>
            <p>`GET /api/v1/workspaces/me/summary` for aggregate cards and counters.</p>
            <p>`GET /api/v1/audit-logs` so this page can surface recent failures and operator changes.</p>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
