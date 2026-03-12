import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { ReferralToolsNav } from "@/components/admin/admin-referral-tools-client";
import { AdminReferralsClient } from "@/components/admin/admin-referrals-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Referrals",
  description: "DutyPe referrals and withdrawals management backed by Firestore."
};

export default async function AdminReferralsPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Referrals, rewards, and withdrawals"
      description="This route replaces the legacy referrals page and links the old referral utility tools from the same React workspace."
    >
      <AdminAuthGate>
        <ReferralToolsNav />
        <AdminReferralsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
