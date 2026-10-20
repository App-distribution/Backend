import Link from "next/link";
import { ChannelBadge } from "@/components/domain-badges";
import { Card, CardContent } from "@/components/ui/card";
import { formatDateTime, formatRelativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { Build, Project } from "@/lib/types";

export function ProjectCard({
  project,
  latestBuilds,
}: {
  project: Project;
  latestBuilds: Build[];
}) {
  const buildCount = latestBuilds.length;
  const lastBuild = latestBuilds[0];

  return (
    <Card className="overflow-hidden">
      <CardContent className="space-y-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold text-[var(--text-strong)]">{project.name}</h3>
            <p className="mt-1 font-[var(--font-mono)] text-sm text-[var(--text-muted)]">{project.packageName}</p>
          </div>
          <div className="rounded-lg bg-[var(--surface-muted)] px-3 py-2 text-right">
            <p className="font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.07em] text-[var(--text-muted)]">Сборки</p>
            <p className="mt-1 text-lg font-semibold">{buildCount}</p>
          </div>
        </div>

        <div className="space-y-2 rounded-[10px] bg-[var(--surface-muted)] p-4">
          <p className="font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.07em] text-[var(--text-muted)]">Последняя публикация</p>
          {lastBuild ? (
            <>
              <div className="flex flex-wrap items-center gap-2">
                <ChannelBadge channel={lastBuild.channel} />
                <span className="font-[var(--font-mono)] text-sm text-[var(--text)]">
                  {lastBuild.versionName} ({lastBuild.versionCode})
                </span>
              </div>
              <p className="text-sm text-[var(--text-muted)]">
                Загружена {formatRelativeTime(lastBuild.uploadDate)} · {formatDateTime(lastBuild.uploadDate)}
              </p>
            </>
          ) : (
            <p className="text-sm text-[var(--text-muted)]">В этом проекте ещё нет сборок.</p>
          )}
        </div>

        <div className="flex flex-wrap gap-3">
          <Link
            className={cn(
              "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition",
              "bg-[var(--primary)] text-[var(--primary-contrast)] hover:bg-[var(--primary-hover)]",
            )}
            href={`/projects/${project.id}`}
          >
            Открыть проект
          </Link>
          <Link
            className={cn(
              "inline-flex items-center justify-center gap-2 rounded-lg border border-[var(--border)] bg-[var(--surface)] px-4 py-2.5 text-sm font-semibold transition",
              "text-[var(--text-strong)] hover:bg-[var(--surface-muted)]",
            )}
            href={`/upload?projectId=${project.id}`}
          >
            Загрузить APK
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
