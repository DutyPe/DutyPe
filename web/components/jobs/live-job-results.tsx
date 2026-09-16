"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";

import { SiteIcon } from "@/components/site-icon";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import { jobSearchParams, type JobSearch, type PublicJobPage, type PublicJobSummary } from "@/lib/jobs/public-listings";
import { jobDirectoryCategories, jobDirectoryCities } from "@/lib/public-site";

type Props = { initial: PublicJobPage; search: JobSearch; heading?: string; showFilters?: boolean };

function visibleJobs(jobs: PublicJobSummary[], now: number) {
  return jobs.filter((job) => job.expiresAt === null || job.expiresAt > now);
}

export function PublicJobCard({ job }: { job: PublicJobSummary }) {
  return (
    <article className="public-job-card">
      <div className="public-job-card-top"><span className="section-label">{job.category.replace(/_/g, " ") || "LOCAL WORK"}</span><span className="status-pill success">Open</span></div>
      <h3><Link href={`/jobs/${encodeURIComponent(job.id)}`}>{job.title}</Link></h3>
      <p>{job.companyName || "Employer details after sign-in"}</p>
      <dl className="public-job-facts">
        <div><dt><SiteIcon name="map-pin" /> Area</dt><dd>{[job.area, job.city].filter(Boolean).join(", ") || "Area not provided"}</dd></div>
        <div><dt><SiteIcon name="wallet" /> Pay</dt><dd>{formatCurrencyRange(job.payAmount, job.payType)}</dd></div>
        {job.jobType ? <div><dt>Work type</dt><dd>{job.jobType.toLowerCase().replace(/_/g, " ")}</dd></div> : null}
        {job.distanceKm !== undefined ? <div><dt>Distance</dt><dd>About {job.distanceKm} km</dd></div> : null}
      </dl>
      <div className="public-job-card-bottom">
        <span>{job.postedAt ? `Posted ${new Date(job.postedAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric", timeZone: "Asia/Kolkata" })}` : "Post date unavailable"}</span>
        <Link href={`/jobs/${encodeURIComponent(job.id)}`} className="text-link">View details<SiteIcon name="arrow-right" /></Link>
      </div>
    </article>
  );
}

export function LiveJobResults({ initial, search, heading = "Live jobs", showFilters = true }: Props) {
  const [result, setResult] = useState(initial);
  const [activeSearch, setActiveSearch] = useState(search);
  const [form, setForm] = useState(search);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [locating, setLocating] = useState(false);
  const [locationNotice, setLocationNotice] = useState<string | null>(null);
  const [now, setNow] = useState(0);
  const pending = useRef<AbortController | null>(null);
  const locationAttempt = useRef(0);
  const loadedMore = useRef(false);
  const latestSearch = useRef(search);
  const setupRequired = result.status === "unavailable" && result.unavailableReason === "setup-required";

  useEffect(() => {
    pending.current?.abort();
    pending.current = null;
    locationAttempt.current += 1;
    setBusy(false);
    setLocating(false);
    setError(null);
    setLocationNotice(null);
    setResult(initial);
    setNow(Date.now());
    setActiveSearch(search);
    setForm(search);
    latestSearch.current = search;
    loadedMore.current = false;
  }, [initial, search]);

  useEffect(() => () => { pending.current?.abort(); locationAttempt.current += 1; }, []);

  const load = useCallback(async (nextSearch: JobSearch, cursor: string | null = null, background = false) => {
    if (setupRequired) return;
    pending.current?.abort();
    const controller = new AbortController();
    pending.current = controller;
    setBusy(true);
    setError(null);
    if (!cursor) loadedMore.current = false;
    const timer = setTimeout(() => controller.abort(), 10_000);
    try {
      const params = jobSearchParams(nextSearch);
      if (cursor) params.set("cursor", cursor);
      const response = await fetch(nextSearch.location ? "/api/jobs" : `/api/jobs?${params}`, nextSearch.location ? {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ...Object.fromEntries(params), location: nextSearch.location }), signal: controller.signal, cache: "no-store"
      } : { signal: controller.signal, cache: "no-store" });
      const page = await response.json() as PublicJobPage;
      if (pending.current !== controller) return;
      if (response.status === 503 && page.status === "unavailable" && page.unavailableReason === "setup-required") {
        setResult({ status: "unavailable", unavailableReason: "setup-required", jobs: [], nextCursor: null, scanned: 0 });
        return;
      }
      if (!response.ok || page.status !== "ready" || !Array.isArray(page.jobs)) throw new Error("Live listings are temporarily unavailable.");
      setResult((previous) => ({ ...page, jobs: cursor ? [...new Map([...previous.jobs, ...page.jobs].map((job) => [job.id, job])).values()] : page.jobs }));
      setNow(Date.now());
      if (cursor) loadedMore.current = true;
    } catch {
      if (pending.current !== controller) return;
      if (!background) setResult((previous) => cursor ? previous : { status: "unavailable", jobs: [], nextCursor: null, scanned: 0 });
      setError(controller.signal.aborted ? "The job search took too long." : "Live listings are temporarily unavailable.");
    } finally {
      clearTimeout(timer);
      if (pending.current === controller) { pending.current = null; setBusy(false); }
    }
  }, [setupRequired]);

  useEffect(() => {
    if (setupRequired) return;
    const timer = setInterval(() => {
      setNow(Date.now());
      if (document.visibilityState === "visible" && !pending.current && !loadedMore.current) void load(latestSearch.current, null, true);
    }, 60_000);
    return () => clearInterval(timer);
  }, [load, setupRequired]);

  function applySearch(nextSearch: JobSearch) {
    if (setupRequired) return;
    latestSearch.current = nextSearch;
    setActiveSearch(nextSearch);
    setForm(nextSearch);
    setResult({ status: "ready", jobs: [], nextCursor: null, scanned: 0 });
    if (showFilters) {
      const params = jobSearchParams(nextSearch);
      window.history.replaceState(null, "", `/jobs${params.size ? `?${params}` : ""}`);
    }
    void load(nextSearch);
  }

  function searchJobs(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    locationAttempt.current += 1;
    setLocating(false);
    applySearch(form);
  }

  function useLocation() {
    if (locating) return;
    if (!navigator.geolocation) { setLocationNotice("Location is unavailable in this browser. Enter a city or area instead."); return; }
    const attempt = ++locationAttempt.current;
    setLocating(true);
    setLocationNotice(null);
    navigator.geolocation.getCurrentPosition((position) => {
      if (attempt !== locationAttempt.current) return;
      setLocating(false);
      setLocationNotice("Using your current location for this search.");
      applySearch({ ...form, city: showFilters ? "" : form.city, area: showFilters ? "" : form.area, location: {
        latitude: position.coords.latitude, longitude: position.coords.longitude, radiusKm: form.location?.radiusKm ?? 5
      } });
    }, (failure) => {
      if (attempt !== locationAttempt.current) return;
      setLocating(false);
      setLocationNotice(failure.code === 1 ? "Location permission was denied. You can still search by city or area." : "We couldn't determine your location. Try again or enter a city or area.");
    }, { enableHighAccuracy: false, timeout: 10_000, maximumAge: 60_000 });
  }

  const jobs = visibleJobs(result.jobs, now);
  const nextPage = result.nextCursor ? `/jobs?${new URLSearchParams({ ...Object.fromEntries(jobSearchParams(activeSearch)), cursor: result.nextCursor })}` : null;

  return (
    <section id="live-jobs" className="live-job-section" aria-labelledby="live-jobs-heading">
      {showFilters ? <header className="directory-page-header"><h1>{activeSearch.location ? "Jobs near you" : activeSearch.city ? `Jobs in ${activeSearch.city}` : "Browse local jobs"}</h1><p>Current openings by city, area and job type.</p></header> : null}
      <div className="discovery-heading"><h2 id="live-jobs-heading">{showFilters ? activeSearch.location ? "Nearby live jobs" : activeSearch.city ? `Live jobs in ${activeSearch.city}` : "Live jobs" : heading}</h2>{!setupRequired ? <button type="button" className="text-link" onClick={() => void load(activeSearch)} disabled={busy}><SiteIcon name="search" />Refresh jobs</button> : null}</div>
      {showFilters ? <form className="live-job-filters" role="search" aria-label="Search live jobs" onSubmit={searchJobs}>
        <label><span>Job title or keyword</span><input name="q" type="search" value={form.query} maxLength={120} onChange={(event) => setForm((current) => ({ ...current, query: event.target.value }))} placeholder="Delivery, driver, cook" /></label>
        <label><span>City</span><input name="city" list="live-job-cities" value={form.city} maxLength={80} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value, location: undefined }))} placeholder="Any city" /><datalist id="live-job-cities">{jobDirectoryCities.map((city) => <option key={city} value={city} />)}</datalist></label>
        <label><span>Area or locality</span><input name="area" value={form.area} maxLength={100} onChange={(event) => setForm((current) => ({ ...current, area: event.target.value, location: undefined }))} placeholder="Madhapur, Indiranagar..." /></label>
        <label><span>Category</span><select name="category" aria-label="Category" value={form.category} onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}>
          <option value="">All categories</option>
          {form.category && !jobDirectoryCategories.some((category) => category.slug === form.category) ? <option value={form.category}>{form.category.replace(/_/g, " ")}</option> : null}
          {jobDirectoryCategories.map((category) => <option key={category.slug} value={category.slug}>{category.name}</option>)}
        </select></label>
        <button type="submit" className="button" disabled={busy || setupRequired}>Find jobs<SiteIcon name="search" /></button>
      </form> : null}
      <div className="nearby-job-controls">
        <button type="button" className="button ghost" onClick={useLocation} disabled={locating || busy || setupRequired}><SiteIcon name="map-pin" />{locating ? "Finding location..." : "Use my location"}</button>
        {showFilters && (form.query || form.city || form.area || form.category || form.location) ? <button type="button" className="text-link" onClick={() => { locationAttempt.current += 1; setLocating(false); setLocationNotice(null); applySearch({ query: "", city: "", area: "", category: "" }); }}>Reset filters<SiteIcon name="x" /></button> : null}
        {form.location ? <>
          <label><span>Within</span><select aria-label="Distance from my location" value={form.location.radiusKm} onChange={(event) => applySearch({ ...form, location: { ...form.location!, radiusKm: Number(event.target.value) } })}>{[1, 2, 5, 10, 25, 50].map((radius) => <option key={radius} value={radius}>{radius} km</option>)}</select></label>
          <button type="button" className="text-link" onClick={() => { locationAttempt.current += 1; setLocationNotice(null); applySearch({ ...form, location: undefined }); }}>Clear location<SiteIcon name="x" /></button>
        </> : null}
      </div>
      {locationNotice ? <p className="location-feedback" role="status">{locationNotice}</p> : null}
      {!busy && (error || result.status === "unavailable") ? <div className="callout live-job-alert" role="alert">
        <p>{setupRequired ? "Live job listings aren't connected yet. Please check back later." : jobs.length ? "We couldn't refresh these listings. The results shown may be out of date." : error || "Live listings are temporarily unavailable."}</p>
        {setupRequired ? <Link href="/contact" className="text-link">Contact support<SiteIcon name="messages-square" /></Link> : <button type="button" className="button ghost" onClick={() => void load(activeSearch)}>Try again</button>}
      </div> : null}
      {busy || jobs.length > 0 || (result.status === "ready" && !error) ? <p className="live-job-count" role="status">{busy && !jobs.length ? "Finding live jobs..." : jobs.length ? `${jobs.length} ${jobs.length === 1 ? "opening" : "openings"} shown${activeSearch.city ? ` in ${activeSearch.city}` : ""}${activeSearch.area ? `, ${activeSearch.area}` : ""}` : result.nextCursor ? "No matches in this batch. Continue to search more listings." : "No current openings match this search."}</p> : null}
      {jobs.length ? <div className="public-job-grid">{jobs.map((job) => <PublicJobCard key={job.id} job={job} />)}</div> : null}
      {nextPage ? <Link href={nextPage} className="button ghost live-jobs-more" aria-disabled={busy} onClick={(event) => { event.preventDefault(); if (!busy) void load(activeSearch, result.nextCursor); }}>{busy ? "Loading..." : jobs.length ? "Load more jobs" : "Search more listings"}<SiteIcon name="arrow-right" /></Link> : null}
      {!jobs.length && result.status === "ready" && !busy ? <Link className="text-link" href="/jobs">Browse all locations<SiteIcon name="arrow-right" /></Link> : null}
    </section>
  );
}