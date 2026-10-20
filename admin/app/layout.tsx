import type { Metadata } from "next";
import { Funnel_Sans, Geist, Geist_Mono } from "next/font/google";
import Script from "next/script";
import { ToastProvider } from "@/components/toast-provider";
import { AuthProvider } from "@/lib/auth-context";
import { QueryProvider } from "@/lib/query-provider";
import { ThemeProvider } from "@/lib/theme-provider";
import { themeInitScript } from "@/lib/theme";
import "react-toastify/dist/ReactToastify.css";
import "./globals.css";
import type { ReactNode } from "react";

const displayFont = Funnel_Sans({
  subsets: ["latin"],
  variable: "--font-display",
});

const uiFont = Geist({
  subsets: ["latin"],
  variable: "--font-ui",
});

const monoFont = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "AppDistribution Admin",
  description: "Release operations console for AppDistribution backend",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ru" suppressHydrationWarning className={`${displayFont.variable} ${uiFont.variable} ${monoFont.variable}`}>
      <body className="font-[var(--font-ui)] text-[var(--text-strong)] antialiased">
        <template
          data-design-contract="appdist-admin-f14071b4"
          dangerouslySetInnerHTML={{
            __html:
              "<!-- THESIS: one continuous release workspace. OWN-WORLD: graphite, milk, lilac, pale rose/blue gradients, quiet borders. STORY: sign in, scan builds, upload and publish. FIRST VIEWPORT: 244px navigation, task header, operational content. FORM: Lovable Utility operate console. FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md -->",
          }}
        />
        <Script id="theme-init" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <ThemeProvider>
          <QueryProvider>
            <AuthProvider>
              {children}
              <ToastProvider />
            </AuthProvider>
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
