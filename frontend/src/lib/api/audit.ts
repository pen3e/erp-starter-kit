import { api } from "@/lib/api/client";
import type { AuditLogResponse, PageResponse } from "@/types/api";

export async function listAuditLogs(page = 0, size = 20): Promise<PageResponse<AuditLogResponse>> {
  const { data } = await api.get<PageResponse<AuditLogResponse>>("/api/v1/audit-logs", {
    params: { page, size },
  });
  return data;
}
