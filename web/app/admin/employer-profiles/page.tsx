"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminEmployerProfilesPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Employer Profiles" description="All employer_profiles documents with every field.">
        <AdminCollectionViewer
          apiPath="/api/admin/employer-profiles"
          dataKey="profiles"
          label="Employer Profiles"
        />
      </AdminShell>
    </AdminAuthGate>
  );
}
