import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { listAuditLogs } from "@/lib/api/audit";
import { formatDateTime } from "@/lib/utils";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const PAGE_SIZE = 20;

export function AuditPage() {
  const [page, setPage] = useState(0);

  const auditQuery = useQuery({
    queryKey: ["audit", page],
    queryFn: () => listAuditLogs(page, PAGE_SIZE),
    placeholderData: keepPreviousData,
  });

  const data = auditQuery.data;
  const logs = data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Audit log"
        description="Security-relevant events recorded for this tenant."
      />

      <div className="rounded-lg border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Time</TableHead>
              <TableHead>Action</TableHead>
              <TableHead>Outcome</TableHead>
              <TableHead>Actor</TableHead>
              <TableHead>Target</TableHead>
              <TableHead>IP</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {auditQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            ) : logs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                  No audit events.
                </TableCell>
              </TableRow>
            ) : (
              logs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell className="whitespace-nowrap text-muted-foreground">
                    {formatDateTime(log.createdAt)}
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="font-mono">
                      {log.action}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={log.outcome === "SUCCESS" ? "success" : "destructive"}>
                      {log.outcome}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{log.actorEmail ?? "—"}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {log.targetType ? `${log.targetType}${log.targetId ? ` #${log.targetId.slice(0, 8)}` : ""}` : "—"}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{log.ipAddress ?? "—"}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {data ? (
        <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
          <span>
            {data.totalElements} event{data.totalElements === 1 ? "" : "s"} · page {data.page + 1} of{" "}
            {Math.max(data.totalPages, 1)}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={data.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
