import { NavLink } from "react-router-dom";
import { KeyRound, LayoutDashboard, ScrollText, ShieldCheck, Users, Boxes } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Permission } from "@/lib/auth/permissions";
import { useAuth } from "@/lib/auth/auth-provider";
import { cn } from "@/lib/utils";

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
  anyOf?: string[];
}

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/users", label: "Users", icon: Users, anyOf: [Permission.USER_READ] },
  { to: "/roles", label: "Roles", icon: ShieldCheck, anyOf: [Permission.ROLE_READ] },
  { to: "/permissions", label: "Permissions", icon: KeyRound, anyOf: [Permission.PERMISSION_READ] },
  { to: "/audit", label: "Audit log", icon: ScrollText, anyOf: [Permission.AUDIT_READ] },
];

export function Sidebar() {
  const { hasAnyPermission } = useAuth();
  const visibleItems = NAV_ITEMS.filter((item) => !item.anyOf || hasAnyPermission(item.anyOf));

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r bg-background md:flex">
      <div className="flex h-16 items-center gap-2 border-b px-6">
        <Boxes className="h-6 w-6 text-primary" />
        <span className="text-lg font-semibold tracking-tight">ERP Starter</span>
      </div>
      <nav className="flex-1 space-y-1 p-3">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
              )
            }
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t p-4 text-xs text-muted-foreground">
        <p>ERP Starter Kit</p>
        <p>Secure multi-tenant foundation</p>
      </div>
    </aside>
  );
}
