"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminReferralStatsClient } from "@/components/admin/admin-referral-stats-client";

export default function AdminReferralStatsPage() {
  return (
    <AdminAuthGate>
      <AdminShell
        title="Referral Stats & Wallets"
        description="Monitor user wallets, earnings, balances in Rupees (₹), and manage referral payouts."
      >
        <AdminReferralStatsClient />
      </AdminShell>
    </AdminAuthGate>
  );
}
