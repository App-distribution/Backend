"use client";

import { useMemo, useState } from "react";
import { ApiError } from "@/lib/api";
import { useMeQuery, useProjectsQuery, useRecentBuildsQuery, useWorkspaceQuery, useCreateProjectMutation } from "@/lib/queries";
import { PageHeader } from "@/components/page-header";
import { ProjectCard } from "@/components/project-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Input, Label } from "@/components/ui/field";
import type { Build } from "@/lib/types";

export default function ProjectsPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const projectsQuery = useProjectsQuery();
  const recentBuildsQuery = useRecentBuildsQuery(40);
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState("");
  const [packageName, setPackageName] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const canCreate = meQuery.data?.role === "ADMIN" && Boolean(workspaceQuery.data?.id);
  const createProject = useCreateProjectMutation(workspaceQuery.data?.id ?? "");

  const buildMap = useMemo(() => {
    return (recentBuildsQuery.data ?? []).reduce<Record<string, Build[]>>((acc, build) => {
      acc[build.projectId] ??= [];
      acc[build.projectId].push(build);
      return acc;
    }, {});
  }, [recentBuildsQuery.data]);

  const submit = async () => {
    if (!workspaceQuery.data?.id) return;
    setMessage(null);
    try {
      await createProject.mutateAsync({ name, packageName });
      setName("");
      setPackageName("");
      setShowCreate(false);
      setMessage("Проект создан");
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Не удалось создать проект");
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Projects"
        title="Проекты"
        description="Приложения, их пакеты, последние сборки и быстрый переход к публикации APK."
        actions={
          canCreate ? (
            <Button variant={showCreate ? "secondary" : "primary"} onClick={() => setShowCreate((current) => !current)}>
              {showCreate ? "Закрыть" : "Создать проект"}
            </Button>
          ) : null
        }
      />

      {showCreate ? (
        <Card>
          <CardContent className="grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
            <div>
              <Label htmlFor="project-name">Название проекта</Label>
              <Input id="project-name" placeholder="Android QA App" value={name} onChange={(event) => setName(event.target.value)} />
            </div>
            <div>
              <Label htmlFor="project-package">Имя пакета</Label>
              <Input
                id="project-package"
                placeholder="com.company.app.qa"
                value={packageName}
                onChange={(event) => setPackageName(event.target.value)}
              />
            </div>
            <Button disabled={!name || !packageName || createProject.isPending} onClick={() => void submit()}>
              Сохранить проект
            </Button>
            {message ? <p className="text-sm text-[var(--text-muted)] lg:col-span-3">{message}</p> : null}
          </CardContent>
        </Card>
      ) : null}

      {projectsQuery.data?.length ? (
        <section className="grid gap-4 xl:grid-cols-2">
          {projectsQuery.data.map((project) => (
            <ProjectCard key={project.id} latestBuilds={buildMap[project.id] ?? []} project={project} />
          ))}
        </section>
      ) : (
        <EmptyState
          title="Проектов пока нет"
          description="Создайте первый проект, чтобы загружать APK и вести историю сборок по каналам."
          action={canCreate ? <Button onClick={() => setShowCreate(true)}>Создать первый проект</Button> : undefined}
        />
      )}
    </div>
  );
}
