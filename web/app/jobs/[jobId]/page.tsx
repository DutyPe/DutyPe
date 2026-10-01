import type { Metadata } from "next";

import { AppLaunchCard } from "@/components/public/app-launch-card";
import { SiteShell } from "@/components/site-shell";

type Props = {
  params: {
    jobId: string;
  };
};

/**
 * A shared job link. The website does not read jobs (no database cost): on a phone with the app,
 * the link opens the job in the app; otherwise this page offers the app.
 */
export const dynamic = "force-static";

export function generateMetadata(): Metadata {
  return {
    title: "Open this job in the DutyPe app",
    description: "See the pay, place and employer, and apply or call in one tap in the DutyPe app.",
    robots: { index: false, follow: true }
  };
}

export default function JobDetailPage({ params }: Props) {
  const jobId = /^[A-Za-z0-9_-]{6,64}$/.test(params.jobId) ? params.jobId : undefined;
  return (
    <SiteShell>
      <AppLaunchCard
        kind="job"
        entityId={jobId}
        headline="Open this job in the DutyPe app"
        description="Jobs are shown in the DutyPe app. If you have it, the job opens there; if not, install it free."
        bullets={["See pay, place and employer", "Apply or call in one tap", "Urgent jobs near you ring on your phone"]}
      />
    </SiteShell>
  );
}

export function generateStaticParams() {
  return [];
}
