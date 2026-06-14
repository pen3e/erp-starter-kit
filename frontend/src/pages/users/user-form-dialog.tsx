import { useEffect, useMemo } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { createUser, updateUser } from "@/lib/api/users";
import { asApiError, errorMessage } from "@/lib/api/errors";
import { strongPassword } from "@/lib/validation";
import type { RoleResponse, UserResponse } from "@/types/api";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FieldError } from "@/components/common/field-error";
import { CheckboxList } from "@/components/common/checkbox-list";

const STATUSES = ["ACTIVE", "INACTIVE", "PENDING"] as const;

function buildSchema(isEdit: boolean) {
  return z
    .object({
      firstName: z.string().min(1, "Required").max(100),
      lastName: z.string().min(1, "Required").max(100),
      email: z.string().max(255),
      phone: z.string().max(30).regex(/^[+0-9 ().-]*$/, "Invalid characters"),
      password: z.string(),
      status: z.enum(STATUSES),
      roleIds: z.array(z.string()),
    })
    .superRefine((val, ctx) => {
      if (!isEdit) {
        if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(val.email)) {
          ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["email"], message: "Enter a valid email" });
        }
        const pwd = strongPassword.safeParse(val.password);
        if (!pwd.success) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            path: ["password"],
            message: pwd.error.issues[0]?.message ?? "Weak password",
          });
        }
      }
    });
}

type FormValues = z.infer<ReturnType<typeof buildSchema>>;

interface UserFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: UserResponse | null;
  roles: RoleResponse[];
  onSaved: () => void;
}

export function UserFormDialog({ open, onOpenChange, user, roles, onSaved }: UserFormDialogProps) {
  const isEdit = Boolean(user);

  const defaults = useMemo<FormValues>(
    () => ({
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      email: user?.email ?? "",
      phone: user?.phone ?? "",
      password: "",
      status: user?.status ?? "ACTIVE",
      roleIds: user ? roles.filter((r) => user.roles.includes(r.name)).map((r) => r.id) : [],
    }),
    [user, roles],
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

  const roleOptions = roles.map((r) => ({ value: r.id, label: r.name, description: r.description }));

  async function onSubmit(values: FormValues) {
    try {
      if (isEdit && user) {
        await updateUser(user.id, {
          firstName: values.firstName,
          lastName: values.lastName,
          phone: values.phone || undefined,
          status: values.status,
          roleIds: values.roleIds,
        });
        toast.success("User updated");
      } else {
        await createUser({
          firstName: values.firstName,
          lastName: values.lastName,
          email: values.email,
          phone: values.phone || undefined,
          password: values.password,
          roleIds: values.roleIds,
        });
        toast.success("User created");
      }
      onSaved();
      onOpenChange(false);
    } catch (error) {
      const apiError = asApiError(error);
      apiError?.fieldErrors?.forEach((fe) => {
        if (["firstName", "lastName", "email", "phone", "password"].includes(fe.field)) {
          setError(fe.field as keyof FormValues, { message: fe.message });
        }
      });
      toast.error(errorMessage(error, "Could not save the user"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit user" : "New user"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Update profile, status and role assignment."
              : "Provision a new user in the current tenant."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="firstName">First name</Label>
              <Input id="firstName" aria-invalid={Boolean(errors.firstName)} {...register("firstName")} />
              <FieldError message={errors.firstName?.message} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastName">Last name</Label>
              <Input id="lastName" aria-invalid={Boolean(errors.lastName)} {...register("lastName")} />
              <FieldError message={errors.lastName?.message} />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              disabled={isEdit}
              aria-invalid={Boolean(errors.email)}
              {...register("email")}
            />
            {isEdit ? (
              <p className="text-xs text-muted-foreground">Email is the login identity and cannot be changed.</p>
            ) : null}
            <FieldError message={errors.email?.message} />
          </div>

          <div className="space-y-2">
            <Label htmlFor="phone">Phone (optional)</Label>
            <Input id="phone" aria-invalid={Boolean(errors.phone)} {...register("phone")} />
            <FieldError message={errors.phone?.message} />
          </div>

          {!isEdit ? (
            <div className="space-y-2">
              <Label htmlFor="password">Temporary password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                aria-invalid={Boolean(errors.password)}
                {...register("password")}
              />
              <FieldError message={errors.password?.message} />
            </div>
          ) : null}

          {isEdit ? (
            <div className="space-y-2">
              <Label>Status</Label>
              <Controller
                control={control}
                name="status"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {STATUSES.map((s) => (
                        <SelectItem key={s} value={s}>
                          {s}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          ) : null}

          <div className="space-y-2">
            <Label>Roles</Label>
            <Controller
              control={control}
              name="roleIds"
              render={({ field }) => (
                <CheckboxList
                  options={roleOptions}
                  selected={field.value}
                  onChange={field.onChange}
                  emptyLabel="No roles available (requires ROLE_READ)."
                />
              )}
            />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Saving…" : isEdit ? "Save changes" : "Create user"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
