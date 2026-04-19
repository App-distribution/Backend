"use client";

import { createColumnHelper } from "@tanstack/react-table";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { BuildDetailPanel } from "@/components/build-detail-panel";
import { BuildStatusBadge, ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input, Label, Select } from "@/components/ui/field";
import { formatDateTime, truncateMiddle } from "@/lib/format";
import { useDeleteBuildMutation, useMeQuery, useProjectBuildsQuery, useProjectsQuery, useRecentBuildsQuery, useUpdateBuildMutation } from "@/lib/queries";
import type { Build, ReleaseChannel } from "@/lib/types";

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
  columnHelper.accessor("branch", {
    header: "Branch",
    cell: (info) => info.getValue() ?? "—",
  }),
  columnHelper.accessor("commitHash", {
    header: "Commit",
    cell: (info) => <span className="font-[var(--font-mono)]">{truncateMiddle(info.getValue(), 4)}</span>,
  }),
  columnHelper.accessor("status", {
    header: "Status",
    cell: (info) => <BuildStatusBadge status={info.getValue()} />,
  }),
  columnHelper.accessor("uploadDate", {
    header: "Uploaded",
    cell: (info) => formatDateTime(info.getValue()),
  }),
];

export default function BuildsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialProjectId = searchParams.get("projectId") ?? "";
  const [projectId, setProjectId] = useState(initialProjectId);
  const [search, setSearch] = useState("");
  const [channel, setChannel] = useState<ReleaseChannel | "">("");
  const [selectedBuild, setSelectedBuild] = useState<Build | null>(null);

  const meQuery = useMeQuery();
  const projectsQuery = useProjectsQuery();
  const recentBuildsQuery = useRecentBuildsQuery(40);
  const scopedBuildsQuery = useProjectBuildsQuery(projectId, { search, channel, limit: 50 }, Boolean(projectId));

  const updateBuild = useUpdateBuildMutation(projectId);
  const deleteBuild = useDeleteBuildMutation(projectId);
  const canManage = meQuery.data?.role === "ADMIN" || meQuery.data?.role === "UPLOADER";

  const rows = projectId ? scopedBuildsQuery.data ?? [] : recentBuildsQuery.data ?? [];

  useEffect(() => {
    if (projectsQuery.data?.length && !projectId) {
      setProjectId(projectsQuery.data[0].id);
    }
  }, [projectId, projectsQuery.data]);

  useEffect(() => {
    if (rows.length && (!selectedBuild || !rows.some((build) => build.id === selectedBuild.id))) {
      setSelectedBuild(rows[0]);
    }
  }, [rows, selectedBuild]);

  const currentProject = useMemo(
    () => projectsQuery.data?.find((project) => project.id === projectId),
    [projectId, projectsQuery.data],
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Builds"
        title="Delivery timeline"
        description="This page is optimized for dense technical scanning. Filters are server-driven per project because the current backend exposes project-scoped build listings."
        actions={<Button onClick={() => router.push("/upload")}>Upload build</Button>}
      />

      <Card>
        <CardHeader title="Build filters" description="Select a project to unlock server-side search and channel filtering." />
        <CardContent className="grid gap-4 lg:grid-cols-[1.2fr_1fr_1fr_auto] lg:items-end">
          <div>
            <Label>Project scope</Label>
            <Select
              value={projectId}
              onChange={(event) => {
                const nextProjectId = event.target.value;
                setProjectId(nextProjectId);
                setSelectedBuild(null);
              }}
            >
              {(projectsQuery.data ?? []).map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Search</Label>
            <Input placeholder="version, branch, commit" value={search} onChange={(event) => setSearch(event.target.value)} />
          </div>
          <div>
            <Label>Channel</Label>
            <Select value={channel} onChange={(event) => setChannel(event.target.value as ReleaseChannel | "")}>
              <option value="">All channels</option>
              <option value="NIGHTLY">Nightly</option>
              <option value="ALPHA">Alpha</option>
              <option value="BETA">Beta</option>
              <option value="RC">RC</option>
              <option value="INTERNAL">Internal</option>
              <option value="CUSTOM">Custom</option>
            </Select>
          </div>
          <Button disabled={!projectId} variant="secondary" onClick={() => router.push(`/projects/${projectId}`)}>
            Open project
          </Button>
        </CardContent>
      </Card>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.5fr)_minmax(360px,0.9fr)]">
        <Card>
          <CardHeader
            title={currentProject ? `${currentProject.name} builds` : "Workspace recent feed"}
            description={
              currentProject
                ? "Project-scoped list from current backend API."
                : "Fallback feed until a workspace-wide builds endpoint is added."
            }
          />
          <CardContent>
            <DataTable columns={columns} data={rows} onRowClick={setSelectedBuild} />
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
