import { AdminApplicationsClient } from "@/components/admin/admin-applications-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Applications",
  description: "DutyPe applications operations backed by Firestore."
};

export default async function AdminApplicationsPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Applications operations and response discipline"
      description="This route now reads the live job_applications collection and is ready for SLA and worker-protection overlays."
    >
      <AdminAuthGate>
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Applications queue</span>
              <h2>Latest application activity</h2>
            </div>
            <p>
              The React route now reads `job_applications` directly from Firestore and
              formats status and dates without the old static page script.
            </p>
          </div>

          <AdminApplicationsClient />
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
