import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { microservicesApi } from "@/api/endpoints";
import type { MicroserviceRequest, MicroserviceResponse } from "@/api/types";

export const microserviceKeys = {
  all: ["microservices"] as const,
  list: () => [...microserviceKeys.all, "list"] as const,
};

export function useMicroservices() {
  return useQuery({
    queryKey: microserviceKeys.list(),
    queryFn: () => microservicesApi.list(),
  });
}

export function useCreateMicroservice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: MicroserviceRequest) => microservicesApi.create(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: microserviceKeys.list() });
    },
  });
}

export function useUpdateMicroservice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: MicroserviceRequest) => microservicesApi.update(body),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: microserviceKeys.list() });
    },
  });
}

export function useDeleteMicroservice() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => microservicesApi.delete(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: microserviceKeys.list() });
    },
  });
}

export type { MicroserviceResponse };
