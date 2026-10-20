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
import { Icon } from "@/components/icons";

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
      <Card>
        <CardContent className="flex min-h-28 items-center justify-center gap-3 py-8 text-sm text-[var(--text-muted)]">
          <Icon className="size-5" name="package" />
          Выберите строку сборки, чтобы посмотреть метаданные и доступные действия.
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
      const nextMessage = error instanceof ApiError ? error.message : "Не удалось выполнить действие";
      setMessage(nextMessage);
    } finally {
      setIsWorking(false);
    }
  };

  return (
    <Card className="overflow-hidden">
      <CardHeader
        title={`${build.versionName} (${build.versionCode})`}
        description="Метаданные сборки и управление публикацией"
      />
      <CardContent className="space-y-5">
        <div className="flex flex-wrap gap-2">
          <BuildStatusBadge status={build.status} />
          <ChannelBadge channel={build.channel} />
          <EnvironmentBadge environment={build.environment} />
          {build.isLatestInChannel ? <Badge tone="success">последняя</Badge> : null}
        </div>

        <dl className="grid gap-3 text-sm sm:grid-cols-2">
          <DetailItem label="Загружена" value={formatDateTime(build.uploadDate)} />
          <DetailItem label="Размер" value={formatBytes(build.fileSize)} />
          <DetailItem label="Тип сборки" value={build.buildType} />
          <DetailItem label="Вариант" value={build.flavor ?? "—"} />
          <DetailItem label="Ветка" value={build.branch ?? "—"} />
          <DetailItem label="Коммит" value={truncateMiddle(build.commitHash, 5)} mono />
          <DetailItem label="Контрольная сумма" value={truncateMiddle(build.checksumSha256, 8)} mono />
          <DetailItem label="Отпечаток" value={truncateMiddle(build.certFingerprint, 8)} mono />
          <DetailItem label="ABIs" value={build.abis.length ? build.abis.join(", ") : "—"} />
          <DetailItem label="SDK" value={`${build.minSdk} → ${build.targetSdk}`} />
        </dl>

        <div>
          <label className="mb-2 block text-sm font-semibold text-[var(--text)]" htmlFor="build-changelog">Что изменилось</label>
          <Textarea id="build-changelog" value={draftChangelog} onChange={(event) => setDraftChangelog(event.target.value)} />
        </div>

        <div className="flex flex-wrap gap-3">
          {canManage ? (
            <>
              <Button
                disabled={isWorking}
                onClick={() =>
                  runAction(() => onUpdate({ changelog: draftChangelog, status: build.status }), "Описание обновлено")
                }
              >
                Сохранить описание
              </Button>
              <Button
                variant="secondary"
                disabled={isWorking}
                onClick={() => runAction(() => onUpdate({ status: "DEPRECATED" }), "Сборка помечена устаревшей")}
              >
                Сделать устаревшей
              </Button>
              <Button
                variant="secondary"
                disabled={isWorking}
                onClick={() => runAction(() => onUpdate({ status: "ARCHIVED" }), "Сборка отправлена в архив")}
              >
                В архив
              </Button>
              <Button
                variant="danger"
                disabled={isWorking}
                onClick={() => {
                  if (!window.confirm("Удалить эту сборку без возможности восстановления?")) return;
                  void runAction(onDelete, "Сборка удалена");
                }}
              >
                Удалить
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
              }, "Ссылка на скачивание скопирована")
            }
          >
            Скопировать ссылку
          </Button>
        </div>

        {message ? (
          <div className="rounded-lg bg-[var(--surface-muted)] px-4 py-3 text-sm text-[var(--text)]">{message}</div>
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
    <div className="rounded-lg bg-[var(--surface-muted)] p-3">
      <dt className="font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.07em] text-[var(--text-muted)]">{label}</dt>
      <dd className={`mt-2 text-sm text-[var(--text-strong)] ${mono ? "font-[var(--font-mono)]" : ""}`}>{value}</dd>
    </div>
  );
}
