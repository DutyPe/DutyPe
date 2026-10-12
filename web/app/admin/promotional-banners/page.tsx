import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminBannersManager } from "@/components/admin/admin-banners-manager";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Promotional Banners",
  description: "Manage promotional banner carousels for DutyPe Home Services, Worker Home, and Employer Home."
};

export default function AdminPromotionalBannersPage() {
  return (
    <AdminShell
      title="Promotional Banners"
      description="Design high-converting promotional banners, customize gradients, and auto-convert images to lightweight WebP (20–40 KB) across Services, Worker Home & Employer Home."
    >
      <AdminAuthGate>
        <AdminBannersManager />
      </AdminAuthGate>
    </AdminShell>
  );
}
