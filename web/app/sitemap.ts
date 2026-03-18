import type { MetadataRoute } from "next";

import { SITE_URL, getKnownLegacySlugs } from "@/lib/public-site";

const lastModified = new Date();

export default function sitemap(): MetadataRoute.Sitemap {
  const canonicalRoutes = [
    "",
    "/jobs",
    "/refer",
    "/worker",
    ...getKnownLegacySlugs().map((slug) => `/${slug}`)
  ];

  const routes = [...new Set(canonicalRoutes)];

  return routes.map((route) => ({
    url: `${SITE_URL}${route}`,
    lastModified,
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
}
