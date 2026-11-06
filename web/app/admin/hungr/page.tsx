import { AdminHungrClient } from "@/components/admin/admin-hungr-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Hungr Pre-Orders & Waitlist Intelligence",
  description: "Khammam & district food pre-order survey demand, locality heatmap & user leads."
};

export default function AdminHungrPage() {
  return (
    <AdminShell
      title="Hungr Breakfast & Snacks Intelligence"
      description="Real-time pre-order demand, neighborhood cluster rankings, and verified waitlist contacts."
    >
      <AdminAuthGate>
        <AdminHungrClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
