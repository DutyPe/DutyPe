"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminWorkerProfilesClient } from "@/components/admin/admin-worker-profiles-client";

export default function AdminWorkerProfilesPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Worker Profiles" description="Manage, filter, contact Khammam workers, and promote to Service Partners.">
        <AdminWorkerProfilesClient />
      </AdminShell>
    </AdminAuthGate>
  );
}
