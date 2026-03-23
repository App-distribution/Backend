import type { Metadata } from "next";
import { IBM_Plex_Mono, Manrope } from "next/font/google";
import { ToastProvider } from "@/components/toast-provider";
import { AuthProvider } from "@/lib/auth-context";
import { QueryProvider } from "@/lib/query-provider";
import "react-toastify/dist/ReactToastify.css";
import "./globals.css";
import type { ReactNode } from "react";

const uiFont = Manrope({
  subsets: ["latin", "cyrillic"],
  variable: "--font-ui",
});

const monoFont = IBM_Plex_Mono({
  subsets: ["latin", "cyrillic"],
  variable: "--font-mono",
  weight: ["400", "500"],
});

export const metadata: Metadata = {
  title: "AppDistribution Admin",
  description: "Release operations console for AppDistribution backend",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ru" className={`${uiFont.variable} ${monoFont.variable}`}>
      <body className="font-[var(--font-ui)] text-[var(--text-strong)] antialiased">
        <QueryProvider>
          <AuthProvider>
            {children}
            <ToastProvider />
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
