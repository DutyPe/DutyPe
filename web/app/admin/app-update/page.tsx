import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminAppUpdateClient } from "@/components/admin/admin-app-update-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin App Update",
  description: "Control the Android home-screen app update prompt."
};

export default function AdminAppUpdatePage() {
  return (
    <AdminShell title="App Update" description="Control the Android update prompt shown on worker and employer home screens.">
      <AdminAuthGate>
        <AdminAppUpdateClient />
      </AdminAuthGate>
    </AdminShell>
  );
}