"use client";

import { ToastContainer } from "react-toastify";
import { useTheme } from "@/lib/theme-provider";

export function ToastProvider() {
  const { resolvedTheme } = useTheme();

  return (
    <ToastContainer
      autoClose={6000}
      closeButton
      draggable={false}
      hideProgressBar
      newestOnTop
      pauseOnFocusLoss={false}
      pauseOnHover
      position="top-right"
      theme={resolvedTheme}
      toastClassName="!rounded-3xl !border !border-[var(--border)] !bg-[var(--surface)] !p-0 !shadow-[var(--shadow-soft)]"
      bodyClassName="!m-0 !px-4 !py-4 !text-sm !leading-6 !whitespace-pre-line !text-[var(--text)]"
    />
  );
}
