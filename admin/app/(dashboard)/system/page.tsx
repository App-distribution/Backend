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
          title="Состояние системы"
          description="Системная информация доступна только администраторам рабочего пространства."
        />
        <EmptyState title="Только для администраторов" description="Войдите с ролью ADMIN, чтобы открыть диагностику." />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="System"
        title="Состояние системы"
        description="Доступные показатели рабочего пространства и текущие ограничения API."
      />

      <section className="grid gap-4 xl:grid-cols-4">
        <StatCard label="Пространство" value={workspaceQuery.data?.slug ?? "—"} hint="Определено из текущей сессии" />
        <StatCard label="Проекты" value={String(projectsQuery.data?.length ?? 0)} hint="Приложения в рабочем пространстве" />
        <StatCard label="Участники" value={String(teamQuery.data?.length ?? 0)} hint="Пользователи рабочего пространства" />
        <StatCard label="Последние сборки" value={String(recentBuildsQuery.data?.length ?? 0)} hint="Текущая выборка публикаций" />
      </section>

      <section className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader title="Доступно сейчас" description="Данные, которые возвращает текущий API." />
          <CardContent className="space-y-3 text-sm text-[var(--text)]">
            <p>Пользователь: {meQuery.data?.email ?? "—"}</p>
            <p>Пространство создано: {workspaceQuery.data ? formatDateTime(workspaceQuery.data.createdAt) : "—"}</p>
            <p>Последняя сборка: {recentBuildsQuery.data?.[0] ? formatDateTime(recentBuildsQuery.data[0].uploadDate) : "—"}</p>
            <p>Health endpoint backend: не предоставлен</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader title="Что добавить в backend" description="Методы API для полноценной системной диагностики." />
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
