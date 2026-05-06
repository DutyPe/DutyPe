import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { DeleteUserByPhoneClient } from "@/components/admin/delete-user-by-phone-client";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Delete User by Phone",
  description: "Emergency cleanup - delete all user data by phone number when they join with wrong role."
};

export default function DeleteUserByPhonePage() {
  return (
    <AdminShell title="Delete User by Phone">
      <AdminAuthGate>
        <DeleteUserByPhoneClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
