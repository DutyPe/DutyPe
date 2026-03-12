import Link from "next/link";

import { SiteShell } from "@/components/site-shell";
import { PLAY_STORE_URL, coreSeoKeywords } from "@/lib/public-site";

const allCategories = [
  { href: "/jobs-near-me", label: "Jobs Near Me", icon: "📍", desc: "Browse all jobs within 1 km to 10 km of your location." },
  { href: "/driver-jobs", label: "Driver Jobs", icon: "🚗", desc: "Personal driver, cab driver, delivery van, and commercial driving roles." },
  { href: "/maid-jobs", label: "Maid Jobs", icon: "🏠", desc: "Part-time and full-time household support, cleaning, and family help." },
  { href: "/delivery-jobs", label: "Delivery Jobs", icon: "📦", desc: "Food, grocery, parcel, and last-mile courier delivery openings." },
  { href: "/cook-jobs", label: "Cook Jobs", icon: "👨‍🍳", desc: "Home cook, hostel cook, tiffin service, and kitchen staff roles." },
  { href: "/helper-jobs", label: "Helper Jobs", icon: "🔧", desc: "Shop helper, office helper, packing, and general operations support." },
  { href: "/security-jobs", label: "Security Jobs", icon: "🛡️", desc: "Security guard, watchman, night shift, and retail security roles." },
  { href: "/cleaner-jobs", label: "Cleaner Jobs", icon: "🧹", desc: "Office, hospital, mall, apartment, and commercial building cleaning." },
  { href: "/warehouse-jobs", label: "Warehouse Jobs", icon: "📦", desc: "Picking, packing, sorting, dispatch, and logistics operations." },
  { href: "/part-time-jobs", label: "Part-Time Jobs", icon: "⏰", desc: "Flexible evening, weekend, and morning shift openings." },
  { href: "/daily-wage-jobs", label: "Daily Wage Jobs", icon: "💰", desc: "Same-day payment roles in construction, loading, and event staffing." },
  { href: "/peon-jobs", label: "Peon Jobs", icon: "📋", desc: "Office peon, school assistant, admin runner, and document support." },
  { href: "/retail-jobs", label: "Retail Jobs", icon: "🏪", desc: "Sales associate, cashier, store keeper, and showroom assistant roles." }
];

const allCities = [
  "Hyderabad", "Bangalore", "Delhi", "Mumbai", "Vijayawada", "Warangal",
  "Tirupati", "Guntur", "Kakinada", "Karimnagar", "Nellore", "Anantapur",
  "Nizamabad", "Rajahmundry", "Khammam"
];

export const metadata = {
  title: "Jobs - Browse by Category and City | DutyPe",
  description: "Browse local jobs by category and city on DutyPe. Find delivery, driver, maid, cook, helper, security, and warehouse jobs near you.",
  keywords: [
    ...coreSeoKeywords,
    "job categories",
    "city wise jobs",
    "delivery boy jobs",
    "driver vacancy near me",
    "maid jobs near me",
    "part time job openings"
  ]
};

export default function JobsHubPage() {
  return (
    <SiteShell>
      <section className="hero">
        <div className="hero-grid">
          <div className="hero-copy">
            <div className="eyebrow-group">
              <span className="eyebrow">📍 All jobs</span>
              <span className="hero-note">13 categories · 15 cities</span>
            </div>
            <h1 className="headline">Browse Jobs by Category and City</h1>
            <p className="lede">
              Find local jobs across delivery, driving, housekeeping, cooking, security,
              warehouse, and many more categories. Browse by job type or by your city —
              all jobs are verified with clear pay and location details.
            </p>
            <div className="button-row">
              <Link href="/jobs-near-me" className="button">
                Jobs near me
              </Link>
              <a href={PLAY_STORE_URL} className="button ghost" target="_blank" rel="noopener noreferrer">
                Download app
              </a>
            </div>
            <div className="hero-chip-row">
              <span className="pill">Free for workers</span>
              <span className="pill">Verified employers</span>
              <span className="pill">Instant apply</span>
            </div>
          </div>

          <aside className="hero-panel hero-panel-enhanced">
            <span className="card-kicker">What you can find</span>
            <h3>Jobs for every skill level</h3>
            <ul className="detail-list detail-list-enhanced">
              <li>
                <strong>13 job categories — driver, delivery, maid, cook, helper, and more</strong>
              </li>
              <li>
                <strong>15 cities — Hyderabad, Bangalore, Delhi, Mumbai, and more</strong>
              </li>
              <li>
                <strong>No experience needed — many openings welcome freshers</strong>
              </li>
              <li>
                <strong>Daily and weekly payments — fast cash-flow options</strong>
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
            Click any category to see available jobs, salary ranges, and common roles
            in that field.
          </p>
        </div>

        <div className="route-grid">
          {allCategories.map((category) => (
            <Link key={category.href} href={category.href} className="route-card">
              <span className="card-kicker">{category.icon}</span>
              <h3>{category.label}</h3>
              <p>{category.desc}</p>
              <p className="route-note">Browse →</p>
            </Link>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Cities</span>
            <h2>Jobs by city</h2>
          </div>
          <p>
            Find local openings in your city. Click any city below to see available jobs.
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

      <section className="section">
        <div className="callout">
          <strong>Get the full experience</strong>
          <span>
            For live listings with real-time updates, download the DutyPe app free
            from the Google Play Store and start applying to jobs near you.
          </span>
        </div>
      </section>
    </SiteShell>
  );
}
