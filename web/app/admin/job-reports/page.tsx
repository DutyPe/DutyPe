"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminJobReportsPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Job Reports" description="All job_reports documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/job-reports"
          dataKey="items"
          label="Job Reports"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
