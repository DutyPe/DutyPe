import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `date,channel,format,audience,headline,link,impressions_7d,replies,shares,clicks_to_install,attributable_installs_30d,verdict,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Content Log</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
