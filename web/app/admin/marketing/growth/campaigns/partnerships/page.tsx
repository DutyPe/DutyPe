import { CsvTable } from "@/components/marketing-shell";

export default function Page() {
  const csv = `date_first_contact,partner_type,partner_name,society_or_org,city,pincode,units_or_members,owner,status,signed_date,first_install_date,installs_attributed,jobs_posted,jobs_filled,monthly_check_in,notes
`;

  return (
    <div className="marketing-page-single">
      <h1>Partnerships</h1>
      <CsvTable csv={csv} />
    </div>
  );
}
