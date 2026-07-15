"use client";

import { useEffect, useMemo, useState } from "react";
import { BuildStatusBadge, ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api, ApiError } from "@/lib/api";
import { formatBytes, formatDateTime } from "@/lib/format";
import type { Build, Project } from "@/lib/types";

export default function PublicBuildsPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [builds, setBuilds] = useState<Record<string, Build[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        const nextProjects = await api.public.listProjects();
        const nextBuilds = await Promise.all(
          nextProjects.map(async (project) => [project.id, await api.public.listBuilds(project.id)] as const),
        );
        if (!cancelled) {
          setProjects(nextProjects);
          setBuilds(Object.fromEntries(nextBuilds));
        }
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof ApiError ? loadError.message : "Не удалось загрузить список сборок.");
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  const availableProjects = useMemo(
    () => projects.filter((project) => (builds[project.id] ?? []).length > 0),
    [builds, projects],
  );

  const download = async (build: Build) => {
    setDownloadingId(build.id);
    setError(null);
    try {
      const response = await api.public.getDownloadUrl(build.id);
      window.location.assign(response.url);
    } catch (downloadError) {
      setError(downloadError instanceof ApiError ? downloadError.message : "Не удалось начать скачивание.");
      setDownloadingId(null);
    }
  };

  return (
    <main className="mx-auto min-h-screen max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-8 flex items-start justify-between gap-4">
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--primary)]">App Distribution</p>
          <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Доступные сборки</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-[var(--text-muted)] sm:text-base">
            Выберите нужную версию и скачайте APK. Вход и регистрация не требуются.
          </p>
        </div>
        <ThemeToggle />
      </header>

      {error ? (
        <div className="mb-5 rounded-2xl border border-[var(--danger)] bg-[var(--surface)] px-4 py-3 text-sm text-[var(--danger)]">
          {error}
        </div>
      ) : null}

      {isLoading ? (
        <Card>
          <CardContent className="py-16 text-center text-sm text-[var(--text-muted)]">Загружаем сборки…</CardContent>
        </Card>
      ) : availableProjects.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center">
            <p className="font-semibold">Доступных сборок пока нет</p>
            <p className="mt-2 text-sm text-[var(--text-muted)]">Как только появится активная сборка, она будет показана здесь.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-8">
          {availableProjects.map((project) => (
            <section key={project.id}>
              <div className="mb-3">
                <h2 className="text-xl font-bold">{project.name}</h2>
                <p className="font-[var(--font-mono)] text-xs text-[var(--text-muted)]">{project.packageName}</p>
              </div>
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {(builds[project.id] ?? []).map((build) => (
                  <Card key={build.id} className="overflow-hidden">
                    <CardContent className="flex h-full flex-col gap-5 p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-2xl font-bold">{build.versionName}</p>
                          <p className="text-xs text-[var(--text-muted)]">Код версии {build.versionCode}</p>
                        </div>
                        {build.isLatestInChannel ? (
                          <span className="rounded-full bg-[var(--primary-soft)] px-3 py-1 text-xs font-semibold text-[var(--primary)]">
                            latest
                          </span>
                        ) : null}
                      </div>

                      <div className="flex flex-wrap gap-2">
                        <BuildStatusBadge status={build.status} />
                        <ChannelBadge channel={build.channel} />
                        <EnvironmentBadge environment={build.environment} />
                      </div>

                      <dl className="grid grid-cols-2 gap-3 text-sm">
                        <div>
                          <dt className="text-xs text-[var(--text-muted)]">Загружена</dt>
                          <dd className="mt-1 font-medium">{formatDateTime(build.uploadDate)}</dd>
                        </div>
                        <div>
                          <dt className="text-xs text-[var(--text-muted)]">Размер</dt>
                          <dd className="mt-1 font-medium">{formatBytes(build.fileSize)}</dd>
                        </div>
                      </dl>

                      {build.changelog ? (
                        <p className="line-clamp-3 text-sm leading-6 text-[var(--text-muted)]">{build.changelog}</p>
                      ) : null}

                      <Button
                        className="mt-auto w-full"
                        disabled={downloadingId === build.id}
                        onClick={() => void download(build)}
                      >
                        {downloadingId === build.id ? "Подготавливаем…" : "Скачать APK"}
                      </Button>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </main>
  );
}
