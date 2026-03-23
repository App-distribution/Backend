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

type RequestInitWithMeta = RequestInit & {
  authenticated?: boolean;
  retryOnAuthFailure?: boolean;
};

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!text) return {} as T;
  return JSON.parse(text) as T;
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
    body: JSON.stringify({ refreshToken: tokens.refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        const payload = await parseJson<{ error?: string; message?: string }>(response).catch(() => null);
        throw new ApiError(payload?.message ?? "Failed to refresh session", response.status, payload?.error);
      }
      const nextTokens = await parseJson<AuthTokens>(response);
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
      return request<AuthTokens>("/auth/verify-otp", {
        authenticated: false,
        method: "POST",
        body: JSON.stringify({ email, otp }),
      });
    },
  },
  workspace: {
    getMe() {
      return request<Workspace>("/workspaces/me");
    },
    getUsers(workspaceId: string) {
      return request<User[]>(`/workspaces/${workspaceId}/users`);
    },
  },
  users: {
    getMe() {
      return request<User>("/users/me");
    },
    updateMe(payload: UpdateProfilePayload) {
      return request<User>("/users/me", {
        method: "PATCH",
        body: JSON.stringify(payload),
      });
    },
  },
  projects: {
    list() {
      return request<Project[]>("/projects");
    },
    get(projectId: string) {
      return request<Project>(`/projects/${projectId}`);
    },
    create(workspaceId: string, payload: CreateProjectPayload) {
      return request<Project>(`/workspaces/${workspaceId}/projects`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
    },
    remove(projectId: string) {
      return request<void>(`/projects/${projectId}`, {
        method: "DELETE",
      });
    },
  },
  builds: {
    listRecent(limit = 20) {
      return request<Build[]>(`/builds/recent?limit=${limit}`);
    },
    listByProject(projectId: string, filters: BuildFilters = {}) {
      const params = new URLSearchParams();
      if (filters.channel) params.set("channel", filters.channel);
      if (filters.search) params.set("search", filters.search);
      params.set("page", String(filters.page ?? 0));
      params.set("limit", String(filters.limit ?? 20));
      const query = params.toString();
      return request<Build[]>(`/projects/${projectId}/builds${query ? `?${query}` : ""}`);
    },
    get(buildId: string) {
      return request<Build>(`/builds/${buildId}`);
    },
    update(buildId: string, payload: BuildUpdatePayload) {
      return request<Build>(`/builds/${buildId}`, {
        method: "PATCH",
        body: JSON.stringify(payload),
      });
    },
    remove(buildId: string) {
      return request<void>(`/builds/${buildId}`, {
        method: "DELETE",
      });
    },
    getDownloadUrl(buildId: string) {
      return request<DownloadUrlResponse>(`/builds/${buildId}/download-url`);
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
            resolve(JSON.parse(xhr.responseText) as Build);
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
