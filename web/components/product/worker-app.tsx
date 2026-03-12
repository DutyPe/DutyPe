"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  arrayRemove,
  arrayUnion,
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
  limit,
  orderBy,
  query,
  setDoc,
  updateDoc,
  where
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  getOrCreateConversationId,
  productConversationRoute
} from "@/lib/firebase/chat-actions";
import { formatCurrencyRange, formatDateTime } from "@/lib/firebase/firestore-helpers";
import {
  attachJobDistances,
  directionsUrl,
  filterJobsByRadius,
  formatDistanceLabel,
  jobDistanceKm,
  workerLocationFromProfile
} from "@/lib/product/location";
import {
  defaultWorkerName,
  isLiveJob,
  missingWorkerFields,
  normalizeProductApplication,
  normalizeProductJob,
  productStatusLabel,
  productStatusTone,
  sortByTimestampDesc,
  workerProfileCompletion,
  type ProductApplication,
  type ProductApplicationStatus,
  type ProductJob
} from "@/lib/product/marketplace";
import type { ProductUserProfile } from "@/lib/product/profile";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type WorkerJobDetailClientProps = SharedProps & {
  jobId: string;
};

type WorkerProfileForm = {
  address: string;
  bio: string;
  dateOfBirth: string;
  experience: string;
  fullName: string;
  gender: string;
  phone: string;
  skills: string;
};

const initialProfileForm: WorkerProfileForm = {
  address: "",
  bio: "",
  dateOfBirth: "",
  experience: "",
  fullName: "",
  gender: "",
  phone: "",
  skills: ""
};

const workerDistanceOptions = [
  { label: "All distances", value: "ALL" },
  { label: "Within 500 m", value: "0.5" },
  { label: "Within 1 km", value: "1" },
  { label: "Within 2 km", value: "2" },
  { label: "Within 5 km", value: "5" },
  { label: "Within 10 km", value: "10" }
] as const;

async function toggleSavedJob(session: ProductSession, jobId: string) {
  const services = getFirebaseServices();

  if (!services || !session.user) {
    return;
  }

  const savedJobs = session.profile?.savedJobs ?? [];
  await updateDoc(doc(services.db, "users", session.user.uid), {
    savedJobs: savedJobs.includes(jobId) ? arrayRemove(jobId) : arrayUnion(jobId),
    updatedAt: Date.now()
  });

  await session.refreshProfile();
}

async function getJobDocument(jobId: string): Promise<ProductJob | null> {
  const services = getFirebaseServices();

  if (!services) {
    return null;
  }

  const byId = await getDoc(doc(services.db, "jobs", jobId));
  if (byId.exists()) {
    return normalizeProductJob(byId.id, byId.data() as Record<string, unknown>);
  }

  const byField = await getDocs(
    query(collection(services.db, "jobs"), where("jobId", "==", jobId), limit(1))
  );

  if (byField.empty) {
    return null;
  }

  const snapshot = byField.docs[0];
  return normalizeProductJob(snapshot.id, snapshot.data() as Record<string, unknown>);
}

function buildWorkerProfilePayload(
  profile: ProductUserProfile | null,
  form: WorkerProfileForm,
  availableRoles: string[]
) {
  const mergedProfile: ProductUserProfile = {
    ...profile,
    ...form,
    activeRole: "WORKER",
    fullName: form.fullName.trim(),
    name: form.fullName.trim() || (profile?.name ?? ""),
    phone: form.phone.trim(),
    role: "WORKER",
    roles: [...new Set([...availableRoles, "WORKER"])]
  };

  return {
    ...mergedProfile,
    profileCompleted: workerProfileCompletion(mergedProfile) >= 80,
    updatedAt: Date.now()
  };
}

function WorkerJobCard({
  actionHref,
  busy,
  directionsHref,
  distanceLabel,
  job,
  onToggleSave,
  saved
}: {
  actionHref: string;
  busy?: boolean;
  directionsHref?: string | null;
  distanceLabel?: string | null;
  job: ProductJob;
  onToggleSave?: (jobId: string) => Promise<void>;
  saved: boolean;
}) {
  return (
    <article className="card market-card">
      <div className="market-card-head">
        <div>
          <span className="card-kicker">{job.category || "LOCAL JOB"}</span>
          <h3>{job.title}</h3>
        </div>
        <span className={`status-pill ${job.isFilled ? "danger" : "success"}`}>
          {job.isFilled ? "Filled" : "Open"}
        </span>
      </div>

      <p className="market-card-subtitle">{job.companyName || "DutyPe employer"}</p>
      <p className="market-card-copy">
        {job.description?.trim()
          ? job.description.trim()
          : "Live DutyPe job card with company, pay, shift, and application flow details."}
      </p>

      <div className="market-card-meta">
        <div className="market-meta-item">
          <span>Location</span>
          <strong>{job.location || "Location pending"}</strong>
        </div>
        <div className="market-meta-item">
          <span>Pay</span>
          <strong>{formatCurrencyRange(job.payAmount, job.payType)}</strong>
        </div>
        <div className="market-meta-item">
          <span>Shift</span>
          <strong>{job.shiftTiming || "Shift timing pending"}</strong>
        </div>
      </div>

      <div className="pill-row">
        <span className="pill">{job.vacancies} vacancies</span>
        <span className="pill">{job.employerTrustTier || "NEW"} trust tier</span>
        {distanceLabel ? <span className="pill">{distanceLabel}</span> : null}
      </div>

      <div className="button-row compact market-card-actions">
        <Link href={actionHref} className="button">
          View details
        </Link>
        {directionsHref ? (
          <a href={directionsHref} target="_blank" rel="noreferrer" className="button ghost">
            Directions
          </a>
        ) : null}
        {onToggleSave ? (
          <button
            type="button"
            className="button ghost"
            disabled={busy}
            onClick={() => void onToggleSave(job.id)}
          >
            {saved ? "Unsave" : "Save"}
          </button>
        ) : null}
      </div>
    </article>
  );
}

function WorkerApplicationCard({
  application
}: {
  application: ProductApplication;
}) {
  const latestUpdate =
    application.statusHistory[application.statusHistory.length - 1]?.notes ||
    "Application is active in the DutyPe workflow.";

  return (
    <article className="card application-card">
      <div className="market-card-head">
        <div>
          <span className="card-kicker">Application</span>
          <h3>{application.jobTitle || "Untitled job"}</h3>
        </div>
        <span className={`status-pill ${productStatusTone(application.status)}`}>
          {productStatusLabel(application.status)}
        </span>
      </div>

      <p className="market-card-subtitle">{application.companyName || "DutyPe employer"}</p>
      <p className="market-card-copy">{latestUpdate}</p>

      <div className="market-card-meta">
        <div className="market-meta-item">
          <span>Applied</span>
          <strong>{formatDateTime(application.appliedAt)}</strong>
        </div>
        <div className="market-meta-item">
          <span>Source</span>
          <strong>{application.source || "DutyPe app flow"}</strong>
        </div>
        <div className="market-meta-item">
          <span>Job route</span>
          <strong>{application.jobLocation || "Location in listing"}</strong>
        </div>
      </div>
    </article>
  );
}

export function WorkerDashboardClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<ProductJob[]>([]);
  const [applications, setApplications] = useState<ProductApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadDashboard() {
      try {
        setLoading(true);
        setError(null);

        const jobsSnapshot = await getDocs(
          query(collection(activeServices.db, "jobs"), orderBy("createdAt", "desc"), limit(12))
        );
        const applicationsSnapshot = await getDocs(
          query(
            collection(activeServices.db, "job_applications"),
            where("workerId", "==", activeUser.uid)
          )
        );

        if (cancelled) {
          return;
        }

        const recentJobs = jobsSnapshot.docs
          .map((snapshot) =>
            normalizeProductJob(snapshot.id, snapshot.data() as Record<string, unknown>)
          )
          .filter(isLiveJob)
          .slice(0, 6);

        const recentApplications = sortByTimestampDesc(
          applicationsSnapshot.docs.map((snapshot) =>
            normalizeProductApplication(snapshot.id, snapshot.data() as Record<string, unknown>)
          ),
          "appliedAt"
        ).slice(0, 5);

        setJobs(recentJobs);
        setApplications(recentApplications);
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load worker dashboard.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      cancelled = true;
    };
  }, [services, session.user]);

  const completion = workerProfileCompletion(session.profile);
  const missingFields = missingWorkerFields(session.profile);
  const savedCount = session.profile?.savedJobs?.length ?? 0;
  const workerLocation = useMemo(
    () => workerLocationFromProfile(session.profile),
    [session.profile]
  );
  const recommendedJobs = useMemo(
    () => attachJobDistances(jobs, workerLocation).slice(0, 6),
    [jobs, workerLocation]
  );

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Profile completion</span>
            <strong>{completion}%</strong>
          </div>
          <div className="product-summary-card">
            <span>Saved jobs</span>
            <strong>{savedCount}</strong>
          </div>
          <div className="product-summary-card">
            <span>Applications</span>
            <strong>{applications.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Discovery base</span>
            <strong>{workerLocation?.label || "Set worker location"}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Next best move</span>
            <strong>{completion < 100 ? "Complete profile first" : "Start applying fast"}</strong>
            <p>
              {completion < 100
                ? "A stronger worker profile improves readiness before employers review your application."
                : "Your profile is in good shape. Move through jobs, save promising openings, and apply quickly."}
            </p>
          </article>

          <article className="product-summary-card">
            <span>Focus now</span>
            <strong>{savedCount > 0 ? "Review saved jobs" : "Build a shortlist"}</strong>
            <p>
              {savedCount > 0
                ? "Your saved list already has candidates. Check pay, shift, and trust tier before applying."
                : "Save jobs that look real, nearby, and clearly paid so you can compare them later."}
            </p>
          </article>
        </div>

        <div className="button-row">
          <Link href="/app/worker/jobs" className="button">
            Browse all jobs
          </Link>
          <Link href="/app/worker/map" className="button ghost">
            Open map
          </Link>
          <Link href="/app/worker/my-jobs" className="button ghost">
            Open my jobs
          </Link>
          <Link href="/app/worker/location" className="button ghost">
            Set location
          </Link>
          <Link href="/app/worker/profile" className="button ghost">
            Complete profile
          </Link>
        </div>

        {!workerLocation ? (
          <div className="callout">
            Save your worker location to unlock nearby sorting and directions across the jobs flow.
          </div>
        ) : null}
        {missingFields.length > 0 ? (
          <div className="callout">Worker profile still missing: {missingFields.join(", ")}.</div>
        ) : null}

        {error ? <div className="callout">Dashboard error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Worker home</span>
            <h2>Recommended jobs</h2>
          </div>
          <p>
            The Android worker home centers nearby job discovery. This web slice now ranks jobs
            against your saved worker location when available.
          </p>
        </div>

        {loading ? (
          <div className="empty-state">Loading live jobs from Firestore.</div>
        ) : recommendedJobs.length === 0 ? (
          <div className="empty-state">No active jobs are available yet.</div>
        ) : (
          <div className="section-grid">
            {recommendedJobs.map(({ distanceKm, job }) => (
              <WorkerJobCard
                key={job.id}
                actionHref={`/app/worker/jobs/${job.id}`}
                directionsHref={directionsUrl(job)}
                distanceLabel={formatDistanceLabel(distanceKm)}
                job={job}
                saved={(session.profile?.savedJobs ?? []).includes(job.id)}
              />
            ))}
          </div>
        )}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">My activity</span>
            <h2>Latest applications</h2>
          </div>
          <p>Application history is loaded from the same `job_applications` collection used by Android.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading your job applications.</div>
        ) : applications.length === 0 ? (
          <div className="empty-state">You have not applied to any jobs yet.</div>
        ) : (
          <div className="application-card-grid">
            {applications.map((application) => (
              <WorkerApplicationCard key={application.id} application={application} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function WorkerJobsClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<ProductJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("ALL");
  const [distanceFilter, setDistanceFilter] = useState("5");
  const [busyJobId, setBusyJobId] = useState<string | null>(null);

  useEffect(() => {
    if (!services) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    let cancelled = false;

    async function loadJobs() {
      try {
        setLoading(true);
        setError(null);

        const snapshot = await getDocs(
          query(collection(activeServices.db, "jobs"), orderBy("createdAt", "desc"), limit(48))
        );

        if (cancelled) {
          return;
        }

        const liveJobs = snapshot.docs
          .map((item) => normalizeProductJob(item.id, item.data() as Record<string, unknown>))
          .filter(isLiveJob);

        setJobs(liveJobs);
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load jobs.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadJobs();

    return () => {
      cancelled = true;
    };
  }, [services]);

  const workerLocation = useMemo(
    () => workerLocationFromProfile(session.profile),
    [session.profile]
  );
  const categories = ["ALL", ...new Set(jobs.map((job) => job.category).filter(Boolean))];
  const radiusKm =
    distanceFilter === "ALL" || !workerLocation ? null : Number(distanceFilter);
  const filteredJobs = useMemo(() => {
    const searchedJobs = jobs.filter((job) => {
      const queryText = `${job.title} ${job.companyName} ${job.location}`.toLowerCase();
      const matchesSearch = !search.trim() || queryText.includes(search.trim().toLowerCase());
      const matchesCategory = category === "ALL" || job.category === category;
      return matchesSearch && matchesCategory;
    });

    return filterJobsByRadius(attachJobDistances(searchedJobs, workerLocation), radiusKm);
  }, [category, jobs, radiusKm, search, workerLocation]);

  async function handleToggleSave(jobId: string) {
    try {
      setBusyJobId(jobId);
      await toggleSavedJob(session, jobId);
    } finally {
      setBusyJobId(null);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Live jobs</span>
            <strong>{filteredJobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Saved jobs</span>
            <strong>{session.profile?.savedJobs?.length ?? 0}</strong>
          </div>
          <div className="product-summary-card">
            <span>Data source</span>
            <strong>Firestore</strong>
          </div>
          <div className="product-summary-card">
            <span>Worker location</span>
            <strong>{workerLocation?.label || "Not saved"}</strong>
          </div>
        </div>

        <div className="product-top-grid">
          <div className="filter-row">
            <label className="inline-field">
              <span>Search jobs</span>
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Delivery, helper, cashier"
              />
            </label>

            <label className="inline-field">
              <span>Category</span>
              <select value={category} onChange={(event) => setCategory(event.target.value)}>
                {categories.map((item) => (
                  <option key={item} value={item}>
                    {item === "ALL" ? "All categories" : item}
                  </option>
                ))}
              </select>
            </label>

            <label className="inline-field">
              <span>Distance</span>
              <select
                value={distanceFilter}
                disabled={!workerLocation}
                onChange={(event) => setDistanceFilter(event.target.value)}
              >
                {workerDistanceOptions.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="pill-row">
            <span className="pill">{filteredJobs.length} live jobs</span>
            <span className="pill">{session.profile?.savedJobs?.length ?? 0} saved</span>
            <span className="pill">
              {workerLocation ? `Nearby base: ${workerLocation.label}` : "Realtime Firestore listings"}
            </span>
          </div>
        </div>

        {!workerLocation ? (
          <div className="callout">
            Save a worker location first to unlock nearby sorting and radius filters.
            {" "}
            <Link href="/app/worker/location">Set location</Link>
          </div>
        ) : null}
        <div className="button-row compact">
          <Link href="/app/worker/map" className="button ghost">
            Open nearby map
          </Link>
        </div>
        {error ? <div className="callout">Jobs error: {error}</div> : null}
      </section>

      <section className="section">
        {loading ? (
          <div className="empty-state">Loading live DutyPe jobs.</div>
        ) : filteredJobs.length === 0 ? (
          <div className="empty-state">No jobs matched the current search.</div>
        ) : (
          <div className="section-grid">
            {filteredJobs.map(({ distanceKm, job }) => (
              <WorkerJobCard
                key={job.id}
                actionHref={`/app/worker/jobs/${job.id}`}
                busy={busyJobId === job.id}
                directionsHref={directionsUrl(job)}
                distanceLabel={formatDistanceLabel(distanceKm)}
                job={job}
                onToggleSave={handleToggleSave}
                saved={(session.profile?.savedJobs ?? []).includes(job.id)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function WorkerJobDetailClient({ jobId, session }: WorkerJobDetailClientProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [job, setJob] = useState<ProductJob | null>(null);
  const [hasApplied, setHasApplied] = useState(false);
  const [coverLetter, setCoverLetter] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [saving, setSaving] = useState(false);
  const [openingChat, setOpeningChat] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadJob() {
      try {
        setLoading(true);
        setError(null);

        const [jobRecord, applicationSnapshot] = await Promise.all([
          getJobDocument(jobId),
          getDocs(
            query(
              collection(activeServices.db, "job_applications"),
              where("workerId", "==", activeUser.uid)
            )
          )
        ]);

        if (cancelled) {
          return;
        }

        if (!jobRecord) {
          setError("Job not found.");
          setJob(null);
          return;
        }

        setJob(jobRecord);
        setHasApplied(
          applicationSnapshot.docs.some((snapshot) => snapshot.get("jobId") === jobId)
        );
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load the selected job.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadJob();

    return () => {
      cancelled = true;
    };
  }, [jobId, services, session.user]);

  async function handleToggleSave() {
    if (!job) {
      return;
    }

    try {
      setSaving(true);
      await toggleSavedJob(session, job.id);
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Unable to update saved jobs.");
    } finally {
      setSaving(false);
    }
  }

  async function handleMessageEmployer() {
    if (!job?.employerId) {
      setError("Employer info is not available for chat.");
      return;
    }

    try {
      setOpeningChat(true);
      setError(null);

      const conversationId = await getOrCreateConversationId(job.employerId, job.jobId || job.id);
      router.push(productConversationRoute("WORKER", conversationId));
    } catch (conversationError) {
      setError(
        conversationError instanceof Error
          ? conversationError.message
          : "Failed to open employer chat."
      );
    } finally {
      setOpeningChat(false);
    }
  }

  async function handleApply() {
    if (!services || !session.user || !job || hasApplied) {
      return;
    }

    const activeServices = services;
    const activeUser = session.user;

    try {
      setSubmitting(true);
      setError(null);

      const applicationRef = doc(collection(activeServices.db, "job_applications"));
      const currentTime = Date.now();
      const workerName = defaultWorkerName(session.profile);

      await setDoc(applicationRef, {
        active: true,
        applicationId: applicationRef.id,
        appliedAt: currentTime,
        companyName: job.companyName,
        coverLetter: coverLetter.trim(),
        employerId: job.employerId,
        id: applicationRef.id,
        jobId: job.id,
        jobLocation: job.location,
        jobTitle: job.title,
        source: "WEB_PORTAL",
        status: "PENDING" as ProductApplicationStatus,
        statusHistory: [
          {
            notes: "Application submitted from web worker flow",
            status: "PENDING",
            systemUpdate: true,
            timestamp: currentTime,
            updatedAt: currentTime,
            updatedBy: activeUser.uid
          }
        ],
        updatedAt: currentTime,
        workerId: activeUser.uid,
        workerName
      });

      await updateDoc(doc(activeServices.db, "jobs", job.id), {
        applicationCount: increment(1),
        lastApplicationAt: currentTime,
        updatedAt: currentTime
      });

      setHasApplied(true);
    } catch (applyError) {
      setError(applyError instanceof Error ? applyError.message : "Application failed.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <div className="empty-state">Loading job details.</div>;
  }

  if (!job) {
    return <div className="empty-state">{error ?? "Job not found."}</div>;
  }

  const isSaved = (session.profile?.savedJobs ?? []).includes(job.id);
  const workerLocation = workerLocationFromProfile(session.profile);
  const distanceKm = jobDistanceKm(job, workerLocation);
  const directionsHref = directionsUrl(job);

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <span className="card-kicker">{job.category || "LOCAL JOB"}</span>
        <h3>{job.title}</h3>
        <p>{job.companyName || "DutyPe employer"}</p>

        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Location</span>
            <strong>{job.location || "Location pending"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Pay</span>
            <strong>{formatCurrencyRange(job.payAmount, job.payType)}</strong>
          </div>
          <div className="product-summary-card">
            <span>Shift</span>
            <strong>{job.shiftTiming || "Shift timing pending"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Status</span>
            <strong>{hasApplied ? "Already applied" : isLiveJob(job) ? "Ready to apply" : "Closed"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Distance</span>
            <strong>{formatDistanceLabel(distanceKm) || "Save worker location"}</strong>
          </div>
        </div>

        <p className="page-intro">
          {job.description || "This job is live in Firestore but does not have a long description yet."}
        </p>

        <div className="button-row">
          <button type="button" className="button ghost" disabled={saving} onClick={() => void handleToggleSave()}>
            {saving ? "Updating..." : isSaved ? "Unsave job" : "Save job"}
          </button>
          <button
            type="button"
            className="button ghost"
            disabled={openingChat || !job.employerId}
            onClick={() => void handleMessageEmployer()}
          >
            {openingChat ? "Opening chat..." : "Message employer"}
          </button>
          {directionsHref ? (
            <a href={directionsHref} target="_blank" rel="noreferrer" className="button ghost">
              Open directions
            </a>
          ) : null}
          <Link href="/app/worker/my-jobs" className="button ghost">
            View my jobs
          </Link>
          <Link href="/app/worker/location" className="button ghost">
            Update location
          </Link>
        </div>

        {error ? <div className="callout">Job flow error: {error}</div> : null}
      </section>

      <section className="detail-grid">
        <article className="detail-panel tone-highlight">
          <span className="card-kicker">Why this job stands out</span>
          <h3>Quick decision signals</h3>
          <ul className="detail-list">
            <li>
              <strong>{job.vacancies} open slot{job.vacancies > 1 ? "s" : ""}</strong>
              <span>Openings are still available right now.</span>
            </li>
            <li>
              <strong>{job.employerTrustTier || "NEW"} employer trust tier</strong>
              <span>Use this together with pay and location clarity before you apply.</span>
            </li>
            <li>
              <strong>{job.jobType || "FULL_TIME"} work pattern</strong>
              <span>Check whether the shift and work type fit your day-to-day schedule.</span>
            </li>
          </ul>
        </article>

        <article className="detail-panel">
          <span className="card-kicker">Before you apply</span>
          <h3>Keep the worker-first safety rules visible</h3>
          <ul className="detail-list">
            <li>
              <strong>Never pay for a job</strong>
              <span>Any request for money should be treated as suspicious immediately.</span>
            </li>
            <li>
              <strong>Verify location and shift</strong>
              <span>Make sure the area, travel time, and schedule are realistic for you.</span>
            </li>
            <li>
              <strong>Use the in-app flow</strong>
              <span>Applications, status changes, and future chat should stay tied to the product account.</span>
            </li>
          </ul>
        </article>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Apply flow</span>
            <h2>Apply like the Android worker route</h2>
          </div>
          <p>
            This creates a real document in `job_applications`, increments the job application
            count, and stores a status history entry.
          </p>
        </div>

        {hasApplied ? (
          <div className="callout">You have already applied for this job. Track it from the My Jobs route.</div>
        ) : (
          <div className="editor-form">
            <label className="editor-form-wide">
              <span>Cover letter</span>
              <textarea
                rows={6}
                value={coverLetter}
                onChange={(event) => setCoverLetter(event.target.value)}
                placeholder="Tell the employer why you are a fit for this role."
              />
            </label>

            <div className="editor-form-actions button-row">
              <button
                type="button"
                className="button"
                disabled={submitting || !isLiveJob(job)}
                onClick={() => void handleApply()}
              >
                {submitting ? "Submitting..." : "Apply now"}
              </button>
              {!isLiveJob(job) ? <div className="callout">This job is no longer open for applications.</div> : null}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

export function WorkerMyJobsClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [applications, setApplications] = useState<ProductApplication[]>([]);
  const [savedJobs, setSavedJobs] = useState<ProductJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyJobId, setBusyJobId] = useState<string | null>(null);
  const workerLocation = useMemo(
    () => workerLocationFromProfile(session.profile),
    [session.profile]
  );

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadMyJobs() {
      try {
        setLoading(true);
        setError(null);

        const applicationSnapshot = await getDocs(
          query(
            collection(activeServices.db, "job_applications"),
            where("workerId", "==", activeUser.uid)
          )
        );

        const savedJobIds = session.profile?.savedJobs ?? [];
        const savedSnapshots = await Promise.all(
          savedJobIds.map((jobId) => getDoc(doc(activeServices.db, "jobs", jobId)))
        );

        if (cancelled) {
          return;
        }

        setApplications(
          sortByTimestampDesc(
            applicationSnapshot.docs.map((snapshot) =>
              normalizeProductApplication(snapshot.id, snapshot.data() as Record<string, unknown>)
            ),
            "appliedAt"
          )
        );

        setSavedJobs(
          savedSnapshots
            .filter((snapshot) => snapshot.exists())
            .map((snapshot) => normalizeProductJob(snapshot.id, snapshot.data() as Record<string, unknown>))
        );
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load worker jobs.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadMyJobs();

    return () => {
      cancelled = true;
    };
  }, [services, session.profile?.savedJobs, session.user]);

  const locatedSavedJobs = useMemo(
    () => attachJobDistances(savedJobs, workerLocation),
    [savedJobs, workerLocation]
  );

  async function handleToggleSave(jobId: string) {
    try {
      setBusyJobId(jobId);
      await toggleSavedJob(session, jobId);
      setSavedJobs((current) => current.filter((job) => job.id !== jobId));
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Unable to update saved jobs.");
    } finally {
      setBusyJobId(null);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Applications</span>
            <strong>{applications.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Saved jobs</span>
            <strong>{savedJobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Current mode</span>
            <strong>Worker</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Applied jobs</span>
            <strong>Track status changes</strong>
            <p>Use this route to see whether jobs are pending, under review, accepted, live, completed, or rejected.</p>
          </article>

          <article className="product-summary-card">
            <span>Saved jobs</span>
            <strong>Compare before you apply</strong>
            <p>Keep promising jobs here until you are ready to compare pay, location, and trust cues.</p>
          </article>
        </div>
      </section>

      {error ? <div className="callout">My Jobs error: {error}</div> : null}

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Applications</span>
            <h2>Applied jobs</h2>
          </div>
          <p>These rows come from `job_applications` and match the worker-side application history flow.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading applied jobs.</div>
        ) : applications.length === 0 ? (
          <div className="empty-state">No applications yet.</div>
        ) : (
          <div className="application-card-grid">
            {applications.map((application) => (
              <WorkerApplicationCard key={application.id} application={application} />
            ))}
          </div>
        )}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Saved jobs</span>
            <h2>Saved for later</h2>
          </div>
          <p>Saved jobs are read from `users.savedJobs`, matching the optimized mobile storage model.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading saved jobs.</div>
        ) : savedJobs.length === 0 ? (
          <div className="empty-state">You have not saved any jobs yet.</div>
        ) : (
          <div className="section-grid">
            {locatedSavedJobs.map(({ distanceKm, job }) => (
              <WorkerJobCard
                key={job.id}
                actionHref={`/app/worker/jobs/${job.id}`}
                busy={busyJobId === job.id}
                directionsHref={directionsUrl(job)}
                distanceLabel={formatDistanceLabel(distanceKm)}
                job={job}
                onToggleSave={handleToggleSave}
                saved
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function WorkerProfileClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [form, setForm] = useState<WorkerProfileForm>(initialProfileForm);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setForm({
      address: session.profile?.address ?? "",
      bio: session.profile?.bio ?? "",
      dateOfBirth: session.profile?.dateOfBirth ?? "",
      experience: session.profile?.experience ?? "",
      fullName: session.profile?.fullName ?? session.profile?.name ?? "",
      gender: session.profile?.gender ?? "",
      phone: session.profile?.phone ?? "",
      skills: session.profile?.skills ?? ""
    });
  }, [session.profile]);

  const previewProfile = {
    ...session.profile,
    ...form,
    fullName: form.fullName,
    role: "WORKER"
  };
  const completion = workerProfileCompletion(previewProfile);
  const missingFields = missingWorkerFields(previewProfile);

  async function handleSave() {
    if (!services || !session.user) {
      return;
    }

    const activeServices = services;
    const activeUser = session.user;

    try {
      setSaving(true);
      setMessage(null);
      setError(null);

      const payload = buildWorkerProfilePayload(session.profile, form, session.availableRoles);

      await updateDoc(doc(activeServices.db, "users", activeUser.uid), payload);
      await setDoc(
        doc(activeServices.db, "worker_profiles", activeUser.uid),
        {
          address: payload.address ?? "",
          bio: payload.bio ?? "",
          dateOfBirth: payload.dateOfBirth ?? "",
          email: activeUser.email ?? payload.email ?? "",
          experience: payload.experience ?? "",
          fullName: payload.fullName ?? payload.name ?? "",
          gender: payload.gender ?? "",
          phone: payload.phone ?? "",
          profileCompleted: payload.profileCompleted ?? false,
          skills: payload.skills ?? "",
          updatedAt: payload.updatedAt ?? Date.now(),
          userId: activeUser.uid
        },
        { merge: true }
      );

      await session.refreshProfile();
      setMessage("Worker profile saved.");
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Failed to save worker profile.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Profile completion</span>
            <strong>{completion}%</strong>
          </div>
          <div className="product-summary-card">
            <span>Missing fields</span>
            <strong>{missingFields.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Referral code</span>
            <strong>{session.profile?.referralCode || "Pending"}</strong>
          </div>
        </div>

        {message ? <div className="callout">{message}</div> : null}
        {error ? <div className="callout">Profile error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Worker profile</span>
            <h2>Edit the fields Android uses for profile completion</h2>
          </div>
          <p>
            Saving here updates `users/{session.user?.uid}` and mirrors the worker data into
            `worker_profiles` for backward compatibility.
          </p>
        </div>

        <div className="detail-grid">
          <div className="detail-panel tone-highlight">
            <span className="card-kicker">Profile checklist</span>
            <h3>What improves worker readiness</h3>
            <ul className="detail-list">
              {missingFields.length > 0 ? (
                missingFields.map((field) => (
                  <li key={field}>
                    <strong>{field}</strong>
                    <span>Complete this field so employers have a clearer profile.</span>
                  </li>
                ))
              ) : (
                <li>
                  <strong>All key fields are filled</strong>
                  <span>Your worker profile is ready for job discovery and applications.</span>
                </li>
              )}
            </ul>
          </div>

          <div className="detail-panel">
            <span className="card-kicker">Current preview</span>
            <h3>{form.fullName || session.profile?.fullName || "Worker profile"}</h3>
            <p>{form.bio || "Add a short summary so employers understand your background quickly."}</p>
            <div className="pill-row">
              <span className="pill">{form.skills || "Skills pending"}</span>
              <span className="pill">{form.experience || "Experience pending"}</span>
              <span className="pill">{form.address || "Address pending"}</span>
            </div>
          </div>
        </div>

        <div className="editor-form">
          <label>
            <span>Full name</span>
            <input
              value={form.fullName}
              onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
            />
          </label>

          <label>
            <span>Phone</span>
            <input
              value={form.phone}
              onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
            />
          </label>

          <label>
            <span>Address</span>
            <input
              value={form.address}
              onChange={(event) => setForm((current) => ({ ...current, address: event.target.value }))}
            />
          </label>

          <label>
            <span>Date of birth</span>
            <input
              type="date"
              value={form.dateOfBirth}
              onChange={(event) => setForm((current) => ({ ...current, dateOfBirth: event.target.value }))}
            />
          </label>

          <label>
            <span>Gender</span>
            <select
              value={form.gender}
              onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}
            >
              <option value="">Select</option>
              <option value="Female">Female</option>
              <option value="Male">Male</option>
              <option value="Other">Other</option>
            </select>
          </label>

          <label>
            <span>Experience</span>
            <input
              value={form.experience}
              onChange={(event) => setForm((current) => ({ ...current, experience: event.target.value }))}
              placeholder="2 years in delivery and customer support"
            />
          </label>

          <label className="editor-form-wide">
            <span>Skills</span>
            <input
              value={form.skills}
              onChange={(event) => setForm((current) => ({ ...current, skills: event.target.value }))}
              placeholder="Delivery, customer handling, field work"
            />
          </label>

          <label className="editor-form-wide">
            <span>Bio</span>
            <textarea
              rows={5}
              value={form.bio}
              onChange={(event) => setForm((current) => ({ ...current, bio: event.target.value }))}
              placeholder="Short worker summary"
            />
          </label>

          <div className="editor-form-actions button-row">
            <button type="button" className="button" disabled={saving} onClick={() => void handleSave()}>
              {saving ? "Saving..." : "Save worker profile"}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
