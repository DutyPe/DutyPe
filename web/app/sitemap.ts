import type { MetadataRoute } from "next";

import { SITE_URL, getKnownLegacySlugs } from "@/lib/public-site";
import { getDiscoverableJobs } from "@/lib/jobs/server";

export const revalidate = 300;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const canonicalRoutes = [
    "",
    "/jobs",
    ...getKnownLegacySlugs().map((slug) => `/${slug}`)
  ];

  const routes = [...new Set(canonicalRoutes)];

  const staticEntries: MetadataRoute.Sitemap = routes.map((route) => ({
    url: `${SITE_URL}${route}`,
    changeFrequency:
      route === ""
        ? "daily"
        : route === "/jobs" || route.startsWith("/jobs-in-")
          ? "daily"
          : route === "/privacy" || route === "/terms" || route === "/refund"
            ? "yearly"
            : "weekly",
    priority:
      route === ""
        ? 1
        : route === "/jobs" || route.startsWith("/jobs-in-") || route.endsWith("-jobs")
          ? 0.9
          : 0.6
  }));

  const jobs = await getDiscoverableJobs();
  return [...staticEntries, ...jobs.map((job) => ({
    url: `${SITE_URL}/jobs/${encodeURIComponent(job.id)}`,
    ...(job.updatedAt ? { lastModified: new Date(job.updatedAt) } : {}),
    changeFrequency: "daily" as const,
    priority: 0.8
  }))];
}
