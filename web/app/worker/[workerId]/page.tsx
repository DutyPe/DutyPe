import { AppRoutePage } from "@/components/public/app-route-page";
import { bridgeMetadata } from "@/lib/bridge-metadata";
import { getPublicWorkerRouteData } from "@/lib/public-site";

type Props = {
  params: {
    workerId: string;
  };
};

export const metadata = bridgeMetadata;

export default function WorkerProfilePage({ params }: Props) {
  const worker = getPublicWorkerRouteData(params.workerId);

  return (
    <AppRoutePage
      kind="worker"
      entityId={params.workerId}
      eyebrow="Worker profile"
      icon="👷"
      title="View worker profile in DutyPe."
      description={`${worker.summary} Open the app to see the live profile, ratings, and hiring actions.`}
      launchHeadline="Open this worker profile in DutyPe"
      launchDescription="Full worker details, ratings, hiring actions, and contact flow continue inside the app."
      bullets={worker.features}
    />
  );
}
