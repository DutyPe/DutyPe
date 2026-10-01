"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminFeedbackPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Feedback" description="In-app feedback from workers and employers (stored in Azure Cosmos DB).">
        <AdminCollectionViewer
          apiPath="/api/admin/feedback"
          dataKey="items"
          label="Feedback"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
