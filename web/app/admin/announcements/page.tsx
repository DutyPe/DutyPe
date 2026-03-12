import { AdminAnnouncementsClient } from "@/components/admin/admin-announcements-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Announcements",
  description: "DutyPe announcements management."
};

export default async function AdminAnnouncementsPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Announcements">
      <AdminAuthGate>
        <AdminAnnouncementsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
