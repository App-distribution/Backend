import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

const baseFieldClass =
  "w-full rounded-2xl border border-[var(--border)] bg-[var(--surface)] px-3.5 py-2.5 text-sm text-[var(--text-strong)] outline-none transition placeholder:text-[var(--text-muted)] focus:border-[var(--primary)] focus:ring-4 focus:ring-[var(--focus-ring)]";

export function Label({ children }: { children: ReactNode }) {
  return <label className="mb-2 block text-sm font-semibold text-[var(--text)]">{children}</label>;
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(baseFieldClass, props.className)} />;
}

export function Textarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn(baseFieldClass, "min-h-28 resize-y", props.className)} />;
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn(baseFieldClass, props.className)} />;
}

export function FieldHint({ children }: { children: ReactNode }) {
  return <p className="mt-2 text-xs text-[var(--text-muted)]">{children}</p>;
}
