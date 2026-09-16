"use client";

import { JobDetailsAccess } from "@/components/jobs/job-details-access";
import { SiteShell } from "@/components/site-shell";

export default function WorkerJobDetailPage({
  params
}: {
  params: { jobId: string };
}) {
  return (
    <SiteShell>
      <section className="directory-page-header"><h1>Job details</h1></section>
      <JobDetailsAccess jobId={params.jobId} />
    </SiteShell>
  );
}
