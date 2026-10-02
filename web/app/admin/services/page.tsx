import { AdminServicesClient } from "@/components/admin/admin-services-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin DutyPe Services",
  description: "Home services: partner approvals, credit top-ups, bookings and prices."
};

export default function AdminServicesPage() {
  return (
    <AdminShell title="DutyPe Services" description="Approve partners, verify credit top-ups, watch bookings and set prices.">
      <AdminAuthGate>
        <AdminServicesClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
