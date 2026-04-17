import { AdminAnnouncementsClient } from "@/components/admin/admin-announcements-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Announcements",
  description: "DutyPe announcements management."
};

export default function AdminAnnouncementsPage() {

  return (
    <AdminShell title="Announcements">
      <AdminAuthGate>
        <AdminAnnouncementsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
