import type { Metadata } from "next";
import Link from "next/link";

import { AppLaunchCard } from "@/components/public/app-launch-card";
import { SiteShell } from "@/components/site-shell";
import { cityLandingTargets, homeBenefits, homeCategories, siteMeta } from "@/lib/public-site";

const heroSignals = [
  {
    label: "Worker-first",
    title: "Fast path to real jobs",
    copy: "The website should get people into nearby openings quickly instead of trapping them in brochure pages."
  },
  {
    label: "App-backed",
    title: "Live actions stay in product",
    copy: "Apply, chat, save, and employer response flows still belong to the live DutyPe app."
  },
  {
    label: "Trust-led",
    title: "Safety stays visible",
    copy: "No fee traps, clearer trust cues, and direct contact framing should be obvious on every route."
  }
];

const proofStrip = [
  {
    value: "No fees",
    label: "Workers should never pay to browse or apply."
  },
  {
    value: "Hyperlocal",
    label: "City and neighborhood intent stays central."
  },
  {
    value: "Direct",
    label: "Worker-to-employer contact with less friction."
  }
];

export const metadata: Metadata = {
  title: "DutyPe",
  description: siteMeta.description
};

export default function HomePage() {
  return (
    <SiteShell>
      <section className="hero hero-home">
        <div className="hero-grid hero-home-grid">
          <div className="hero-copy">
            <div className="eyebrow-group">
              <span className="eyebrow">Hyperlocal hiring</span>
              <span className="hero-note">Bold black-and-white UI, responsive across every screen.</span>
            </div>

            <h1 className="headline">Local hiring that feels fast, clear, and worker-first.</h1>

            <p className="lede">
              {siteMeta.description} The new website keeps the same DutyPe tone as the
              app: direct, safe, and built for workers who need nearby jobs without
              middlemen.
            </p>

            <div className="button-row">
              <Link href="/app/worker/jobs" className="button">
                Browse live jobs
              </Link>
              <Link href="/app/employer/post-job" className="button ghost">
                Post a job
              </Link>
            </div>

            <div className="hero-chip-row">
              <span className="pill">No middlemen</span>
              <span className="pill">No worker fees</span>
              <span className="pill">Direct employer contact</span>
              <span className="pill">Built for mobile speed</span>
            </div>

            <div className="brand-stat-row">
              {proofStrip.map((item) => (
                <div key={item.value} className="stat-card">
                  <strong>{item.value}</strong>
                  <span>{item.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="hero-rail">
            <div className="orb-stage">
              <div className="logo-orb hero-orb">DutyPe</div>
              <p className="orb-caption">
                The web surface should feel like the app brand grew outward, not like a
                separate generic website.
              </p>

              <div className="signal-stack">
                {heroSignals.map((signal) => (
                  <article key={signal.title} className="signal-card">
                    <span className="signal-label">{signal.label}</span>
                    <strong>{signal.title}</strong>
                    <p>{signal.copy}</p>
                  </article>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Core promise</span>
            <h2>Why the product lands differently</h2>
          </div>
          <p>
            The UI should carry the same product message everywhere: nearby jobs,
            faster movement, and visible safety cues.
          </p>
        </div>

        <div className="section-grid">
          {homeBenefits.map((benefit, index) => (
            <article key={benefit.title} className="card">
              <span className="card-kicker">Signal {String(index + 1).padStart(2, "0")}</span>
              <h3>{benefit.title}</h3>
              <p>{benefit.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Explore routes</span>
            <h2>Categories and city entry points</h2>
          </div>
          <p>
            Public pages should capture search intent cleanly, then hand users into the
            live worker or employer flows when they are ready to act.
          </p>
        </div>

        <div className="route-grid">
          {homeCategories.map((category) => (
            <Link key={category.href} href={category.href} className="route-card">
              <span className="card-kicker">Public route</span>
              <h3>{category.label}</h3>
              <p>Cleaner SEO entry, stronger trust framing, and better handoff into the app.</p>
              <p className="route-note">Open route</p>
            </Link>
          ))}
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
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">App handoff</span>
            <h2>Public website outside, live product inside</h2>
          </div>
          <p>
            The public site builds trust and intent. The app handles the real actions:
            applications, profile state, chat, and hiring decisions.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">What the website should do</span>
            <h3>Set context without creating friction</h3>
            <ul className="detail-list">
              <li>
                <strong>Explain the value fast</strong>
                <span>Workers and employers should understand the product in a few seconds.</span>
              </li>
              <li>
                <strong>Keep the trust layer visible</strong>
                <span>No-pay warnings, direct contact rules, and product boundaries stay obvious.</span>
              </li>
              <li>
                <strong>Send users into live flows</strong>
                <span>Search intent should end in app-backed actions instead of dead-end pages.</span>
              </li>
            </ul>
          </article>

          <AppLaunchCard
            kind="home"
            autoOpen={false}
            headline="Open the full DutyPe app"
            description="The best experience still lives in the product app, where listings, applications, chat, and status changes stay connected to Firebase."
            bullets={[
              "Live job discovery and saved jobs",
              "Worker applications and employer review",
              "Direct chat, notifications, and profile actions"
            ]}
            badge="App-first product"
          />
        </div>
      </section>
    </SiteShell>
  );
}
