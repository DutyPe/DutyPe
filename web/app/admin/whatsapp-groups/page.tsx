"use client";

import { AdminShell } from "@/components/admin-shell";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminWhatsappGroupsClient } from "@/components/admin/admin-whatsapp-groups-client";

export default function AdminWhatsappGroupsPage() {
  return (
    <AdminAuthGate>
      <AdminShell
        title="WhatsApp Broadcasts & Contact Export"
        description="Filter and export worker and employer contacts to CSV, vCard (.vcf), or WhatsApp broadcast lists."
      >
        <AdminWhatsappGroupsClient />
      </AdminShell>
    </AdminAuthGate>
  );
}
