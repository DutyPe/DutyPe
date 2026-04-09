import type { MetadataRoute } from "next";

import { SITE_URL, getKnownLegacySlugs } from "@/lib/public-site";

const lastModified = new Date();

export default function sitemap(): MetadataRoute.Sitemap {
  const canonicalRoutes = [
    "",
    "/jobs",
    "/jobs-near-me",
    "/refer",
    "/worker",
    "/contact",
    "/faq",
    "/safety",
    "/privacy",
    "/terms",
    "/refund",
    "/accountdeletion",
    ...getKnownLegacySlugs().map((slug) => `/${slug}`)
  ];

  const routes = [...new Set(canonicalRoutes)];

  return routes.map((route) => ({
    url: `${SITE_URL}${route}`,
    lastModified,
    changeFrequency:
      route === ""
        ? "daily"
        : route === "/jobs" || route === "/jobs-near-me"
          ? "daily"
          : route.startsWith("/jobs-in-") || route.endsWith("-jobs")
            ? "daily"
            : route === "/privacy" || route === "/terms" || route === "/refund" || route === "/accountdeletion"
              ? "yearly"
              : "weekly",
    priority:
      route === ""
        ? 1.0
        : route === "/jobs" || route === "/jobs-near-me"
          ? 0.95
          : route.startsWith("/jobs-in-")
            ? 0.9
            : route.endsWith("-jobs")
              ? 0.85
              : route.includes("-jobs-")
                ? 0.8
                : route === "/contact" || route === "/faq" || route === "/safety"
                  ? 0.6
                  : 0.5
  }));
}
