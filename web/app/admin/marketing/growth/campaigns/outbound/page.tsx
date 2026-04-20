import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `date,channel,sequence_id,prospect_name,prospect_org,prospect_role,city,pincode,opener_variant,follow_up_count,reply,verdict,founder_hours,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Outbound</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
