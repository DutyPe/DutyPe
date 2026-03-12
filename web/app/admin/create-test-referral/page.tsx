import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCreateTestReferralClient } from "@/components/admin/admin-referral-tools-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Create Test Referral",
  description: "Create a completed test referral and credit the referrer."
};

export default async function AdminCreateTestReferralPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Create a test referral"
      description="React replacement for the old manual referral-credit test utility."
    >
      <AdminAuthGate>
        <AdminCreateTestReferralClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
