import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import { WhatsAppContactsClient } from "@/components/admin/whatsapp-contacts-client";

export const metadata = {
  title: "WhatsApp Broadcast Contacts — DutyPe Admin",
  description: "Export and copy Worker & Employer phone numbers for WhatsApp group broadcasts."
};

export default function WhatsAppGroupsPage() {
  return (
    <AdminAuthGate>
      <AdminShell
        title="WhatsApp Broadcast & Community Groups"
        description="Filter, copy, and export Worker & Employer phone numbers for WhatsApp emergency broadcast groups."
      >
        <WhatsAppContactsClient />
      </AdminShell>
    </AdminAuthGate>
  );
}
