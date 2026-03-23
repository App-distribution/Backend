"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type {
  BuildFilters,
  BuildUpdatePayload,
  CreateProjectPayload,
  UpdateProfilePayload,
  UploadBuildPayload,
  UploadProgressState,
} from "@/lib/types";

export const queryKeys = {
  me: ["me"] as const,
  workspace: ["workspace"] as const,
  projects: ["projects"] as const,
  project: (projectId: string) => ["project", projectId] as const,
  projectBuilds: (projectId: string, filters: BuildFilters) => ["project-builds", projectId, filters] as const,
  recentBuilds: (limit: number) => ["recent-builds", limit] as const,
  build: (buildId: string) => ["build", buildId] as const,
  team: (workspaceId: string) => ["team", workspaceId] as const,
};

export function useMeQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: api.users.getMe,
    enabled,
  });
}

export function useWorkspaceQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.workspace,
    queryFn: api.workspace.getMe,
    enabled,
  });
}

export function useProjectsQuery(enabled = true) {
  return useQuery({
    queryKey: queryKeys.projects,
    queryFn: api.projects.list,
    enabled,
  });
}

export function useProjectQuery(projectId: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.project(projectId),
    queryFn: () => api.projects.get(projectId),
    enabled,
  });
}

export function useRecentBuildsQuery(limit = 24, enabled = true) {
  return useQuery({
    queryKey: queryKeys.recentBuilds(limit),
    queryFn: () => api.builds.listRecent(limit),
    enabled,
  });
}

export function useProjectBuildsQuery(projectId: string, filters: BuildFilters, enabled = true) {
  return useQuery({
    queryKey: queryKeys.projectBuilds(projectId, filters),
    queryFn: () => api.builds.listByProject(projectId, filters),
    enabled: enabled && Boolean(projectId),
  });
}

export function useTeamQuery(workspaceId: string | null | undefined, enabled = true) {
  return useQuery({
    queryKey: queryKeys.team(workspaceId ?? "missing"),
    queryFn: () => api.workspace.getUsers(workspaceId!),
    enabled: enabled && Boolean(workspaceId),
  });
}

export function useCreateProjectMutation(workspaceId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateProjectPayload) => api.projects.create(workspaceId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
    },
  });
}

export function useDeleteProjectMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (projectId: string) => api.projects.remove(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
    },
  });
}

export function useUpdateBuildMutation(projectId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ buildId, payload }: { buildId: string; payload: BuildUpdatePayload }) =>
      api.builds.update(buildId, payload),
    onSuccess: (build) => {
      if (projectId) {
        queryClient.invalidateQueries({ queryKey: ["project-builds", projectId] });
      }
      queryClient.invalidateQueries({ queryKey: ["build", build.id] });
      queryClient.invalidateQueries({ queryKey: ["recent-builds"] });
    },
  });
}

export function useDeleteBuildMutation(projectId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (buildId: string) => api.builds.remove(buildId),
    onSuccess: () => {
      if (projectId) {
        queryClient.invalidateQueries({ queryKey: ["project-builds", projectId] });
      }
      queryClient.invalidateQueries({ queryKey: ["recent-builds"] });
    },
  });
}

export function useProfileMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateProfilePayload) => api.users.updateMe(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me });
    },
  });
}

export function useUploadBuildMutation(onProgress?: (progress: UploadProgressState) => void) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UploadBuildPayload) => api.builds.upload(payload, onProgress),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects });
      queryClient.invalidateQueries({ queryKey: ["recent-builds"] });
      queryClient.invalidateQueries({ queryKey: ["project-builds"] });
    },
  });
}
