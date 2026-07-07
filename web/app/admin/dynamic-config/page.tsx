import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminDynamicConfigClient } from "@/components/admin/admin-dynamic-config-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Dynamic Config",
  description: "Configure launcher icons, in-app banners, and feature flags."
};

export default function AdminDynamicConfigPage() {
  return (
    <AdminShell title="Dynamic Config" description="Manage launcher icons, feature flags, and settings dynamically without an app store update.">
      <AdminAuthGate>
        <AdminDynamicConfigClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
