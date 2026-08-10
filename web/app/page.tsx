import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

import { AppLaunchCard } from "@/components/public/app-launch-card";
import { HeroSignalDeck } from "@/components/public/hero-signal-deck";
import { HomeImmersiveLayer } from "@/components/public/home-immersive-layer";
import { Hero3D } from "@/components/public/hero-3d";
import { MotionLayer } from "@/components/public/motion-layer";
import { HomeInteractiveSuite } from "@/components/public/home-interactive-suite";
import { WorkCaseDeck } from "@/components/public/work-case-deck";
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

const audiencePanels = [
  {
    label: "Worker flow",
    title: "Search and apply in minutes",
    copy: "Workers can open nearby jobs, check pay, review employer details, and apply without forms or agent calls.",
    href: "/jobs-near-me",
    cta: "Browse jobs"
  },
  {
    label: "Employer flow",
    title: "Post openings and hire locally",
    copy: "Local businesses can publish openings, review applicants, and continue hiring conversations inside the app.",
    href: "/employer",
    cta: "See employer flow"
  },
  {
    label: "Referral flow",
    title: "Grow trust through referrals",
    copy: "DutyPe also supports referral-led growth so workers can invite others and help reliable opportunities travel faster.",
    href: "/refer",
    cta: "Explore referrals"
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

const trustPoints = [
  "Workers should never pay to access a job or attend an interview.",
  "DutyPe focuses on verified employers, clear pay expectations, and direct worker-employer contact.",
  "Support, safety, privacy, and refund policies are publicly indexed for transparency and trust."
];

const homeFaqs = [
  {
    question: "Is DutyPe free for workers?",
    answer:
      "Yes. Workers can browse jobs, apply, and connect with employers without paying fees or middlemen charges."
  },
  {
    question: "What kind of jobs can I find on DutyPe?",
    answer:
      "DutyPe focuses on local hiring categories like delivery, driver, maid, helper, cook, cleaner, warehouse, retail, and part-time jobs."
  },
  {
    question: "Can employers hire directly through DutyPe?",
    answer:
      "Yes. Employers can post openings, receive worker interest, and continue hiring communication inside the app."
  },
  {
    question: "Which cities does DutyPe support?",
    answer:
      "The platform is designed for local hiring across Indian cities, with dedicated search pages for major locations and category-based landing pages."
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

const interactiveCategoryTargets = allCategories.map(({ href, label }) => ({ href, label }));
const interactiveCityTargets = allCities.map((city) => ({
  href: `/jobs-in-${city.toLowerCase()}`,
  label: `Jobs in ${city}`
}));

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
    <SiteShell hideTopBar>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
      />

      <div className="home-interactive-experience home-editorial">
        <HomeImmersiveLayer />
        <MotionLayer />

      <section className="hero hero-home hero-editorial">
        <Hero3D />
        <div className="hero-grid hero-home-grid hero-editorial-grid">
          <div className="hero-copy">
            <div className="eyebrow-group">
              <span className="hero-brand-mark">
                <Image
                  src="/icon.webp"
                  alt="DutyPe"
                  width={40}
                  height={40}
                  priority
                />
                <span>DutyPe</span>
              </span>
              <span className="eyebrow">🇮🇳 Made in India</span>
              <span className="hero-note">Local job discovery and hiring for India</span>
            </div>

            <h1 className="headline">
              Find <span className="dp-accent-underline">Local Jobs</span> Near You
            </h1>

            <p className="lede">
              DutyPe helps workers discover verified nearby jobs and helps employers hire
              local staff faster. Browse delivery, driver, maid, cook, helper, cleaner,
              warehouse, retail, and part-time roles with clear location-first discovery.
            </p>

            <form className="hero-search" action="/jobs">
              <label>
                <span>What job?</span>
                <input name="q" type="search" placeholder="Delivery, driver, cook" />
              </label>
              <label>
                <span>Where?</span>
                <input name="city" type="search" placeholder="Hyderabad, Khammam" />
              </label>
              <button type="submit" className="button hero-search-button">
                Search Jobs
              </button>
            </form>

            <div className="button-row">
              <a href={PLAY_STORE_URL} className="button" target="_blank" rel="noopener noreferrer">
                Download App
              </a>
              <Link href="/jobs-near-me" className="button ghost">
                Jobs Near Me
              </Link>
              <Link href="/contact" className="button ghost">
                Contact Team
              </Link>
            </div>

            <div className="hero-chip-row">
              <span className="pill">No middlemen</span>
              <span className="pill">No worker fees</span>
              <span className="pill">Verified employers</span>
              <span className="pill">Instant apply</span>
              <span className="pill">Daily payments</span>
            </div>

            <div className="hero-proof-grid">
              <article className="hero-proof-card dp-glass dp-lift" data-reveal data-tilt>
                <span className="card-kicker">Search intent</span>
                <strong>Built around jobs near me</strong>
                <p>Category and city landing pages make it easier for workers to discover relevant openings fast.</p>
              </article>
              <article className="hero-proof-card dp-glass dp-lift" data-reveal data-tilt>
                <span className="card-kicker">Trust layer</span>
                <strong>Clear policies and direct contact</strong>
                <p>Safety, privacy, and support routes stay visible so the website feels legitimate and useful before app install.</p>
              </article>
            </div>
          </div>

          <div className="hero-rail">
            <div className="orb-stage">
              <HeroSignalDeck signals={heroSignals} />
            </div>
          </div>
        </div>
      </section>

      <WorkCaseDeck />

      <HomeInteractiveSuite
        categories={interactiveCategoryTargets}
        cities={interactiveCityTargets}
      />

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Platform overview</span>
            <h2>How DutyPe works for everyone</h2>
          </div>
          <p>
            Whether you are looking for work, hiring local staff, or growing
            through referrals — DutyPe has a clear path for you.
          </p>
        </div>

        <div className="spotlight-grid">
          {audiencePanels.map((panel) => (
            <article key={panel.title} className="spotlight-card">
              <span className="card-kicker">{panel.label}</span>
              <h3>{panel.title}</h3>
              <p>{panel.copy}</p>
              <Link href={panel.href} className="route-note route-note-link">
                {panel.cta}
              </Link>
            </article>
          ))}
        </div>

        <div className="quick-link-grid">
          {homeCategories.map((category) => (
            <Link key={category.href} href={category.href} className="mini-route-card">
              <strong>{category.label}</strong>
              <span>Open landing page</span>
            </Link>
          ))}
        </div>
      </section>

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

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Browse by city</span>
            <h2>Jobs in your city</h2>
          </div>
          <p>
            Find local openings in major Indian cities. Pick your city to see
            roles across all categories.
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
              <span>Open city landing page</span>
            </Link>
          ))}
        </div>

        <div className="pill-row city-pill-row">
          {allCities.map((city) => (
            <Link key={city} href={`/jobs-in-${city.toLowerCase()}`} className="pill pill-link">
              {city}
            </Link>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Trust &amp; transparency</span>
            <h2>Why workers and employers trust DutyPe</h2>
          </div>
          <p>
            Clear policies, verified employers, and a strict no-fee-for-workers promise
            make DutyPe a safer way to find and offer work.
          </p>
        </div>

        <div className="trust-grid">
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

          <article className="trust-panel">
            <span className="card-kicker">Trust layer</span>
            <h3>Visible support, safety, and company identity</h3>
            <ul className="trust-list">
              {trustPoints.map((point) => (
                <li key={point}>{point}</li>
              ))}
            </ul>
            <div className="hero-chip-row">
              <span className="pill">{siteMeta.companyName}</span>
              <span className="pill">{siteMeta.supportEmail}</span>
            </div>
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

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Frequently asked</span>
            <h2>Common questions about DutyPe</h2>
          </div>
          <p>
            Quick answers to what workers and employers ask us most.
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
