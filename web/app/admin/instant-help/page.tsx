import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminInstantHelpClient } from "@/components/admin/admin-instant-help-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Instant Help",
  description: "DutyPe urgent request metrics, response speed, and fill-rate operations."
};

export default function AdminInstantHelpPage() {
  return (
    <AdminShell title="Instant Help" description="Urgent request health, response speed, and fill-rate metrics.">
      <AdminAuthGate>
        <AdminInstantHelpClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
