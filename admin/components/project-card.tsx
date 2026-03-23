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
          <div className="rounded-2xl bg-[var(--surface-muted)] px-3 py-2 text-right">
            <p className="text-xs uppercase tracking-[0.18em] text-[var(--text-muted)]">Builds</p>
            <p className="mt-1 text-lg font-semibold">{buildCount}</p>
          </div>
        </div>

        <div className="space-y-2 rounded-3xl bg-[var(--surface-muted)] p-4">
          <p className="text-xs uppercase tracking-[0.18em] text-[var(--text-muted)]">Latest delivery</p>
          {lastBuild ? (
            <>
              <div className="flex flex-wrap items-center gap-2">
                <ChannelBadge channel={lastBuild.channel} />
                <span className="font-[var(--font-mono)] text-sm text-[var(--text)]">
                  {lastBuild.versionName} ({lastBuild.versionCode})
                </span>
              </div>
              <p className="text-sm text-[var(--text-muted)]">
                Uploaded {formatRelativeTime(lastBuild.uploadDate)} · {formatDateTime(lastBuild.uploadDate)}
              </p>
            </>
          ) : (
            <p className="text-sm text-[var(--text-muted)]">No builds uploaded for this project yet.</p>
          )}
        </div>

        <div className="flex flex-wrap gap-3">
          <Link
            className={cn(
              "inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-semibold transition",
              "bg-[var(--primary)] text-white shadow-[var(--shadow-soft)] hover:bg-teal-700",
            )}
            href={`/projects/${project.id}`}
          >
            Open project
          </Link>
          <Link
            className={cn(
              "inline-flex items-center justify-center gap-2 rounded-2xl border border-[var(--border)] bg-white px-4 py-2.5 text-sm font-semibold transition",
              "text-[var(--text-strong)] hover:bg-[var(--surface-muted)]",
            )}
            href={`/upload?projectId=${project.id}`}
          >
            Upload APK
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
