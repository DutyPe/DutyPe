import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `date,session_type,location,pincode,founder_hours,pamphlets_distributed,conversations,installs_same_day,profile_completes_7d,best_quote,top_objection,verdict,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Field Visits</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
