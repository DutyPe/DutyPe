import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminTestReferralClient } from "@/components/admin/admin-referral-tools-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Test Referral",
  description: "Inspect a referral code and its linked referrals."
};

export default async function AdminTestReferralPage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Inspect a referral code"
      description="React replacement for the old referral-code inspection utility."
    >
      <AdminAuthGate>
        <AdminTestReferralClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
