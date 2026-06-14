import { createBrowserRouter } from "react-router-dom";
import { AppLayout } from "@/components/layout/app-layout";
import { ProtectedRoute } from "@/components/auth/protected-route";
import { Permission } from "@/lib/auth/permissions";
import { LoginPage } from "@/pages/login-page";
import { DashboardPage } from "@/pages/dashboard-page";
import { UsersPage } from "@/pages/users/users-page";
import { RolesPage } from "@/pages/roles/roles-page";
import { PermissionsPage } from "@/pages/permissions-page";
import { AuditPage } from "@/pages/audit-page";
import { ForbiddenPage } from "@/pages/forbidden-page";
import { NotFoundPage } from "@/pages/not-found-page";

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    // Everything below requires authentication; the AppLayout renders the shell + <Outlet/>.
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      {
        path: "users",
        element: (
          <ProtectedRoute anyOf={[Permission.USER_READ]}>
            <UsersPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "roles",
        element: (
          <ProtectedRoute anyOf={[Permission.ROLE_READ]}>
            <RolesPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "permissions",
        element: (
          <ProtectedRoute anyOf={[Permission.PERMISSION_READ]}>
            <PermissionsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "audit",
        element: (
          <ProtectedRoute anyOf={[Permission.AUDIT_READ]}>
            <AuditPage />
          </ProtectedRoute>
        ),
      },
      { path: "403", element: <ForbiddenPage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
