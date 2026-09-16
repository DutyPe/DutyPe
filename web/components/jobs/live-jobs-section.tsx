import { getLiveJobPage } from "@/lib/jobs/server";
import type { JobSearch } from "@/lib/jobs/public-listings";
import { SITE_URL } from "@/lib/public-site";

import { LiveJobResults } from "./live-job-results";

export async function LiveJobsSection({ search, heading, showFilters = false, cursor = null }: { search: JobSearch; heading?: string; showFilters?: boolean; cursor?: string | null }) {
  const initial = await getLiveJobPage(search, cursor);
  const schema = initial.jobs.length ? {
    "@context": "https://schema.org", "@type": "ItemList",
    itemListElement: initial.jobs.map((job, index) => ({ "@type": "ListItem", position: index + 1, name: job.title, url: `${SITE_URL}/jobs/${encodeURIComponent(job.id)}` }))
  } : null;
  return <>
    {schema ? <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(schema).replace(/</g, "\\u003c") }} /> : null}
    <LiveJobResults initial={initial} search={search} heading={heading} showFilters={showFilters} />
  </>;
}