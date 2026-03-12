import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminPostJobClient } from "@/components/admin/admin-post-job-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Post Job",
  description: "Post a new job from the admin console."
};

export default async function AdminPostJobPage() {
  await requireAdminSession();

  return (
    <AdminShell title="Post Job">
      <AdminAuthGate>
        <AdminPostJobClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
