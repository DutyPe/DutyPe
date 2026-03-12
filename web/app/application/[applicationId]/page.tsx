import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";

type Props = {
  params: {
    applicationId: string;
  };
};

export const metadata = bridgeMetadata;

export default function ApplicationRoutePage({ params }: Props) {
  return (
    <AppRoutePage
      kind="application"
      entityId={params.applicationId}
      eyebrow="Application route"
      icon="📝"
      title="Open your application in DutyPe."
      description="Application status, employer actions, and worker updates continue inside the live app."
      bullets={[
        "Track status changes",
        "View employer response",
        "Continue the flow in the app"
      ]}
    />
  );
}
