import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminPostJobClient } from "@/components/admin/admin-post-job-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Post Job",
  description: "Post a new job from the admin console."
};

export default function AdminPostJobPage() {

  return (
    <AdminShell title="Post Job">
      <AdminAuthGate>
        <AdminPostJobClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
