import type { LabelHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils";

const baseFieldClass =
  "min-h-11 w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-3.5 py-2.5 text-sm text-[var(--text-strong)] outline-none transition placeholder:text-[var(--text-muted)] focus:border-[var(--accent-strong)] focus:ring-4 focus:ring-[var(--focus-ring)]";

export function Label({ children, className, ...props }: LabelHTMLAttributes<HTMLLabelElement> & { children: ReactNode }) {
  return <label {...props} className={cn("mb-2 block text-[13px] font-semibold text-[var(--text-strong)]", className)}>{children}</label>;
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={cn(baseFieldClass, props.className)} />;
}

export function Textarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} className={cn(baseFieldClass, "min-h-28 resize-y leading-6", props.className)} />;
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={cn(baseFieldClass, props.className)} />;
}

export function FieldHint({ children }: { children: ReactNode }) {
  return <p className="mt-2 text-[12px] leading-5 text-[var(--text-muted)]">{children}</p>;
}
