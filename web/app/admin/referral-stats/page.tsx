"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminReferralStatsPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Referral Stats" description="All referral_stats documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/referral-stats"
          dataKey="items"
          label="Referral Stats"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
