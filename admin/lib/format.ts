import type { Build, BuildEnvironment, BuildStatus, ReleaseChannel, UserRole } from "@/lib/types";

const dateTimeFormatter = new Intl.DateTimeFormat("ru-RU", {
  dateStyle: "medium",
  timeStyle: "short",
});

export function formatDateTime(value: number | string | null | undefined) {
  if (!value) return "—";
  const date = typeof value === "string" ? new Date(value) : new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : dateTimeFormatter.format(date);
}

export function formatBytes(bytes: number | null | undefined) {
  if (!bytes && bytes !== 0) return "—";
  const units = ["B", "KB", "MB", "GB"];
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(value >= 100 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export function formatRelativeTime(value: number | string | null | undefined) {
  if (!value) return "—";
  const date = typeof value === "string" ? new Date(value) : new Date(value);
  const diffMs = date.getTime() - Date.now();
  const rtf = new Intl.RelativeTimeFormat("ru", { numeric: "auto" });
  const minutes = Math.round(diffMs / (1000 * 60));
  if (Math.abs(minutes) < 60) return rtf.format(minutes, "minute");
  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) return rtf.format(hours, "hour");
  const days = Math.round(hours / 24);
  return rtf.format(days, "day");
}

export function truncateMiddle(value: string | null | undefined, visible = 6) {
  if (!value) return "—";
  if (value.length <= visible * 2 + 1) return value;
  return `${value.slice(0, visible)}…${value.slice(-visible)}`;
}

export function formatRole(role: UserRole) {
  return {
    ADMIN: "Admin",
    UPLOADER: "Uploader",
    TESTER: "Tester",
    VIEWER: "Viewer",
  }[role];
}

export function formatChannel(channel: ReleaseChannel) {
  return channel.toLowerCase();
}

export function formatEnvironment(environment: BuildEnvironment) {
  return environment.toLowerCase().replace("_", " ");
}

export function formatBuildStatus(status: BuildStatus) {
  return status.toLowerCase();
}

export function groupBuildsByChannel(builds: Build[]) {
  return builds.reduce<Record<string, Build[]>>((acc, build) => {
    acc[build.channel] ??= [];
    acc[build.channel].push(build);
    return acc;
  }, {});
}
