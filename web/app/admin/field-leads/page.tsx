import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminFieldLeadsClient } from "@/components/admin/admin-field-leads-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Field registrations",
  description: "Umbrella-desk registrations by field agents: leads, joins and agent payouts."
};

export default function AdminFieldLeadsPage() {
  return (
    <AdminShell title="Field registrations" description="Agents register people at help desks; follow up and pay agents per person who joins.">
      <AdminAuthGate>
        <AdminFieldLeadsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
