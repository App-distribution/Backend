"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Icon } from "@/components/icons";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { FieldHint, Input, Label, Select, Textarea } from "@/components/ui/field";
import { ApiError } from "@/lib/api";
import { formatBytes } from "@/lib/format";
import { useMeQuery, useProjectsQuery, useUploadBuildMutation } from "@/lib/queries";

const uploadSchema = z.object({
  projectId: z.string().min(1, "Выберите проект"),
  environment: z.enum(["DEV", "QA", "STAGING", "PROD_LIKE"]),
  channel: z.enum(["NIGHTLY", "ALPHA", "BETA", "RC", "INTERNAL", "CUSTOM"]),
  buildType: z.string().min(1, "Укажите тип сборки"),
  flavor: z.string().optional(),
  branch: z.string().optional(),
  commitHash: z.string().optional(),
  changelog: z.string().optional(),
});

type UploadFormData = z.infer<typeof uploadSchema>;

export default function UploadPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const meQuery = useMeQuery();
  const projectsQuery = useProjectsQuery();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [stage, setStage] = useState("Ожидаем APK");
  const [message, setMessage] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);

  const uploadMutation = useUploadBuildMutation((nextProgress) => {
    setStage("Загружаем APK");
    setProgress(nextProgress.progress);
  });

  const canUpload = meQuery.data?.role === "ADMIN" || meQuery.data?.role === "UPLOADER";

  const form = useForm<UploadFormData>({
    resolver: zodResolver(uploadSchema),
    defaultValues: {
      projectId: searchParams.get("projectId") ?? "",
      environment: "QA",
      channel: "INTERNAL",
      buildType: "debug",
      flavor: "",
      branch: "",
      commitHash: "",
      changelog: "",
    },
  });

  useEffect(() => {
    if (!form.getValues("projectId") && projectsQuery.data?.length) {
      form.setValue("projectId", projectsQuery.data[0].id);
    }
  }, [form, projectsQuery.data]);

  const selectedProjectId = form.watch("projectId");
  const selectedEnvironment = form.watch("environment");
  const selectedChannel = form.watch("channel");
  const selectedProject = useMemo(
    () => projectsQuery.data?.find((project) => project.id === selectedProjectId),
    [projectsQuery.data, selectedProjectId],
  );

  const selectApkFile = (file: File | null) => {
    if (file && !file.name.toLowerCase().endsWith(".apk")) {
      setSelectedFile(null);
      setIsError(true);
      setMessage("Можно загрузить только файл с расширением .apk");
      setStage("Неверный формат файла");
      setProgress(0);
      return;
    }
    setSelectedFile(file);
    setMessage(null);
    setIsError(false);
    setStage(file ? "APK выбран" : "Ожидаем APK");
    setProgress(0);
  };

  const submit = form.handleSubmit(async (values) => {
    if (!selectedFile) {
      setIsError(true);
      setMessage("Выберите APK-файл перед публикацией.");
      return;
    }
    setMessage(null);
    setIsError(false);
    setStage("Подготавливаем загрузку");
    setProgress(0);
    try {
      await uploadMutation.mutateAsync({
        ...values,
        file: selectedFile,
        flavor: values.flavor || undefined,
        branch: values.branch || undefined,
        commitHash: values.commitHash || undefined,
        changelog: values.changelog || undefined,
      });
      setStage("Сборка опубликована");
      setProgress(100);
      setMessage("Загрузка завершена. Открываем проект…");
      router.push(`/projects/${values.projectId}`);
    } catch (error) {
      setStage("Загрузка не выполнена");
      setIsError(true);
      setMessage(error instanceof ApiError ? error.message : "Не удалось загрузить сборку. Попробуйте снова.");
    }
  });

  if (!canUpload) {
    return (
      <div className="space-y-5">
        <PageHeader
          eyebrow="Upload"
          title="Новая сборка"
          description="Загрузка доступна администраторам и пользователям с ролью Uploader."
        />
        <EmptyState title="Недостаточно прав" description="Войдите под учётной записью администратора или загрузчика сборок." />
      </div>
    );
  }

  const hasFile = Boolean(selectedFile);
  const isPublishing = uploadMutation.isPending;
  const isPublished = progress === 100 && !isError;

  return (
    <div className="space-y-5">
      <PageHeader
        eyebrow="Upload"
        title="Новая сборка"
        description="Добавьте APK и проверьте метаданные перед публикацией в выбранный канал."
        actions={
          <div className="space-y-4">
            <div className="flex items-center justify-center gap-2 rounded-lg border border-[#dad7e8] bg-[color:color-mix(in_srgb,var(--surface)_78%,transparent)] px-4 py-2.5 font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.05em] dark:border-[var(--border)]">
              <Icon className="size-4 text-[var(--success)]" name="shield-check" />
              Доступ: ADMIN / UPLOADER
            </div>
            <ol aria-label="Этапы публикации" className="flex items-center justify-end gap-2 text-[10px] font-semibold">
              <UploadStep complete={hasFile} current={!hasFile} label="Файл" number={1} />
              <UploadStep complete={false} current={hasFile && !isPublishing && !isPublished} label="Данные" number={2} />
              <UploadStep complete={isPublished} current={isPublishing} label="Публикация" number={3} />
            </ol>
          </div>
        }
      />

      <form className="grid gap-4 xl:grid-cols-[minmax(0,1.55fr)_358px]" onSubmit={submit}>
        <Card>
          <CardHeader title="APK и метаданные" description="Версия и код сборки будут извлечены сервером после загрузки." />
          <CardContent className="grid gap-x-4 gap-y-5 md:grid-cols-2">
            <div className="md:col-span-2">
              <Label htmlFor="apk-file">APK-файл</Label>
              <label
                className="app-gradient flex min-h-[122px] cursor-pointer flex-col justify-center gap-4 rounded-[10px] border border-[#cfcbe0] p-5 transition-colors hover:border-[var(--accent-strong)] focus-within:outline focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-[var(--accent-strong)] sm:flex-row sm:items-center dark:border-[var(--border)]"
                htmlFor="apk-file"
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => {
                  event.preventDefault();
                  selectApkFile(event.dataTransfer.files?.[0] ?? null);
                }}
              >
                <span className="grid size-[52px] shrink-0 place-items-center rounded-[10px] bg-[var(--text-strong)] text-[var(--surface)]">
                  <Icon className="size-6" name="file-archive" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold text-[var(--text-strong)]">
                    {selectedFile?.name ?? "Перетащите APK сюда или выберите файл"}
                  </span>
                  <span className="mt-2 block font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">
                    {selectedFile ? `${formatBytes(selectedFile.size)} · файл готов к загрузке` : "APK · до 500 MB"}
                  </span>
                  {selectedFile ? (
                    <span className="mt-3 inline-flex items-center gap-1.5 rounded-md bg-[var(--badge-success-bg)] px-2.5 py-1.5 font-[var(--font-mono)] text-[9px] font-semibold uppercase text-[var(--badge-success-text)]">
                      <Icon className="size-3.5" name="check" />
                      APK выбран
                    </span>
                  ) : null}
                </span>
                <span className="inline-flex min-h-10 items-center justify-center rounded-lg border border-[var(--border)] bg-[var(--surface)] px-4 text-xs font-semibold text-[var(--text-strong)]">
                  {selectedFile ? "Заменить APK" : "Выбрать APK"}
                </span>
              </label>
              <input
                accept=".apk"
                className="sr-only"
                id="apk-file"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  selectApkFile(file);
                }}
                type="file"
              />
              <FieldHint>Сервер принимает multipart-загрузку размером до 500 MB.</FieldHint>
            </div>

            <div>
              <Label htmlFor="upload-project">Проект</Label>
              <Select aria-describedby={form.formState.errors.projectId ? "upload-project-error" : undefined} aria-invalid={Boolean(form.formState.errors.projectId)} id="upload-project" {...form.register("projectId")}>
                {(projectsQuery.data ?? []).map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </Select>
              {form.formState.errors.projectId ? <FieldError id="upload-project-error">{form.formState.errors.projectId.message}</FieldError> : null}
            </div>
            <div>
              <Label htmlFor="upload-environment">Окружение</Label>
              <Select id="upload-environment" {...form.register("environment")}>
                <option value="DEV">DEV</option>
                <option value="QA">QA</option>
                <option value="STAGING">STAGING</option>
                <option value="PROD_LIKE">PROD_LIKE</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="upload-channel">Канал</Label>
              <Select id="upload-channel" {...form.register("channel")}>
                <option value="NIGHTLY">NIGHTLY</option>
                <option value="ALPHA">ALPHA</option>
                <option value="BETA">BETA</option>
                <option value="RC">RC</option>
                <option value="INTERNAL">INTERNAL</option>
                <option value="CUSTOM">CUSTOM</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="upload-build-type">Тип сборки</Label>
              <Input aria-describedby={form.formState.errors.buildType ? "upload-build-type-error" : undefined} aria-invalid={Boolean(form.formState.errors.buildType)} id="upload-build-type" {...form.register("buildType")} placeholder="debug / release" />
              {form.formState.errors.buildType ? <FieldError id="upload-build-type-error">{form.formState.errors.buildType.message}</FieldError> : null}
            </div>
            <div>
              <Label htmlFor="upload-flavor">Вариант сборки</Label>
              <Input id="upload-flavor" {...form.register("flavor")} placeholder="qa, prod" />
            </div>
            <div>
              <Label htmlFor="upload-branch">Ветка</Label>
              <Input id="upload-branch" {...form.register("branch")} placeholder="feature/build-flow" />
            </div>
            <div>
              <Label htmlFor="upload-commit">Хеш коммита</Label>
              <Input id="upload-commit" {...form.register("commitHash")} placeholder="d34db33f" />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="upload-changelog">Что изменилось</Label>
              <Textarea id="upload-changelog" {...form.register("changelog")} placeholder="Коротко опишите изменения для тестировщиков." />
            </div>
          </CardContent>
        </Card>

        <Card className="h-fit xl:sticky xl:top-[88px]">
          <CardHeader title="Перед публикацией" description="Проверьте, куда попадёт эта сборка." />
          <CardContent className="space-y-5">
            <div className="rounded-[10px] bg-[var(--surface-muted)] p-4">
              <p className="font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.07em] text-[var(--text-muted)]">Проект</p>
              <p className="mt-2 text-sm font-semibold">{selectedProject?.name ?? "—"}</p>
              <p className="mt-1 truncate font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">
                {selectedProject?.packageName ?? "Проект не выбран"}
              </p>
            </div>

            <dl className="divide-y divide-[var(--border-soft)] text-xs">
              <SummaryRow label="Файл" value={selectedFile?.name ?? "Не выбран"} />
              <SummaryRow label="Размер" value={selectedFile ? formatBytes(selectedFile.size) : "—"} mono />
              <SummaryRow label="Окружение" value={selectedEnvironment} mono />
              <SummaryRow label="Канал" value={selectedChannel} mono />
            </dl>

            <div className="app-gradient rounded-[10px] border border-[#ddd9ea] p-4 dark:border-[var(--border)]">
              <p className="font-[var(--font-mono)] text-[9px] font-semibold uppercase tracking-[0.07em] text-[var(--text-muted)]">Куда публикуем</p>
              <p className="mt-2 font-[var(--font-mono)] text-[11px] font-semibold">{selectedEnvironment} · {selectedChannel}</p>
            </div>

            <div>
              <div className="mb-2 flex items-center justify-between text-xs">
                <span className={isError ? "font-semibold text-[var(--danger)]" : "font-semibold text-[var(--text)]"}>{stage}</span>
                <span className="font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">{progress}%</span>
              </div>
              <div
                aria-label={stage}
                aria-valuemax={100}
                aria-valuemin={0}
                aria-valuenow={progress}
                className="h-1.5 overflow-hidden rounded-full bg-[var(--surface-muted)]"
                role="progressbar"
              >
                <div
                  className={`h-full rounded-full transition-[width] duration-300 ${isError ? "bg-[var(--danger)]" : "bg-[var(--success)]"}`}
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>

            {message ? (
              <div aria-live="polite" className={`rounded-lg px-3 py-2.5 text-xs leading-5 ${isError ? "bg-[var(--badge-danger-bg)] text-[var(--badge-danger-text)]" : "bg-[var(--badge-success-bg)] text-[var(--badge-success-text)]"}`} role={isError ? "alert" : "status"}>
                {message}
              </div>
            ) : null}

            <Button className="h-[50px] w-full" disabled={isPublishing || !selectedFile} type="submit">
              {isPublishing ? "Публикуем…" : "Опубликовать сборку"}
              {!isPublishing ? <Icon className="size-[17px]" name="arrow-right" /> : null}
            </Button>
            <p className="text-center text-[10px] text-[var(--text-muted)]">Действие будет записано в аудит.</p>
          </CardContent>
        </Card>
      </form>
    </div>
  );
}

function UploadStep({ complete, current, label, number }: { complete: boolean; current: boolean; label: string; number: number }) {
  return (
    <li className={`flex items-center gap-1.5 ${complete ? "text-[var(--success)]" : current ? "text-[var(--text-strong)]" : "text-[var(--text-muted)]"}`}>
      <span
        className={`grid size-6 place-items-center rounded-full font-[var(--font-mono)] text-[9px] font-bold ${
          complete
            ? "bg-[var(--badge-success-bg)] text-[var(--badge-success-text)]"
            : current
              ? "bg-[var(--text-strong)] text-[var(--surface)]"
              : "bg-[var(--surface)] text-[var(--text-muted)]"
        }`}
      >
        {complete ? <Icon className="size-3.5" name="check" /> : number}
      </span>
      <span>{label}</span>
    </li>
  );
}

function SummaryRow({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="grid grid-cols-[86px_minmax(0,1fr)] gap-3 py-3">
      <dt className="text-[var(--text-muted)]">{label}</dt>
      <dd className={`truncate text-right text-[var(--text-strong)] ${mono ? "font-[var(--font-mono)] text-[10px]" : ""}`} title={value}>
        {value}
      </dd>
    </div>
  );
}

function FieldError({ children, id }: { children?: string; id?: string }) {
  if (!children) return null;
  return <p className="mt-2 text-xs text-[var(--danger)]" id={id}>{children}</p>;
}
