import Link from "next/link";

import { SiteShell } from "@/components/site-shell";

export default function NotFound() {
  return (
    <SiteShell>
      <section className="hero">
        <span className="eyebrow">Route not found</span>
        <h1 className="headline">This page has not been mapped into the new web layer yet.</h1>
        <p className="lede">
          The Next.js scaffold is in place, but only the first public and admin routes
          have been created. Add the missing route here instead of creating another
          static HTML file.
        </p>
        <div className="button-row">
          <Link href="/" className="button">
            Return to overview
          </Link>
          <Link href="/jobs" className="button ghost">
            Browse job routes
          </Link>
        </div>
      </section>
    </SiteShell>
  );
}
