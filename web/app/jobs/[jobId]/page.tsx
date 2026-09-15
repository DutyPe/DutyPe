import type { Metadata } from "next";
import Link from "next/link";
import { notFound, permanentRedirect } from "next/navigation";
import { cache } from "react";

import { JobDetailsAccess } from "@/components/jobs/job-details-access";
import { SiteShell } from "@/components/site-shell";
import { getPublicJob } from "@/lib/jobs/server";
import { validJobId } from "@/lib/jobs/public-listings";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import { SITE_URL, jobDirectoryCities } from "@/lib/public-site";

type Props = {
  params: {
    jobId: string;
  };
};

export const revalidate = 60;
const loadJob = cache(getPublicJob);

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  if (!validJobId(params.jobId)) return { robots: { index: false, follow: true } };
  const result = await loadJob(params.jobId);
  if (!result.job) return { title: "Job unavailable", robots: { index: false, follow: true } };
  const job = result.job;
  const title = `${job.title}${job.city ? ` in ${job.city}` : ""}${job.companyName ? ` | ${job.companyName}` : ""}`;
  const description = `${job.title}${job.companyName ? ` at ${job.companyName}` : ""}${job.city ? ` in ${job.city}` : ""}. ${formatCurrencyRange(job.payAmount, job.payType)}. Sign in for the full job description and work details.`;
  const url = `${SITE_URL}/jobs/${encodeURIComponent(job.id)}`;
  return { title, description, alternates: { canonical: url }, robots: { index: true, follow: true }, openGraph: { title, description, url, type: "website" } };
}

export default async function JobDetailPage({ params }: Props) {
  if (!validJobId(params.jobId)) notFound();
  const result = await loadJob(params.jobId);
  if (result.status === "ready" && !result.job) notFound();
  if (result.job && result.job.id !== params.jobId) permanentRedirect(`/jobs/${encodeURIComponent(result.job.id)}`);
  const job = result.job;
  const schema = job ? {
    "@context": "https://schema.org", "@type": "WebPage", name: job.title,
    url: `${SITE_URL}/jobs/${encodeURIComponent(job.id)}`,
    description: `${job.title}${job.companyName ? ` at ${job.companyName}` : ""}${job.city ? ` in ${job.city}` : ""}`,
    breadcrumb: { "@type": "BreadcrumbList", itemListElement: [
      { "@type": "ListItem", position: 1, name: "Jobs", item: `${SITE_URL}/jobs` },
      { "@type": "ListItem", position: 2, name: job.title, item: `${SITE_URL}/jobs/${encodeURIComponent(job.id)}` }
    ] }
  } : null;

  return (
    <SiteShell>
      {schema ? <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(schema).replace(/</g, "\\u003c") }} /> : null}
      <nav className="resource-breadcrumb" aria-label="Breadcrumb"><Link href="/jobs">Live jobs</Link><span aria-current="page">{job?.title || "Job details"}</span></nav>
      <header className="directory-page-header">
        <h1>{job?.title || "Job details"}</h1>
        {job ? <>
          <p>{job.companyName}</p>
          <p>{[job.area, job.city].filter(Boolean).join(", ") || "Area not provided"} · {formatCurrencyRange(job.payAmount, job.payType)}</p>
          {job.city && jobDirectoryCities.includes(job.city) ? <Link className="text-link" href={`/jobs-in-${job.city.toLowerCase()}`}>More jobs in {job.city}</Link> : null}
        </> : <p role="alert">The job summary is temporarily unavailable. Please try again later.</p>}
      </header>
      <JobDetailsAccess jobId={params.jobId} />
    </SiteShell>
  );
}
