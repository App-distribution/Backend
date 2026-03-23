"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";

const emailSchema = z.object({
  email: z.email("Введите корректный email"),
});

const otpSchema = z.object({
  otp: z.string().min(4, "Введите OTP код"),
});

export default function LoginPage() {
  const router = useRouter();
  const { saveTokens } = useAuth();
  const [step, setStep] = useState<"email" | "otp">("email");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const emailForm = useForm<z.infer<typeof emailSchema>>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: "" },
  });

  const otpForm = useForm<z.infer<typeof otpSchema>>({
    resolver: zodResolver(otpSchema),
    defaultValues: { otp: "" },
  });

  const submitEmail = emailForm.handleSubmit(async (values) => {
    setIsSubmitting(true);
    setMessage(null);
    try {
      await api.auth.requestOtp(values.email);
      setEmail(values.email);
      setStep("otp");
      setMessage(`OTP code was requested for ${values.email}`);
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Failed to request OTP");
    } finally {
      setIsSubmitting(false);
    }
  });

  const submitOtp = otpForm.handleSubmit(async (values) => {
    setIsSubmitting(true);
    setMessage(null);
    try {
      const tokens = await api.auth.verifyOtp(email, values.otp);
      saveTokens(tokens);
      router.replace("/overview");
    } catch (error) {
      setMessage(error instanceof ApiError ? error.message : "Failed to verify OTP");
    } finally {
      setIsSubmitting(false);
    }
  });

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <Card className="w-full max-w-lg overflow-hidden">
        <div className="bg-[var(--primary)] px-6 py-8 text-white">
          <p className="text-xs uppercase tracking-[0.24em] text-white/70">AppDistribution</p>
          <h1 className="mt-3 text-3xl font-semibold">Web Admin</h1>
          <p className="mt-2 max-w-md text-sm text-white/80">
            OTP-based sign in for release managers, workspace admins and observers.
          </p>
        </div>
        <CardContent className="space-y-6 p-6">
          {step === "email" ? (
            <form className="space-y-4" onSubmit={submitEmail}>
              <div>
                <Label>Email</Label>
                <Input placeholder="name@company.com" {...emailForm.register("email")} />
                {emailForm.formState.errors.email ? (
                  <p className="mt-2 text-sm text-[var(--danger)]">{emailForm.formState.errors.email.message}</p>
                ) : null}
              </div>
              <Button className="w-full" disabled={isSubmitting} type="submit">
                Request OTP
              </Button>
            </form>
          ) : (
            <form className="space-y-4" onSubmit={submitOtp}>
              <div className="rounded-2xl bg-[var(--surface-muted)] px-4 py-3 text-sm text-[var(--text)]">
                OTP sent to <span className="font-semibold">{email}</span>
              </div>
              <div>
                <Label>OTP code</Label>
                <Input placeholder="123456" {...otpForm.register("otp")} />
                {otpForm.formState.errors.otp ? (
                  <p className="mt-2 text-sm text-[var(--danger)]">{otpForm.formState.errors.otp.message}</p>
                ) : null}
              </div>
              <div className="flex gap-3">
                <Button variant="secondary" className="flex-1" onClick={() => setStep("email")} type="button">
                  Change email
                </Button>
                <Button className="flex-1" disabled={isSubmitting} type="submit">
                  Verify
                </Button>
              </div>
            </form>
          )}
          {message ? <p className="text-sm text-[var(--text-muted)]">{message}</p> : null}
        </CardContent>
      </Card>
    </div>
  );
}
