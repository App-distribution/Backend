"use client";

import { createColumnHelper } from "@tanstack/react-table";
import Link from "next/link";
import { useMemo } from "react";
import { ChannelHealthChart } from "@/components/charts/channel-health-chart";
import { EnvironmentDistributionChart } from "@/components/charts/environment-distribution-chart";
import { BuildStatusBadge, ChannelBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { formatDateTime, formatRelativeTime } from "@/lib/format";
import { useMeQuery, useProjectsQuery, useRecentBuildsQuery, useTeamQuery, useWorkspaceQuery } from "@/lib/queries";
import type { Build } from "@/lib/types";

const columnHelper = createColumnHelper<Build>();

const columns = [
  columnHelper.accessor("versionName", {
    header: "Version",
    cell: (info) => (
      <div>
        <p className="font-semibold text-[var(--text-strong)]">{info.getValue()}</p>
        <p className="font-[var(--font-mono)] text-xs text-[var(--text-muted)]">{info.row.original.versionCode}</p>
      </div>
    ),
  }),
  columnHelper.accessor("channel", {
    header: "Channel",
    cell: (info) => <ChannelBadge channel={info.getValue()} />,
  }),
  columnHelper.accessor("status", {
    header: "Status",
    cell: (info) => <BuildStatusBadge status={info.getValue()} />,
  }),
  columnHelper.accessor("uploadDate", {
    header: "Uploaded",
    cell: (info) => (
      <div>
        <p>{formatRelativeTime(info.getValue())}</p>
        <p className="text-xs text-[var(--text-muted)]">{formatDateTime(info.getValue())}</p>
      </div>
    ),
  }),
];

export default function OverviewPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const projectsQuery = useProjectsQuery();
  const recentBuildsQuery = useRecentBuildsQuery(30);
  const teamQuery = useTeamQuery(workspaceQuery.data?.id, meQuery.data?.role === "ADMIN");

  const builds = recentBuildsQuery.data ?? [];
  const projects = projectsQuery.data ?? [];
  const users = teamQuery.data ?? [];

  const flaggedBuilds = builds.filter((build) => build.status === "DEPRECATED" || build.status === "MANDATORY");
  const freshProjectsCount = new Set(builds.map((build) => build.projectId)).size;
  const lastUpload = builds[0]?.uploadDate;

  const activityItems = useMemo(
    () =>
      builds.slice(0, 6).map((build) => ({
        id: build.id,
        title: `${build.versionName} · ${build.channel.toLowerCase()}`,
        description: build.changelog ?? "No release notes provided",
        time: formatRelativeTime(build.uploadDate),
      })),
    [builds],
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Workspace overview"
        title="Release activity at a glance"
        description="This screen is optimized for signal, not BI. It answers what changed, where delivery attention is needed and how healthy the current release flow looks."
      />

      <section className="grid gap-4 xl:grid-cols-4">
        <StatCard label="Projects" value={String(projects.length)} hint={`${freshProjectsCount} had recent delivery activity`} />
        <StatCard label="Uploads / 30 recent" value={String(builds.length)} hint={lastUpload ? `Latest ${formatRelativeTime(lastUpload)}` : "No recent uploads"} />
        <StatCard
          label="Team members"
          value={meQuery.data?.role === "ADMIN" ? String(users.length) : "—"}
          hint={meQuery.data?.role === "ADMIN" ? "Workspace users connected to this backend" : "Visible to admin role"}
        />
        <StatCard label="Attention required" value={String(flaggedBuilds.length)} hint="Deprecated or mandatory builds in recent feed" delta={flaggedBuilds.length ? "review" : "healthy"} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.6fr)_minmax(320px,0.8fr)]">
        <Card>
          <CardHeader
            title="Recent builds"
            description="Latest workspace deliveries across all projects. Use Builds page for project-scoped workflows."
            action={
              <Link className="text-sm font-semibold text-[var(--primary)]" href="/builds">
                Open builds
              </Link>
            }
          />
          <CardContent>
            {builds.length ? (
              <DataTable columns={columns} data={builds.slice(0, 8)} />
            ) : (
              <EmptyState title="No recent builds" description="Once upload flow is used, this table becomes the main operational feed." />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader
            title="Workspace activity"
            description="Backend exposes build feed today. Audit API can later replace this stopgap panel."
          />
          <CardContent className="space-y-4">
            {activityItems.length ? (
              activityItems.map((item) => (
                <div key={item.id} className="rounded-3xl bg-[var(--surface-muted)] p-4">
                  <div className="flex items-center justify-between gap-3">
                    <h3 className="font-semibold text-[var(--text-strong)]">{item.title}</h3>
                    <Badge tone="primary">{item.time}</Badge>
                  </div>
                  <p className="mt-2 text-sm text-[var(--text-muted)]">{item.description}</p>
                </div>
              ))
            ) : (
              <p className="text-sm text-[var(--text-muted)]">No activity yet.</p>
            )}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        <Card>
          <CardHeader title="Channel health" description="Active vs flagged statuses inside the current recent feed." />
          <CardContent>
            <ChannelHealthChart builds={builds} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="Environment distribution" description="Where recent build volume is concentrated." />
          <CardContent>
            <EnvironmentDistributionChart builds={builds} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="Attention required" description="Work that deserves manual review right now." />
          <CardContent className="space-y-3">
            {flaggedBuilds.length ? (
              flaggedBuilds.slice(0, 5).map((build) => (
                <div key={build.id} className="rounded-3xl bg-[var(--surface-muted)] p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-semibold">{build.versionName}</p>
                    <BuildStatusBadge status={build.status} />
                  </div>
                  <p className="mt-1 text-sm text-[var(--text-muted)]">
                    {build.channel.toLowerCase()} · {formatDateTime(build.uploadDate)}
                  </p>
                </div>
              ))
            ) : (
              <p className="text-sm text-[var(--text-muted)]">No flagged builds in the current recent feed.</p>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
