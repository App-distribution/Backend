"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";
import type { CreateUserPayload } from "@/lib/types";

const ROLES: CreateUserPayload["role"][] = ["UPLOADER", "TESTER", "VIEWER"];

const schema = z.object({
  email: z.string().email("Valid email required"),
  name: z.string().min(1, "Name required"),
  role: z.enum(["UPLOADER", "TESTER", "VIEWER"]),
});

type FormValues = z.infer<typeof schema>;

interface CreateUserDialogProps {
  workspaceId: string;
  onCreated: (generatedPassword: string) => void;
  onClose: () => void;
}

export function CreateUserDialog({ workspaceId, onCreated, onClose }: CreateUserDialogProps) {
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", name: "", role: "TESTER" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    try {
      const result = await api.workspace.createUser(workspaceId, values);
      onCreated(result.generatedPassword);
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "Failed to create user");
    }
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-md mx-4">
        <CardContent className="space-y-4 p-6">
          <h2 className="text-lg font-semibold">Add member</h2>
          <form className="space-y-4" onSubmit={onSubmit}>
            <div>
              <Label>Email</Label>
              <Input placeholder="name@company.com" type="email" {...register("email")} />
              {errors.email && <p className="mt-1 text-sm text-[var(--danger)]">{errors.email.message}</p>}
            </div>
            <div>
              <Label>Name</Label>
              <Input placeholder="Full name" {...register("name")} />
              {errors.name && <p className="mt-1 text-sm text-[var(--danger)]">{errors.name.message}</p>}
            </div>
            <div>
              <Label>Role</Label>
              <select
                className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-sm"
                {...register("role")}
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            {serverError && <p className="text-sm text-[var(--danger)]">{serverError}</p>}
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={onClose}>
                Cancel
              </Button>
              <Button className="flex-1" type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Creating…" : "Create"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
