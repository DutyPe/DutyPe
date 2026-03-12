import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminJobsClient } from "@/components/admin/admin-jobs-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Jobs",
  description: "DutyPe jobs management — edit, delete, moderate."
};

export default async function AdminJobsPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Jobs">
      <AdminAuthGate>
        <AdminJobsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
