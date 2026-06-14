import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { KeyRound, ScrollText, ShieldCheck, Users } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useAuth } from "@/lib/auth/auth-provider";
import { Permission } from "@/lib/auth/permissions";
import { listUsers } from "@/lib/api/users";
import { listRoles } from "@/lib/api/roles";
import { listAuditLogs } from "@/lib/api/audit";
import { PageHeader } from "@/components/common/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface StatCardProps {
  to: string;
  label: string;
  icon: LucideIcon;
  value: number | undefined;
  loading: boolean;
}

function StatCard({ to, label, icon: Icon, value, loading }: StatCardProps) {
  return (
    <Link to={to}>
      <Card className="transition-colors hover:border-primary/50">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium text-muted-foreground">{label}</CardTitle>
          <Icon className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-3xl font-bold">{loading ? "…" : (value ?? "—")}</div>
        </CardContent>
      </Card>
    </Link>
  );
}

export function DashboardPage() {
  const { user, hasPermission } = useAuth();

  const canUsers = hasPermission(Permission.USER_READ);
  const canRoles = hasPermission(Permission.ROLE_READ);
  const canAudit = hasPermission(Permission.AUDIT_READ);

  const usersQuery = useQuery({
    queryKey: ["dashboard", "users"],
    queryFn: () => listUsers({ page: 0, size: 1 }),
    enabled: canUsers,
  });
  const rolesQuery = useQuery({
    queryKey: ["dashboard", "roles"],
    queryFn: () => listRoles(0, 1),
    enabled: canRoles,
  });
  const auditQuery = useQuery({
    queryKey: ["dashboard", "audit"],
    queryFn: () => listAuditLogs(0, 1),
    enabled: canAudit,
  });

  return (
    <div>
      <PageHeader
        title={`Welcome back`}
        description={user ? `Signed in as ${user.email} · tenant “${user.tenant}”` : undefined}
      />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {canUsers ? (
          <StatCard
            to="/users"
            label="Users"
            icon={Users}
            value={usersQuery.data?.totalElements}
            loading={usersQuery.isLoading}
          />
        ) : null}
        {canRoles ? (
          <StatCard
            to="/roles"
            label="Roles"
            icon={ShieldCheck}
            value={rolesQuery.data?.totalElements}
            loading={rolesQuery.isLoading}
          />
        ) : null}
        {canAudit ? (
          <StatCard
            to="/audit"
            label="Audit events"
            icon={ScrollText}
            value={auditQuery.data?.totalElements}
            loading={auditQuery.isLoading}
          />
        ) : null}
        {hasPermission(Permission.PERMISSION_READ) ? (
          <StatCard to="/permissions" label="Permissions" icon={KeyRound} value={undefined} loading={false} />
        ) : null}
      </div>
    </div>
  );
}
