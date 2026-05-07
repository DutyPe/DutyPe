"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCollectionViewer } from "@/components/admin/admin-collection-viewer";

export default function AdminPhoneRolesPage() {
  return (
    <AdminAuthGate>
      <AdminShell title="Phone Roles" description="All phoneRoles documents with every Firestore field.">
        <AdminCollectionViewer
          apiPath="/api/admin/phone-roles"
          dataKey="items"
          label="Phone Roles"
          hiddenFields={["updatedAt"]}
        />
      </AdminShell>
    </AdminAuthGate>
  );
}