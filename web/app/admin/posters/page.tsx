import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
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
      description="Print-ready posters for live jobs, worker acquisition, and employer acquisition."
    >
      <AdminAuthGate>
        <AdminPostersClient initialJobId={searchParams?.jobId} />
      </AdminAuthGate>
    </AdminShell>
  );
}