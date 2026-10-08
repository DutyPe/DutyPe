import { AdminExpensesClient } from "@/components/admin/admin-expenses-client";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

export const metadata = {
  title: "Admin Marketing Expenses & Revenue ROI",
  description: "Track money spent on posters, ads, influencers vs revenue earned and users acquired.",
};

export default function AdminExpensesPage() {
  return (
    <AdminShell
      title="Marketing Expenses & ROI"
      description="Track spend on posters, digital ads, influencers, and field desks alongside revenue and user growth."
    >
      <AdminAuthGate>
        <AdminExpensesClient />
      </AdminAuthGate>
    </AdminShell>
  );
}
