import Link from "next/link";

import { SiteShell } from "@/components/site-shell";
import { cityLandingTargets, homeCategories } from "@/lib/public-site";

export const metadata = {
  title: "Jobs",
  description: "Browse DutyPe job categories, city pages, and public app-open routes."
};

export default function JobsHubPage() {
  return (
    <SiteShell>
      <section className="hero">
        <div className="hero-grid">
          <div>
            <span className="eyebrow">Jobs hub</span>
            <h1 className="headline">Browse local job routes before you open the app.</h1>
            <p className="lede">
              The old website had separate category pages, city pages, and deep-link
              pages. This hub replaces that sprawl with one route system.
            </p>
            <div className="button-row">
              <Link href="/app/worker/jobs" className="button">
                Open live jobs
              </Link>
              <Link href="/jobs-near-me" className="button ghost">
                Jobs near me
              </Link>
            </div>
          </div>

          <aside className="hero-panel">
            <span className="card-kicker">What moved here</span>
            <h3>Legacy public footprint</h3>
            <ul className="detail-list">
              <li>
                <strong>Category pages</strong>
                <span>Delivery, driver, maid, cook, helper, cleaner, security, warehouse.</span>
              </li>
              <li>
                <strong>City pages</strong>
                <span>Jobs in Hyderabad, Bangalore, Delhi, Mumbai, Vijayawada, and more.</span>
              </li>
              <li>
                <strong>Deep-link routes</strong>
                <span>Job, worker, employer, chat, referral, profile, and notifications handoff.</span>
              </li>
            </ul>
          </aside>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Categories</span>
            <h2>Popular job categories</h2>
          </div>
          <p>
            These routes preserve the legacy search intent while using a cleaner UI
            and shared page logic.
          </p>
        </div>

        <div className="section-grid">
          {homeCategories.map((category) => (
            <Link key={category.href} href={category.href} className="route-card">
              <span className="card-kicker">Category page</span>
              <h3>{category.label}</h3>
              <p>SEO landing copy, safety framing, and clean handoff into the DutyPe app.</p>
              <p className="route-note">Open reusable route</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Cities</span>
            <h2>City landing pages</h2>
          </div>
          <p>
            The old site used many one-off HTML files for city SEO. The new site keeps
            those pages but drives them through patterns instead of duplication.
          </p>
        </div>

        <div className="pill-row">
          {cityLandingTargets.map((city) => (
            <Link
              key={city}
              href={`/jobs-in-${city.toLowerCase()}`}
              className="pill pill-link"
            >
              Jobs in {city}
            </Link>
          ))}
        </div>

        <div className="callout">
          If you want the actual live marketplace, continue to the worker app routes.
          The public site is the discovery and trust surface. The product app remains
          the action surface.
        </div>
      </section>
    </SiteShell>
  );
}
