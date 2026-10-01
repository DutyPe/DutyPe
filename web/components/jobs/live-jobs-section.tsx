import { AppConversionCard } from "@/components/public/app-conversion-card";
import type { JobSearch } from "@/lib/jobs/public-listings";

/**
 * Where the website used to list live jobs. Jobs are only shown in the app now (the website reads
 * no job data, so it adds no database cost): this is an install / open-in-app card.
 */
export async function LiveJobsSection({ search, heading }: { search: JobSearch; heading?: string; showFilters?: boolean; cursor?: string | null }) {
  const place = search.city || search.area;
  return (
    <section aria-label={heading || "Jobs in the DutyPe app"}>
      <AppConversionCard
        title={place ? `See live jobs in ${place} on the DutyPe app` : "See live jobs near you on the DutyPe app"}
        subtitle="Nearest jobs first, urgent jobs that ring on your phone, and one-tap apply or call. Free for workers."
        categoryOrCity={place || undefined}
      />
    </section>
  );
}
