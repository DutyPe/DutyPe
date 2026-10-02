import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminMarketingKit } from "@/components/admin/admin-marketing-kit";
import { AdminPostersClient } from "@/components/admin/admin-posters-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Posters",
  description: "Printable DutyPe posters for posted jobs, workers, and employers."
};

export default function AdminPostersPage({
  searchParams
}: {
  searchParams?: { jobId?: string };
}) {
  return (
    <AdminShell
      title="Posters"
      description="Print-ready posters, Instagram posts & stories and WhatsApp templates for customers, partners, employers and workers."
    >
      <AdminAuthGate>
        <AdminPostersClient initialJobId={searchParams?.jobId} />
        <AdminMarketingKit />
      </AdminAuthGate>
    </AdminShell>
  );
}