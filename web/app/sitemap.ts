import type { MetadataRoute } from "next";

import { SITE_URL, getKnownLegacySlugs } from "@/lib/public-site";

const lastModified = new Date("2026-03-11");

export default function sitemap(): MetadataRoute.Sitemap {
  const routes = [
    "",
    "/jobs",
    "/refer",
    "/worker",
    ...getKnownLegacySlugs().map((slug) => `/${slug}`)
  ];

  return routes.map((route) => ({
    url: `${SITE_URL}${route}`,
    lastModified,
    changeFrequency: route === "" ? "daily" : route.startsWith("/jobs-in-") ? "daily" : "weekly",
    priority:
      route === ""
        ? 1
        : route.startsWith("/jobs-in-") || route.endsWith("-jobs")
          ? 0.9
          : 0.6
  }));
}
