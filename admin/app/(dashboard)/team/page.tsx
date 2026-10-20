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
    header: "Имя",
    cell: (info) => (
      <div>
        <p className="font-semibold">{info.getValue()}</p>
        <p className="text-xs text-[var(--text-muted)]">{info.row.original.email}</p>
      </div>
    ),
  }),
  columnHelper.accessor("role", {
    header: "Роль",
    cell: (info) => <RoleBadge role={info.getValue()} />,
  }),
  columnHelper.accessor("createdAt", {
    header: "Добавлен",
    cell: (info) => formatDateTime(info.getValue()),
  }),
  columnHelper.display({
    id: "fcm",
    header: "FCM",
    cell: () => <Badge tone="neutral">нет данных</Badge>,
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
        title="Команда"
        description="Пользователи рабочего пространства и их роли. Сейчас список доступен только для просмотра."
      />
      {!isAdmin ? (
        <EmptyState
          title="Только для администраторов"
          description="Список команды доступен пользователям с ролью ADMIN."
        />
      ) : (
        <Card>
          <CardHeader title="Пользователи с доступом" description="Роли и дата присоединения к рабочему пространству." />
          <CardContent>
            {teamQuery.data?.length ? (
              <DataTable columns={columns} data={teamQuery.data} />
            ) : (
              <EmptyState title="Участников пока нет" description="Пользователи появятся после первого входа по одноразовому коду." />
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
