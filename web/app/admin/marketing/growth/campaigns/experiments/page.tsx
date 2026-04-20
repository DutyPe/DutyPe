import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `exp_id,name,start_date,end_date,channel,hypothesis,success_metric,kill_metric,actual_result,decision,next_action,total_cost_inr,founder_hours,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Experiments</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
