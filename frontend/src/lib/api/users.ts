import { api } from "@/lib/api/client";
import type {
  CreateUserRequest,
  PageResponse,
  UpdateUserRequest,
  UserResponse,
  UserStatus,
} from "@/types/api";

export interface UserQuery {
  search?: string;
  status?: UserStatus | "";
  page?: number;
  size?: number;
}

export async function listUsers(query: UserQuery): Promise<PageResponse<UserResponse>> {
  const { data } = await api.get<PageResponse<UserResponse>>("/api/v1/users", {
    params: {
      search: query.search || undefined,
      status: query.status || undefined,
      page: query.page ?? 0,
      size: query.size ?? 20,
    },
  });
  return data;
}

export async function getUser(id: string): Promise<UserResponse> {
  const { data } = await api.get<UserResponse>(`/api/v1/users/${id}`);
  return data;
}

export async function createUser(body: CreateUserRequest): Promise<UserResponse> {
  const { data } = await api.post<UserResponse>("/api/v1/users", body);
  return data;
}

export async function updateUser(id: string, body: UpdateUserRequest): Promise<UserResponse> {
  const { data } = await api.put<UserResponse>(`/api/v1/users/${id}`, body);
  return data;
}

export async function deleteUser(id: string): Promise<void> {
  await api.delete(`/api/v1/users/${id}`);
}
