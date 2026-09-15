import Link from "next/link";

import type { Metadata } from "next";
import { LiveJobsSection } from "@/components/jobs/live-jobs-section";
import { SiteIcon } from "@/components/site-icon";
import { SiteShell } from "@/components/site-shell";
import { coreSeoKeywords, getJobDiscovery, SITE_URL } from "@/lib/public-site";
import { parseJobSearch, validJobId } from "@/lib/jobs/public-listings";

export const revalidate = 60;

type SearchParams = { q?: string | string[]; city?: string | string[]; area?: string | string[]; category?: string | string[]; cursor?: string | string[] };

const first = (value: string | string[] | undefined, maximum: number) => (Array.isArray(value) ? value[0] : value)?.slice(0, maximum) ?? "";

export function generateMetadata({ searchParams = {} }: { searchParams?: SearchParams }): Metadata {
  const city = getJobDiscovery("", first(searchParams.city, 80)).city;
  const filtered = Boolean(searchParams.q || searchParams.area || searchParams.category || searchParams.cursor);
  return {
    title: city ? `Live Jobs in ${city}` : "Live Local Jobs",
    description: `Browse current delivery, driving, home support and other local job openings${city ? ` in ${city}` : " by city and area"}. Sign in for full details.`,
    keywords: [...coreSeoKeywords, "live jobs", ...(city ? [`jobs in ${city}`] : [])],
    alternates: { canonical: `${SITE_URL}${city && !filtered ? `/jobs-in-${city.toLowerCase()}` : "/jobs"}` },
    robots: { index: !filtered, follow: true }
  };
}

export default function JobsHubPage({ searchParams = {} }: { searchParams?: SearchParams }) {
  const search = parseJobSearch({ q: first(searchParams.q, 120), city: first(searchParams.city, 80), area: first(searchParams.area, 100), category: first(searchParams.category, 40) });
  const cursor = first(searchParams.cursor, 1500);

  return (
    <SiteShell>
      <div className="jobs-directory-page">
        <nav className="resource-breadcrumb" aria-label="Breadcrumb">
          <Link href="/">Home</Link><SiteIcon name="arrow-right" /><span aria-current="page">Live jobs</span>
        </nav>
        <LiveJobsSection search={search} heading={search.city ? `Live jobs in ${search.city}` : "Live jobs"} showFilters cursor={validJobId(cursor) ? cursor : null} />
        <nav className="button-row compact" aria-label="Browse job guides">
          <Link href="/#categories-heading" className="text-link">Explore job categories<SiteIcon name="arrow-right" /></Link>
          <Link href="/#cities-heading" className="text-link">Browse city guides<SiteIcon name="map-pin" /></Link>
        </nav>
      </div>
    </SiteShell>
  );
}
