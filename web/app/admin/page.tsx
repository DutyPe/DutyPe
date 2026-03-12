import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminDashboardClient } from "@/components/admin/admin-dashboard-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Dashboard",
  description: "DutyPe admin dashboard — overview of platform activity."
};

export default async function AdminDashboardPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Dashboard">
      <AdminAuthGate>
        <AdminDashboardClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
