"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminSavedJobsPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Saved Jobs" description="All saved_jobs documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/saved-jobs"
          dataKey="items"
          label="Saved Jobs"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
