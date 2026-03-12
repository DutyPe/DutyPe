import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminJobsClient } from "@/components/admin/admin-jobs-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Jobs",
  description: "DutyPe jobs management backed by Firestore."
};

export default async function AdminJobsPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Jobs moderation and quality control"
      description="This page now reads the live jobs collection and replaces the static jobs admin page as the next migration target."
    >
      <AdminAuthGate>
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Jobs collection</span>
              <h2>Latest jobs from Firestore</h2>
            </div>
            <p>
              The React admin route now reads the same `jobs` collection as the legacy
              admin HTML page, with delete support built into the table.
            </p>
          </div>

          <AdminJobsClient />
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
