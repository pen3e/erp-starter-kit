import { Boxes } from "lucide-react";
import { UserMenu } from "./user-menu";

export function Topbar() {
  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b bg-background px-4 md:px-6">
      <div className="flex items-center gap-2 md:hidden">
        <Boxes className="h-5 w-5 text-primary" />
        <span className="font-semibold">ERP Starter</span>
      </div>
      <div className="ml-auto">
        <UserMenu />
      </div>
    </header>
  );
}
