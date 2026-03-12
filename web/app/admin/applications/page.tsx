import { AdminApplicationsClient } from "@/components/admin/admin-applications-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Applications",
  description: "DutyPe applications management."
};

export default async function AdminApplicationsPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Applications">
      <AdminAuthGate>
        <AdminApplicationsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
