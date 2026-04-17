import { AdminApplicationsClient } from "@/components/admin/admin-applications-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Applications",
  description: "DutyPe applications management."
};

export default function AdminApplicationsPage() {

  return (
    <AdminShell title="Applications">
      <AdminAuthGate>
        <AdminApplicationsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
