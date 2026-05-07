"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminWorkerProfilesPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Worker Profiles" description="Current worker_profiles collection fields.">
        <AdminCollectionViewer
          apiPath="/api/admin/worker-profiles"
          dataKey="profiles"
          label="Worker Profiles"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
