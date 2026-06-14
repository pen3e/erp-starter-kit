import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { toast } from "sonner";
import { Boxes, Loader2 } from "lucide-react";
import { useAuth } from "@/lib/auth/auth-provider";
import { errorMessage } from "@/lib/api/errors";
import { tokenStore } from "@/lib/auth/token-store";
import { emailSchema } from "@/lib/validation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FieldError } from "@/components/common/field-error";

const schema = z.object({
  tenant: z
    .string()
    .min(1, "Tenant is required")
    .regex(/^[a-z0-9][a-z0-9_-]*$/, "Lower-case letters, digits, '-' and '_' only"),
  email: emailSchema,
  password: z.string().min(1, "Password is required"),
  rememberMe: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

interface LocationState {
  from?: { pathname?: string };
}

export function LoginPage() {
  const { status, signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const defaultTenant =
    tokenStore.getTenant() ?? import.meta.env.VITE_DEFAULT_TENANT ?? "demo";

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { tenant: defaultTenant, email: "", password: "", rememberMe: false },
  });

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(values: FormValues) {
    try {
      await signIn(
        { email: values.email, password: values.password, rememberMe: values.rememberMe },
        values.tenant,
      );
      const target = (location.state as LocationState | null)?.from?.pathname ?? "/";
      navigate(target, { replace: true });
    } catch (error) {
      // Backend returns a generic message to prevent user enumeration; surface it as-is.
      toast.error(errorMessage(error, "Invalid credentials"));
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-3 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Boxes className="h-6 w-6" />
          </div>
          <div>
            <CardTitle className="text-2xl">ERP Starter Kit</CardTitle>
            <CardDescription>Sign in to your tenant workspace</CardDescription>
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div className="space-y-2">
              <Label htmlFor="tenant">Tenant</Label>
              <Input
                id="tenant"
                autoCapitalize="none"
                spellCheck={false}
                aria-invalid={Boolean(errors.tenant)}
                {...register("tenant")}
              />
              <FieldError message={errors.tenant?.message} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="username"
                aria-invalid={Boolean(errors.email)}
                {...register("email")}
              />
              <FieldError message={errors.email?.message} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.password)}
                {...register("password")}
              />
              <FieldError message={errors.password?.message} />
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-input accent-primary"
                {...register("rememberMe")}
              />
              Remember me on this device
            </label>
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Signing in…
                </>
              ) : (
                "Sign in"
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
