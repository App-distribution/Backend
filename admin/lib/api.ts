"use client";

import {
  clearStoredTokens,
  getStoredTokens,
  setStoredTokens,
} from "@/lib/session";
import type {
  AuthTokens,
  Build,
  BuildFilters,
  BuildUpdatePayload,
  CreateProjectPayload,
  DownloadUrlResponse,
  MessageResponse,
  Project,
  UpdateProfilePayload,
  UploadBuildPayload,
  UploadProgressState,
  User,
  Workspace,
} from "@/lib/types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080/api/v1";

export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

let refreshPromise: Promise<AuthTokens> | null = null;

type ApiAuthTokens = {
  access_token: string;
  refresh_token: string;
};

type ApiWorkspace = {
  id: string;
  name: string;
  slug: string;
  owner_id: string | null;
  created_at: string;
};

type ApiUser = {
  id: string;
  email: string;
  name: string;
  role: User["role"];
  workspace_id: string | null;
  created_at: number;
};

type ApiProject = {
  id: string;
  workspace_id: string;
  name: string;
  package_name: string;
  icon_url: string | null;
  created_at: number;
};

type ApiBuild = {
  id: string;
  project_id: string;
  version_name: string;
  version_code: number;
  build_number: string | null;
  flavor: string | null;
  build_type: string;
  environment: Build["environment"];
  channel: Build["channel"];
  branch: string | null;
  commit_hash: string | null;
  uploader_name: string;
  upload_date: number;
  changelog: string | null;
  file_size: number;
  checksum_sha256: string;
  min_sdk: number;
  target_sdk: number;
  cert_fingerprint: string | null;
  abis: string[];
  status: Build["status"];
  expiry_date: number | null;
  is_latest_in_channel: boolean;
};

type ApiDownloadUrlResponse = {
  url: string;
  expires_at: number;
};

type RequestInitWithMeta = RequestInit & {
  authenticated?: boolean;
  retryOnAuthFailure?: boolean;
};

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) return {} as T;
  return JSON.parse(text) as T;
}

function mapAuthTokens(tokens: ApiAuthTokens): AuthTokens {
  return {
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token,
  };
}

function mapWorkspace(workspace: ApiWorkspace): Workspace {
  return {
    id: workspace.id,
    name: workspace.name,
    slug: workspace.slug,
    ownerId: workspace.owner_id,
    createdAt: workspace.created_at,
  };
}

function mapUser(user: ApiUser): User {
  return {
    id: user.id,
    email: user.email,
    name: user.name,
    role: user.role,
    workspaceId: user.workspace_id,
    createdAt: user.created_at,
  };
}

function mapProject(project: ApiProject): Project {
  return {
    id: project.id,
    workspaceId: project.workspace_id,
    name: project.name,
    packageName: project.package_name,
    iconUrl: project.icon_url,
    createdAt: project.created_at,
  };
}

function mapBuild(build: ApiBuild): Build {
  return {
    id: build.id,
    projectId: build.project_id,
    versionName: build.version_name,
    versionCode: build.version_code,
    buildNumber: build.build_number,
    flavor: build.flavor,
    buildType: build.build_type,
    environment: build.environment,
    channel: build.channel,
    branch: build.branch,
    commitHash: build.commit_hash,
    uploaderName: build.uploader_name,
    uploadDate: build.upload_date,
    changelog: build.changelog,
    fileSize: build.file_size,
    checksumSha256: build.checksum_sha256,
    minSdk: build.min_sdk,
    targetSdk: build.target_sdk,
    certFingerprint: build.cert_fingerprint,
    abis: build.abis,
    status: build.status,
    expiryDate: build.expiry_date,
    isLatestInChannel: build.is_latest_in_channel,
  };
}

function mapDownloadUrlResponse(response: ApiDownloadUrlResponse): DownloadUrlResponse {
  return {
    url: response.url,
    expiresAt: response.expires_at,
  };
}

async function refreshTokens() {
  if (refreshPromise) return refreshPromise;
  const tokens = getStoredTokens();
  if (!tokens?.refreshToken) {
    throw new ApiError("Session expired", 401, "SESSION_EXPIRED");
  }

  refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refresh_token: tokens.refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        const payload = await parseJson<{ error?: string; message?: string }>(response).catch(() => null);
        throw new ApiError(payload?.message ?? "Failed to refresh session", response.status, payload?.error);
      }
      const nextTokens = mapAuthTokens(await parseJson<ApiAuthTokens>(response));
      setStoredTokens(nextTokens);
      return nextTokens;
    })
    .catch((error) => {
      clearStoredTokens();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

async function request<T>(path: string, init: RequestInitWithMeta = {}): Promise<T> {
  const { authenticated = true, retryOnAuthFailure = true, headers, ...rest } = init;
  const resolvedHeaders = new Headers(headers);

  if (!resolvedHeaders.has("Content-Type") && rest.body && !(rest.body instanceof FormData)) {
    resolvedHeaders.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const tokens = getStoredTokens();
    if (!tokens?.accessToken) {
      throw new ApiError("Unauthorized", 401, "UNAUTHORIZED");
    }
    resolvedHeaders.set("Authorization", `Bearer ${tokens.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: resolvedHeaders,
  });

  if (response.status === 401 && authenticated && retryOnAuthFailure) {
    const tokens = await refreshTokens();
    const retryHeaders = new Headers(resolvedHeaders);
    retryHeaders.set("Authorization", `Bearer ${tokens.accessToken}`);
    return request<T>(path, {
      ...rest,
      headers: retryHeaders,
      authenticated,
      retryOnAuthFailure: false,
    });
  }

  if (!response.ok) {
    const payload = await parseJson<{ error?: string; message?: string }>(response).catch(() => null);
    throw new ApiError(payload?.message ?? "Request failed", response.status, payload?.error);
  }

  if (response.status === 204) return undefined as T;
  return parseJson<T>(response);
}

export const api = {
  auth: {
    requestOtp(email: string) {
      return request<MessageResponse>("/auth/request-otp", {
        authenticated: false,
        method: "POST",
        body: JSON.stringify({ email }),
      });
    },
    verifyOtp(email: string, otp: string) {
      return request<ApiAuthTokens>("/auth/verify-otp", {
        authenticated: false,
        method: "POST",
        body: JSON.stringify({ email, otp }),
      }).then(mapAuthTokens);
    },
  },
  workspace: {
    getMe() {
      return request<ApiWorkspace>("/workspaces/me").then(mapWorkspace);
    },
    getUsers(workspaceId: string) {
      return request<ApiUser[]>(`/workspaces/${workspaceId}/users`).then((users) => users.map(mapUser));
    },
  },
  users: {
    getMe() {
      return request<ApiUser>("/users/me").then(mapUser);
    },
    updateMe(payload: UpdateProfilePayload) {
      return request<ApiUser>("/users/me", {
        method: "PATCH",
        body: JSON.stringify({
          name: payload.name,
          fcm_token: payload.fcmToken,
        }),
      }).then(mapUser);
    },
  },
  projects: {
    list() {
      return request<ApiProject[]>("/projects").then((projects) => projects.map(mapProject));
    },
    get(projectId: string) {
      return request<ApiProject>(`/projects/${projectId}`).then(mapProject);
    },
    create(workspaceId: string, payload: CreateProjectPayload) {
      return request<ApiProject>(`/workspaces/${workspaceId}/projects`, {
        method: "POST",
        body: JSON.stringify({
          name: payload.name,
          package_name: payload.packageName,
        }),
      }).then(mapProject);
    },
    remove(projectId: string) {
      return request<void>(`/projects/${projectId}`, {
        method: "DELETE",
      });
    },
  },
  builds: {
    listRecent(limit = 20) {
      return request<ApiBuild[]>(`/builds/recent?limit=${limit}`).then((builds) => builds.map(mapBuild));
    },
    listByProject(projectId: string, filters: BuildFilters = {}) {
      const params = new URLSearchParams();
      if (filters.channel) params.set("channel", filters.channel);
      if (filters.search) params.set("search", filters.search);
      params.set("page", String(filters.page ?? 0));
      params.set("limit", String(filters.limit ?? 20));
      const query = params.toString();
      return request<ApiBuild[]>(`/projects/${projectId}/builds${query ? `?${query}` : ""}`).then((builds) =>
        builds.map(mapBuild),
      );
    },
    get(buildId: string) {
      return request<ApiBuild>(`/builds/${buildId}`).then(mapBuild);
    },
    update(buildId: string, payload: BuildUpdatePayload) {
      return request<ApiBuild>(`/builds/${buildId}`, {
        method: "PATCH",
        body: JSON.stringify(payload),
      }).then(mapBuild);
    },
    remove(buildId: string) {
      return request<void>(`/builds/${buildId}`, {
        method: "DELETE",
      });
    },
    getDownloadUrl(buildId: string) {
      return request<ApiDownloadUrlResponse>(`/builds/${buildId}/download-url`).then(mapDownloadUrlResponse);
    },
    upload(payload: UploadBuildPayload, onProgress?: (progress: UploadProgressState) => void) {
      const tokens = getStoredTokens();
      if (!tokens?.accessToken) {
        return Promise.reject(new ApiError("Unauthorized", 401, "UNAUTHORIZED"));
      }

      return new Promise<Build>((resolve, reject) => {
        const formData = new FormData();
        formData.set("projectId", payload.projectId);
        formData.set("environment", payload.environment);
        formData.set("channel", payload.channel);
        formData.set("buildType", payload.buildType);
        if (payload.flavor) formData.set("flavor", payload.flavor);
        if (payload.branch) formData.set("branch", payload.branch);
        if (payload.commitHash) formData.set("commitHash", payload.commitHash);
        if (payload.changelog) formData.set("changelog", payload.changelog);
        formData.set("apk", payload.file);

        const xhr = new XMLHttpRequest();
        xhr.open("POST", `${API_BASE_URL}/builds/upload`);
        xhr.setRequestHeader("Authorization", `Bearer ${tokens.accessToken}`);

        xhr.upload.addEventListener("progress", (event) => {
          if (!event.lengthComputable || !onProgress) return;
          onProgress({
            loaded: event.loaded,
            total: event.total,
            progress: Math.round((event.loaded / event.total) * 100),
          });
        });

        xhr.onreadystatechange = () => {
          if (xhr.readyState !== XMLHttpRequest.DONE) return;
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve(mapBuild(JSON.parse(xhr.responseText) as ApiBuild));
            return;
          }
          try {
            const payload = JSON.parse(xhr.responseText) as { error?: string; message?: string };
            reject(new ApiError(payload.message ?? "Upload failed", xhr.status, payload.error));
          } catch {
            reject(new ApiError("Upload failed", xhr.status));
          }
        };

        xhr.onerror = () => reject(new ApiError("Network error during upload", 0, "NETWORK_ERROR"));
        xhr.send(formData);
      });
    },
  },
};
