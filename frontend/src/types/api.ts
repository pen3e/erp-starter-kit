// Mirror of the backend DTOs (com.company.erp.*.dto). Keep in sync with the API contract.

export type UserStatus = "ACTIVE" | "INACTIVE" | "PENDING";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginPayload {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface UserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  status: UserStatus;
  lastLoginAt?: string | null;
  createdAt: string;
  roles: string[];
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  password: string;
  roleIds: string[];
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  status?: UserStatus;
  roleIds?: string[];
}

export interface RoleResponse {
  id: string;
  name: string;
  description?: string | null;
  systemRole: boolean;
  permissions: string[];
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  permissionIds: string[];
}

export interface UpdateRoleRequest {
  description?: string;
  permissionIds?: string[];
}

export interface PermissionResponse {
  id: string;
  name: string;
  description?: string | null;
}

export interface AuditLogResponse {
  id: string;
  action: string;
  outcome: string;
  actorId?: string | null;
  actorEmail?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  targetType?: string | null;
  targetId?: string | null;
  message?: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  traceId: string;
  fieldErrors?: ApiFieldError[];
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
