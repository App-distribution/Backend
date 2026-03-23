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
      setMessage("Project created");
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Failed to create project");
    }
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Projects"
        title="Project catalog"
        description="Projects are the operational boundary for uploads, channel checks and release history. Cards emphasize quick scanning over raw CRUD."
        actions={
          canCreate ? (
            <Button variant={showCreate ? "secondary" : "primary"} onClick={() => setShowCreate((current) => !current)}>
              {showCreate ? "Close" : "Create project"}
            </Button>
          ) : null
        }
      />

      {showCreate ? (
        <Card>
          <CardContent className="grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
            <div>
              <Label>Project name</Label>
              <Input placeholder="Android QA App" value={name} onChange={(event) => setName(event.target.value)} />
            </div>
            <div>
              <Label>Package name</Label>
              <Input
                placeholder="com.company.app.qa"
                value={packageName}
                onChange={(event) => setPackageName(event.target.value)}
              />
            </div>
            <Button disabled={!name || !packageName || createProject.isPending} onClick={() => void submit()}>
              Save project
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
          title="No projects yet"
          description="Create the first project to unlock APK uploads and channel-based build history."
          action={canCreate ? <Button onClick={() => setShowCreate(true)}>Create first project</Button> : undefined}
        />
      )}
    </div>
  );
}
