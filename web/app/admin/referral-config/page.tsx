import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { ReferralToolsNav } from "@/components/admin/admin-referral-tools-client";
import { AdminReferralConfigClient } from "@/components/admin/admin-referral-config-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Referral Config",
  description: "Tune referral reward amounts and withdrawal limits."
};

export default function AdminReferralConfigPage() {
  return (
    <AdminShell title="Referral Config">
      <AdminAuthGate>
        <ReferralToolsNav />
        <AdminReferralConfigClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
