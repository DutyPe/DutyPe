import { AdminAnnouncementsClient } from "@/components/admin/admin-announcements-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Announcements",
  description: "DutyPe announcements management backed by Firestore."
};

export default async function AdminAnnouncementsPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Announcements, campaigns, and app-open messaging"
      description="This page now reads and writes the announcements collection directly from the React admin app."
    >
      <AdminAuthGate>
        <AdminAnnouncementsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
