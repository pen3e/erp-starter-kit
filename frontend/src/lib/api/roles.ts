import { api } from "@/lib/api/client";
import type {
  CreateRoleRequest,
  PageResponse,
  RoleResponse,
  UpdateRoleRequest,
} from "@/types/api";

export async function listRoles(page = 0, size = 100): Promise<PageResponse<RoleResponse>> {
  const { data } = await api.get<PageResponse<RoleResponse>>("/api/v1/roles", {
    params: { page, size, sort: "name,asc" },
  });
  return data;
}

export async function getRole(id: string): Promise<RoleResponse> {
  const { data } = await api.get<RoleResponse>(`/api/v1/roles/${id}`);
  return data;
}

export async function createRole(body: CreateRoleRequest): Promise<RoleResponse> {
  const { data } = await api.post<RoleResponse>("/api/v1/roles", body);
  return data;
}

export async function updateRole(id: string, body: UpdateRoleRequest): Promise<RoleResponse> {
  const { data } = await api.put<RoleResponse>(`/api/v1/roles/${id}`, body);
  return data;
}

export async function deleteRole(id: string): Promise<void> {
  await api.delete(`/api/v1/roles/${id}`);
}
