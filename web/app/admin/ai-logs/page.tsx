"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminAiLogsPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="DutyPe AI Logs" description="What employers ask DutyPe AI and what it replied (stored in Azure Cosmos DB).">
        <AdminCollectionViewer
          apiPath="/api/admin/ai-logs"
          dataKey="items"
          label="DutyPe AI Logs"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
