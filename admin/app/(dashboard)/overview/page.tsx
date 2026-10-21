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
    header: "Версия",
    cell: (info) => (
      <div>
        <p className="font-semibold text-[var(--text-strong)]">{info.getValue()}</p>
        <p className="font-[var(--font-mono)] text-xs text-[var(--text-muted)]">{info.row.original.versionCode}</p>
      </div>
    ),
  }),
  columnHelper.accessor("channel", {
    header: "Канал",
    cell: (info) => <ChannelBadge channel={info.getValue()} />,
  }),
  columnHelper.accessor("status", {
    header: "Статус",
    cell: (info) => <BuildStatusBadge status={info.getValue()} />,
  }),
  columnHelper.accessor("uploadDate", {
    header: "Загружена",
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
        description: build.changelog ?? "Описание изменений не добавлено",
        time: formatRelativeTime(build.uploadDate),
      })),
    [builds],
  );

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Overview"
        title="Обзор релизов"
        description="Главные показатели, последние сборки и сигналы, которые требуют внимания команды."
      />

      <section className="grid gap-4 xl:grid-cols-4">
        <StatCard label="Проекты" value={String(projects.length)} hint={`${freshProjectsCount} с недавней активностью`} />
        <StatCard label="Последние загрузки" value={String(builds.length)} hint={lastUpload ? `Последняя ${formatRelativeTime(lastUpload)}` : "Загрузок пока нет"} />
        <StatCard
          label="Участники"
          value={meQuery.data?.role === "ADMIN" ? String(users.length) : "—"}
          hint={meQuery.data?.role === "ADMIN" ? "Пользователи рабочего пространства" : "Доступно администратору"}
        />
        <StatCard label="Требуют внимания" value={String(flaggedBuilds.length)} hint="Устаревшие или обязательные сборки" delta={flaggedBuilds.length ? "проверить" : "в норме"} />
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.6fr)_minmax(320px,0.8fr)]">
        <Card>
          <CardHeader
            title="Последние сборки"
            description="Недавние публикации во всех проектах рабочего пространства."
            action={
              <Link className="text-sm font-semibold text-[var(--primary)]" href="/builds">
                Все сборки
              </Link>
            }
          />
          <CardContent>
            {builds.length ? (
              <DataTable columns={columns} data={builds.slice(0, 8)} />
            ) : (
              <EmptyState title="Сборок пока нет" description="После первой загрузки здесь появится рабочая лента публикаций." />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader
            title="Активность"
            description="Последние изменения в потоке релизов."
          />
          <CardContent className="space-y-4">
            {activityItems.length ? (
              activityItems.map((item) => (
                <div key={item.id} className="rounded-[10px] bg-[var(--surface-muted)] p-4">
                  <div className="flex items-center justify-between gap-3">
                    <h3 className="font-semibold text-[var(--text-strong)]">{item.title}</h3>
                    <Badge tone="primary">{item.time}</Badge>
                  </div>
                  <p className="mt-2 line-clamp-3 whitespace-pre-line text-sm text-[var(--text-muted)]">{item.description}</p>
                </div>
              ))
            ) : (
              <p className="text-sm text-[var(--text-muted)]">Активности пока нет.</p>
            )}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        <Card>
          <CardHeader title="Состояние каналов" description="Активные сборки и статусы, требующие внимания." />
          <CardContent>
            <ChannelHealthChart builds={builds} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="По окружениям" description="Распределение последних сборок по средам." />
          <CardContent>
            <EnvironmentDistributionChart builds={builds} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="Требуют внимания" description="Сборки, которые стоит проверить вручную." />
          <CardContent className="space-y-3">
            {flaggedBuilds.length ? (
              flaggedBuilds.slice(0, 5).map((build) => (
                <div key={build.id} className="rounded-[10px] bg-[var(--surface-muted)] p-4">
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
              <p className="text-sm text-[var(--text-muted)]">Сборок с предупреждениями нет.</p>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
