import type { MetadataRoute } from "next";

import { SITE_URL, getKnownLegacySlugs } from "@/lib/public-site";

export const dynamic = "force-static";

export default function sitemap(): MetadataRoute.Sitemap {
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

  // Job pages are not listed: jobs live in the app (the website reads no job data).
  return staticEntries;
}
