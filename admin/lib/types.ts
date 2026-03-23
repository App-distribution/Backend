export type UserRole = "ADMIN" | "UPLOADER" | "TESTER" | "VIEWER";
export type BuildEnvironment = "DEV" | "QA" | "STAGING" | "PROD_LIKE";
export type ReleaseChannel = "NIGHTLY" | "ALPHA" | "BETA" | "RC" | "INTERNAL" | "CUSTOM";
export type BuildStatus = "ACTIVE" | "DEPRECATED" | "ARCHIVED" | "MANDATORY";

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface MessageResponse {
  message: string;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  ownerId: string | null;
  createdAt: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  workspaceId: string | null;
  createdAt: number;
}

export interface Project {
  id: string;
  workspaceId: string;
  name: string;
  packageName: string;
  iconUrl: string | null;
  createdAt: number;
}

export interface Build {
  id: string;
  projectId: string;
  versionName: string;
  versionCode: number;
  buildNumber: string | null;
  flavor: string | null;
  buildType: string;
  environment: BuildEnvironment;
  channel: ReleaseChannel;
  branch: string | null;
  commitHash: string | null;
  uploaderName: string;
  uploadDate: number;
  changelog: string | null;
  fileSize: number;
  checksumSha256: string;
  minSdk: number;
  targetSdk: number;
  certFingerprint: string | null;
  abis: string[];
  status: BuildStatus;
  expiryDate: number | null;
  isLatestInChannel: boolean;
}

export interface DownloadUrlResponse {
  url: string;
  expiresAt: number;
}

export interface BuildFilters {
  channel?: ReleaseChannel | "";
  search?: string;
  page?: number;
  limit?: number;
}

export interface BuildUpdatePayload {
  changelog?: string | null;
  status?: BuildStatus | null;
}

export interface CreateProjectPayload {
  name: string;
  packageName: string;
}

export interface UpdateProfilePayload {
  name?: string | null;
  fcmToken?: string | null;
}

export interface UploadBuildPayload {
  projectId: string;
  environment: BuildEnvironment;
  channel: ReleaseChannel;
  buildType: string;
  flavor?: string;
  branch?: string;
  commitHash?: string;
  changelog?: string;
  file: File;
}

export interface UploadProgressState {
  loaded: number;
  total: number;
  progress: number;
}
