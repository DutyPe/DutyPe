import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminCheckAndCreateCodeClient } from "@/components/admin/admin-referral-tools-client";
import { AdminShell } from "@/components/admin-shell";
import { requireAdminSession } from "@/lib/firebase/admin-session";

export const metadata = {
  title: "Admin Check Code",
  description: "Check a user by phone number and create a referral code."
};

export default async function AdminCheckAndCreateCodePage() {
  await requireAdminSession();

  return (
    <AdminShell
      title="Check user and create referral code"
      description="React replacement for the old referral-code lookup and creation utility."
    >
      <AdminAuthGate>
        <AdminCheckAndCreateCodeClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
