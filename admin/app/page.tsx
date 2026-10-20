"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { BrandMark } from "@/components/brand-mark";
import { BuildStatusBadge, ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { Icon } from "@/components/icons";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
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
    <main className="min-h-screen bg-[var(--background)]">
      <header className="border-b border-[var(--border-soft)] bg-[var(--surface)]">
        <div className="mx-auto flex h-[68px] max-w-[1280px] items-center gap-3 px-4 sm:px-6">
          <BrandMark className="mr-auto" />
          <Link className="hidden text-[13px] font-medium text-[var(--text-muted)] hover:text-[var(--text-strong)] sm:inline" href="/login">
            Войти в админку
          </Link>
          <ThemeToggle compact />
          <Link
            className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-[var(--primary)] px-4 text-[13px] font-semibold text-[var(--primary-contrast)] transition-colors hover:bg-[var(--primary-hover)]"
            href="/upload"
          >
            <Icon className="size-[17px]" name="cloud-upload" />
            <span className="hidden sm:inline">Загрузить APK</span>
          </Link>
        </div>
      </header>

      <div className="app-page-enter mx-auto max-w-[1280px] px-4 py-7 sm:px-6 lg:py-10">
        <section className="app-gradient overflow-hidden rounded-[16px] border border-[#dad7e8] px-6 py-8 dark:border-[var(--border)] sm:px-9 sm:py-10">
          <div className="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h1 className="font-[var(--font-display)] text-[clamp(2.5rem,5vw,4.25rem)] font-bold leading-none tracking-[-0.035em]">
                Доступные сборки
              </h1>
              <p className="mt-4 max-w-2xl text-base leading-7 text-[var(--text-muted)]">
                Выберите нужную версию и скачайте APK. Вход и регистрация для скачивания не требуются.
              </p>
              <p className="mt-6 font-[var(--font-mono)] text-[10px] font-medium tracking-[0.04em] text-[var(--text-muted)]">
                {availableProjects.length} проекта · обновление в реальном времени
              </p>
            </div>
            <Link
              className="inline-flex min-h-12 w-fit items-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--surface)] px-5 text-[13px] font-semibold text-[var(--text-strong)] transition-colors hover:bg-[var(--surface-muted)]"
              href="/login"
            >
              Открыть рабочую область
              <Icon className="size-[17px]" name="arrow-right" />
            </Link>
          </div>
        </section>

        {error ? (
          <div className="mt-5 rounded-lg bg-[var(--badge-danger-bg)] px-4 py-3 text-sm text-[var(--badge-danger-text)]" role="alert">
            {error}
          </div>
        ) : null}

        {isLoading ? (
          <div className="mt-6 rounded-xl border border-[var(--border-soft)] bg-[var(--surface)] px-6 py-20 text-center text-sm text-[var(--text-muted)]">
            Загружаем сборки…
          </div>
        ) : availableProjects.length === 0 ? (
          <div className="mt-6 rounded-xl border border-dashed border-[var(--border)] bg-[var(--surface)] px-6 py-20 text-center">
            <p className="font-[var(--font-display)] text-xl font-bold">Доступных сборок пока нет</p>
            <p className="mt-2 text-sm text-[var(--text-muted)]">Как только появится активная сборка, она будет показана здесь.</p>
          </div>
        ) : (
          <div className="mt-10 space-y-12">
            {availableProjects.map((project) => (
              <section key={project.id}>
                <div className="mb-5 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <h2 className="font-[var(--font-display)] text-2xl font-bold tracking-[-0.02em]">{project.name}</h2>
                    <p className="mt-1 font-[var(--font-mono)] text-[11px] text-[var(--text-muted)]">{project.packageName}</p>
                  </div>
                  <p className="text-xs text-[var(--text-muted)]">{(builds[project.id] ?? []).length} сборок</p>
                </div>
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {(builds[project.id] ?? []).map((build) => (
                    <article
                      className="flex min-h-[292px] flex-col rounded-xl border border-[var(--border-soft)] bg-[var(--surface)] p-5 shadow-[var(--shadow-soft)] transition-transform duration-200 hover:-translate-y-0.5"
                      key={build.id}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="font-[var(--font-display)] text-2xl font-bold tracking-[-0.02em]">{build.versionName}</p>
                          <p className="mt-1 font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">код {build.versionCode}</p>
                        </div>
                        {build.isLatestInChannel ? (
                          <span className="rounded-md bg-[var(--primary-soft)] px-2.5 py-1.5 font-[var(--font-mono)] text-[9px] font-semibold uppercase text-[var(--badge-primary-text)]">
                            последняя
                          </span>
                        ) : null}
                      </div>

                      <div className="mt-5 flex flex-wrap gap-2">
                        <BuildStatusBadge status={build.status} />
                        <ChannelBadge channel={build.channel} />
                        <EnvironmentBadge environment={build.environment} />
                      </div>

                      <dl className="mt-5 grid grid-cols-2 gap-4 border-t border-[var(--border-soft)] pt-4 text-[13px]">
                        <div>
                          <dt className="text-xs text-[var(--text-muted)]">Загружена</dt>
                          <dd className="mt-1 font-medium">{formatDateTime(build.uploadDate)}</dd>
                        </div>
                        <div>
                          <dt className="text-xs text-[var(--text-muted)]">Размер</dt>
                          <dd className="mt-1 font-[var(--font-mono)] text-[11px] font-medium">{formatBytes(build.fileSize)}</dd>
                        </div>
                      </dl>

                      {build.changelog ? (
                        <p className="mt-4 line-clamp-2 text-[13px] leading-5 text-[var(--text-muted)]">{build.changelog}</p>
                      ) : null}

                      <Button
                        className="mt-auto w-full"
                        disabled={downloadingId === build.id}
                        onClick={() => void download(build)}
                      >
                        <Icon className="size-[17px]" name="download" />
                        {downloadingId === build.id ? "Подготавливаем…" : "Скачать APK"}
                      </Button>
                    </article>
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
