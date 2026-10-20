"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { BrandMark } from "@/components/brand-mark";
import { Icon } from "@/components/icons";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/field";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

const emailSchema = z.object({
  email: z.email("Введите корректный email"),
});

const otpSchema = z.object({
  otp: z.string().min(4, "Введите OTP-код"),
});

const releaseItems = [
  { version: "7.10.38", target: "QA · INTERNAL", state: "ГОТОВА", tone: "success" },
  { version: "7.10.37", target: "STAGING · INTERNAL", state: "В ИСТОРИИ", tone: "neutral" },
  { version: "7.10.36", target: "PROD_LIKE · INTERNAL", state: "В ИСТОРИИ", tone: "neutral" },
] as const;

export default function LoginPage() {
  const router = useRouter();
  const { saveTokens } = useAuth();
  const [step, setStep] = useState<"email" | "otp">("email");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const emailForm = useForm<z.infer<typeof emailSchema>>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: "" },
  });

  const otpForm = useForm<z.infer<typeof otpSchema>>({
    resolver: zodResolver(otpSchema),
    defaultValues: { otp: "" },
  });

  const requestCode = async (nextEmail: string) => {
    setIsSubmitting(true);
    setMessage(null);
    setIsError(false);
    try {
      await api.auth.requestOtp(nextEmail);
      setEmail(nextEmail);
      setStep("otp");
      setMessage(`Код отправлен на ${nextEmail}`);
    } catch (error) {
      setIsError(true);
      setMessage(error instanceof ApiError ? error.message : "Не удалось отправить OTP-код");
    } finally {
      setIsSubmitting(false);
    }
  };

  const submitEmail = emailForm.handleSubmit(async (values) => requestCode(values.email));

  const continueWithExistingCode = emailForm.handleSubmit((values) => {
    setEmail(values.email);
    setStep("otp");
    setIsError(false);
    setMessage("Введите полученный ранее код.");
  });

  const submitOtp = otpForm.handleSubmit(async (values) => {
    setIsSubmitting(true);
    setMessage(null);
    setIsError(false);
    try {
      const tokens = await api.auth.verifyOtp(email, values.otp);
      saveTokens(tokens);
      const requestedPath = new URLSearchParams(window.location.search).get("next");
      router.replace(requestedPath?.startsWith("/") && !requestedPath.startsWith("//") ? requestedPath : "/builds");
    } catch (error) {
      setIsError(true);
      setMessage(error instanceof ApiError ? error.message : "Код не подошёл. Проверьте его и попробуйте снова.");
    } finally {
      setIsSubmitting(false);
    }
  });

  return (
    <main className="grid min-h-screen bg-[var(--background)] p-4 lg:grid-cols-[minmax(520px,0.88fr)_minmax(560px,1.12fr)] lg:p-6">
      <section className="app-gradient relative hidden min-h-[640px] flex-col overflow-hidden rounded-[20px] border border-[#dad7e8] p-8 dark:border-[var(--border)] sm:p-12 lg:flex">
        <BrandMark />
        <div className="mt-20 max-w-[530px]">
          <h1 className="font-[var(--font-display)] text-[clamp(2.8rem,4vw,3.25rem)] font-bold leading-[1.02] tracking-[-0.035em]">
            <span className="block">Сборки доходят до</span>
            <span className="block">тестировщиков без</span>
            <span className="block">лишних переходов.</span>
          </h1>
          <p className="mt-6 max-w-[470px] text-base leading-7 text-[var(--text-muted)]">
            Управляйте APK, каналами и окружениями в одной рабочей консоли.
          </p>
        </div>

        <div className="mt-auto rounded-[14px] border border-[#dad7e8] bg-[color:color-mix(in_srgb,var(--surface)_86%,transparent)] p-5 shadow-[var(--shadow-soft)] backdrop-blur-sm dark:border-[var(--border)]">
          <div className="mb-6 flex items-center justify-between gap-4">
            <p className="text-[13px] font-semibold">Сегодня в релизном потоке</p>
            <p className="font-[var(--font-mono)] text-[10px] text-[var(--text-muted)]">3 окружения</p>
          </div>
          <div className="relative space-y-5 before:absolute before:bottom-4 before:left-[8px] before:top-4 before:w-px before:bg-[var(--accent)]">
            {releaseItems.map((item, index) => (
              <div className="relative grid grid-cols-[20px_1fr_auto] items-center gap-4" key={item.version}>
                <span
                  className={index === 0 ? "z-10 size-4 rounded-full border-[3px] border-white bg-[var(--text-strong)]" : "z-10 size-4 rounded-full border-[3px] border-white bg-[var(--accent)]"}
                />
                <div>
                  <p className="font-[var(--font-display)] text-lg font-bold">{item.version}</p>
                  <p className="mt-1 font-[var(--font-mono)] text-[10px] tracking-[0.04em] text-[var(--text-muted)]">{item.target}</p>
                </div>
                <span
                  className={item.tone === "success"
                    ? "rounded-md bg-[var(--badge-success-bg)] px-3 py-2 font-[var(--font-mono)] text-[9px] font-semibold text-[var(--badge-success-text)]"
                    : "rounded-md bg-[var(--surface-muted)] px-3 py-2 font-[var(--font-mono)] text-[9px] font-semibold text-[var(--text-muted)]"}
                >
                  {item.state}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="relative flex min-h-[calc(100vh-2rem)] items-center justify-center px-2 py-24 sm:px-10 lg:min-h-[640px] lg:px-16 lg:py-20">
        <BrandMark className="absolute left-0 top-0 lg:hidden" />
        <div className="absolute right-0 top-0 flex items-center gap-3">
          <span className="hidden text-xs text-[var(--text-muted)] sm:inline">Системная тема</span>
          <ThemeToggle compact />
        </div>

        <div className="w-full max-w-[456px]">
          <h2 className="font-[var(--font-display)] text-4xl font-bold tracking-[-0.025em]">
            {step === "email" ? "Войти в AppDist" : "Введите код"}
          </h2>
          <p className="mt-2 text-[15px] leading-6 text-[var(--text-muted)]">
            {step === "email" ? "Получите одноразовый код на рабочую почту." : <>Мы отправили код на <strong className="font-semibold text-[var(--text-strong)]">{email}</strong>.</>}
          </p>

          {step === "email" ? (
            <form className="mt-10 space-y-4" onSubmit={submitEmail}>
              <div>
                <Label htmlFor="login-email">Рабочая почта</Label>
                <div className="relative">
                  <Icon className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-[var(--text-muted)]" name="mail" />
                  <Input
                    aria-describedby={emailForm.formState.errors.email ? "login-email-error" : undefined}
                    aria-invalid={Boolean(emailForm.formState.errors.email)}
                    autoComplete="email"
                    className="h-[52px] bg-[var(--surface)] pl-11"
                    id="login-email"
                    placeholder="name@company.com"
                    {...emailForm.register("email")}
                  />
                </div>
                {emailForm.formState.errors.email ? (
                  <p className="mt-2 text-sm text-[var(--danger)]" id="login-email-error">{emailForm.formState.errors.email.message}</p>
                ) : null}
              </div>
              <Button className="h-[52px] w-full" disabled={isSubmitting} type="submit">
                {isSubmitting ? "Отправляем код…" : "Получить код"}
                {!isSubmitting ? <Icon className="size-[17px]" name="arrow-right" /> : null}
              </Button>
              <Button
                className="h-[52px] w-full"
                disabled={isSubmitting}
                onClick={() => void continueWithExistingCode()}
                type="button"
                variant="secondary"
              >
                У меня уже есть код
              </Button>
              <p className="text-center text-xs text-[var(--text-muted)]">Код действует 10 минут. Пароль не нужен.</p>
            </form>
          ) : (
            <form className="mt-10 space-y-4" onSubmit={submitOtp}>
              <div>
                <Label htmlFor="login-otp">OTP-код</Label>
                <Input
                  aria-describedby={otpForm.formState.errors.otp ? "login-otp-error" : undefined}
                  aria-invalid={Boolean(otpForm.formState.errors.otp)}
                  autoComplete="one-time-code"
                  autoFocus
                  className="h-[56px] bg-[var(--surface)] text-center font-[var(--font-mono)] text-xl tracking-[0.28em]"
                  inputMode="numeric"
                  id="login-otp"
                  placeholder="123456"
                  {...otpForm.register("otp")}
                />
                {otpForm.formState.errors.otp ? (
                  <p className="mt-2 text-sm text-[var(--danger)]" id="login-otp-error">{otpForm.formState.errors.otp.message}</p>
                ) : null}
              </div>
              <Button className="h-[52px] w-full" disabled={isSubmitting} type="submit">
                {isSubmitting ? "Проверяем код…" : "Войти"}
                {!isSubmitting ? <Icon className="size-[17px]" name="arrow-right" /> : null}
              </Button>
              <div className="flex flex-wrap items-center justify-center gap-x-5 gap-y-2 text-xs">
                <button className="text-[var(--text-muted)] underline-offset-4 hover:text-[var(--text-strong)] hover:underline" onClick={() => setStep("email")} type="button">
                  Изменить почту
                </button>
                <button className="text-[var(--text-muted)] underline-offset-4 hover:text-[var(--text-strong)] hover:underline" disabled={isSubmitting} onClick={() => void requestCode(email)} type="button">
                  Отправить код снова
                </button>
              </div>
            </form>
          )}

          {message ? (
            <div aria-live="polite" className={`mt-5 rounded-lg px-4 py-3 text-sm ${isError ? "bg-[var(--badge-danger-bg)] text-[var(--badge-danger-text)]" : "bg-[var(--badge-success-bg)] text-[var(--badge-success-text)]"}`} role={isError ? "alert" : "status"}>
              {message}
            </div>
          ) : null}
        </div>

        <p className="absolute bottom-1 text-center text-xs text-[var(--text-muted)]">
          Доступ только для администраторов и загрузчиков сборок.
        </p>
      </section>
    </main>
  );
}
