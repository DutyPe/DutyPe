import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminUsersClient } from "@/components/admin/admin-users-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Users",
  description: "DutyPe users management — roles, referrals, accounts."
};

export default async function AdminUsersPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Users">
      <AdminAuthGate>
        <AdminUsersClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
