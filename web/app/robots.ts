import type { MetadataRoute } from "next";

import { SITE_URL } from "@/lib/public-site";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: [
        "/admin/",
        "/api/",
        "/app/",
        "/app-redirect",
        "/application/",
        "/refer/",
        "/profile",
        "/notifications",
        "/join"
      ]
    },
    host: SITE_URL,
    sitemap: `${SITE_URL}/sitemap.xml`
  };
}
