"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { formatBytes, formatDateTime, truncateMiddle } from "@/lib/format";
import type { Build } from "@/lib/types";
import { BuildStatusBadge, ChannelBadge, EnvironmentBadge } from "@/components/domain-badges";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Textarea } from "@/components/ui/field";

export function BuildDetailPanel({
  build,
  canManage,
  onUpdate,
  onDelete,
}: {
  build: Build | null;
  canManage: boolean;
  onUpdate: (payload: { changelog?: string | null; status?: Build["status"] | null }) => Promise<void>;
  onDelete: () => Promise<void>;
}) {
  const [isWorking, setIsWorking] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [draftChangelog, setDraftChangelog] = useState(build?.changelog ?? "");

  useEffect(() => {
    setDraftChangelog(build?.changelog ?? "");
    setMessage(null);
  }, [build]);

  if (!build) {
    return (
      <Card className="sticky top-4">
        <CardContent className="py-14 text-center text-sm text-[var(--text-muted)]">
          Select a build row to inspect metadata, signed download links and status actions.
        </CardContent>
      </Card>
    );
  }

  const runAction = async (fn: () => Promise<void>, successMessage: string) => {
    setIsWorking(true);
    setMessage(null);
    try {
      await fn();
      setMessage(successMessage);
    } catch (error) {
      const nextMessage = error instanceof ApiError ? error.message : "Action failed";
      setMessage(nextMessage);
    } finally {
      setIsWorking(false);
    }
  };

  return (
    <Card className="sticky top-4 overflow-hidden">
      <CardHeader
        title={`${build.versionName} (${build.versionCode})`}
        description="Build metadata and release operations"
      />
      <CardContent className="space-y-5">
        <div className="flex flex-wrap gap-2">
          <BuildStatusBadge status={build.status} />
          <ChannelBadge channel={build.channel} />
          <EnvironmentBadge environment={build.environment} />
          {build.isLatestInChannel ? <Badge tone="success">latest</Badge> : null}
        </div>

        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          <DetailItem label="Uploaded at" value={formatDateTime(build.uploadDate)} />
          <DetailItem label="File size" value={formatBytes(build.fileSize)} />
          <DetailItem label="Build type" value={build.buildType} />
          <DetailItem label="Flavor" value={build.flavor ?? "—"} />
          <DetailItem label="Branch" value={build.branch ?? "—"} />
          <DetailItem label="Commit" value={truncateMiddle(build.commitHash, 5)} mono />
          <DetailItem label="Checksum" value={truncateMiddle(build.checksumSha256, 8)} mono />
          <DetailItem label="Fingerprint" value={truncateMiddle(build.certFingerprint, 8)} mono />
          <DetailItem label="ABIs" value={build.abis.length ? build.abis.join(", ") : "—"} />
          <DetailItem label="SDK" value={`${build.minSdk} → ${build.targetSdk}`} />
        </dl>

        <div>
          <p className="mb-2 text-sm font-semibold text-[var(--text)]">Release notes</p>
          <Textarea value={draftChangelog} onChange={(event) => setDraftChangelog(event.target.value)} />
        </div>

        <div className="flex flex-wrap gap-3">
          {canManage ? (
            <>
              <Button
                disabled={isWorking}
                onClick={() =>
                  runAction(() => onUpdate({ changelog: draftChangelog, status: build.status }), "Build updated")
                }
              >
                Save changelog
              </Button>
              <Button
                variant="secondary"
                disabled={isWorking}
                onClick={() => runAction(() => onUpdate({ status: "DEPRECATED" }), "Build marked deprecated")}
              >
                Mark deprecated
              </Button>
              <Button
                variant="secondary"
                disabled={isWorking}
                onClick={() => runAction(() => onUpdate({ status: "ARCHIVED" }), "Build archived")}
              >
                Archive
              </Button>
              <Button
                variant="danger"
                disabled={isWorking}
                onClick={() => {
                  if (!window.confirm("Delete this build permanently?")) return;
                  void runAction(onDelete, "Build deleted");
                }}
              >
                Delete
              </Button>
            </>
          ) : null}
          <Button
            variant="ghost"
            disabled={isWorking}
            onClick={() =>
              runAction(async () => {
                const response = await api.builds.getDownloadUrl(build.id);
                await navigator.clipboard.writeText(response.url);
              }, "Signed download URL copied")
            }
          >
            Copy download URL
          </Button>
        </div>

        {message ? (
          <div className="rounded-2xl bg-[var(--surface-muted)] px-4 py-3 text-sm text-[var(--text)]">{message}</div>
        ) : null}
      </CardContent>
    </Card>
  );
}

function DetailItem({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="rounded-2xl bg-[var(--surface-muted)] p-3">
      <dt className="text-xs uppercase tracking-[0.16em] text-[var(--text-muted)]">{label}</dt>
      <dd className={`mt-2 text-sm text-[var(--text-strong)] ${mono ? "font-[var(--font-mono)]" : ""}`}>{value}</dd>
    </div>
  );
}
