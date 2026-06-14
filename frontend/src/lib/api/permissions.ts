import { api } from "@/lib/api/client";
import type { PermissionResponse } from "@/types/api";

export async function listPermissions(): Promise<PermissionResponse[]> {
  const { data } = await api.get<PermissionResponse[]>("/api/v1/permissions");
  return data;
}
