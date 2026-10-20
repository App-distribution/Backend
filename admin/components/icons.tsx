import type { SVGProps } from "react";

export type IconName =
  | "arrow-down-up"
  | "arrow-right"
  | "bell"
  | "boxes"
  | "check"
  | "chevron-down"
  | "chevrons-up-down"
  | "cloud-upload"
  | "download"
  | "ellipsis"
  | "external-link"
  | "file-archive"
  | "layout-dashboard"
  | "log-out"
  | "mail"
  | "menu"
  | "package"
  | "package-check"
  | "search"
  | "settings"
  | "shield-check"
  | "sun-moon"
  | "user"
  | "users"
  | "x";

const paths: Record<IconName, React.ReactNode> = {
  "arrow-down-up": <><path d="m3 16 4 4 4-4" /><path d="M7 20V4" /><path d="m21 8-4-4-4 4" /><path d="M17 4v16" /></>,
  "arrow-right": <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
  boxes: <><path d="m12 2 4 2.3v4.6L12 11.2 8 8.9V4.3L12 2Z" /><path d="m5 12 4 2.3v4.6L5 21.2l-4-2.3v-4.6L5 12Z" /><path d="m19 12 4 2.3v4.6L19 21.2l-4-2.3v-4.6l4-2.3Z" /></>,
  check: <path d="m5 12 4 4L19 6" />,
  "chevron-down": <path d="m6 9 6 6 6-6" />,
  "chevrons-up-down": <><path d="m7 15 5 5 5-5" /><path d="m7 9 5-5 5 5" /></>,
  "cloud-upload": <><path d="M12 13v8" /><path d="m8 17 4-4 4 4" /><path d="M5 17a4 4 0 0 1-.7-7.9A6 6 0 0 1 16 7a4.5 4.5 0 0 1 2.5 8.2" /></>,
  download: <><path d="M12 3v12" /><path d="m7 10 5 5 5-5" /><path d="M5 21h14" /></>,
  ellipsis: <><circle cx="5" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1" fill="currentColor" stroke="none" /></>,
  "external-link": <><path d="M15 3h6v6" /><path d="m10 14 11-11" /><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></>,
  "file-archive": <><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z" /><path d="M14 2v6h6" /><path d="M10 12h2" /><path d="M10 16h2" /><path d="M10 20h2" /></>,
  "layout-dashboard": <><rect width="7" height="9" x="3" y="3" rx="1" /><rect width="7" height="5" x="14" y="3" rx="1" /><rect width="7" height="9" x="14" y="12" rx="1" /><rect width="7" height="5" x="3" y="16" rx="1" /></>,
  "log-out": <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5" /><path d="M21 12H9" /></>,
  mail: <><rect width="18" height="14" x="3" y="5" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  menu: <><path d="M4 7h16" /><path d="M4 12h16" /><path d="M4 17h16" /></>,
  package: <><path d="m12 2 9 5-9 5-9-5 9-5Z" /><path d="m3 7 9 5 9-5" /><path d="M3 7v10l9 5 9-5V7" /><path d="M12 12v10" /></>,
  "package-check": <><path d="m12 2 9 5-9 5-9-5 9-5Z" /><path d="M3 7v10l9 5 9-5V7" /><path d="M12 12v10" /><path d="m7.5 9.5 2 2 4-4" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 0 0 9 4.6 1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1Z" /></>,
  "shield-check": <><path d="M20 13c0 5-3.5 7.5-8 9-4.5-1.5-8-4-8-9V5l8-3 8 3v8Z" /><path d="m9 12 2 2 4-4" /></>,
  "sun-moon": <><circle cx="8" cy="8" r="3" /><path d="M8 2v1" /><path d="M8 13v1" /><path d="m3.8 3.8.7.7" /><path d="m11.5 11.5.7.7" /><path d="M2 8h1" /><path d="M13 8h1" /><path d="M16 4a6 6 0 1 0 4 10 7 7 0 0 1-4-10Z" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.9" /><path d="M16 3.1a4 4 0 0 1 0 7.8" /></>,
  x: <><path d="m6 6 12 12" /><path d="m18 6-12 12" /></>,
};

export function Icon({ name, ...props }: { name: IconName } & SVGProps<SVGSVGElement>) {
  return (
    <svg
      aria-hidden="true"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
      {...props}
    >
      {paths[name]}
    </svg>
  );
}
