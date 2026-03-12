import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminDashboardClient } from "@/components/admin/admin-dashboard-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";
import { adminModules } from "@/lib/site-data";

export const metadata = {
  title: "Admin",
  description: "DutyPe admin workspace scaffolded in Next.js."
};

export default async function AdminOverviewPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Move the admin panel out of scattered static HTML."
      description="The current repo already contains browser-import Firebase admin pages. This React workspace is the structured replacement target for operations, moderation, and announcements."
    >
      <AdminAuthGate>
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Live dashboard</span>
              <h2>Current Firestore counts</h2>
            </div>
            <p>
              This is the first real data bridge from the Next admin app into the same
              Firebase project used by the legacy admin HTML pages.
            </p>
          </div>

          <AdminDashboardClient />
        </section>

        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Migration scope</span>
              <h2>Core admin modules</h2>
            </div>
            <p>
              This pass now covers the main legacy admin views, while keeping the data
              bridge and auth policy explicit instead of hidden inside static-page scripts.
            </p>
          </div>

          <div className="admin-grid">
            {adminModules.map((module) => (
              <article key={module.title} className="admin-panel">
                <span className="card-kicker">Module</span>
                <h3>{module.title}</h3>
                <p>{module.summary}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Why React here</span>
              <h2>What this fixes over the current admin pages</h2>
            </div>
            <p>
              The existing admin surface works, but it is fragmented. A shared React
              layer gives you reusable layouts, real route hierarchy, typed data models,
              and a single place to mirror the Firestore admin policy.
            </p>
          </div>

          <div className="callout">
            Current remaining gap: admin access is now protected by a server session,
            but the policy still keys off email allowlists. The next upgrade is custom
            claims or a stronger RBAC model.
          </div>
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
