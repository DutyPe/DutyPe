import Link from "next/link";
import type { ReactNode } from "react";

import { SiteIcon } from "@/components/site-icon";
import {
  PLAY_STORE_URL,
  jobCategoryHref,
  jobDirectoryCategories,
  jobDirectoryCities,
  type JobDirectoryCategory,
  type LegacyPageDescriptor
} from "@/lib/public-site";

export function JobSearchForm({ query = "", city = "" }: { query?: string; city?: string }) {
  return (
    <form action="/jobs" method="get" role="search" className="job-search" key={`${query}:${city}`}>
      <label className="search-field">
        <SiteIcon name="search" />
        <span className="search-field-content">
          <span>Job title or keyword</span>
          <input name="q" type="search" placeholder="Delivery, driver, cook..." defaultValue={query} maxLength={120} />
        </span>
      </label>
      <label className="search-field search-location">
        <SiteIcon name="map-pin" />
        <span className="search-field-content">
          <span>City or location</span>
          <select name="city" defaultValue={city}>
            <option value="">All cities</option>
            {jobDirectoryCities.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
        </span>
        <SiteIcon name="chevron-down" />
      </label>
      <button className="button search-submit" type="submit">
        <SiteIcon name="search" /> Find jobs
      </button>
    </form>
  );
}

export function CategoryGrid({
  categories = jobDirectoryCategories,
  city = ""
}: {
  categories?: readonly JobDirectoryCategory[];
  city?: string;
}) {
  return (
    <div className="category-grid">
      {categories.map((category) => (
        <Link className="category-tile" href={jobCategoryHref(category, city)} key={category.slug}>
          <div className="category-tile-top">
            <span className={`category-icon tone-${category.tone}`}><SiteIcon name={category.icon} /></span>
            <SiteIcon name="arrow-up-right" className="category-arrow" />
          </div>
          <h3>{category.name}</h3>
          <p>{category.description}</p>
        </Link>
      ))}
    </div>
  );
}

export function CityLinks({ compact = false, selectedCity = "", category }: { compact?: boolean; selectedCity?: string; category?: JobDirectoryCategory }) {
  const cities = compact ? jobDirectoryCities.slice(0, 6) : jobDirectoryCities;

  return (
    <div className="city-directory">
      {cities.map((city) => (
        <Link
          key={city}
          href={category ? jobCategoryHref(category, city) : `/jobs-in-${city.toLowerCase()}`}
          className={`city-link${city === selectedCity ? " selected" : ""}`}
          aria-current={city === selectedCity ? "true" : undefined}
        >
          <SiteIcon name="map-pin" />
          <span>{city}</span>
          <SiteIcon name="arrow-up-right" />
        </Link>
      ))}
    </div>
  );
}

export function JobCategoryGuide({ page, liveJobs }: { page: LegacyPageDescriptor; liveJobs?: ReactNode }) {
  const guide = page.category;
  if (!guide) return null;

  const category = jobDirectoryCategories.find((item) => item.slug === guide.slug);
  const otherCategories = jobDirectoryCategories.filter((item) => item.slug !== guide.slug).slice(0, 4);

  return (
    <div className="category-page">
      <nav className="resource-breadcrumb" aria-label="Breadcrumb">
        <Link href="/">Home</Link><SiteIcon name="arrow-right" />
        <Link href="/jobs">All jobs</Link><SiteIcon name="arrow-right" />
        {guide.city ? <><Link href={`/jobs-in-${guide.city.toLowerCase()}`}>Jobs in {guide.city}</Link><SiteIcon name="arrow-right" /></> : null}
        <span aria-current="page">{page.title}</span>
      </nav>

      <header className="category-page-header">
        <div className="category-title-row">
          <span className={`category-icon tone-${category?.tone ?? "mint"}`}><SiteIcon name={category?.icon ?? "briefcase-business"} /></span>
          <div><span className="section-label">A GUIDE TO LOCAL WORK</span><h1>{page.title}</h1></div>
          <span className="category-free-label"><SiteIcon name="check" />Free for workers</span>
        </div>
        <p>{page.intro}</p>
        <a href="#live-jobs" className="category-mobile-live text-link">View live jobs<SiteIcon name="arrow-right" /></a>
        <JobSearchForm query={guide.slug} city={guide.city ?? ""} />
      </header>

      {liveJobs}

      <div className="category-pay-band">
        <SiteIcon name="wallet" />
        <div><span className="section-label">INDICATIVE EARNINGS</span><strong>{guide.salary}</strong><p>Actual pay depends on the employer, location, hours, and incentives.</p></div>
      </div>

      <div className="category-page-layout">
        <div className="category-main-content">
          {page.blocks.map((block) => block.kind === "list" ? (
            <section key={block.title} className="category-content-section">
              <h2>{block.title}</h2>
              {block.intro ? <p>{block.intro}</p> : null}
              <ul className={block.title === "Common roles" ? "category-role-list" : "category-checklist"}>
                {block.items.map((item) => <li key={item}><SiteIcon name={block.title === "Common roles" ? category?.icon ?? "briefcase-business" : "check"} /><span>{item}</span></li>)}
              </ul>
            </section>
          ) : null)}

          <section className="category-content-section" aria-labelledby="category-cities-heading">
            <div className="category-section-heading"><h2 id="category-cities-heading">{category?.name ?? "Local work"} by city</h2><span>{jobDirectoryCities.length} cities</span></div>
            <CityLinks category={category} selectedCity={guide.city ?? ""} />
          </section>
        </div>

        <aside className="category-page-aside">
          <section className="category-live-action">
            <span className="section-label">YOUR NEXT OPPORTUNITY</span>
            <h2>Current openings</h2>
            <a href="#live-jobs" className="button">View live jobs<SiteIcon name="arrow-right" /></a>
          </section>
          <section className="category-safety-note">
            <SiteIcon name="shield-check" />
            <h2>Never pay to get hired.</h2>
            <p>Registration fees and security deposits are warning signs.</p>
            <Link href="/safety" className="text-link">Stay safe on DutyPe<SiteIcon name="arrow-right" /></Link>
          </section>
          <nav className="category-related" aria-label="Related categories">
            <span className="section-label">OTHER CATEGORIES</span>
            {otherCategories.map((item) => (
              <Link key={item.slug} href={jobCategoryHref(item, guide.city ?? "")}><SiteIcon name={item.icon} /><span>{item.name}</span><SiteIcon name="arrow-up-right" /></Link>
            ))}
            <Link href="/jobs" className="text-link">All categories<SiteIcon name="arrow-right" /></Link>
          </nav>
        </aside>
      </div>
    </div>
  );
}

export function WorkerAppActions() {
  return (
    <div className="button-row">
      <a href={PLAY_STORE_URL} className="button">Apply in the app<SiteIcon name="arrow-up-right" /></a>
      <a href={PLAY_STORE_URL} className="button ghost">Contact employer<SiteIcon name="messages-square" /></a>
    </div>
  );
}

export function CityJobGuide({ page, liveJobs }: { page: LegacyPageDescriptor; liveJobs?: ReactNode }) {
  if (!page.city) return null;
  return (
    <div className="jobs-directory-page">
      <nav className="resource-breadcrumb" aria-label="Breadcrumb">
        <Link href="/">Home</Link><SiteIcon name="arrow-right" /><Link href="/jobs">All jobs</Link><SiteIcon name="arrow-right" />
        <span aria-current="page">{page.title}</span>
      </nav>
      <header className="directory-page-header">
        <span className="section-label">DUTYPE LOCAL JOBS</span>
        <h1>{page.title}</h1>
        <p>{page.intro}</p>
        <JobSearchForm city={page.city} />
      </header>
      {liveJobs}
      <section className="discovery-section" aria-labelledby="city-category-heading">
        <h2 id="city-category-heading">Job categories in {page.city}</h2>
        <CategoryGrid city={page.city} />
      </section>
      <section className="discovery-section">
        <h2>Find work in {page.city}</h2>
        <p>Availability, pay and work locations vary by employer. Check current listings in the DutyPe Android app.</p>
        <a href="#live-jobs" className="button">View live jobs<SiteIcon name="arrow-right" /></a>
      </section>
      <section className="discovery-section">
        <h2>Before accepting a job in {page.city}</h2>
        <ul className="resource-list">
          <li>Confirm the exact work address and your travel time.</li>
          <li>Agree on pay, working hours and payment dates with the employer.</li>
          <li>Never pay a recruitment fee or security deposit to get a job.</li>
        </ul>
        <Link href="/safety" className="text-link">Worker safety<SiteIcon name="arrow-right" /></Link>
      </section>
      <section className="discovery-section">
        <h2>Hiring in {page.city}?</h2>
        <Link href="/app/employer/post-job" className="button">Post a job<SiteIcon name="plus" /></Link>
      </section>
      <section className="discovery-section" aria-labelledby="other-city-heading">
        <h2 id="other-city-heading">Explore jobs by city</h2>
        <CityLinks selectedCity={page.city} />
      </section>
    </div>
  );
}