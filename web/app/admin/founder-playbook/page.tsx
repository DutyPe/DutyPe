import { AdminShell } from "@/components/admin-shell";
import { FounderPlaybookClient } from "@/components/admin/founder-playbook-client";

export const metadata = {
  title: "Founder Playbook — DutyPe",
  description: "Business fundamentals, Khammam launch strategy, demand-supply economics, and missing feature tracker."
};

export default function FounderPlaybookPage() {
  return (
    <AdminShell
      title="Founder Playbook"
      description="Your personal business compass — GTM strategy, Khammam launch checklist, unit economics, and what to build next."
    >
      <FounderPlaybookClient />
    </AdminShell>
  );
}
