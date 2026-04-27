"use client";

import { useEffect, useMemo, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import { SITE_URL, siteMeta } from "@/lib/public-site";

type PosterSection = "jobs" | "workers" | "employers";

type PosterJob = {
  id: string;
  title?: string;
  companyName?: string;
  location?: { lat?: number; lng?: number } | string;
  addressText?: string;
  companyCity?: string;
  payAmount?: number | string;
  salary?: number | string;
  payType?: string;
  salaryType?: string;
  vacancies?: number;
  description?: string;
  category?: string;
  jobType?: string;
  shift?: string;
  shiftTiming?: string;
  experienceRequired?: string;
  gender?: string;
  contactNumber?: string;
  whatsappNumber?: string;
  status?: string;
};

type StaticPoster = {
  id: string;
  eyebrow: string;
  headline: string;
  subhead: string;
  bullets: string[];
  footerTitle: string;
  footerBody: string;
};

const workerPosters: StaticPoster[] = [
  {
    id: "worker-nearby-jobs",
    eyebrow: "For workers",
    headline: "Find local jobs near you",
    subhead: "Daily wage, monthly salary, part-time and full-time jobs in your area.",
    bullets: ["Free for workers", "Apply in seconds", "Delivery, helper, cook, driver, security and more"],
    footerTitle: "Download DutyPe",
    footerBody: "Open the app, complete your profile, and start applying today."
  },
  {
    id: "worker-no-middleman",
    eyebrow: "For workers",
    headline: "No middleman. No job fee.",
    subhead: "Talk directly with employers and apply to verified local openings.",
    bullets: ["Jobs around your location", "Direct employer contact", "Track application status"],
    footerTitle: "Start with DutyPe",
    footerBody: "Visit the app and search jobs within 1 km, 5 km, or 10 km."
  }
];

const employerPosters: StaticPoster[] = [
  {
    id: "employer-hire-fast",
    eyebrow: "For employers",
    headline: "Need workers fast?",
    subhead: "Post a local job on DutyPe and reach nearby workers quickly.",
    bullets: ["Post hiring requirements", "Review applicant profiles", "Accept or reject from one place"],
    footerTitle: "Hire with DutyPe",
    footerBody: "Create a job post and share the printable poster with nearby workers."
  },
  {
    id: "employer-local-hiring",
    eyebrow: "For employers",
    headline: "Hire nearby workers for your business",
    subhead: "Useful for shops, restaurants, PGs, offices, warehouses, and service teams.",
    bullets: ["Local worker reach", "Job posters for offline marketing", "Applications in the app"],
    footerTitle: "Post on DutyPe",
    footerBody: "Keep hiring simple: job details, pay, shift, location, and vacancies."
  }
];

function renderLocation(job: PosterJob): string {
  if (job.addressText?.trim()) return job.addressText;
  if (job.companyCity?.trim()) return job.companyCity;
  if (typeof job.location === "string" && job.location.trim()) return job.location;
  if (job.location && typeof job.location === "object") {
    const { lat, lng } = job.location;
    if (typeof lat === "number" && typeof lng === "number") return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
  }
  return "Location available in app";
}

function cleanSiteUrl() {
  return SITE_URL.replace(/^https?:\/\//, "").replace(/\/$/, "");
}

function jobDetails(job: PosterJob) {
  const details = [
    ["Pay", formatCurrencyRange(job.payAmount ?? job.salary, job.payType ?? job.salaryType)],
    ["Shift", job.shiftTiming || job.shift || "Flexible"],
    ["Location", renderLocation(job)],
    ["Vacancies", job.vacancies ? String(job.vacancies) : "Multiple openings"],
    ["Experience", job.experienceRequired || "Open to suitable workers"],
    ["Gender", job.gender || "Any"]
  ];

  if (job.contactNumber) details.push(["Contact", job.contactNumber]);

  return details;
}

function printIdFor(kind: PosterSection, id: string) {
  return `${kind}-${id}`;
}

export function AdminPostersClient({ initialJobId }: { initialJobId?: string }) {
  const [activeSection, setActiveSection] = useState<PosterSection>(initialJobId ? "jobs" : "jobs");
  const [jobs, setJobs] = useState<PosterJob[]>([]);
  const [selectedJobId, setSelectedJobId] = useState(initialJobId ?? "");
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [printingId, setPrintingId] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadJobs() {
      try {
        setLoading(true);
        const response = await adminApiFetch("/api/admin/jobs", { cache: "no-store" });
        const payload = (await response.json()) as { jobs?: PosterJob[]; error?: string };

        if (!response.ok) throw new Error(payload.error || "Failed to load jobs.");
        if (!isMounted) return;

        const rows = payload.jobs ?? [];
        setJobs(rows);
        setSelectedJobId((current) => current || rows[0]?.id || "");
        setError(null);
      } catch (loadError) {
        if (isMounted) setError(loadError instanceof Error ? loadError.message : "Failed to load jobs.");
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    void loadJobs();

    return () => {
      isMounted = false;
    };
  }, []);

  const filteredJobs = useMemo(() => {
    const needle = searchTerm.trim().toLowerCase();
    if (!needle) return jobs;
    return jobs.filter((job) => {
      return [job.title, job.companyName, renderLocation(job), job.jobType, job.category]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(needle));
    });
  }, [jobs, searchTerm]);

  const selectedJob = jobs.find((job) => job.id === selectedJobId) ?? filteredJobs[0] ?? jobs[0];

  function printPoster(targetId: string) {
    setPrintingId(targetId);
    document.body.classList.add("printing-poster");

    window.setTimeout(() => {
      const cleanup = () => {
        document.body.classList.remove("printing-poster");
        setPrintingId(null);
      };

      window.addEventListener("afterprint", cleanup, { once: true });
      window.print();
      window.setTimeout(cleanup, 1200);
    }, 60);
  }

  function renderJobPoster(job: PosterJob) {
    const targetId = printIdFor("jobs", job.id);
    const description = job.description?.trim() || "Job details are available in the DutyPe app.";

    return (
      <article className={`print-poster job-print-poster ${printingId === targetId ? "poster-print-target" : ""}`}>
        <div className="poster-brand-row">
          <span className="poster-brand-mark">DP</span>
          <div>
            <strong>DutyPe</strong>
            <span>Local jobs near you</span>
          </div>
        </div>

        <div className="poster-main-copy">
          <span className="poster-eyebrow">Hiring now</span>
          <h2>{job.title || "Job opening"}</h2>
          <p className="poster-company">{job.companyName || "Employer on DutyPe"}</p>
          <p className="poster-description">{description}</p>
        </div>

        <dl className="poster-detail-grid">
          {jobDetails(job).map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>

        <div className="poster-bottom-band">
          <div>
            <strong>Apply or share in DutyPe</strong>
            <span>{cleanSiteUrl()}/jobs/{job.id}</span>
          </div>
          <div className="poster-app-badge">Free app</div>
        </div>
      </article>
    );
  }

  function renderStaticPoster(kind: "workers" | "employers", poster: StaticPoster) {
    const targetId = printIdFor(kind, poster.id);

    return (
      <article className={`print-poster static-print-poster ${printingId === targetId ? "poster-print-target" : ""}`}>
        <div className="poster-brand-row">
          <span className="poster-brand-mark">DP</span>
          <div>
            <strong>DutyPe</strong>
            <span>{siteMeta.strapline}</span>
          </div>
        </div>

        <div className="poster-main-copy">
          <span className="poster-eyebrow">{poster.eyebrow}</span>
          <h2>{poster.headline}</h2>
          <p className="poster-description large">{poster.subhead}</p>
        </div>

        <ul className="poster-bullet-list">
          {poster.bullets.map((bullet) => (
            <li key={bullet}>{bullet}</li>
          ))}
        </ul>

        <div className="poster-bottom-band">
          <div>
            <strong>{poster.footerTitle}</strong>
            <span>{poster.footerBody}</span>
          </div>
          <div className="poster-app-badge">{cleanSiteUrl()}</div>
        </div>
      </article>
    );
  }

  return (
    <section className="poster-workspace">
      <aside className="poster-side-panel">
        <p className="poster-side-kicker">Poster types</p>
        <button
          className={`poster-side-link ${activeSection === "jobs" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveSection("jobs")}
        >
          <span>Posted job posters</span>
          <small>Print one poster per live job</small>
        </button>
        <button
          className={`poster-side-link ${activeSection === "workers" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveSection("workers")}
        >
          <span>Worker posters</span>
          <small>Bring job seekers to DutyPe</small>
        </button>
        <button
          className={`poster-side-link ${activeSection === "employers" ? "active" : ""}`}
          type="button"
          onClick={() => setActiveSection("employers")}
        >
          <span>Employer posters</span>
          <small>Bring businesses to DutyPe</small>
        </button>
      </aside>

      <div className="poster-main-panel">
        {activeSection === "jobs" && (
          <div className="poster-section-stack">
            <div className="poster-control-card">
              <div>
                <h2>Posted job poster</h2>
                <p>Select any posted job and print a clean A4 hiring poster for offline marketing.</p>
              </div>
              <div className="poster-job-controls">
                <input
                  className="admin-search"
                  type="text"
                  placeholder="Search job, company, or location"
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                />
                <select
                  className="admin-filter"
                  value={selectedJob?.id ?? ""}
                  onChange={(event) => setSelectedJobId(event.target.value)}
                  disabled={loading || filteredJobs.length === 0}
                >
                  {filteredJobs.map((job) => (
                    <option key={job.id} value={job.id}>
                      {job.title || "Untitled job"} - {job.companyName || "Company"}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {loading && <div className="admin-loading"><div className="admin-loading-spinner" /><p>Loading posted jobs...</p></div>}
            {error && <div className="admin-error">{error}</div>}
            {!loading && !selectedJob && <div className="admin-empty"><p>No jobs found for poster printing.</p></div>}
            {selectedJob && (
              <div className="poster-preview-shell">
                <div className="poster-preview-toolbar">
                  <strong>{selectedJob.title || "Job poster"}</strong>
                  <button className="button" type="button" onClick={() => printPoster(printIdFor("jobs", selectedJob.id))}>
                    Print job poster
                  </button>
                </div>
                {renderJobPoster(selectedJob)}
              </div>
            )}
          </div>
        )}

        {activeSection === "workers" && (
          <PosterTemplateGrid
            title="Worker posters"
            description="Printable posters for bringing local job seekers into DutyPe."
            posters={workerPosters}
            renderPoster={(poster) => renderStaticPoster("workers", poster)}
            onPrint={(poster) => printPoster(printIdFor("workers", poster.id))}
          />
        )}

        {activeSection === "employers" && (
          <PosterTemplateGrid
            title="Employer posters"
            description="Printable posters for convincing businesses to post jobs on DutyPe."
            posters={employerPosters}
            renderPoster={(poster) => renderStaticPoster("employers", poster)}
            onPrint={(poster) => printPoster(printIdFor("employers", poster.id))}
          />
        )}
      </div>
    </section>
  );
}

function PosterTemplateGrid({
  title,
  description,
  posters,
  renderPoster,
  onPrint
}: {
  title: string;
  description: string;
  posters: StaticPoster[];
  renderPoster: (poster: StaticPoster) => JSX.Element;
  onPrint: (poster: StaticPoster) => void;
}) {
  return (
    <div className="poster-section-stack">
      <div className="poster-control-card">
        <div>
          <h2>{title}</h2>
          <p>{description}</p>
        </div>
      </div>

      <div className="poster-template-grid">
        {posters.map((poster) => (
          <div className="poster-preview-shell" key={poster.id}>
            <div className="poster-preview-toolbar">
              <strong>{poster.headline}</strong>
              <button className="button" type="button" onClick={() => onPrint(poster)}>
                Print poster
              </button>
            </div>
            {renderPoster(poster)}
          </div>
        ))}
      </div>
    </div>
  );
}