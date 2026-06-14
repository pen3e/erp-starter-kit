import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Lock, MoreHorizontal, Plus } from "lucide-react";
import { toast } from "sonner";
import { deleteRole, listRoles } from "@/lib/api/roles";
import { listPermissions } from "@/lib/api/permissions";
import { errorMessage } from "@/lib/api/errors";
import { Permission } from "@/lib/auth/permissions";
import { useAuth } from "@/lib/auth/auth-provider";
import type { RoleResponse } from "@/types/api";
import { PageHeader } from "@/components/common/page-header";
import { Can } from "@/components/auth/can";
import { ConfirmDialog } from "@/components/common/confirm-dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { RoleFormDialog } from "./role-form-dialog";

export function RolesPage() {
  const queryClient = useQueryClient();
  const { hasPermission } = useAuth();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<RoleResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<RoleResponse | null>(null);

  const rolesQuery = useQuery({
    queryKey: ["roles", "list"],
    queryFn: () => listRoles(0, 200),
  });

  const permissionsQuery = useQuery({
    queryKey: ["permissions", "all"],
    queryFn: listPermissions,
    enabled: hasPermission(Permission.PERMISSION_READ),
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ["roles"] });
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    try {
      await deleteRole(deleteTarget.id);
      toast.success("Role deleted");
      refresh();
    } catch (error) {
      toast.error(errorMessage(error, "Could not delete the role"));
    } finally {
      setDeleteTarget(null);
    }
  }

  const roles = rolesQuery.data?.content ?? [];
  const permissions = permissionsQuery.data ?? [];

  return (
    <div>
      <PageHeader
        title="Roles"
        description="Group permissions into roles and assign them to users."
        actions={
          <Can permission={Permission.ROLE_CREATE}>
            <Button
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              <Plus className="h-4 w-4" />
              New role
            </Button>
          </Can>
        }
      />

      <div className="rounded-lg border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Description</TableHead>
              <TableHead>Permissions</TableHead>
              <TableHead className="w-12" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {rolesQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={4} className="py-10 text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            ) : roles.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="py-10 text-center text-muted-foreground">
                  No roles yet.
                </TableCell>
              </TableRow>
            ) : (
              roles.map((role) => (
                <TableRow key={role.id}>
                  <TableCell className="font-medium">
                    <span className="flex items-center gap-2">
                      {role.name}
                      {role.systemRole ? (
                        <Badge variant="muted" className="gap-1">
                          <Lock className="h-3 w-3" /> system
                        </Badge>
                      ) : null}
                    </span>
                  </TableCell>
                  <TableCell className="text-muted-foreground">{role.description ?? "—"}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{role.permissions.length}</Badge>
                  </TableCell>
                  <TableCell>
                    <Can anyOf={[Permission.ROLE_UPDATE, Permission.ROLE_DELETE]}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                            <span className="sr-only">Open actions</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <Can permission={Permission.ROLE_UPDATE}>
                            <DropdownMenuItem
                              onSelect={() => {
                                setEditing(role);
                                setFormOpen(true);
                              }}
                            >
                              Edit
                            </DropdownMenuItem>
                          </Can>
                          <Can permission={Permission.ROLE_DELETE}>
                            <DropdownMenuItem
                              className="text-destructive focus:text-destructive"
                              disabled={role.systemRole}
                              onSelect={() => {
                                if (!role.systemRole) setDeleteTarget(role);
                              }}
                            >
                              Delete
                            </DropdownMenuItem>
                          </Can>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </Can>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <RoleFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        role={editing}
        permissions={permissions}
        onSaved={refresh}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null);
        }}
        title="Delete role"
        description={deleteTarget ? `Permanently delete the role “${deleteTarget.name}”?` : undefined}
        confirmLabel="Delete"
        destructive
        onConfirm={confirmDelete}
      />
    </div>
  );
}
