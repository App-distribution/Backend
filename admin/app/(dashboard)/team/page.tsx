"use client";

import { useState } from "react";
import { createColumnHelper } from "@tanstack/react-table";
import { RoleBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { CreateUserDialog } from "@/components/create-user-dialog";
import { PasswordResultDialog } from "@/components/password-result-dialog";
import { formatDateTime } from "@/lib/format";
import { useMeQuery, useTeamQuery, useWorkspaceQuery } from "@/lib/queries";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";

export default function TeamPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const isAdmin = meQuery.data?.role === "ADMIN";
  const teamQuery = useTeamQuery(workspaceQuery.data?.id, isAdmin);

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);

  const workspaceId = workspaceQuery.data?.id;

  const handleCreated = (password: string) => {
    setShowCreateDialog(false);
    setGeneratedPassword(password);
    teamQuery.refetch();
  };

  const handleResetPassword = async (userId: string) => {
    if (!workspaceId) return;
    try {
      const result = await api.workspace.resetPassword(workspaceId, userId);
      setGeneratedPassword(result.generatedPassword);
    } catch {
      // error handled by api layer toast
    }
  };

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
    ...(isAdmin
      ? [
          columnHelper.display({
            id: "actions",
            header: "",
            cell: (info) => (
              <Button
                variant="secondary"
                className="px-2 py-1 text-xs"
                onClick={() => handleResetPassword(info.row.original.id)}
                title="Reset password"
              >
                Reset pwd
              </Button>
            ),
          }),
        ]
      : []),
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Team"
        title="Workspace members"
        description="Admins can add members and reset their passwords."
        actions={
          isAdmin ? (
            <Button onClick={() => setShowCreateDialog(true)}>
              Add member
            </Button>
          ) : undefined
        }
      />
      {!isAdmin ? (
        <EmptyState
          title="Admins only"
          description="The workspace user list is visible to admins only."
        />
      ) : (
        <Card>
          <CardHeader title="People with access" />
          <CardContent>
            {teamQuery.data?.length ? (
              <DataTable columns={columns} data={teamQuery.data} />
            ) : (
              <EmptyState title="No members" description="Add members using the button above." />
            )}
          </CardContent>
        </Card>
      )}

      {showCreateDialog && workspaceId && (
        <CreateUserDialog
          workspaceId={workspaceId}
          onCreated={handleCreated}
          onClose={() => setShowCreateDialog(false)}
        />
      )}

      {generatedPassword && (
        <PasswordResultDialog
          password={generatedPassword}
          onClose={() => setGeneratedPassword(null)}
        />
      )}
    </div>
  );
}
