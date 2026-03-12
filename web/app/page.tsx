import type { Metadata } from "next";
import Link from "next/link";

import { AppLaunchCard } from "@/components/public/app-launch-card";
import { SiteShell } from "@/components/site-shell";
import {
  PLAY_STORE_URL,
  cityLandingTargets,
  coreSeoKeywords,
  homeBenefits,
  homeCategories,
  homeStats,
  siteMeta
} from "@/lib/public-site";

const heroSignals = [
  {
    label: "For workers",
    title: "Find jobs near you",
    copy: "Browse verified local openings — delivery, driver, maid, cook, helper, and more — all within your area."
  },
  {
    label: "For employers",
    title: "Hire local staff fast",
    copy: "Post a job and get applications from nearby workers within hours. No middlemen, no agency fees."
  },
  {
    label: "Safe & trusted",
    title: "Your safety comes first",
    copy: "Verified employers, in-app reporting, and a strict no-fee policy protect every worker on DutyPe."
  }
];

const howItWorks = [
  {
    step: "01",
    title: "Download the app",
    description: "Get DutyPe free from the Google Play Store. Set up your profile in under 2 minutes."
  },
  {
    step: "02",
    title: "Enable your location",
    description: "Allow location access so DutyPe can show jobs within 1 km, 5 km, or 10 km of you."
  },
  {
    step: "03",
    title: "Browse & apply instantly",
    description: "One-tap apply to multiple jobs — no lengthy forms. Your profile is shared directly with the employer."
  },
  {
    step: "04",
    title: "Chat & get hired",
    description: "Message employers directly through the app. Many workers get hired within 24 to 48 hours."
  }
];

const allCategories = [
  { href: "/jobs-near-me", label: "Jobs Near Me", icon: "📍" },
  { href: "/driver-jobs", label: "Driver Jobs", icon: "🚗" },
  { href: "/maid-jobs", label: "Maid Jobs", icon: "🏠" },
  { href: "/delivery-jobs", label: "Delivery Jobs", icon: "📦" },
  { href: "/cook-jobs", label: "Cook Jobs", icon: "👨‍🍳" },
  { href: "/helper-jobs", label: "Helper Jobs", icon: "🔧" },
  { href: "/security-jobs", label: "Security Jobs", icon: "🛡️" },
  { href: "/cleaner-jobs", label: "Cleaner Jobs", icon: "🧹" },
  { href: "/warehouse-jobs", label: "Warehouse Jobs", icon: "📦" },
  { href: "/part-time-jobs", label: "Part-Time Jobs", icon: "⏰" },
  { href: "/daily-wage-jobs", label: "Daily Wage Jobs", icon: "💰" },
  { href: "/peon-jobs", label: "Peon Jobs", icon: "📋" },
  { href: "/retail-jobs", label: "Retail Jobs", icon: "🏪" }
];

const allCities = [
  "Hyderabad", "Bangalore", "Delhi", "Mumbai", "Vijayawada", "Warangal",
  "Tirupati", "Guntur", "Kakinada", "Karimnagar", "Nellore", "Anantapur",
  "Nizamabad", "Rajahmundry", "Khammam"
];

export const metadata: Metadata = {
  title: "DutyPe - Find Local Jobs Near You",
  description: siteMeta.description,
  keywords: [
    ...coreSeoKeywords,
    "jobs near me for freshers",
    "instant job apply",
    "local job vacancy",
    "trusted hiring platform",
    "worker employer direct contact"
  ],
  openGraph: {
    title: "DutyPe - Local Jobs Near You",
    description: siteMeta.description,
    type: "website"
  }
};

export default function HomePage() {
  return (
    <SiteShell>
      {/* ── Hero ── */}
      <section className="hero hero-home">
        <div className="hero-grid hero-home-grid">
          <div className="hero-copy">
            <div className="eyebrow-group">
              <span className="eyebrow">🇮🇳 Made in India</span>
              <span className="hero-note">India's fastest growing local job platform</span>
            </div>

            <h1 className="headline">Find Local Jobs Near You</h1>

            <p className="lede">
              Connect with verified employers instantly. No middlemen, no fees.
              Get hired faster with DutyPe — browse delivery, driver, maid, cook,
              helper, and hundreds of other local jobs near your location.
            </p>

            <div className="button-row">
              <a href={PLAY_STORE_URL} className="button" target="_blank" rel="noopener noreferrer">
                Download App
              </a>
              <Link href="/jobs-near-me" className="button ghost">
                Jobs Near Me
              </Link>
            </div>

            <div className="hero-chip-row">
              <span className="pill">No middlemen</span>
              <span className="pill">No worker fees</span>
              <span className="pill">Verified employers</span>
              <span className="pill">Instant apply</span>
              <span className="pill">Daily payments</span>
            </div>
          </div>

          <div className="hero-rail">
            <div className="orb-stage">
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

      {/* ── Stats strip ── */}
      <section className="section">
        <div className="brand-stat-row" style={{ justifyContent: "center", gap: "2rem", flexWrap: "wrap" }}>
          {homeStats.map((stat) => (
            <div key={stat.label} className="stat-card">
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ── Features / Benefits ── */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Why DutyPe</span>
            <h2>Built for workers and employers</h2>
          </div>
          <p>
            Everything you need to find work or hire locally — fast, safe, and
            completely free for job seekers.
          </p>
        </div>

        <div className="section-grid">
          {homeBenefits.map((benefit, index) => (
            <article key={benefit.title} className="card">
              <span className="card-kicker">{["📍", "⚡", "💰", "🔒", "💬", "🆓"][index]}</span>
              <h3>{benefit.title}</h3>
              <p>{benefit.description}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── How it works ── */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">How it works</span>
            <h2>Start finding jobs in minutes</h2>
          </div>
          <p>
            Four simple steps from download to getting hired. No lengthy forms,
            no waiting.
          </p>
        </div>

        <div className="section-grid">
          {howItWorks.map((item) => (
            <article key={item.step} className="card">
              <span className="card-kicker">Step {item.step}</span>
              <h3>{item.title}</h3>
              <p>{item.description}</p>
            </article>
          ))}
        </div>
      </section>

      {/* ── Browse by category ── */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Browse jobs</span>
            <h2>Popular job categories</h2>
          </div>
          <p>
            Find the right type of work near your location. Every category has
            local listings with clear pay and employer details.
          </p>
        </div>

        <div className="route-grid">
          {allCategories.map((category) => (
            <Link key={category.href} href={category.href} className="route-card">
              <span className="card-kicker">{category.icon}</span>
              <h3>{category.label}</h3>
              <p>Find verified {category.label.toLowerCase()} near you with clear pay, shift, and location details.</p>
              <p className="route-note">Browse →</p>
            </Link>
          ))}
        </div>
      </section>

      {/* ── Browse by city ── */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Browse by city</span>
            <h2>Jobs in your city</h2>
          </div>
          <p>
            Find local openings in major Indian cities. Click your city to see
            available jobs.
          </p>
        </div>

        <div className="pill-row">
          {allCities.map((city) => (
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

      {/* ── App handoff ── */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Get started</span>
            <h2>Ready to find your next job?</h2>
          </div>
          <p>
            Join thousands of workers who have found jobs through DutyPe.
            Download the app and start your journey today.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">For workers</span>
            <h3>Everything you need to get hired</h3>
            <ul className="detail-list">
              <li>
                <strong>Jobs near your location</strong>
                <span>Find openings within walking distance or a short commute.</span>
              </li>
              <li>
                <strong>One-tap apply</strong>
                <span>Apply to multiple jobs in seconds — no forms, no hassle.</span>
              </li>
              <li>
                <strong>Direct employer chat</strong>
                <span>Message employers directly without middlemen or agency fees.</span>
              </li>
              <li>
                <strong>Daily and weekly payments</strong>
                <span>Many jobs offer fast cash-flow with daily or weekly pay options.</span>
              </li>
            </ul>
          </article>

          <AppLaunchCard
            kind="home"
            autoOpen={false}
            headline="Download DutyPe"
            description="Get the full experience — live job listings, instant apply, direct employer chat, and real-time notifications."
            bullets={[
              "Free for workers — no hidden fees",
              "Verified local employers",
              "Location-based job discovery",
              "In-app chat and application tracking"
            ]}
            badge="Free download"
          />
        </div>
      </section>
    </SiteShell>
  );
}
