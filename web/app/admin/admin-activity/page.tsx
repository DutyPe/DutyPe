"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminActivityPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Admin Activity" description="Every admin write: who, what and when (stored in Azure Cosmos DB).">
        <AdminCollectionViewer
          apiPath="/api/admin/admin-activity"
          dataKey="items"
          label="Admin Activity"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
