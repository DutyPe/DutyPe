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
      <section className="discovery-intro">
        <div className="discovery-context">
          <span className="section-label"><SiteIcon name="map-pin" /> LOCAL WORK. REAL OPPORTUNITIES.</span>
          <Link href="/jobs#live-jobs" className="text-link">View live jobs <SiteIcon name="arrow-up-right" /></Link>
        </div>
        <h1>Local jobs. <span>Closer to home.</span></h1>
        <p>Your next opportunity could be just around the corner.</p>
        <JobSearchForm />
        <div className="popular-searches">
          <span>Popular searches:</span>
          <Link href="/jobs?q=delivery">Delivery</Link>
          <Link href="/jobs?q=driver">Driver</Link>
          <Link href="/jobs?q=part-time">Part-time</Link>
          <Link href="/jobs?q=helper">Helper</Link>
        </div>
        <div className="discovery-assurances">
          <span><SiteIcon name="check" /> Free for job seekers</span>
          <span><SiteIcon name="users" /> Direct employer connections</span>
          <Link href="/safety"><SiteIcon name="shield-check" /> Your safety comes first</Link>
        </div>
      </section>

      <section className="discovery-section" aria-labelledby="categories-heading">
        <div className="discovery-heading">
          <div>
            <span className="section-label">THERE&apos;S A PLACE FOR YOUR SKILLS</span>
            <h2 id="categories-heading">What kind of work suits you?</h2>
          </div>
          <Link href="/jobs" className="text-link">Search live openings <SiteIcon name="arrow-right" /></Link>
        </div>
        <CategoryGrid categories={jobDirectoryCategories} />
      </section>

      <section className="discovery-section local-work-section">
        <div>
          <div className="discovery-heading">
            <div><span className="section-label">LESS COMMUTE. MORE LIFE.</span><h2 id="cities-heading">Work in your city</h2></div>
          </div>
          <CityLinks />
        </div>
        <div className="employer-spotlight">
          <span className="section-label"><SiteIcon name="briefcase-business" /> FOR LOCAL BUSINESSES</span>
          <h2>Good people.<br />Right in your neighbourhood.</h2>
          <p>Find the next person for your shop, home, or growing team.</p>
          <Link href="/app/employer/post-job" className="button">Post a job <SiteIcon name="arrow-up-right" /></Link>
        </div>
      </section>

      <section className="safety-strip" aria-labelledby="safety-heading">
        <SiteIcon name="shield-check" />
        <div>
          <h2 id="safety-heading">Your next job shouldn&apos;t cost you.</h2>
          <p>Never pay a recruiter or employer to get hired. Finding work on DutyPe is free.</p>
        </div>
        <Link href="/safety" className="text-link">Stay safe <SiteIcon name="arrow-up-right" /></Link>
      </section>

      <section className="app-band" aria-labelledby="app-heading">
        <Image src="/dutype-logo.webp" alt="DutyPe Android app" width={72} height={72} />
        <div>
          <h2 id="app-heading">A world of local work. In your pocket.</h2>
          <p>DutyPe for Android. Always close by.</p>
        </div>
        <a href={PLAY_STORE_URL} className="button ghost" target="_blank" rel="noopener noreferrer"><SiteIcon name="smartphone" /> Get the app <SiteIcon name="arrow-up-right" /></a>
      </section>
    </SiteShell>
  );
}
