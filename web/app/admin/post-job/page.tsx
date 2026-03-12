import Link from "next/link";

import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Post Job",
  description: "Compatibility route for the retired static admin post-job page."
};

export default async function AdminPostJobPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Post job route migrated out of static admin HTML"
      description="The old admin panel had a standalone post-job HTML form. The React migration keeps a compatibility route here while the actual job composer lives in the shared product flow."
    >
      <AdminAuthGate>
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Compatibility route</span>
              <h2>Legacy admin post-job page status</h2>
            </div>
            <p>
              This route exists so old admin links stop breaking. The current React
              product already has a working job composer under the employer app flow.
            </p>
          </div>

          <div className="detail-grid">
            <article className="detail-panel">
              <span className="card-kicker">Current path</span>
              <h3>Use the shared job composer</h3>
              <p>
                The maintained job posting UI is the same shared form used in the
                React product app, not a separate static admin page.
              </p>
              <div className="button-row compact">
                <Link href="/app/employer/post-job" className="button">
                  Open job composer
                </Link>
                <Link href="/admin/jobs" className="button ghost">
                  Back to jobs admin
                </Link>
              </div>
            </article>

            <article className="detail-panel">
              <span className="card-kicker">Migration note</span>
              <h3>What changed from the old HTML page</h3>
              <ul className="detail-list">
                <li>
                  <strong>No separate static form</strong>
                  <span>The static HTML composer is retired in favor of shared React flows.</span>
                </li>
                <li>
                  <strong>Server-gated admin route</strong>
                  <span>Access now respects the same admin session policy as the rest of the React admin.</span>
                </li>
                <li>
                  <strong>Old links still supported</strong>
                  <span>Legacy `/admin/post-job.html` will redirect here after the Next app is deployed.</span>
                </li>
              </ul>
            </article>
          </div>
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
