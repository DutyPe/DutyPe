import type { Metadata } from "next";

import { AppConversionCard } from "@/components/public/app-conversion-card";
import { SiteShell } from "@/components/site-shell";
import { SITE_URL, coreSeoKeywords } from "@/lib/public-site";

/** Jobs are shown only in the app; this page is a static install card (no server, no database). */
export const dynamic = "force-static";

export const metadata: Metadata = {
  title: "Live Local Jobs - DutyPe App",
  description: "See live delivery, driving, helper and other local jobs near you in the DutyPe app. Nearest first, urgent jobs ring on your phone.",
  keywords: [...coreSeoKeywords, "live jobs", "jobs near me"],
  alternates: { canonical: `${SITE_URL}/jobs` }
};

export default function JobsHubPage() {
  return (
    <SiteShell>
      <div className="jobs-directory-page">
        <AppConversionCard
          title="See live jobs near you on the DutyPe app"
          subtitle="Nearest jobs first, urgent jobs that ring on your phone, and one-tap apply or call. Free for workers."
        />
      </div>
    </SiteShell>
  );
}
