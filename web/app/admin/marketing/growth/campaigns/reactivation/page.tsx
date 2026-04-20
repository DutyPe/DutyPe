import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `date,cohort,trigger,channel,recipients,delivered,opened,clicked,activated,reward_paid_inr,verdict,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Reactivation</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
