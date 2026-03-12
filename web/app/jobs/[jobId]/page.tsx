import { AppRoutePage } from "@/components/public/app-route-page";
import { getBridgeMetadata } from "@/lib/bridge-metadata";
import { getPublicJobRouteData } from "@/lib/public-site";

type Props = {
  params: {
    jobId: string;
  };
};

export const metadata = getBridgeMetadata({
  title: "Job Details - DutyPe",
  description: "Open job details in DutyPe to view salary, location, trust cues, and apply instantly.",
  keywords: ["job details", "apply for job", "job vacancy", "nearby job openings"]
});

export default function JobDetailPage({ params }: Props) {
  const job = getPublicJobRouteData(params.jobId);

  return (
    <AppRoutePage
      kind="job"
      entityId={params.jobId}
      eyebrow="Job route"
      icon="💼"
      title="Find your next job in DutyPe."
      description={`${job.company}. Open the app to view the live listing, trust cues, and apply action.`}
      launchHeadline="Open this job in DutyPe"
      launchDescription="The live listing, trust cues, apply action, and chat flow continue inside the app."
      bullets={job.features}
    />
  );
}
