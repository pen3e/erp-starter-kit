import { useQuery } from "@tanstack/react-query";
import { listPermissions } from "@/lib/api/permissions";
import { PageHeader } from "@/components/common/page-header";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function PermissionsPage() {
  const permissionsQuery = useQuery({ queryKey: ["permissions", "all"], queryFn: listPermissions });
  const permissions = permissionsQuery.data ?? [];

  return (
    <div>
      <PageHeader
        title="Permissions"
        description="The fixed catalogue of permissions you can compose into roles (read-only)."
      />
      <div className="rounded-lg border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Permission</TableHead>
              <TableHead>Description</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {permissionsQuery.isLoading ? (
              <TableRow>
                <TableCell colSpan={2} className="py-10 text-center text-muted-foreground">
                  Loading…
                </TableCell>
              </TableRow>
            ) : (
              permissions.map((permission) => (
                <TableRow key={permission.id}>
                  <TableCell>
                    <Badge variant="outline" className="font-mono">
                      {permission.name}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {permission.description ?? "—"}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
