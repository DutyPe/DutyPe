"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminReferralCodesClient } from "@/components/admin/admin-referral-codes-client";

export default function AdminReferralCodesPage() {
  return (
    <AdminAuthGate>
      <AdminShell
        title="Referral Codes"
        description="Directory of all user referral codes paired with mobile numbers, names, wallet balances, and performance metrics."
      >
        <AdminReferralCodesClient />
      </AdminShell>
    </AdminAuthGate>
  );
}
