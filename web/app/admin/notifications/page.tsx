import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminNotificationsClient } from "@/components/admin/admin-notifications-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Notifications",
  description: "DutyPe notification broadcasts and role-targeted alerts."
};

export default function AdminNotificationsPage() {
  return (
    <AdminShell title="Notifications">
      <AdminAuthGate>
        <AdminNotificationsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}