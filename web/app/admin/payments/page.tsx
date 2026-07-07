import { AdminPaymentsClient } from "@/components/admin/admin-payments-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Subscription Payments",
  description: "DutyPe subscription payments and QR verification control panel."
};

export default function AdminPaymentsPage() {
  return (
    <AdminShell title="Subscription Payments" description="Approve employer transaction submissions, scan QR codes uploader, and manual job extensions.">
      <AdminAuthGate>
        <AdminPaymentsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
