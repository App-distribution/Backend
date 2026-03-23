"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ApiError } from "@/lib/api";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { FieldHint, Input, Label, Select, Textarea } from "@/components/ui/field";
import { formatBytes } from "@/lib/format";
import { useMeQuery, useProjectsQuery, useUploadBuildMutation } from "@/lib/queries";

const uploadSchema = z.object({
  projectId: z.string().min(1, "Project is required"),
  environment: z.enum(["DEV", "QA", "STAGING", "PROD_LIKE"]),
  channel: z.enum(["NIGHTLY", "ALPHA", "BETA", "RC", "INTERNAL", "CUSTOM"]),
  buildType: z.string().min(1, "Build type is required"),
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
  const [stage, setStage] = useState("Idle");
  const [message, setMessage] = useState<string | null>(null);

  const uploadMutation = useUploadBuildMutation((nextProgress) => {
    setStage("Uploading APK");
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
  const selectedProject = useMemo(
    () => projectsQuery.data?.find((project) => project.id === selectedProjectId),
    [projectsQuery.data, selectedProjectId],
  );

  const submit = form.handleSubmit(async (values) => {
    if (!selectedFile) {
      setMessage("APK file is required");
      return;
    }
    setMessage(null);
    setStage("Preparing upload");
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
      setStage("Build saved");
      setProgress(100);
      setMessage("Upload completed");
      router.push(`/projects/${values.projectId}`);
    } catch (error) {
      setStage("Failed");
      setMessage(error instanceof ApiError ? error.message : "Upload failed");
    }
  });

  if (!canUpload) {
    return (
      <div className="space-y-4">
        <PageHeader
          eyebrow="Upload"
          title="Publish a new APK"
          description="Uploading builds is restricted to Admin and Uploader roles."
        />
        <EmptyState title="Insufficient permissions" description="Use an admin or uploader account to access the release upload flow." />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Upload"
        title="Publish a new APK"
        description="Upload is designed as a controlled release operation. Metadata fields stay visible while the transfer is in progress."
      />

      <form className="grid gap-4 xl:grid-cols-[minmax(0,1.3fr)_380px]" onSubmit={submit}>
        <Card>
          <CardHeader title="Build payload" description="APK file plus release metadata sent to /builds/upload" />
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div className="md:col-span-2">
              <Label>APK file</Label>
              <input
                accept=".apk"
                className="block w-full rounded-3xl border border-dashed border-[var(--border)] bg-[var(--surface-muted)] px-4 py-8 text-sm"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
                type="file"
              />
              <FieldHint>Current backend accepts multipart upload up to 500 MB.</FieldHint>
            </div>
            <div>
              <Label>Project</Label>
              <Select {...form.register("projectId")}>
                {(projectsQuery.data ?? []).map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <Label>Build type</Label>
              <Input {...form.register("buildType")} placeholder="debug / release" />
            </div>
            <div>
              <Label>Environment</Label>
              <Select {...form.register("environment")}>
                <option value="DEV">DEV</option>
                <option value="QA">QA</option>
                <option value="STAGING">STAGING</option>
                <option value="PROD_LIKE">PROD_LIKE</option>
              </Select>
            </div>
            <div>
              <Label>Channel</Label>
              <Select {...form.register("channel")}>
                <option value="NIGHTLY">NIGHTLY</option>
                <option value="ALPHA">ALPHA</option>
                <option value="BETA">BETA</option>
                <option value="RC">RC</option>
                <option value="INTERNAL">INTERNAL</option>
                <option value="CUSTOM">CUSTOM</option>
              </Select>
            </div>
            <div>
              <Label>Flavor</Label>
              <Input {...form.register("flavor")} placeholder="qa, prod" />
            </div>
            <div>
              <Label>Branch</Label>
              <Input {...form.register("branch")} placeholder="feature/build-flow" />
            </div>
            <div>
              <Label>Commit hash</Label>
              <Input {...form.register("commitHash")} placeholder="d34db33f" />
            </div>
            <div className="md:col-span-2">
              <Label>Changelog</Label>
              <Textarea {...form.register("changelog")} placeholder="Summarize what testers need to know." />
            </div>
          </CardContent>
        </Card>

        <Card className="h-fit">
          <CardHeader title="Upload summary" description="Live session state during the release operation." />
          <CardContent className="space-y-4">
            <div className="rounded-3xl bg-[var(--surface-muted)] p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-[var(--text-muted)]">Selected project</p>
              <p className="mt-2 text-base font-semibold">{selectedProject?.name ?? "—"}</p>
              <p className="mt-1 font-[var(--font-mono)] text-sm text-[var(--text-muted)]">
                {selectedProject?.packageName ?? "No package selected"}
              </p>
            </div>
            <div className="rounded-3xl bg-[var(--surface-muted)] p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-[var(--text-muted)]">Selected file</p>
              <p className="mt-2 text-sm font-semibold">{selectedFile?.name ?? "No APK selected"}</p>
              <p className="mt-1 text-sm text-[var(--text-muted)]">{selectedFile ? formatBytes(selectedFile.size) : "—"}</p>
            </div>
            <div>
              <div className="mb-2 flex items-center justify-between text-sm">
                <span className="font-semibold text-[var(--text)]">{stage}</span>
                <span className="text-[var(--text-muted)]">{progress}%</span>
              </div>
              <div className="h-2 rounded-full bg-[var(--surface-muted)]">
                <div className="h-2 rounded-full bg-[var(--primary)] transition-all" style={{ width: `${progress}%` }} />
              </div>
            </div>
            {message ? <p className="text-sm text-[var(--text-muted)]">{message}</p> : null}
            <Button className="w-full" disabled={uploadMutation.isPending} type="submit">
              Start upload
            </Button>
          </CardContent>
        </Card>
      </form>
    </div>
  );
}
