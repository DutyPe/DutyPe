import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminUsersClient } from "@/components/admin/admin-users-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Users",
  description: "DutyPe users management backed by Firestore."
};

export default async function AdminUsersPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Users, roles, and referral identity"
      description="This route replaces the static users page and exposes the live users collection inside the shared admin shell."
    >
      <AdminAuthGate>
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Users collection</span>
              <h2>Latest user accounts from Firestore</h2>
            </div>
            <p>
              The React route now covers role visibility, referral codes, and the same
              user collection the old admin HTML page depended on.
            </p>
          </div>

          <AdminUsersClient />
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
