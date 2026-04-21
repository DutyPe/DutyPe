"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminRatingsPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Ratings" description="All ratings documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/ratings"
          dataKey="items"
          label="Ratings"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
