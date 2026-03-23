import { Badge } from "@/components/ui/badge";
import { formatBuildStatus, formatChannel, formatEnvironment, formatRole } from "@/lib/format";
import type { BuildEnvironment, BuildStatus, ReleaseChannel, UserRole } from "@/lib/types";

export function RoleBadge({ role }: { role: UserRole }) {
  const tone = {
    ADMIN: "danger",
    UPLOADER: "primary",
    TESTER: "success",
    VIEWER: "neutral",
  } as const;

  return <Badge tone={tone[role]}>{formatRole(role)}</Badge>;
}

export function BuildStatusBadge({ status }: { status: BuildStatus }) {
  const tone = {
    ACTIVE: "success",
    DEPRECATED: "warning",
    ARCHIVED: "neutral",
    MANDATORY: "danger",
  } as const;

  return <Badge tone={tone[status]}>{formatBuildStatus(status)}</Badge>;
}

export function ChannelBadge({ channel }: { channel: ReleaseChannel }) {
  const tone = {
    NIGHTLY: "neutral",
    ALPHA: "warning",
    BETA: "primary",
    RC: "danger",
    INTERNAL: "success",
    CUSTOM: "neutral",
  } as const;

  return <Badge tone={tone[channel]}>{formatChannel(channel)}</Badge>;
}

export function EnvironmentBadge({ environment }: { environment: BuildEnvironment }) {
  return <Badge tone="neutral">{formatEnvironment(environment)}</Badge>;
}
