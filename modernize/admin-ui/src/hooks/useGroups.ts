import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { groupsApi } from "@/api/endpoints";
import type { GroupRequest, GroupResponse } from "@/api/types";

export const groupKeys = {
  all: ["groups"] as const,
  list: () => [...groupKeys.all, "list"] as const,
};

export function useGroups() {
  return useQuery({
    queryKey: groupKeys.list(),
    queryFn: () => groupsApi.list(),
  });
}

export function useCreateGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: GroupRequest) => groupsApi.save(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: groupKeys.list() });
    },
  });
}

export function useBindUserGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, groupId }: { userId: number; groupId: number }) =>
      groupsApi.bindUser(userId, groupId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: groupKeys.list() });
    },
  });
}

export type { GroupResponse };
