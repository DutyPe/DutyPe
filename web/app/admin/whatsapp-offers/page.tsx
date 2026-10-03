import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminWhatsappOffersClient } from "@/components/admin/admin-whatsapp-offers-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "WhatsApp Offers",
  description: "Send an approved WhatsApp offer template to users who switched WhatsApp offers on."
};

export default function AdminWhatsappOffersPage() {
  return (
    <AdminShell
      title="WhatsApp Offers"
      description="Festival wishes and offers on WhatsApp — only to people who switched it on in Settings, at most once a week."
    >
      <AdminAuthGate>
        <AdminWhatsappOffersClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
