import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

import { CategoryGrid, CityLinks, JobSearchForm } from "@/components/public/job-discovery";
import { SiteIcon } from "@/components/site-icon";
import { SiteShell } from "@/components/site-shell";
import {
  PLAY_STORE_URL,
  coreSeoKeywords,
  jobDirectoryCategories,
  siteMeta
} from "@/lib/public-site";

export const metadata: Metadata = {
  title: "DutyPe - Find Daily Wage Work, Warehouse Jobs & Same-Day Pay Near You",
  description: "Find verified daily wage work, same-day warehouse placement, and instant local helper gigs near you. Direct hiring with 0% commission and guaranteed same-day pay.",
  keywords: [
    ...coreSeoKeywords,
    "how to find daily wage work",
    "how to work for daily pay",
    "how can warehouse workers find same-day placement jobs",
    "where can i find daily-paying shifts",
    "how to find gig work near me",
    "warehouse helper jobs",
    "daily pay jobs near me",
    "same-day pay jobs",
    "daily wage work near me",
    "home services khammam",
    "home services hyderabad",
    "instant local hiring",
    "dutype app"
  ],
  openGraph: {
    title: "DutyPe - Find Daily Wage Work, Warehouse Jobs & Same-Day Pay Near You",
    description: "Find verified daily wage work, same-day warehouse placement, and instant local helper gigs near you. Direct hiring with 0% commission.",
    type: "website"
  }
};

const FEATURED_SERVICES = [
  {
    id: "clean_deep",
    name: "House Sweeping & Deep Mopping",
    category: "Cleaning",
    offerPrice: 299,
    mrp: 399,
    discountPct: 25,
    duration: "60 mins",
    rating: "4.8 (1.2k)",
    tag: "Bestseller",
    badgeColor: "#dcfce7",
    textColor: "#15803d"
  },
  {
    id: "ac_jet",
    name: "AC Deep Clean & Filter Jet Wash",
    category: "AC Repair",
    offerPrice: 499,
    mrp: 699,
    discountPct: 28,
    duration: "45 mins",
    rating: "4.9 (3.4k)",
    tag: "Summer Special",
    badgeColor: "#e0f2fe",
    textColor: "#0369a1"
  },
  {
    id: "elec_fan",
    name: "Ceiling Fan / Switchboard Fix",
    category: "Electrician",
    offerPrice: 149,
    mrp: 199,
    discountPct: 25,
    duration: "30 mins",
    rating: "4.8 (980)",
    tag: "Essential",
    badgeColor: "#fef3c7",
    textColor: "#b45309"
  },
  {
    id: "plumb_tap",
    name: "Tap Leakage & Blockage Clear",
    category: "Plumber",
    offerPrice: 149,
    mrp: 199,
    discountPct: 25,
    duration: "30 mins",
    rating: "4.7 (810)",
    tag: "Quick Fix",
    badgeColor: "#f1f5f9",
    textColor: "#475569"
  },
  {
    id: "appl_ro",
    name: "RO Water Purifier Service & Filter",
    category: "Appliance",
    offerPrice: 299,
    mrp: 449,
    discountPct: 33,
    duration: "45 mins",
    rating: "4.9 (1.1k)",
    tag: "Popular",
    badgeColor: "#dcfce7",
    textColor: "#15803d"
  },
  {
    id: "veh_wash",
    name: "Car Foam Wash & Interior Vacuum",
    category: "Vehicle Care",
    offerPrice: 249,
    mrp: 349,
    discountPct: 28,
    duration: "40 mins",
    rating: "4.8 (640)",
    tag: "Doorstep",
    badgeColor: "#f3e8ff",
    textColor: "#7e22ce"
  }
];

const SERVICE_CATEGORIES = [
  { id: "cleaning", name: "Cleaning", icon: "sparkles", count: "12 services" },
  { id: "ac", name: "AC Repair", icon: "wind", count: "8 services" },
  { id: "electrician", name: "Electrician", icon: "zap", count: "14 services" },
  { id: "plumber", name: "Plumber", icon: "droplet", count: "11 services" },
  { id: "appliance", name: "Appliances", icon: "tv", count: "9 services" },
  { id: "carpenter", name: "Carpenter", icon: "hammer", count: "7 services" },
  { id: "painter", name: "Painting", icon: "brush", count: "6 services" },
  { id: "vehicle", name: "Vehicle Wash", icon: "car", count: "5 services" }
];


const HOME_FAQS = [
  {
    question: "How to find daily wage work?",
    answer: "Daily wage work can be found by directly connecting with local businesses, contractors, and retail shops looking for same-day labor. DutyPe eliminates the hassle of waiting at labor addas or dealing with middlemen by listing verified, daily-wage openings within 5 to 10 km of your location with upfront pay rates.",
    cta: "Download the DutyPe app on the Google Play Store today to browse active daily wage jobs and start working near your home."
  },
  {
    question: "How to work for daily pay?",
    answer: "Working for daily pay involves taking on short-term gigs, loading/unloading shifts, or helper tasks where wages are disbursed at the end of each completed shift. DutyPe connects you directly to verified local employers and warehouse hubs who guarantee instant, same-day payouts directly via cash or UPI.",
    cta: "Download the DutyPe app on the Google Play Store now to apply for daily-paying roles and secure your earnings today."
  },
  {
    question: "How to find gig work near me?",
    answer: "The quickest way to find gig work near you is to look for hyperlocal opportunities in delivery, loading, retail assistance, or event setups that match your current skills. DutyPe uses smart GPS-based matching to instantly show you verified on-demand shifts and gig tasks right in your neighborhood.",
    cta: "Download the DutyPe app on the Google Play Store to unlock flexible local gigs and start earning on your own schedule."
  },
  {
    question: "Where can I find daily-paying shifts?",
    answer: "Daily-paying shifts are commonly available across logistics hubs, distribution centers, supermarkets, and construction projects. DutyPe aggregates all verified daily-paying shifts in your area into a single live feed, allowing you to view transparent wages, shift timings, and employer details upfront.",
    cta: "Download the DutyPe app from the Google Play Store now to claim your first daily-paying shift today."
  },
  {
    question: "How can warehouse workers find same-day placement jobs in their area?",
    answer: "Finding same-day warehouse placement usually requires contacting multiple staffing agencies or visiting distant industrial zones in person. With DutyPe, warehouse workers can instantly discover verified packing, sorting, and loading shifts near them, skip lengthy paperwork, and get placed on the same day.",
    cta: "Download the DutyPe app on the Google Play Store today to secure same-day warehouse shifts near you."
  },
  {
    question: "How to find helpers or temporary workers near me?",
    answer: "Finding reliable temporary workers or helpers is fastest when you post immediate requirements to a hyper-local network of active daily laborers. DutyPe enables shop owners, warehouse supervisors, and event managers to post open requirements in under 60 seconds and hire background-verified helpers with zero commissions.",
    cta: "Download the DutyPe app from Google Play or post your requirement today to get matched with skilled local helpers within minutes."
  },
  {
    question: "How to apply at Pepu Ahmedabad (or local warehousing hubs)?",
    answer: "Applying for shifts at prominent warehousing clusters like Pepu in Ahmedabad or major distribution hubs typically requires direct recruiter links or on-site supervisor contacts. DutyPe partners with logistics employers and warehousing networks across major hubs to list instant entry-level helper, sorting, and packing shifts with zero brokerage fees.",
    cta: "Download the DutyPe app on the Google Play Store now to view open warehouse hub shifts in Ahmedabad and get hired immediately."
  },
  {
    question: "How to apply for warehouse helper jobs instantly?",
    answer: "To apply for warehouse helper jobs instantly without resume screenings or third-party recruiters, you need a direct-contact mobile hiring platform. DutyPe lets you apply for verified warehouse helper, loader, and inventory shifts with a single tap, connecting you directly with warehouse supervisors via call or WhatsApp.",
    cta: "Download the DutyPe app on the Google Play Store today to apply and start your warehouse job without delay."
  },
  {
    question: "Do part-time jobs offer daily payouts?",
    answer: "Yes, many part-time roles—especially in warehouse inventory handling, shop assistance, delivery, and event management—offer same-day or end-of-shift payouts. DutyPe specifically tags all part-time listings that offer daily payouts so you know exactly when and how much you will be paid before accepting a shift.",
    cta: "Download the DutyPe app on the Google Play Store now to discover part-time jobs with guaranteed daily pay."
  },
  {
    question: "How to get matched with local employers looking for immediate daily staff?",
    answer: "Getting matched with employers who need immediate daily staff requires a real-time platform that pairs active local job seekers with urgent hiring requests. DutyPe’s location-based matching algorithm connects you instantly with nearby shops, warehouses, and contractors actively hiring for daily shifts right now.",
    cta: "Download the DutyPe app on Google Play today and turn on availability to start receiving immediate daily job offers."
  }
];

const homeFaqSchema = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: HOME_FAQS.map((faq) => ({
    "@type": "Question",
    name: faq.question,
    acceptedAnswer: {
      "@type": "Answer",
      text: `${faq.answer} ${faq.cta}`
    }
  }))
};

export default function HomePage() {
  return (
    <SiteShell>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(homeFaqSchema).replace(/</g, "\\u003c") }}
      />
      {/* HERO SECTION: Dual Pillar (Home Services + Local Work) */}
      <section className="discovery-intro" style={{ textAlign: "center", maxWidth: 960, margin: "0 auto" }}>
        <div className="discovery-context" style={{ justifyContent: "center" }}>
          <span className="section-label" style={{ display: "inline-flex", alignItems: "center", gap: 6, fontWeight: 700 }}>
            <SiteIcon name="map-pin" /> NOW LIVE IN KHAMMAM &amp; HYDERABAD
          </span>
          <Link href={PLAY_STORE_URL} className="text-link" target="_blank" rel="noopener noreferrer">
            Get ₹35 Off First Booking <SiteIcon name="arrow-up-right" />
          </Link>
        </div>

        <h1 style={{ fontSize: "clamp(2rem, 5vw, 3.2rem)", fontWeight: 800, lineHeight: 1.15, margin: "16px 0 12px" }}>
          Expert Home Services. <br />
          <span style={{ color: "#087f68" }}>Delivered in 60 Minutes.</span>
        </h1>
        <p style={{ fontSize: "clamp(1rem, 2vw, 1.2rem)", color: "#475569", maxWidth: 680, margin: "0 auto 24px" }}>
          Doorstep cleaning, AC repair, electricians, and plumbers by verified pros with upfront fixed pricing. Plus, instant local hiring for shops, offices, and homes.
        </p>

        {/* Dual Primary CTA */}
        <div style={{ display: "flex", gap: 12, justifyContent: "center", flexWrap: "wrap", marginBottom: 28 }}>
          <a
            href={PLAY_STORE_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="button"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 8,
              padding: "14px 24px",
              fontSize: "1rem",
              fontWeight: 700,
              borderRadius: 8
            }}
          >
            <SiteIcon name="smartphone" /> Book on DutyPe App <SiteIcon name="arrow-up-right" />
          </a>
          <Link
            href="/jobs"
            className="button ghost"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: 8,
              padding: "14px 24px",
              fontSize: "1rem",
              fontWeight: 600,
              borderRadius: 8
            }}
          >
            Find Local Jobs <SiteIcon name="arrow-right" />
          </Link>
        </div>

        {/* Assurance Pills */}
        <div className="discovery-assurances" style={{ justifyContent: "center" }}>
          <span><SiteIcon name="check" /> 60-min doorstep arrival</span>
          <span><SiteIcon name="shield-check" /> 100% Police &amp; Skill Checked</span>
          <span><SiteIcon name="check" /> Pay after work is done</span>
          <span><SiteIcon name="users" /> 0% commission on jobs</span>
        </div>
      </section>

      {/* POPULAR HOME SERVICES (Urban Company / Pronto Style Showcase) */}
      <section className="discovery-section" style={{ marginTop: 24 }}>
        <div className="discovery-heading" style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", flexWrap: "wrap", gap: 12, marginBottom: 20 }}>
          <div>
            <span className="section-label" style={{ color: "#087f68", fontWeight: 700 }}>VERIFIED DOORSTEP PROFESSIONALS</span>
            <h2 style={{ fontSize: "1.75rem", fontWeight: 800, margin: "4px 0 0" }}>Trending Home Services</h2>
            <p style={{ color: "#64748b", margin: "4px 0 0", fontSize: 14 }}>Transparent fixed rates · Real MRP discounts · Zero surprise inspection charges</p>
          </div>
          <a href={PLAY_STORE_URL} target="_blank" rel="noopener noreferrer" className="text-link" style={{ fontWeight: 600 }}>
            View all 80+ services in app <SiteIcon name="arrow-up-right" />
          </a>
        </div>

        {/* Service Cards Grid */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 16 }}>
          {FEATURED_SERVICES.map((svc) => (
            <div
              key={svc.id}
              style={{
                background: "#ffffff",
                borderRadius: 14,
                border: "1px solid #e2e8f0",
                padding: 18,
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
                transition: "transform 0.15s ease, box-shadow 0.15s ease"
              }}
            >
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                  <span style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.5, color: "#64748b" }}>
                    {svc.category}
                  </span>
                  <span
                    style={{
                      background: svc.badgeColor,
                      color: svc.textColor,
                      fontSize: 10.5,
                      fontWeight: 700,
                      padding: "2px 8px",
                      borderRadius: 999
                    }}
                  >
                    {svc.tag}
                  </span>
                </div>

                <h3 style={{ fontSize: 16, fontWeight: 700, color: "#0f172a", margin: "0 0 6px", lineHeight: 1.3 }}>
                  {svc.name}
                </h3>

                <div style={{ display: "flex", alignItems: "center", gap: 10, fontSize: 12, color: "#64748b", marginBottom: 14 }}>
                  <span>★ {svc.rating}</span>
                  <span>•</span>
                  <span>⏱ {svc.duration}</span>
                </div>
              </div>

              <div style={{ borderTop: "1px solid #f1f5f9", paddingTop: 12, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
                    <span style={{ fontSize: 19, fontWeight: 800, color: "#0f172a" }}>₹{svc.offerPrice}</span>
                    <span style={{ fontSize: 13, color: "#94a3b8", textDecoration: "line-through" }}>₹{svc.mrp}</span>
                  </div>
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#16a34a" }}>{svc.discountPct}% OFF</span>
                </div>

                <a
                  href={PLAY_STORE_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{
                    background: "#0f172a",
                    color: "#ffffff",
                    padding: "8px 14px",
                    borderRadius: 8,
                    fontSize: 12.5,
                    fontWeight: 700,
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 4
                  }}
                >
                  Book Pro
                </a>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* WHY DUTYPE: 4 QUALITY PILLARS */}
      <section style={{ background: "#ffffff", borderRadius: 16, border: "1px solid #e2e8f0", padding: clampPad, margin: "24px 0" }}>
        <div style={{ textAlign: "center", maxWidth: 640, margin: "0 auto 28px" }}>
          <span className="section-label" style={{ color: "#087f68", fontWeight: 700 }}>THE DUTYPE PROMISE</span>
          <h2 style={{ fontSize: "1.8rem", fontWeight: 800, margin: "6px 0 8px" }}>Why 50,000+ Families Choose DutyPe</h2>
          <p style={{ color: "#64748b", fontSize: 14, margin: 0 }}>Built for reliability, speed, and safety in every neighbourhood.</p>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 20 }}>
          <div style={{ padding: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: "#ecfdf5", color: "#087f68", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, marginBottom: 12, fontWeight: 800 }}>
              ⚡
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>60-Min Doorstep Dispatch</h3>
            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, margin: 0 }}>Need help right now? Our nearest verified partner reaches your home in under an hour.</p>
          </div>

          <div style={{ padding: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: "#f0fdf4", color: "#16a34a", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, marginBottom: 12 }}>
              🛡️
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>100% Police &amp; Skill Checked</h3>
            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, margin: 0 }}>Government Aadhaar verification, background checks, and physical skill tests before approval.</p>
          </div>

          <div style={{ padding: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: "#eff6ff", color: "#0284c7", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, marginBottom: 12 }}>
              🏷️
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>Fixed Upfront Pricing</h3>
            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, margin: 0 }}>No haggling or surprise extra bills. What you see on the screen is exactly what you pay.</p>
          </div>

          <div style={{ padding: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 10, background: "#fef3c7", color: "#b45309", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, marginBottom: 12 }}>
              💳
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 700, margin: "0 0 6px" }}>Pay Only After Service</h3>
            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, margin: 0 }}>Inspect the work first. Pay seamlessly via UPI, QR, or cash only when you are 100% satisfied.</p>
          </div>
        </div>
      </section>

      {/* LOCAL JOBS & SEARCH (Second Pillar) */}
      <section className="discovery-section" id="jobs-section">
        <div className="discovery-heading">
          <div>
            <span className="section-label">DIRECT EMPLOYER CONNECTIONS · 0% COMMISSION</span>
            <h2>Looking for Work or Hiring Staff?</h2>
            <p style={{ color: "#64748b", fontSize: 14, margin: "4px 0 0" }}>Connecting local workers with shops, homes, offices, and warehouses.</p>
          </div>
          <Link href="/jobs" className="text-link">Explore all job openings <SiteIcon name="arrow-right" /></Link>
        </div>

        <JobSearchForm />

        <div className="popular-searches" style={{ marginTop: 12 }}>
          <span>Popular searches:</span>
          <Link href="/jobs?q=delivery">Delivery</Link>
          <Link href="/jobs?q=driver">Driver</Link>
          <Link href="/jobs?q=part-time">Part-time</Link>
          <Link href="/jobs?q=helper">Helper</Link>
          <Link href="/jobs?q=cook">Cook</Link>
          <Link href="/jobs?q=electrician">Electrician</Link>
        </div>
      </section>

      {/* JOB CATEGORIES GRID */}
      <section className="discovery-section" aria-labelledby="categories-heading">
        <div className="discovery-heading">
          <div>
            <span className="section-label">CHOOSE YOUR TRADE</span>
            <h2 id="categories-heading">Explore Work by Category</h2>
          </div>
          <Link href="/jobs" className="text-link">Search live openings <SiteIcon name="arrow-right" /></Link>
        </div>
        <CategoryGrid categories={jobDirectoryCategories} />
      </section>

      {/* CITIES & BUSINESS SPOTLIGHT */}
      <section className="discovery-section local-work-section">
        <div>
          <div className="discovery-heading">
            <div>
              <span className="section-label">LESS COMMUTE. MORE LIFE.</span>
              <h2 id="cities-heading">Work in your city</h2>
            </div>
          </div>
          <CityLinks />
        </div>
        <div className="employer-spotlight" style={{ background: "#0f172a", color: "#ffffff", borderRadius: 16 }}>
          <span className="section-label" style={{ color: "#38bdf8" }}><SiteIcon name="briefcase-business" /> FOR LOCAL BUSINESSES</span>
          <h2 style={{ color: "#ffffff" }}>Good people.<br />Right in your neighbourhood.</h2>
          <p style={{ color: "#94a3b8" }}>Hire trusted staff for your shop, warehouse, clinic, or home with direct contact.</p>
          <Link href={PLAY_STORE_URL} className="button" style={{ background: "#ffffff", color: "#0f172a" }}>
            Post a job <SiteIcon name="arrow-up-right" />
          </Link>
        </div>
      </section>

      {/* FREQUENTLY ASKED QUESTIONS SECTION (Optimized for Search Featured Snippets & Local Work) */}
      <section className="discovery-section" id="faq-section" aria-labelledby="faq-heading" style={{ marginTop: 36 }}>
        <div className="discovery-heading" style={{ textAlign: "center", maxWidth: 760, margin: "0 auto 28px" }}>
          <span className="section-label" style={{ color: "#087f68", fontWeight: 700 }}>
            CHOOSE WORK · SAME-DAY EARNINGS
          </span>
          <h2 id="faq-heading" style={{ fontSize: "clamp(1.6rem, 3.5vw, 2.2rem)", fontWeight: 800, margin: "8px 0 10px" }}>
            Frequently Asked Questions
          </h2>
          <p style={{ color: "#64748b", fontSize: "0.95rem", margin: 0, lineHeight: 1.6 }}>
            Quick, direct answers for job seekers looking for daily wage labor, warehouse placement, and instant local hiring.
          </p>
        </div>

        <div style={{ maxWidth: 840, margin: "0 auto", display: "grid", gap: 14 }}>
          {HOME_FAQS.map((faq, idx) => (
            <details
              key={faq.question}
              className="resource-faq-item"
              open={idx === 0}
              style={{
                background: "#ffffff",
                borderRadius: 12,
                border: "1px solid #e2e8f0",
                padding: "6px 18px",
                boxShadow: "0 1px 3px rgba(0,0,0,0.03)"
              }}
            >
              <summary style={{ cursor: "pointer", fontWeight: 700, fontSize: "1.05rem", color: "#0f172a", minHeight: 52 }}>
                <h3 style={{ fontSize: "1.02rem", fontWeight: 700, margin: 0, display: "inline" }}>
                  {faq.question}
                </h3>
                <SiteIcon name="chevron-down" />
              </summary>
              <div style={{ padding: "8px 4px 16px", color: "#334155", fontSize: "0.93rem", lineHeight: 1.65 }}>
                <p style={{ margin: "0 0 10px 0" }}>{faq.answer}</p>
                <div style={{ background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 8, padding: "10px 14px", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 8 }}>
                  <span style={{ color: "#166534", fontWeight: 600, fontSize: "0.88rem" }}>
                    {faq.cta}
                  </span>
                  <a
                    href={PLAY_STORE_URL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="button"
                    style={{
                      fontSize: "0.82rem",
                      padding: "6px 12px",
                      background: "#087f68",
                      color: "#ffffff",
                      borderRadius: 6,
                      fontWeight: 700,
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4
                    }}
                  >
                    Get App <SiteIcon name="arrow-up-right" />
                  </a>
                </div>
              </div>
            </details>
          ))}
        </div>
      </section>

      {/* SAFETY FIRST STRIP */}
      <section className="safety-strip" aria-labelledby="safety-heading">
        <SiteIcon name="shield-check" />
        <div>
          <h2 id="safety-heading">Your next job shouldn&apos;t cost you.</h2>
          <p>Never pay a recruiter or employer to get hired. Finding work on DutyPe is always 100% free.</p>
        </div>
        <Link href="/safety" className="text-link">Stay safe <SiteIcon name="arrow-up-right" /></Link>
      </section>

      {/* APP DOWNLOAD CTA BAND */}
      <section className="app-band" aria-labelledby="app-heading" style={{ background: "#0f172a", color: "#ffffff", borderRadius: 16 }}>
        <Image src="/dutype-logo.webp" alt="DutyPe Android app" width={72} height={72} style={{ borderRadius: 16 }} />
        <div>
          <h2 id="app-heading" style={{ color: "#ffffff" }}>A world of local services &amp; jobs. In your pocket.</h2>
          <p style={{ color: "#94a3b8" }}>DutyPe for Android. Always nearby, always verified.</p>
        </div>
        <a href={PLAY_STORE_URL} className="button ghost" target="_blank" rel="noopener noreferrer" style={{ borderColor: "#ffffff", color: "#ffffff" }}>
          <SiteIcon name="smartphone" /> Get Google Play App <SiteIcon name="arrow-up-right" />
        </a>
      </section>
    </SiteShell>
  );
}

const clampPad = "clamp(20px, 3vw, 36px)";
