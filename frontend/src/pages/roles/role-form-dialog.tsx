import { useEffect, useMemo } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { createRole, updateRole } from "@/lib/api/roles";
import { asApiError, errorMessage } from "@/lib/api/errors";
import type { PermissionResponse, RoleResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FieldError } from "@/components/common/field-error";
import { CheckboxList } from "@/components/common/checkbox-list";

function buildSchema(isEdit: boolean) {
  return z
    .object({
      name: z.string().max(80),
      description: z.string().max(255),
      permissionIds: z.array(z.string()),
    })
    .superRefine((val, ctx) => {
      if (!isEdit) {
        if (val.name.trim().length === 0) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["name"], message: "Required" });
        } else if (!/^[A-Za-z0-9 _-]+$/.test(val.name)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["name"],
            message: "Letters, digits, spaces, '-' and '_' only",
          });
        }
      }
    });
}

type FormValues = z.infer<ReturnType<typeof buildSchema>>;

interface RoleFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  role: RoleResponse | null;
  permissions: PermissionResponse[];
  onSaved: () => void;
}

export function RoleFormDialog({
  open,
  onOpenChange,
  role,
  permissions,
  onSaved,
}: RoleFormDialogProps) {
  const isEdit = Boolean(role);
  const isSystem = Boolean(role?.systemRole);

  const defaults = useMemo<FormValues>(
    () => ({
      name: role?.name ?? "",
      description: role?.description ?? "",
      permissionIds: role
        ? permissions.filter((p) => role.permissions.includes(p.name)).map((p) => p.id)
        : [],
    }),
    [role, permissions],
  );

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(buildSchema(isEdit)),
    defaultValues: defaults,
  });

  useEffect(() => {
    if (open) reset(defaults);
  }, [open, defaults, reset]);

  const permissionOptions = permissions.map((p) => ({
    value: p.id,
    label: p.name,
    description: p.description,
  }));

  async function onSubmit(values: FormValues) {
    try {
      if (isEdit && role) {
        await updateRole(role.id, {
          description: values.description || undefined,
          // The permission set of a system role is immutable on the backend.
          permissionIds: isSystem ? undefined : values.permissionIds,
        });
        toast.success("Role updated");
      } else {
        await createRole({
          name: values.name,
          description: values.description || undefined,
          permissionIds: values.permissionIds,
        });
        toast.success("Role created");
      }
      onSaved();
      onOpenChange(false);
    } catch (error) {
      const apiError = asApiError(error);
      apiError?.fieldErrors?.forEach((fe) => {
        if (["name", "description"].includes(fe.field)) {
          setError(fe.field as keyof FormValues, { message: fe.message });
        }
      });
      toast.error(errorMessage(error, "Could not save the role"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit role" : "New role"}</DialogTitle>
          <DialogDescription>
            {isSystem
              ? "This is a system role. You can edit its description, but its permissions are locked."
              : "Group permissions into a role you can assign to users."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-2">
            <Label htmlFor="name">Name</Label>
            <Input
              id="name"
              disabled={isEdit}
              aria-invalid={Boolean(errors.name)}
              {...register("name")}
            />
            {isEdit ? (
              <p className="text-xs text-muted-foreground">Role name cannot be changed.</p>
            ) : null}
            <FieldError message={errors.name?.message} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Input id="description" aria-invalid={Boolean(errors.description)} {...register("description")} />
            <FieldError message={errors.description?.message} />
          </div>

          <div className="space-y-2">
            <Label>Permissions</Label>
            <Controller
              control={control}
              name="permissionIds"
              render={({ field }) => (
                <CheckboxList
                  options={permissionOptions}
                  selected={field.value}
                  onChange={field.onChange}
                  disabled={isSystem}
                  columns={2}
                  emptyLabel="No permissions available (requires PERMISSION_READ)."
                />
              )}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Saving…" : isEdit ? "Save changes" : "Create role"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
