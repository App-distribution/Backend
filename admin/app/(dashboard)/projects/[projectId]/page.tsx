"use client";

import { createColumnHelper } from "@tanstack/react-table";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { BuildDetailPanel } from "@/components/build-detail-panel";
import { ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { StatCard } from "@/components/ui/stat-card";
import { formatDateTime } from "@/lib/format";
import { useDeleteBuildMutation, useDeleteProjectMutation, useMeQuery, useProjectBuildsQuery, useProjectQuery, useUpdateBuildMutation } from "@/lib/queries";
import type { Build } from "@/lib/types";

const columnHelper = createColumnHelper<Build>();

const columns = [
  columnHelper.accessor("versionName", {
    header: "Version",
    cell: (info) => (
      <div>
        <p className="font-semibold">{info.getValue()}</p>
        <p className="font-[var(--font-mono)] text-xs text-[var(--text-muted)]">{info.row.original.versionCode}</p>
      </div>
    ),
  }),
  columnHelper.accessor("channel", {
    header: "Channel",
    cell: (info) => <ChannelBadge channel={info.getValue()} />,
  }),
  columnHelper.accessor("environment", {
    header: "Environment",
    cell: (info) => <EnvironmentBadge environment={info.getValue()} />,
  }),
  columnHelper.accessor("uploadDate", {
    header: "Uploaded",
    cell: (info) => formatDateTime(info.getValue()),
  }),
];

export default function ProjectDetailPage() {
  const router = useRouter();
  const params = useParams();
  const projectId = String(params.projectId ?? "");
  const meQuery = useMeQuery();
  const projectQuery = useProjectQuery(projectId);
  const buildsQuery = useProjectBuildsQuery(projectId, { limit: 50 }, Boolean(projectId));
  const [selectedBuild, setSelectedBuild] = useState<Build | null>(null);

  const updateBuild = useUpdateBuildMutation(projectId);
  const deleteBuild = useDeleteBuildMutation(projectId);
  const deleteProject = useDeleteProjectMutation();
  const canManage = meQuery.data?.role === "ADMIN" || meQuery.data?.role === "UPLOADER";
  const canDeleteProject = meQuery.data?.role === "ADMIN";

  useEffect(() => {
    if (
      buildsQuery.data?.length &&
      (!selectedBuild || !buildsQuery.data.some((build) => build.id === selectedBuild.id))
    ) {
      setSelectedBuild(buildsQuery.data[0]);
    }
  }, [buildsQuery.data, selectedBuild]);

  const latestByChannel = useMemo(() => {
    return (buildsQuery.data ?? []).filter((build) => build.isLatestInChannel);
  }, [buildsQuery.data]);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Project detail"
        title={projectQuery.data?.name ?? "Project"}
        description={projectQuery.data?.packageName ?? "Loading package metadata"}
        actions={
          <>
            <Button onClick={() => router.push(`/upload?projectId=${projectId}`)}>Upload APK</Button>
            {canDeleteProject ? (
              <Button
                variant="danger"
                onClick={async () => {
                  if (!window.confirm("Delete this project and its metadata?")) return;
                  await deleteProject.mutateAsync(projectId);
                  router.replace("/projects");
                }}
              >
                Delete project
              </Button>
            ) : null}
          </>
        }
      />

      <section className="grid gap-4 xl:grid-cols-4">
        <StatCard
          label="Builds"
          value={String(buildsQuery.data?.length ?? 0)}
          hint="Full project delivery history currently loaded"
        />
        <StatCard
          label="Latest channels"
          value={String(latestByChannel.length)}
          hint="Channels with current latest build marker"
        />
        <StatCard
          label="Mandatory / deprecated"
          value={String((buildsQuery.data ?? []).filter((build) => build.status !== "ACTIVE").length)}
          hint="Builds with non-active status"
        />
        <StatCard
          label="Created"
          value={projectQuery.data ? formatDateTime(projectQuery.data.createdAt) : "—"}
          hint="Project creation timestamp from backend"
        />
      </section>

      <Card>
        <CardHeader title="Channel strip" description="Fast scan of the latest build represented in every release channel." />
        <CardContent className="flex flex-wrap gap-3">
          {latestByChannel.length ? (
            latestByChannel.map((build) => (
              <button
                key={build.id}
                className="rounded-3xl border border-[var(--border)] bg-[var(--surface-muted)] px-4 py-3 text-left transition hover:border-teal-500"
                onClick={() => setSelectedBuild(build)}
                type="button"
              >
                <div className="flex items-center gap-2">
                  <ChannelBadge channel={build.channel} />
                  <span className="font-[var(--font-mono)] text-sm">
                    {build.versionName} ({build.versionCode})
                  </span>
                </div>
              </button>
            ))
          ) : (
            <p className="text-sm text-[var(--text-muted)]">Latest channel markers are not available yet.</p>
          )}
        </CardContent>
      </Card>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(360px,0.9fr)]">
        <Card>
          <CardHeader title="Project builds" description="Click a row to inspect delivery metadata and actions." />
          <CardContent>
            {buildsQuery.data?.length ? (
              <DataTable columns={columns} data={buildsQuery.data} onRowClick={setSelectedBuild} />
            ) : (
              <EmptyState
                title="No builds for this project"
                description="Use Upload to publish the first APK and populate this project timeline."
                action={<Button onClick={() => router.push(`/upload?projectId=${projectId}`)}>Upload APK</Button>}
              />
            )}
          </CardContent>
        </Card>

        <BuildDetailPanel
          build={selectedBuild}
          canManage={Boolean(canManage)}
          onDelete={async () => {
            if (!selectedBuild) return;
            await deleteBuild.mutateAsync(selectedBuild.id);
            setSelectedBuild(null);
          }}
          onUpdate={async (payload) => {
            if (!selectedBuild) return;
            const updated = await updateBuild.mutateAsync({ buildId: selectedBuild.id, payload });
            setSelectedBuild(updated);
          }}
        />
      </section>
    </div>
  );
}
