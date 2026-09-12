import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

import { AppLaunchCard } from "@/components/public/app-launch-card";
import { SiteShell } from "@/components/site-shell";
import {
  PLAY_STORE_URL,
  cityLandingTargets,
  coreSeoKeywords,
  homeBenefits,
  homeStats,
  siteMeta
} from "@/lib/public-site";

const howItWorks = [
  {
    step: "01",
    title: "Download the App",
    description: "Get DutyPe free from the Google Play Store. Create your worker profile in under 2 minutes."
  },
  {
    step: "02",
    title: "Set Your Location",
    description: "Enable location access to instantly view verified jobs within 1 km to 10 km of your neighborhood."
  },
  {
    step: "03",
    title: "One-Tap Apply",
    description: "Apply to multiple openings directly without long forms or resumes. Your profile reaches the employer instantly."
  },
  {
    step: "04",
    title: "Call & Get Hired",
    description: "Connect directly with verified employers through phone or in-app chat. Most workers get hired within 24–48 hours."
  }
];

const employerSteps = [
  {
    step: "01",
    title: "Post a Job in 2 Minutes",
    description: "Specify role, location, salary, shift timings, and vacancy requirements easily."
  },
  {
    step: "02",
    title: "Receive Nearby Applications",
    description: "Get instant interest from verified local workers living close to your job location."
  },
  {
    step: "03",
    title: "Hire Directly with Zero Fees",
    description: "Call applicants directly, schedule interviews, and onboard talent with no agency commissions."
  }
];

const trustPoints = [
  "Strict zero-fee policy: Workers never pay any fee to access jobs or attend interviews.",
  "Verified employers: Every job posting undergoes automated & manual verification.",
  "Direct communication: Workers and employers connect directly with zero middleman interference.",
  "Transparent pay: Clear salary ranges, shift timings, and daily/weekly payment disclosures."
];

const homeFaqs = [
  {
    question: "Is DutyPe free for workers?",
    answer:
      "Yes, 100% free. Job seekers can browse listings, apply with one tap, and speak directly with employers without paying any registration or agency fees."
  },
  {
    question: "What types of jobs are available on DutyPe?",
    answer:
      "DutyPe specializes in local blue-collar and gray-collar roles including delivery executives, drivers, maids, cooks, helpers, warehouse associates, security guards, retail staff, and part-time workers."
  },
  {
    question: "How do employers hire through DutyPe?",
    answer:
      "Employers can post job vacancies within minutes on the DutyPe app or website, view verified local applicants, and contact them directly to hire fast."
  },
  {
    question: "Which locations are currently supported?",
    answer:
      "DutyPe is live across major Indian cities including Hyderabad, Bangalore, Chennai, Coimbatore, Delhi NCR, Mumbai, Vijayawada, Warangal, Tirupati, and more."
  },
  {
    question: "How fast can I get hired?",
    answer:
      "Because DutyPe matches candidates based on exact proximity, employers typically review applications and call candidates within 24 to 48 hours."
  }
];

const allCategories = [
  { href: "/jobs-near-me", label: "Jobs Near Me", icon: "📍", count: "2,500+ jobs" },
  { href: "/delivery-jobs", label: "Delivery Partner", icon: "📦", count: "1,800+ jobs" },
  { href: "/driver-jobs", label: "Driver (Cab / Commercial)", icon: "🚗", count: "1,200+ jobs" },
  { href: "/warehouse-jobs", label: "Warehouse Associate", icon: "🏬", count: "950+ jobs" },
  { href: "/maid-jobs", label: "Maid & Housekeeping", icon: "🏠", count: "800+ jobs" },
  { href: "/cook-jobs", label: "Cook & Chef", icon: "👨‍🍳", count: "650+ jobs" },
  { href: "/helper-jobs", label: "Helper & Assistant", icon: "🔧", count: "1,100+ jobs" },
  { href: "/security-jobs", label: "Security Guard", icon: "🛡️", count: "750+ jobs" },
  { href: "/cleaner-jobs", label: "Cleaner & Sanitization", icon: "🧹", count: "600+ jobs" },
  { href: "/part-time-jobs", label: "Part-Time & Flexible", icon: "⏰", count: "900+ jobs" },
  { href: "/daily-wage-jobs", label: "Daily Wage & Urgent", icon: "💰", count: "550+ jobs" },
  { href: "/retail-jobs", label: "Retail & Counter Staff", icon: "🏪", count: "700+ jobs" },
  { href: "/peon-jobs", label: "Office Boy & Peon", icon: "📋", count: "450+ jobs" }
];

const allCities = [
  "Hyderabad", "Bangalore", "Chennai", "Coimbatore", "Delhi", "Mumbai",
  "Vijayawada", "Warangal", "Tirupati", "Guntur", "Kakinada", "Karimnagar",
  "Nellore", "Anantapur", "Nizamabad", "Rajahmundry", "Khammam"
];

export const metadata: Metadata = {
  title: "DutyPe — Jobs Near Me | Local Hiring App for Workers & Employers in India",
  description:
    "Find jobs near you — delivery, driver, maid, cook, helper, security, warehouse, retail, part-time & daily wage jobs. Apply free on DutyPe. No middlemen, verified employers, instant hiring.",
  keywords: [
    ...coreSeoKeywords,
    "job app india",
    "local job vacancy",
    "trusted hiring platform",
    "worker employer direct contact",
    "local hiring app india",
    "hire nearby workers",
    "jobs hiring today",
    "same day jobs near me",
    "walk in jobs near me",
    "jobs for women near me",
    "jobs without interview near me",
    "immediate joining jobs",
    `${siteMeta.companyName.toLowerCase()} dutype`
  ],
  alternates: {
    canonical: "/"
  },
  openGraph: {
    title: "DutyPe — Jobs Near Me | Local Hiring App for Workers & Employers",
    description:
      "Find delivery, driver, maid, cook, helper, security, warehouse & part-time jobs near you. Apply free — no middlemen, verified employers.",
    type: "website",
    url: "/"
  }
};

export default function HomePage() {
  const structuredData = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "Organization",
        name: siteMeta.companyName,
        brand: { "@type": "Brand", name: siteMeta.name },
        url: "https://dutype.in",
        email: siteMeta.supportEmail,
        description: siteMeta.description,
        areaServed: { "@type": "Country", name: "India" },
        address: {
          "@type": "PostalAddress",
          addressLocality: "Hyderabad",
          addressRegion: "Telangana",
          addressCountry: "IN"
        },
        sameAs: [PLAY_STORE_URL]
      },
      {
        "@type": "WebSite",
        name: siteMeta.name,
        url: "https://dutype.in",
        description: siteMeta.description,
        inLanguage: "en-IN",
        potentialAction: {
          "@type": "SearchAction",
          target: "https://dutype.in/jobs-near-me?q={search_term_string}",
          "query-input": "required name=search_term_string"
        }
      },
      {
        "@type": "MobileApplication",
        name: siteMeta.name,
        operatingSystem: "Android",
        applicationCategory: "BusinessApplication",
        downloadUrl: PLAY_STORE_URL,
        offers: {
          "@type": "Offer",
          price: "0",
          priceCurrency: "INR"
        }
      },
      {
        "@type": "FAQPage",
        mainEntity: homeFaqs.map((faq) => ({
          "@type": "Question",
          name: faq.question,
          acceptedAnswer: {
            "@type": "Answer",
            text: faq.answer
          }
        }))
      }
    ]
  };

  return (
    <SiteShell hideTopBar hideAds>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />

      <div className="home-editorial">
        {/* HERO SECTION */}
        <section className="hero hero-home hero-editorial">
          <div className="hero-grid hero-home-grid hero-editorial-grid">
            <div className="hero-copy">
              <div className="eyebrow-group">
                <span className="hero-brand-mark">
                  <Image
                    src="/icon.webp"
                    alt="DutyPe"
                    width={36}
                    height={36}
                    priority
                  />
                  <span>DutyPe</span>
                </span>
                <span className="eyebrow">🇮🇳 India&apos;s Local Job Discovery Platform</span>
              </div>

              <h1 className="headline">
                Find <span className="dp-accent-underline">Verified Local Jobs</span> Near You
              </h1>

              <p className="lede">
                DutyPe connects workers directly with verified employers across India.
                Discover delivery, driver, maid, cook, helper, warehouse, and retail jobs
                with transparent salaries and direct employer contact.
              </p>

              {/* SEARCH BAR */}
              <form className="hero-search" action="/jobs">
                <label>
                  <span>Job Role</span>
                  <input name="q" type="search" placeholder="e.g. Delivery, Driver, Cook, Helper" />
                </label>
                <label>
                  <span>City / Location</span>
                  <input name="city" type="search" placeholder="e.g. Hyderabad, Coimbatore, Bangalore" />
                </label>
                <button type="submit" className="button hero-search-button">
                  Search Jobs
                </button>
              </form>

              {/* CALL TO ACTION BUTTONS */}
              <div className="button-row">
                <a href={PLAY_STORE_URL} className="button" target="_blank" rel="noopener noreferrer">
                  Download Free App
                </a>
                <Link href="/jobs-near-me" className="button ghost">
                  Browse Jobs Near Me
                </Link>
                <Link href="/employer" className="button ghost">
                  Post a Job (Employers)
                </Link>
              </div>

              {/* TRUST CHIPS */}
              <div className="hero-chip-row">
                <span className="pill">✓ 100% Free for Workers</span>
                <span className="pill">✓ Verified Direct Employers</span>
                <span className="pill">✓ Zero Agency Fees</span>
                <span className="pill">✓ Daily &amp; Weekly Payouts</span>
                <span className="pill">✓ In-App Direct Chat</span>
              </div>
            </div>

            {/* HERO RIGHT RAIL — LIVE JOBS SHOWCASE */}
            <div className="hero-rail">
              <div className="detail-panel tone-highlight" style={{ padding: "24px 28px" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
                  <span className="card-kicker" style={{ margin: 0, padding: 0 }}>Live Openings</span>
                  <span style={{ fontSize: "0.78rem", color: "var(--green-deep)", fontWeight: 700 }}>● Updated Just Now</span>
                </div>
                
                <h3 style={{ fontSize: "1.25rem", margin: "0 0 16px" }}>Featured Local Roles</h3>
                
                <div style={{ display: "grid", gap: "10px" }}>
                  <div style={{ background: "#ffffff", padding: "12px 16px", borderRadius: "10px", border: "1px solid var(--edge)" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 700, fontSize: "0.94rem" }}>
                      <span>📦 Delivery Executive</span>
                      <span style={{ color: "var(--green-deep)" }}>₹18,000 - ₹25,000</span>
                    </div>
                    <div style={{ fontSize: "0.82rem", color: "var(--ink-soft)", marginTop: "4px" }}>
                      Amazon, Blinkit, Swiggy · Daily Payouts Available
                    </div>
                  </div>

                  <div style={{ background: "#ffffff", padding: "12px 16px", borderRadius: "10px", border: "1px solid var(--edge)" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 700, fontSize: "0.94rem" }}>
                      <span>🚗 Driver (Commercial / Cab)</span>
                      <span style={{ color: "var(--green-deep)" }}>₹20,000 - ₹32,000</span>
                    </div>
                    <div style={{ fontSize: "0.82rem", color: "var(--ink-soft)", marginTop: "4px" }}>
                      Logistics &amp; Private · Day/Night Shift Options
                    </div>
                  </div>

                  <div style={{ background: "#ffffff", padding: "12px 16px", borderRadius: "10px", border: "1px solid var(--edge)" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 700, fontSize: "0.94rem" }}>
                      <span>🏬 Warehouse Associate</span>
                      <span style={{ color: "var(--green-deep)" }}>₹16,000 - ₹23,000</span>
                    </div>
                    <div style={{ fontSize: "0.82rem", color: "var(--ink-soft)", marginTop: "4px" }}>
                      Picking, Packing, Scanning · PF &amp; ESI Benefits
                    </div>
                  </div>

                  <div style={{ background: "#ffffff", padding: "12px 16px", borderRadius: "10px", border: "1px solid var(--edge)" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 700, fontSize: "0.94rem" }}>
                      <span>🏠 Cook &amp; Housekeeping</span>
                      <span style={{ color: "var(--green-deep)" }}>₹15,000 - ₹22,000</span>
                    </div>
                    <div style={{ fontSize: "0.82rem", color: "var(--ink-soft)", marginTop: "4px" }}>
                      Homes &amp; Restaurants · Local Neighborhood Matches
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: "18px", textAlign: "center" }}>
                  <Link href="/jobs-near-me" className="route-note route-note-link" style={{ fontSize: "0.9rem" }}>
                    Explore All 10,000+ Verified Jobs →
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* STATS BAR */}
        <section className="section" style={{ paddingBlock: "40px" }}>
          <div className="brand-stat-row" style={{ justifyContent: "center", gap: "2rem", flexWrap: "wrap" }}>
            {homeStats.map((stat) => (
              <div key={stat.label} className="stat-card" style={{ minWidth: "180px" }}>
                <strong>{stat.value}</strong>
                <span>{stat.label}</span>
              </div>
            ))}
          </div>
        </section>

        {/* POPULAR CATEGORIES */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Explore Categories</span>
              <h2>Popular Job Categories</h2>
            </div>
            <p>
              Find the exact type of job that fits your skills and schedule.
              Every listing includes verified pay, shift timings, and location details.
            </p>
          </div>

          <div className="route-grid">
            {allCategories.map((category) => (
              <Link key={category.href} href={category.href} className="route-card">
                <span className="card-kicker">{category.icon}</span>
                <h3>{category.label}</h3>
                <p>{category.count}</p>
                <p className="route-note">Browse jobs →</p>
              </Link>
            ))}
          </div>
        </section>

        {/* HOW IT WORKS */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Simple Process</span>
              <h2>How DutyPe Works for Job Seekers</h2>
            </div>
            <p>
              Get hired in four simple steps without lengthy forms, middlemen, or application fees.
            </p>
          </div>

          <div className="journey-grid">
            {howItWorks.map((item) => (
              <article key={item.step} className="card">
                <span className="card-kicker">Step {item.step}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </article>
            ))}
          </div>
        </section>

        {/* FOR EMPLOYERS */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">For Employers</span>
              <h2>Hire Local Staff Faster</h2>
            </div>
            <p>
              Post job vacancies directly to nearby verified workers and hire within 24 to 48 hours.
            </p>
          </div>

          <div className="journey-grid">
            {employerSteps.map((item) => (
              <article key={item.step} className="card">
                <span className="card-kicker">Step {item.step}</span>
                <h3>{item.title}</h3>
                <p>{item.description}</p>
              </article>
            ))}
          </div>

          <div style={{ textAlign: "center", marginTop: "32px" }}>
            <Link href="/employer" className="button">
              Post a Job as Employer →
            </Link>
          </div>
        </section>

        {/* WHY CHOOSE DUTYPE */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Platform Benefits</span>
              <h2>Why Workers &amp; Employers Choose DutyPe</h2>
            </div>
            <p>
              Built specifically for local hiring across Indian cities with a focus on trust and speed.
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

        {/* CITIES */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Locations</span>
              <h2>Find Jobs in Your City</h2>
            </div>
            <p>
              Explore local openings across major cities and tier-2 growth hubs in India.
            </p>
          </div>

          <div className="quick-link-grid city-link-grid">
            {cityLandingTargets.map((city) => (
              <Link
                key={city}
                href={`/jobs-in-${city.toLowerCase()}`}
                className="mini-route-card"
              >
                <strong>Jobs in {city}</strong>
                <span>View local openings</span>
              </Link>
            ))}
          </div>

          <div className="pill-row city-pill-row" style={{ marginTop: "24px" }}>
            {allCities.map((city) => (
              <Link key={city} href={`/jobs-in-${city.toLowerCase()}`} className="pill pill-link">
                {city}
              </Link>
            ))}
          </div>
        </section>

        {/* TRUST & APP DOWNLOAD */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">Safety &amp; Trust</span>
              <h2>Our Trust &amp; Safety Commitment</h2>
            </div>
            <p>
              DutyPe was created to eliminate job scams, middlemen, and predatory recruitment fees.
            </p>
          </div>

          <div className="trust-grid">
            <article className="trust-panel">
              <span className="card-kicker">Core Principles</span>
              <h3>Safe, Direct &amp; Transparent Hiring</h3>
              <ul className="trust-list">
                {trustPoints.map((point) => (
                  <li key={point}>{point}</li>
                ))}
              </ul>
              <div className="hero-chip-row" style={{ marginTop: "20px" }}>
                <span className="pill">🏢 {siteMeta.companyName}</span>
                <span className="pill">✉️ {siteMeta.supportEmail}</span>
              </div>
            </article>

            <AppLaunchCard
              kind="home"
              autoOpen={false}
              headline="Get the DutyPe Android App"
              description="Enjoy the full experience: real-time location matching, 1-tap instant application, direct employer calling, and daily new job alerts."
              bullets={[
                "100% free for workers — zero hidden fees",
                "Instant call & in-app chat with employers",
                "Verified company profiles & salary details",
                "Daily alerts for nearby openings"
              ]}
              badge="Free on Google Play"
            />
          </div>
        </section>

        {/* FAQ */}
        <section className="section">
          <div className="section-header">
            <div>
              <span className="tag">FAQs</span>
              <h2>Frequently Asked Questions</h2>
            </div>
            <p>
              Common questions from job seekers and employers about DutyPe.
            </p>
          </div>

          <div className="faq-stack">
            {homeFaqs.map((item) => (
              <details key={item.question} className="faq-item faq-item-enhanced">
                <summary>{item.question}</summary>
                <p>{item.answer}</p>
              </details>
            ))}
          </div>
        </section>
      </div>
    </SiteShell>
  );
}
