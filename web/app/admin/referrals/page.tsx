import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { ReferralToolsNav } from "@/components/admin/admin-referral-tools-client";
import { AdminReferralsClient } from "@/components/admin/admin-referrals-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Referrals",
  description: "DutyPe referrals and withdrawals management."
};

export default function AdminReferralsPage() {

  return (
    <AdminShell title="Referrals">
      <AdminAuthGate>
        <ReferralToolsNav />
        <AdminReferralsClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
