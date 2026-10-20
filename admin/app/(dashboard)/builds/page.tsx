"use client";

import { createColumnHelper } from "@tanstack/react-table";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { BuildDetailPanel } from "@/components/build-detail-panel";
import { BuildStatusBadge, ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { Icon } from "@/components/icons";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Input, Select } from "@/components/ui/field";
import { formatBytes, formatDateTime, truncateMiddle } from "@/lib/format";
import {
  useDeleteBuildMutation,
  useMeQuery,
  useProjectBuildsQuery,
  useProjectsQuery,
  useRecentBuildsQuery,
  useUpdateBuildMutation,
} from "@/lib/queries";
import type { Build, ReleaseChannel } from "@/lib/types";

const columnHelper = createColumnHelper<Build>();

const columns = [
  columnHelper.accessor("versionName", {
    header: "Версия",
    cell: (info) => (
      <div>
        <p className="font-[var(--font-display)] text-base font-bold tracking-[-0.015em]">{info.getValue()}</p>
        <p className="mt-1 font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">код {info.row.original.versionCode}</p>
      </div>
    ),
  }),
  columnHelper.accessor("environment", {
    header: "Окружение",
    cell: (info) => <EnvironmentBadge environment={info.getValue()} />,
  }),
  columnHelper.accessor("channel", {
    header: "Канал",
    cell: (info) => <span className="font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">{info.getValue()}</span>,
  }),
  columnHelper.accessor("status", {
    header: "Статус",
    cell: (info) => <BuildStatusBadge status={info.getValue()} />,
  }),
  columnHelper.accessor("branch", {
    header: "Ветка",
    cell: (info) => (
      <span className="block max-w-48 truncate font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]" title={info.getValue() ?? undefined}>
        {info.getValue() ?? "—"}
      </span>
    ),
  }),
  columnHelper.accessor("commitHash", {
    header: "Коммит",
    cell: (info) => <span className="font-[var(--font-mono)] text-[10px]">{truncateMiddle(info.getValue(), 4)}</span>,
  }),
  columnHelper.accessor("uploadDate", {
    header: "Загружена",
    cell: (info) => <span className="whitespace-nowrap">{formatDateTime(info.getValue())}</span>,
  }),
  columnHelper.accessor("fileSize", {
    header: "Размер",
    cell: (info) => <span className="whitespace-nowrap font-[var(--font-mono)] text-[10px]">{formatBytes(info.getValue())}</span>,
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

  const currentProject = useMemo(
    () => projectsQuery.data?.find((project) => project.id === projectId),
    [projectId, projectsQuery.data],
  );

  return (
    <div className="space-y-5">
      <PageHeader
        eyebrow="Builds"
        title="Доступные сборки"
        description={`Проверяйте версии, каналы и окружения. ${rows.length} сборок в текущей выборке.`}
        actions={
          canManage ? (
            <Button className="min-w-[190px]" onClick={() => router.push(`/upload?projectId=${projectId}`)}>
              <Icon className="size-[18px]" name="cloud-upload" />
              Загрузить APK
            </Button>
          ) : null
        }
      />

      <section aria-label="Фильтры сборок" className="flex flex-col gap-3 xl:flex-row xl:items-center">
        <div className="relative min-w-0 flex-[1.5]">
          <Icon className="pointer-events-none absolute left-3.5 top-1/2 size-[17px] -translate-y-1/2 text-[var(--text-muted)]" name="search" />
          <Input
            aria-label="Поиск по версии, ветке или коммиту"
            className="bg-[var(--surface)] pl-10"
            placeholder="Версия, ветка или коммит"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <Select
          aria-label="Проект"
          className="bg-[var(--surface)] xl:max-w-[260px]"
          value={projectId}
          onChange={(event) => {
            setProjectId(event.target.value);
            setSelectedBuild(null);
          }}
        >
          {(projectsQuery.data ?? []).map((project) => (
            <option key={project.id} value={project.id}>
              {project.name}
            </option>
          ))}
        </Select>
        <Select
          aria-label="Канал"
          className="bg-[var(--surface)] xl:max-w-[180px]"
          value={channel}
          onChange={(event) => setChannel(event.target.value as ReleaseChannel | "")}
        >
          <option value="">Все каналы</option>
          <option value="NIGHTLY">NIGHTLY</option>
          <option value="ALPHA">ALPHA</option>
          <option value="BETA">BETA</option>
          <option value="RC">RC</option>
          <option value="INTERNAL">INTERNAL</option>
          <option value="CUSTOM">CUSTOM</option>
        </Select>
        <Button disabled={!projectId} variant="secondary" onClick={() => router.push(`/projects/${projectId}`)}>
          Открыть проект
          <Icon className="size-4" name="arrow-right" />
        </Button>
      </section>

      <section aria-label={currentProject ? `Сборки проекта ${currentProject.name}` : "Последние сборки"}>
        {rows.length ? (
          <DataTable
            columns={columns}
            data={rows}
            mobileVisibleColumnIds={["versionName", "environment", "status"]}
            onRowClick={setSelectedBuild}
            rowLabel={(build) => `Открыть сборку ${build.versionName}, окружение ${build.environment}, статус ${build.status}`}
          />
        ) : scopedBuildsQuery.isLoading || recentBuildsQuery.isLoading ? (
          <div className="rounded-xl border border-[var(--border-soft)] bg-[var(--surface)] px-6 py-20 text-center text-sm text-[var(--text-muted)]">
            Загружаем сборки…
          </div>
        ) : (
          <EmptyState
            title="Сборки не найдены"
            description="Измените фильтры или загрузите новый APK для выбранного проекта."
            action={canManage ? <Button onClick={() => router.push(`/upload?projectId=${projectId}`)}>Загрузить APK</Button> : undefined}
          />
        )}
      </section>

      {selectedBuild ? (
        <section className="pt-1">
          <BuildDetailPanel
            build={selectedBuild}
            canManage={Boolean(canManage)}
            onDelete={async () => {
              await deleteBuild.mutateAsync(selectedBuild.id);
              setSelectedBuild(null);
            }}
            onUpdate={async (payload) => {
              const updated = await updateBuild.mutateAsync({ buildId: selectedBuild.id, payload });
              setSelectedBuild(updated);
            }}
          />
        </section>
      ) : (
        <p className="text-center text-xs text-[var(--text-muted)]">Нажмите на строку, чтобы открыть метаданные и действия сборки.</p>
      )}
    </div>
  );
}
