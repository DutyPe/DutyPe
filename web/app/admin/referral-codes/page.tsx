"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminReferralCodesPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Referral Codes" description="All referral_codes documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/referral-codes"
          dataKey="items"
          label="Referral Codes"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
