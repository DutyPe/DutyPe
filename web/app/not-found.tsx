import Link from "next/link";

import { SiteShell } from "@/components/site-shell";

export default function NotFound() {
  return (
    <SiteShell>
      <section className="hero">
        <div className="hero-grid" style={{ gridTemplateColumns: "1fr" }}>
          <div className="hero-copy" style={{ textAlign: "center", justifyItems: "center" }}>
            <span className="eyebrow">404</span>
            <h1 className="headline" style={{ maxWidth: "none" }}>Page Not Found</h1>
            <p className="lede" style={{ maxWidth: "48ch", margin: "0 auto" }}>
              The page you are looking for does not exist or has been moved.
              Try browsing jobs or returning to the homepage.
            </p>
            <div className="button-row" style={{ justifyContent: "center" }}>
              <Link href="/" className="button">
                Go to homepage
              </Link>
              <Link href="/jobs" className="button ghost">
                Browse jobs
              </Link>
            </div>
          </div>
        </div>
      </section>
    </SiteShell>
  );
}
