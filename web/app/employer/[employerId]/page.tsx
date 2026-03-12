import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";
import { getPublicEmployerRouteData } from "@/lib/public-site";

type Props = {
  params: {
    employerId: string;
  };
};

export const metadata = bridgeMetadata;

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
