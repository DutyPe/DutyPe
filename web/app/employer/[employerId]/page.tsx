import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";
import { getPublicEmployerRouteData } from "@/lib/public-site";

type Props = {
  params: {
    employerId: string;
  };
};

export const metadata = getBridgeMetadata({
  title: "Employer Profile - DutyPe",
  description: "Open employer profile route in DutyPe for trust info, active jobs, and worker communication.",
  keywords: ["employer profile", "company hiring", "job provider", "trusted employer"]
});

export default function EmployerProfilePage({ params }: Props) {
  const employer = getPublicEmployerRouteData(params.employerId);

  return (
    <AppRoutePage
      kind="employer"
      entityId={params.employerId}
      eyebrow="Employer page"
      icon="🏢"
      title="Open this employer in DutyPe."
      description={`${employer.summary} Trust tier, live openings, and worker communication continue inside the app.`}
      launchHeadline="Open this employer in DutyPe"
      launchDescription="Trust tier, live openings, response signals, and worker communication continue inside the app."
      bullets={employer.features}
    />
  );
}
