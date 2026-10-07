import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminServiceCatalogClient } from "@/components/admin/admin-service-catalog-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Service 3D Icons & Catalog",
  description: "Upload 3D isometric clay service & category renders, monitor mobile payload (KB), and manage service pricing and listings."
};

export default function AdminServiceCatalogPage() {
  return (
    <AdminShell
      title="Service Icons & Catalog"
      description="Upload 3D clay isometric renders, inspect real-time image sizes in KB, and manage live service catalog pricing."
    >
      <AdminAuthGate>
        <AdminServiceCatalogClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
