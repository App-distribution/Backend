"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ApiError } from "@/lib/api";
import { PageHeader } from "@/components/page-header";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";
import { formatDateTime } from "@/lib/format";
import { useMeQuery, useProfileMutation, useWorkspaceQuery } from "@/lib/queries";

const profileSchema = z.object({
  name: z.string().min(1, "Укажите имя"),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export default function ProfilePage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const profileMutation = useProfileMutation();
  const [message, setMessage] = useState<string | null>(null);

  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: "",
    },
  });

  useEffect(() => {
    if (meQuery.data) {
      form.reset({
        name: meQuery.data.name,
      });
    }
  }, [form, meQuery.data]);

  const submit = form.handleSubmit(async (values) => {
    setMessage(null);
    try {
      await profileMutation.mutateAsync({
        name: values.name,
      });
      setMessage("Профиль обновлён");
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Не удалось обновить профиль");
    }
  });

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Profile"
        title="Настройки профиля"
        description="Данные учётной записи и контекст текущего рабочего пространства."
      />

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_360px]">
        <Card>
          <CardHeader title="Данные профиля" description="Имя пользователя в административной панели." />
          <CardContent>
            <form className="space-y-4" onSubmit={submit}>
              <div>
                <Label htmlFor="profile-name">Имя</Label>
                <Input id="profile-name" {...form.register("name")} />
              </div>
              <Button disabled={profileMutation.isPending} type="submit">
                Сохранить
              </Button>
              {message ? <p className="text-sm text-[var(--text-muted)]">{message}</p> : null}
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader title="Текущая сессия" description="Данные аккаунта только для чтения." />
          <CardContent className="space-y-3 text-sm text-[var(--text)]">
            <p>Email: {meQuery.data?.email ?? "—"}</p>
            <p>Роль: {meQuery.data?.role ?? "—"}</p>
            <p>Пространство: {workspaceQuery.data?.name ?? "—"}</p>
            <p>Добавлен: {meQuery.data ? formatDateTime(meQuery.data.createdAt) : "—"}</p>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
