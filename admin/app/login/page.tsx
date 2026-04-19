"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";

const schema = z.object({
  email: z.string().email("Введите корректный email"),
  password: z.string().min(1, "Введите пароль"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const { saveTokens } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      const tokens = await api.auth.login(values.email, values.password);
      saveTokens(tokens);
      router.replace("/overview");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка входа. Попробуйте снова.");
    }
  });

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-10">
      <ThemeToggle className="absolute right-4 top-4 sm:right-6 sm:top-6" />
      <Card className="w-full max-w-lg overflow-hidden">
        <div className="bg-[var(--primary)] px-6 py-8 text-[var(--primary-contrast)]">
          <p className="text-xs uppercase tracking-[0.24em] text-[var(--primary-contrast-muted)]">AppDistribution</p>
          <h1 className="mt-3 text-3xl font-semibold">Web Admin</h1>
          <p className="mt-2 max-w-md text-sm text-[var(--primary-contrast-soft)]">
            Sign in with your credentials.
          </p>
        </div>
        <CardContent className="space-y-6 p-6">
          <form className="space-y-4" onSubmit={onSubmit}>
            <div>
              <Label>Email</Label>
              <Input placeholder="name@company.com" type="email" {...register("email")} />
              {errors.email && (
                <p className="mt-2 text-sm text-[var(--danger)]">{errors.email.message}</p>
              )}
            </div>
            <div>
              <Label>Password</Label>
              <Input placeholder="••••••••" type="password" {...register("password")} />
              {errors.password && (
                <p className="mt-2 text-sm text-[var(--danger)]">{errors.password.message}</p>
              )}
            </div>
            {error && <p className="text-sm text-[var(--danger)]">{error}</p>}
            <Button className="w-full" disabled={isSubmitting} type="submit">
              {isSubmitting ? "Signing in…" : "Sign In"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
