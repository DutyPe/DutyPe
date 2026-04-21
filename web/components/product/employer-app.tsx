"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  setDoc,
  updateDoc,
  where
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatCurrencyRange, formatDateTime } from "@/lib/firebase/firestore-helpers";
import {
  employerBaseLocationFromProfile,
  hasValidCoordinates
} from "@/lib/product/location";
import type { ProductWorkLocation } from "@/lib/product/profile";
import {
  canEmployerAcceptOrReject,
  canEmployerMoveToUnderReview,
  canEditEmployerJob,
  defaultCompanyName,
  employerJobEditRestrictionMessage,
  employerProfileCompletion,
  isLiveJob,
  missingEmployerFields,
  normalizeProductApplication,
  normalizeProductJob,
  productStatusLabel,
  productStatusTone,
  sortByTimestampDesc,
  toStorageApplicationStatus,
  type ProductApplication,
  type ProductApplicationStatus,
  type ProductJob
} from "@/lib/product/marketplace";
import { createPendingWorkVerification } from "@/lib/product/work-verification";
import {
  incrementWorkLocationUsage,
  normalizeWorkLocations
} from "@/lib/product/work-locations";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type EmployerEditJobClientProps = SharedProps & {
  jobId: string;
};

type EmployerApplicationsClientProps = SharedProps & {
  backHref?: string;
  jobId?: string;
  jobTitle?: string | null;
};

type EmployerJobForm = {
  category: string;
  companyName: string;
  contactNumber: string;
  description: string;
  gender: string;
  jobType: string;
  latitude: string;
  location: string;
  longitude: string;
  payAmount: string;
  payType: string;
  shiftTiming: string;
  title: string;
  vacancies: string;
};

const initialJobForm: EmployerJobForm = {
  category: "",
  companyName: "",
  contactNumber: "",
  description: "",
  gender: "ANY",
  jobType: "FULL_TIME",
  latitude: "",
  location: "",
  longitude: "",
  payAmount: "",
  payType: "MONTHLY",
  shiftTiming: "",
  title: "",
  vacancies: "1"
};

function toCoordinateText(value: unknown): string {
  return typeof value === "number" && Number.isFinite(value) && value !== 0
    ? value.toFixed(6)
    : "";
}

function matchesEmployerLocationForm(
  form: EmployerJobForm,
  location: ProductWorkLocation | null | undefined
): boolean {
  if (!location?.id || !location.address?.trim()) {
    return false;
  }

  const latitude = Number(form.latitude);
  const longitude = Number(form.longitude);

  if (!hasValidCoordinates(latitude, longitude)) {
    return false;
  }

  return (
    form.location.trim().toLowerCase() === location.address.trim().toLowerCase() &&
    Math.abs(latitude - (location.latitude ?? 0)) < 0.000001 &&
    Math.abs(longitude - (location.longitude ?? 0)) < 0.000001
  );
}

function getBrowserPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      reject(new Error("Browser geolocation is not available on this device."));
      return;
    }

    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      maximumAge: 60_000,
      timeout: 15_000
    });
  });
}

function EmployerJobCard({
  action,
  actionLabel,
  applicantsHref,
  busy,
  editHref,
  job
}: {
  action?: () => Promise<void>;
  actionLabel?: string;
  applicantsHref?: string;
  busy?: boolean;
  editHref?: string;
  job: ProductJob;
}) {
  const canEdit = canEditEmployerJob(job);
  const editRestriction = canEdit ? null : employerJobEditRestrictionMessage(job);

  return (
    <article className="card market-card">
      <div className="market-card-head">
        <div>
          <span className="card-kicker">{job.category || "JOB POST"}</span>
          <h3>{job.title}</h3>
        </div>
        <span className={`status-pill ${job.isActive ? "success" : "neutral"}`}>
          {job.isFilled ? "Filled" : job.isActive ? "Live" : "Paused"}
        </span>
      </div>

      <p className="market-card-subtitle">{job.companyName || "DutyPe employer"}</p>
      <p className="market-card-copy">
        {job.description?.trim()
          ? job.description.trim()
          : "Live employer posting with pay, vacancy, and application status in one place."}
      </p>

      <div className="market-card-meta">
        <div className="market-meta-item">
          <span>Location</span>
          <strong>{job.addressText?.trim() || `${job.location.lat}, ${job.location.lng}`}</strong>
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
        <span className="pill">{job.applicationCount} applications</span>
        <span className="pill">{job.employerTrustTier || "NEW"} trust tier</span>
        <span className="pill">{canEdit ? "Editable now" : "Edit window closed"}</span>
      </div>

      {editRestriction ? <div className="callout">{editRestriction}</div> : null}

      {action && actionLabel ? (
        <div className="button-row compact market-card-actions">
          {editHref ? (
            <Link href={editHref} className="button ghost">
              Edit job
            </Link>
          ) : null}
          <button type="button" className="button ghost" disabled={busy} onClick={() => void action()}>
            {busy ? "Updating..." : actionLabel}
          </button>
          <Link href={applicantsHref ?? "/app/employer/applications"} className="button">
            {applicantsHref ? "View applicants" : "Review applications"}
          </Link>
        </div>
      ) : editHref || applicantsHref ? (
        <div className="button-row compact market-card-actions">
          {editHref ? (
            <Link href={editHref} className="button ghost">
              Edit job
            </Link>
          ) : null}
          <Link href={applicantsHref ?? "/app/employer/applications"} className="button">
            {applicantsHref ? "View applicants" : "Review applications"}
          </Link>
        </div>
      ) : null}
    </article>
  );
}

function EmployerApplicationCard({
  application,
  busy,
  detailHref,
  onMessageWorker,
  onStatusUpdate,
  showActions
}: {
  application: ProductApplication;
  busy?: boolean;
  detailHref?: string;
  onMessageWorker?: () => Promise<void>;
  onStatusUpdate?: (nextStatus: ProductApplicationStatus) => Promise<void>;
  showActions?: boolean;
}) {
  const [startingChat, setStartingChat] = useState(false);
  const statusHistory = Array.isArray(application.statusHistory) ? application.statusHistory : [];
  const lastHistoryEntry = statusHistory.length > 0 ? statusHistory[statusHistory.length - 1] as { notes?: string } : null;
  const latestUpdate =
    lastHistoryEntry?.notes ||
    "Application is active in the employer review flow.";

  async function handleMessageWorker() {
    if (!onMessageWorker) {
      return;
    }

    try {
      setStartingChat(true);
      await onMessageWorker();
    } finally {
      setStartingChat(false);
    }
  }

  return (
    <article className="card application-card">
      <div className="market-card-head">
        <div>
          <span className="card-kicker">Worker application</span>
          <h3>{application.workerName || "DutyPe worker"}</h3>
        </div>
        <span className={`status-pill ${productStatusTone(application.status)}`}>
          {productStatusLabel(application.status)}
        </span>
      </div>

      <p className="market-card-subtitle">{application.jobTitle || "Untitled job"}</p>
      <p className="market-card-copy">{application.coverLetter || latestUpdate}</p>

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
          <span>Latest note</span>
          <strong>{latestUpdate}</strong>
        </div>
      </div>

      {showActions || onStatusUpdate || onMessageWorker ? (
        <div className="button-row compact market-card-actions">
          {detailHref ? (
            <Link href={detailHref} className="button ghost">
              Full review
            </Link>
          ) : null}
          <Link
            href={`/app/employer/workers/${application.workerId}?applicationId=${application.id}`}
            className="button ghost"
          >
            Worker profile
          </Link>
          {onMessageWorker ? (
            <button
              type="button"
              className="button ghost"
              disabled={busy || startingChat}
              onClick={() => void handleMessageWorker()}
            >
              {startingChat ? "Opening chat..." : "Message worker"}
            </button>
          ) : null}
          {showActions && onStatusUpdate && canEmployerMoveToUnderReview(application.status) ? (
            <button
              type="button"
              className="button ghost"
              disabled={busy}
              onClick={() => void onStatusUpdate("UNDER_REVIEW")}
            >
              {busy ? "Updating..." : "Review"}
            </button>
          ) : null}
          {showActions && onStatusUpdate && canEmployerAcceptOrReject(application.status) ? (
            <>
              <button
                type="button"
                className="button"
                disabled={busy}
                onClick={() => void onStatusUpdate("ACCEPTED")}
              >
                {busy ? "Updating..." : "Accept"}
              </button>
              <button
                type="button"
                className="button ghost"
                disabled={busy}
                onClick={() => void onStatusUpdate("REJECTED")}
              >
                {busy ? "Updating..." : "Reject"}
              </button>
            </>
          ) : null}
        </div>
      ) : null}
    </article>
  );
}

export function EmployerDashboardClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
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
          query(collection(activeServices.db, "jobs"), where("employerId", "==", activeUser.uid))
        );
        const applicationsSnapshot = await getDocs(
          query(
            collection(activeServices.db, "applications"),
            where("employerId", "==", activeUser.uid)
          )
        );

        if (cancelled) {
          return;
        }

        setJobs(
          sortByTimestampDesc(
            jobsSnapshot.docs.map((snapshot) =>
              normalizeProductJob(snapshot.id, snapshot.data() as Record<string, unknown>)
            ),
            "createdAt"
          ).slice(0, 6)
        );
        setApplications(
          sortByTimestampDesc(
            applicationsSnapshot.docs.map((snapshot) =>
              normalizeProductApplication(snapshot.id, snapshot.data() as Record<string, unknown>)
            ),
            "appliedAt"
          ).slice(0, 6)
        );
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load employer dashboard.");
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

  async function handleOpenConversation(_application: ProductApplication) {
    setError("Direct messaging is no longer available. Please reach the worker via the phone number listed on their application.");
  }

  // Reference unused params to satisfy lint when chat is disabled.
  void handleOpenConversation;
  const completion = employerProfileCompletion(session.profile);
  const missingFields = missingEmployerFields(session.profile);
  const pendingApplications = applications.filter(
    (application) => application.status === "PENDING" || application.status === "UNDER_REVIEW"
  ).length;

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Profile completion</span>
            <strong>{completion}%</strong>
          </div>
          <div className="product-summary-card">
            <span>Active jobs</span>
            <strong>{jobs.filter((job) => job.isActive && !job.isFilled).length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Pending applications</span>
            <strong>{pendingApplications}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Pipeline focus</span>
            <strong>{pendingApplications > 0 ? "Review workers now" : "Keep jobs attractive"}</strong>
            <p>
              {pendingApplications > 0
                ? "You already have workers waiting. Move them forward quickly before interest drops."
                : "If applications are slow, improve pay clarity, location detail, and shift information."}
            </p>
          </article>

          <article className="product-summary-card">
            <span>Employer readiness</span>
            <strong>{missingFields.length > 0 ? "Finish identity details" : "Ready to hire"}</strong>
            <p>
              {missingFields.length > 0
                ? "A stronger employer profile makes job posts feel more credible to workers."
                : "Your employer identity looks complete enough to support live hiring routes."}
            </p>
          </article>
        </div>

        <div className="button-row">
          <Link href="/app/employer/post-job" className="button">
            Post a job
          </Link>
          <Link href="/app/employer/locations" className="button ghost">
            Manage locations
          </Link>
          <Link href="/app/employer/jobs" className="button ghost">
            Manage my jobs
          </Link>
          <Link href="/app/employer/applications" className="button ghost">
            Review applications
          </Link>
        </div>

        {missingFields.length > 0 ? (
          <div className="callout">Employer profile still missing: {missingFields.join(", ")}.</div>
        ) : null}

        {error ? <div className="callout">Dashboard error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer dashboard</span>
            <h2>Recent job posts</h2>
          </div>
          <p>This route mirrors the employer dashboard view in Android with live Firestore jobs and applications.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading employer jobs.</div>
        ) : jobs.length === 0 ? (
          <div className="empty-state">No jobs posted yet.</div>
        ) : (
          <div className="section-grid">
            {jobs.map((job) => (
              <EmployerJobCard
                key={job.id}
                applicantsHref={`/app/employer/jobs/${job.id}/applications`}
                editHref={canEditEmployerJob(job) ? `/app/employer/jobs/${job.id}` : undefined}
                job={job}
              />
            ))}
          </div>
        )}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Hiring pipeline</span>
            <h2>Latest applications</h2>
          </div>
          <p>Applications are read from `applications`, the same collection used by the Kotlin services.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading applications.</div>
        ) : applications.length === 0 ? (
          <div className="empty-state">No worker applications yet.</div>
        ) : (
          <div className="application-card-grid">
            {applications.map((application) => (
              <EmployerApplicationCard
                key={application.id}
                application={application}
                detailHref={`/app/employer/applications/${application.id}`}
                onMessageWorker={() => handleOpenConversation(application)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export function EmployerPostJobClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [form, setForm] = useState<EmployerJobForm>(initialJobForm);
  const [submitting, setSubmitting] = useState(false);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const savedBusinessLocation = employerBaseLocationFromProfile(session.profile);
  const savedWorkLocations = useMemo(
    () => normalizeWorkLocations(session.profile?.workLocations),
    [session.profile?.workLocations]
  );
  const matchedSavedLocation = useMemo(
    () =>
      savedWorkLocations.find((location) => matchesEmployerLocationForm(form, location)) ?? null,
    [form, savedWorkLocations]
  );

  useEffect(() => {
    setForm((current) => ({
      ...current,
      companyName: current.companyName || session.profile?.companyName || defaultCompanyName,
      contactNumber: current.contactNumber || session.profile?.contactPhone || session.profile?.phone || "",
      latitude: current.latitude || toCoordinateText(session.profile?.businessLatitude),
      location: current.location || session.profile?.businessAddress || "",
      longitude: current.longitude || toCoordinateText(session.profile?.businessLongitude)
    }));
  }, [session.profile]);

  function handleApplySavedLocation(location: ProductWorkLocation) {
    setError(null);
    setForm((current) => ({
      ...current,
      latitude: toCoordinateText(location.latitude),
      location: location.address ?? current.location,
      longitude: toCoordinateText(location.longitude)
    }));
  }

  async function handleUseCurrentLocation() {
    try {
      setLocating(true);
      setError(null);

      const position = await getBrowserPosition();
      setForm((current) => ({
        ...current,
        latitude: position.coords.latitude.toFixed(6),
        longitude: position.coords.longitude.toFixed(6)
      }));
    } catch (locationError) {
      setError(
        locationError instanceof Error
          ? locationError.message
          : "Unable to get the current location for this job."
      );
    } finally {
      setLocating(false);
    }
  }

  async function handleSubmit() {
    if (!services || !session.user) {
      return;
    }

    const activeServices = services;
    const activeUser = session.user;

    try {
      setSubmitting(true);
      setError(null);

      const jobRef = doc(collection(activeServices.db, "jobs"));
      const currentTime = Date.now();
      const vacancies = Math.max(1, Number(form.vacancies || "1"));
      const latitude = Number(form.latitude);
      const longitude = Number(form.longitude);
      const hasCoordinates = hasValidCoordinates(latitude, longitude);
      const nextWorkLocations =
        matchedSavedLocation?.id
          ? incrementWorkLocationUsage(savedWorkLocations, matchedSavedLocation.id)
          : savedWorkLocations;

      await updateDoc(doc(activeServices.db, "users", activeUser.uid), {
        activeRole: "EMPLOYER",
        businessAddress: form.location.trim(),
        businessLatitude: hasCoordinates ? latitude : 0,
        businessLongitude: hasCoordinates ? longitude : 0,
        companyName: form.companyName.trim(),
        contactEmail: activeUser.email ?? session.profile?.email ?? "",
        contactPhone: form.contactNumber.trim(),
        role: "EMPLOYER",
        roles: [...new Set([...session.availableRoles, "EMPLOYER"])],
        updatedAt: currentTime,
        workLocations: nextWorkLocations
      });

      await setDoc(
        doc(activeServices.db, "employer_profiles", activeUser.uid),
        {
          businessAddress: form.location.trim(),
          businessLatitude: hasCoordinates ? latitude : 0,
          businessLongitude: hasCoordinates ? longitude : 0,
          companyName: form.companyName.trim(),
          contactEmail: activeUser.email ?? session.profile?.email ?? "",
          contactPhone: form.contactNumber.trim(),
          updatedAt: currentTime,
          userId: activeUser.uid,
          workLocations: nextWorkLocations
        },
        { merge: true }
      );

      await setDoc(doc(activeServices.db, "jobs", jobRef.id), {
        acceptedCount: 0,
        applicationCount: 0,
        addressText: form.location.trim(),
        companyName: form.companyName.trim(),
        contactNumber: form.contactNumber.trim(),
        createdAt: currentTime,
        description: form.description.trim(),
        employerId: activeUser.uid,
        employerTrustTier: session.profile?.trustTier || "NEW",
        expiryDays: 15,
        expiresAt: currentTime + 15 * 24 * 60 * 60 * 1000,
        gender: form.gender,
        jobId: jobRef.id,
        jobType: form.jobType,
        location: {
          lat: hasCoordinates ? latitude : 0,
          lng: hasCoordinates ? longitude : 0
        },
        salary: Number(form.payAmount.trim()) || 0,
        salaryType: form.payType,
        shiftTiming: form.shiftTiming.trim(),
        status: "open",
        title: form.title.trim(),
        updatedAt: currentTime,
        vacancies
      });

      await session.refreshProfile();
      router.push("/app/employer/jobs");
      router.refresh();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to post job.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Post job error: {error}</div> : null}

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer post job</span>
            <h2>Create a live Firestore job</h2>
          </div>
          <p>
            This uses the same core job shape as the Android `JobFirestoreService`: `jobId`,
            `employerId`, `createdAt`, `applicationCount`, `vacancies`, `status`, and `salary`.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">Before you publish</span>
            <h3>What makes a job post convert better</h3>
            <ul className="detail-list">
              <li>
                <strong>Clear pay</strong>
                <span>Workers decide faster when pay type and amount are obvious.</span>
              </li>
              <li>
                <strong>Precise location</strong>
                <span>Hyperlocal hiring works best when the route and area are easy to understand.</span>
              </li>
              <li>
                <strong>Real shift details</strong>
                <span>Ambiguous timing creates drop-off before applications are submitted.</span>
              </li>
            </ul>
          </article>

          <article className="detail-panel">
            <span className="card-kicker">Employer preview</span>
            <h3>{form.title || "Your next job post"}</h3>
            <p>
              {form.description || "Write a short job summary so workers can decide quickly whether this role is worth opening."}
            </p>
            <div className="pill-row">
              <span className="pill">{form.companyName || "Company pending"}</span>
              <span className="pill">{form.location || "Location pending"}</span>
              <span className="pill">{form.payAmount || "Pay pending"}</span>
              <span className="pill">
                {hasValidCoordinates(Number(form.latitude), Number(form.longitude))
                  ? "Map coordinates ready"
                  : "Coordinates pending"}
              </span>
            </div>
            <div className="button-row compact">
              <button
                type="button"
                className="button ghost"
                disabled={locating}
                onClick={() => void handleUseCurrentLocation()}
              >
                {locating ? "Detecting..." : "Use current location"}
              </button>
              <Link href="/app/employer/locations" className="button ghost">
                Manage saved locations
              </Link>
            </div>
          </article>
        </div>

        {savedWorkLocations.length > 0 ? (
          <article className="detail-panel">
            <div className="section-header">
              <div>
                <span className="tag">Saved employer locations</span>
                <h2>Reuse a frequent job site</h2>
              </div>
              <p>
                These locations come from the employer location manager. Selecting one fills the
                job location fields and keeps the most-used sites ranked first.
              </p>
            </div>

            <div className="product-chip-row location-chip-grid">
              {savedWorkLocations.slice(0, 6).map((location) => (
                <button
                  key={location.id}
                  type="button"
                  className={`product-chip ${matchedSavedLocation?.id === location.id ? "active" : ""}`}
                  onClick={() => handleApplySavedLocation(location)}
                >
                  <strong>{location.label || "Saved location"}</strong>
                  <small>{location.address || "Address pending"}</small>
                </button>
              ))}
            </div>

            <div className="pill-row">
              <span className="pill">{savedWorkLocations.length} saved locations</span>
              <span className="pill">
                {matchedSavedLocation ? `Using ${matchedSavedLocation.label || "saved location"}` : "Select a saved site"}
              </span>
            </div>
          </article>
        ) : (
          <div className="callout">
            Save frequent job sites in <Link href="/app/employer/locations">Employer locations</Link> to
            reuse them here.
          </div>
        )}

        <div className="editor-form">
          <label>
            <span>Job title</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
            />
          </label>

          <label>
            <span>Company name</span>
            <input
              value={form.companyName}
              onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))}
            />
          </label>

          <label>
            <span>Location</span>
            <input
              value={form.location}
              onChange={(event) => setForm((current) => ({ ...current, location: event.target.value }))}
            />
          </label>

          <label>
            <span>Latitude</span>
            <input
              value={form.latitude}
              onChange={(event) => setForm((current) => ({ ...current, latitude: event.target.value }))}
              placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.latitude) : "17.448294"}
            />
          </label>

          <label>
            <span>Longitude</span>
            <input
              value={form.longitude}
              onChange={(event) => setForm((current) => ({ ...current, longitude: event.target.value }))}
              placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.longitude) : "78.391487"}
            />
          </label>

          <label>
            <span>Contact number</span>
            <input
              value={form.contactNumber}
              onChange={(event) => setForm((current) => ({ ...current, contactNumber: event.target.value }))}
            />
          </label>

          <label>
            <span>Pay amount</span>
            <input
              value={form.payAmount}
              onChange={(event) => setForm((current) => ({ ...current, payAmount: event.target.value }))}
              placeholder="18000"
            />
          </label>

          <label>
            <span>Pay type</span>
            <select
              value={form.payType}
              onChange={(event) => setForm((current) => ({ ...current, payType: event.target.value }))}
            >
              <option value="MONTHLY">Monthly</option>
              <option value="DAILY">Daily</option>
              <option value="HOURLY">Hourly</option>
            </select>
          </label>

          <label>
            <span>Category</span>
            <input
              value={form.category}
              onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
              placeholder="Delivery, helper, retail"
            />
          </label>

          <label>
            <span>Shift timing</span>
            <input
              value={form.shiftTiming}
              onChange={(event) => setForm((current) => ({ ...current, shiftTiming: event.target.value }))}
              placeholder="Day shift"
            />
          </label>

          <label>
            <span>Job type</span>
            <select
              value={form.jobType}
              onChange={(event) => setForm((current) => ({ ...current, jobType: event.target.value }))}
            >
              <option value="FULL_TIME">Full time</option>
              <option value="PART_TIME">Part time</option>
              <option value="SHIFT">Shift</option>
            </select>
          </label>

          <label>
            <span>Gender</span>
            <select
              value={form.gender}
              onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}
            >
              <option value="ANY">Any</option>
              <option value="Female">Female</option>
              <option value="Male">Male</option>
            </select>
          </label>

          <label>
            <span>Vacancies</span>
            <input
              type="number"
              min="1"
              value={form.vacancies}
              onChange={(event) => setForm((current) => ({ ...current, vacancies: event.target.value }))}
            />
          </label>

          <label className="editor-form-wide">
            <span>Description</span>
            <textarea
              rows={6}
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              placeholder="Role details, responsibilities, and candidate expectations"
            />
          </label>

          <div className="editor-form-actions button-row">
            <button type="button" className="button" disabled={submitting} onClick={() => void handleSubmit()}>
              {submitting ? "Posting..." : "Post job"}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

export function EmployerEditJobClient({ jobId, session }: EmployerEditJobClientProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [job, setJob] = useState<ProductJob | null>(null);
  const [form, setForm] = useState<EmployerJobForm>(initialJobForm);
  const [deleting, setDeleting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const savedWorkLocations = useMemo(
    () => normalizeWorkLocations(session.profile?.workLocations),
    [session.profile?.workLocations]
  );
  const matchedSavedLocation = useMemo(
    () =>
      savedWorkLocations.find((location) => matchesEmployerLocationForm(form, location)) ?? null,
    [form, savedWorkLocations]
  );

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

        const snapshot = await getDoc(doc(activeServices.db, "jobs", jobId));
        if (!snapshot.exists()) {
          throw new Error("This job could not be found.");
        }

        const nextJob = normalizeProductJob(
          snapshot.id,
          snapshot.data() as Record<string, unknown>
        );

        if (nextJob.employerId !== activeUser.uid) {
          throw new Error("You can only edit jobs posted by your own employer account.");
        }

        if (cancelled) {
          return;
        }

        setJob(nextJob);
        setForm({
          category: nextJob.category || "General",
          companyName:
            nextJob.companyName ||
            session.profile?.companyName ||
            defaultCompanyName,
          contactNumber:
            nextJob.contactNumber ||
            session.profile?.contactPhone ||
            session.profile?.phone ||
            "",
          description: nextJob.description || "",
          gender: nextJob.gender || "ANY",
          jobType: nextJob.jobType || "FULL_TIME",
          latitude: toCoordinateText(nextJob.latitude),
          location: nextJob.addressText || `${nextJob.location.lat}, ${nextJob.location.lng}`,
          longitude: toCoordinateText(nextJob.longitude),
          payAmount: String(nextJob.payAmount ?? ""),
          payType: nextJob.payType || "MONTHLY",
          shiftTiming: nextJob.shiftTiming || "",
          title: nextJob.title || "",
          vacancies: String(nextJob.vacancies || 1)
        });
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load job for editing.");
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
  }, [jobId, services, session.profile, session.user]);

  function handleApplySavedLocation(location: ProductWorkLocation) {
    setError(null);
    setForm((current) => ({
      ...current,
      latitude: toCoordinateText(location.latitude),
      location: location.address ?? current.location,
      longitude: toCoordinateText(location.longitude)
    }));
  }

  async function handleUseCurrentLocation() {
    try {
      setLocating(true);
      setError(null);

      const position = await getBrowserPosition();
      setForm((current) => ({
        ...current,
        latitude: position.coords.latitude.toFixed(6),
        longitude: position.coords.longitude.toFixed(6)
      }));
    } catch (locationError) {
      setError(
        locationError instanceof Error
          ? locationError.message
          : "Unable to get the current location for this job."
      );
    } finally {
      setLocating(false);
    }
  }

  async function handleSubmit() {
    if (!services || !session.user || !job) {
      return;
    }

    if (!canEditEmployerJob(job)) {
      setError(employerJobEditRestrictionMessage(job));
      return;
    }

    try {
      setSaving(true);
      setError(null);

      const currentTime = Date.now();
      const typedLatitude = Number(form.latitude);
      const typedLongitude = Number(form.longitude);
      const hasTypedCoordinates = hasValidCoordinates(typedLatitude, typedLongitude);
      const hasExistingCoordinates = hasValidCoordinates(job.latitude, job.longitude);
      const finalLatitude = hasTypedCoordinates
        ? typedLatitude
        : hasExistingCoordinates
          ? job.latitude
          : 0;
      const finalLongitude = hasTypedCoordinates
        ? typedLongitude
        : hasExistingCoordinates
          ? job.longitude
          : 0;
      const nextWorkLocations =
        matchedSavedLocation?.id
          ? incrementWorkLocationUsage(savedWorkLocations, matchedSavedLocation.id)
          : savedWorkLocations;

      await updateDoc(doc(services.db, "users", session.user.uid), {
        activeRole: "EMPLOYER",
        businessAddress: form.location.trim(),
        businessLatitude: finalLatitude,
        businessLongitude: finalLongitude,
        companyName: form.companyName.trim(),
        contactEmail: session.user.email ?? session.profile?.email ?? "",
        contactPhone: form.contactNumber.trim(),
        role: "EMPLOYER",
        roles: [...new Set([...session.availableRoles, "EMPLOYER"])],
        updatedAt: currentTime,
        workLocations: nextWorkLocations
      });

      await setDoc(
        doc(services.db, "employer_profiles", session.user.uid),
        {
          businessAddress: form.location.trim(),
          businessLatitude: finalLatitude,
          businessLongitude: finalLongitude,
          companyName: form.companyName.trim(),
          contactEmail: session.user.email ?? session.profile?.email ?? "",
          contactPhone: form.contactNumber.trim(),
          updatedAt: currentTime,
          userId: session.user.uid,
          workLocations: nextWorkLocations
        },
        { merge: true }
      );

      await updateDoc(doc(services.db, "jobs", job.id), {
        addressText: form.location.trim(),
        companyName: form.companyName.trim(),
        contactNumber: form.contactNumber.trim(),
        description: form.description.trim(),
        gender: form.gender,
        jobType: form.jobType,
        location: {
          lat: finalLatitude,
          lng: finalLongitude
        },
        salary: Number(form.payAmount.trim()) || 0,
        salaryType: form.payType,
        shiftTiming: form.shiftTiming.trim(),
        title: form.title.trim(),
        updatedAt: currentTime,
        vacancies: Math.max(1, Number(form.vacancies || "1"))
      });

      await session.refreshProfile();
      router.push("/app/employer/jobs");
      router.refresh();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to update job.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteJob() {
    if (!services || !job) {
      return;
    }

    if (!canEditEmployerJob(job)) {
      setError(employerJobEditRestrictionMessage(job));
      return;
    }

    try {
      setDeleting(true);
      setError(null);

      await deleteDoc(doc(services.db, "jobs", job.id));
      router.push("/app/employer/jobs");
      router.refresh();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete job.");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="empty-state">Loading job details for editing.</div>
        </section>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="detail-panel">
            <span className="card-kicker">Job not available</span>
            <h3>We could not open this employer job.</h3>
            <p>{error || "The job may have been removed or belongs to another employer account."}</p>
            <div className="button-row">
              <Link href="/app/employer/jobs" className="button">
                Back to my jobs
              </Link>
            </div>
          </div>
        </section>
      </div>
    );
  }

  const canEdit = canEditEmployerJob(job);
  const editRestriction = employerJobEditRestrictionMessage(job);

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Edit job error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Posted job</span>
            <strong>{job.title}</strong>
          </div>
          <div className="product-summary-card">
            <span>Edit window</span>
            <strong>{canEdit ? "Open" : "Closed"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Applications</span>
            <strong>{job.applicationCount}</strong>
          </div>
        </div>

        <div className="button-row">
          <Link href="/app/employer/jobs" className="button ghost">
            Back to my jobs
          </Link>
          <Link href={`/app/employer/jobs/${job.id}/applications`} className="button ghost">
            View applicants
          </Link>
          {canEdit ? (
            <button
              type="button"
              className="button ghost danger"
              disabled={deleting}
              onClick={() => setShowDeleteConfirm((current) => !current)}
            >
              {showDeleteConfirm ? "Close delete" : "Delete job"}
            </button>
          ) : null}
        </div>

        {!canEdit ? <div className="callout">{editRestriction}</div> : null}
        {showDeleteConfirm && canEdit ? (
          <div className="callout">
            This permanently deletes the job posting from Firestore. Existing applicants will no
            longer have an active job document to attach to.
            <div className="button-row compact">
              <button
                type="button"
                className="button ghost danger"
                disabled={deleting}
                onClick={() => void handleDeleteJob()}
              >
                {deleting ? "Deleting..." : "Delete permanently"}
              </button>
              <button
                type="button"
                className="button ghost"
                disabled={deleting}
                onClick={() => setShowDeleteConfirm(false)}
              >
                Keep job
              </button>
            </div>
          </div>
        ) : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer edit job</span>
            <h2>Update a live job post</h2>
          </div>
          <p>
            This editor follows the Android employer flow: same saved-location shortcuts, same
            seven-day edit limit, and the same Firestore job document shape.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">Live preview</span>
            <h3>{form.title || job.title}</h3>
            <p>
              {form.description ||
                "Keep the summary short, precise, and useful enough for workers to judge quickly."}
            </p>
            <div className="pill-row">
              <span className="pill">{form.companyName || "Company pending"}</span>
              <span className="pill">{form.location || "Location pending"}</span>
              <span className="pill">{form.payAmount || "Pay pending"}</span>
              <span className="pill">{job.isActive ? "Currently live" : "Currently paused"}</span>
            </div>
            <div className="button-row compact">
              <button
                type="button"
                className="button ghost"
                disabled={locating || !canEdit}
                onClick={() => void handleUseCurrentLocation()}
              >
                {locating ? "Detecting..." : "Use current location"}
              </button>
              <Link href="/app/employer/locations" className="button ghost">
                Manage saved locations
              </Link>
            </div>
          </article>

          <article className="detail-panel">
            <span className="card-kicker">Original post</span>
            <h3>{job.companyName || "DutyPe employer"}</h3>
            <p>{job.addressText?.trim() || `${job.location.lat}, ${job.location.lng}`}</p>
            <div className="pill-row">
              <span className="pill">{job.category || "Category pending"}</span>
              <span className="pill">{job.shiftTiming || "Shift pending"}</span>
              <span className="pill">{job.vacancies} vacancies</span>
            </div>
          </article>
        </div>

        {savedWorkLocations.length > 0 ? (
          <article className="detail-panel">
            <div className="section-header">
              <div>
                <span className="tag">Saved employer locations</span>
                <h2>Swap in a repeat work site</h2>
              </div>
              <p>
                Reusing a saved site updates the location fields here and keeps your most-used
                sites ranked first in employer locations.
              </p>
            </div>

            <div className="product-chip-row location-chip-grid">
              {savedWorkLocations.slice(0, 6).map((location) => (
                <button
                  key={location.id}
                  type="button"
                  className={`product-chip ${matchedSavedLocation?.id === location.id ? "active" : ""}`}
                  disabled={!canEdit}
                  onClick={() => handleApplySavedLocation(location)}
                >
                  <strong>{location.label || "Saved location"}</strong>
                  <small>{location.address || "Address pending"}</small>
                </button>
              ))}
            </div>
          </article>
        ) : null}

        <div className="editor-form">
          <label>
            <span>Job title</span>
            <input
              disabled={!canEdit}
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
            />
          </label>

          <label>
            <span>Company name</span>
            <input
              disabled={!canEdit}
              value={form.companyName}
              onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))}
            />
          </label>

          <label>
            <span>Location</span>
            <input
              disabled={!canEdit}
              value={form.location}
              onChange={(event) => setForm((current) => ({ ...current, location: event.target.value }))}
            />
          </label>

          <label>
            <span>Latitude</span>
            <input
              disabled={!canEdit}
              value={form.latitude}
              onChange={(event) => setForm((current) => ({ ...current, latitude: event.target.value }))}
              placeholder={toCoordinateText(job.latitude) || "17.448294"}
            />
          </label>

          <label>
            <span>Longitude</span>
            <input
              disabled={!canEdit}
              value={form.longitude}
              onChange={(event) => setForm((current) => ({ ...current, longitude: event.target.value }))}
              placeholder={toCoordinateText(job.longitude) || "78.391487"}
            />
          </label>

          <label>
            <span>Contact number</span>
            <input
              disabled={!canEdit}
              value={form.contactNumber}
              onChange={(event) => setForm((current) => ({ ...current, contactNumber: event.target.value }))}
            />
          </label>

          <label>
            <span>Pay amount</span>
            <input
              disabled={!canEdit}
              value={form.payAmount}
              onChange={(event) => setForm((current) => ({ ...current, payAmount: event.target.value }))}
            />
          </label>

          <label>
            <span>Pay type</span>
            <select
              disabled={!canEdit}
              value={form.payType}
              onChange={(event) => setForm((current) => ({ ...current, payType: event.target.value }))}
            >
              <option value="MONTHLY">Monthly</option>
              <option value="DAILY">Daily</option>
              <option value="HOURLY">Hourly</option>
            </select>
          </label>

          <label>
            <span>Category</span>
            <input
              disabled={!canEdit}
              value={form.category}
              onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
            />
          </label>

          <label>
            <span>Shift timing</span>
            <input
              disabled={!canEdit}
              value={form.shiftTiming}
              onChange={(event) => setForm((current) => ({ ...current, shiftTiming: event.target.value }))}
            />
          </label>

          <label>
            <span>Job type</span>
            <select
              disabled={!canEdit}
              value={form.jobType}
              onChange={(event) => setForm((current) => ({ ...current, jobType: event.target.value }))}
            >
              <option value="FULL_TIME">Full time</option>
              <option value="PART_TIME">Part time</option>
              <option value="SHIFT">Shift</option>
            </select>
          </label>

          <label>
            <span>Gender</span>
            <select
              disabled={!canEdit}
              value={form.gender}
              onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}
            >
              <option value="ANY">Any</option>
              <option value="Female">Female</option>
              <option value="Male">Male</option>
            </select>
          </label>

          <label>
            <span>Vacancies</span>
            <input
              disabled={!canEdit}
              min="1"
              type="number"
              value={form.vacancies}
              onChange={(event) => setForm((current) => ({ ...current, vacancies: event.target.value }))}
            />
          </label>

          <label className="editor-form-wide">
            <span>Description</span>
            <textarea
              disabled={!canEdit}
              rows={6}
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
            />
          </label>

          <div className="editor-form-actions button-row">
            <button
              type="button"
              className="button"
              disabled={saving || !canEdit}
              onClick={() => void handleSubmit()}
            >
              {saving ? "Saving..." : "Update job"}
            </button>
            <Link href="/app/employer/jobs" className="button ghost">
              Cancel
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

export function EmployerJobsClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<ProductJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyJobId, setBusyJobId] = useState<string | null>(null);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadJobs() {
      try {
        setLoading(true);
        setError(null);

        const snapshot = await getDocs(
          query(collection(activeServices.db, "jobs"), where("employerId", "==", activeUser.uid))
        );

        if (cancelled) {
          return;
        }

        setJobs(
          sortByTimestampDesc(
            snapshot.docs.map((item) =>
              normalizeProductJob(item.id, item.data() as Record<string, unknown>)
            ),
            "createdAt"
          )
        );
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load employer jobs.");
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
  }, [services, session.user]);

  async function handleToggleActive(job: ProductJob) {
    if (!services) {
      return;
    }

    try {
      setBusyJobId(job.id);
      const nextStatus = job.status === "open" ? "closed" : "open";
      await updateDoc(doc(services.db, "jobs", job.id), {
        status: nextStatus,
        updatedAt: Date.now()
      });
      setJobs((current) =>
        current.map((item) =>
          item.id === job.id
            ? {
                ...item,
                isActive: nextStatus === "open",
                status: nextStatus
              }
            : item
        )
      );
    } catch (toggleError) {
      setError(toggleError instanceof Error ? toggleError.message : "Failed to update job visibility.");
    } finally {
      setBusyJobId(null);
    }
  }

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Jobs error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Total jobs</span>
            <strong>{jobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Currently open</span>
            <strong>{jobs.filter(isLiveJob).length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Total applications</span>
            <strong>{jobs.reduce((sum, job) => sum + (job.applicationCount ?? 0), 0)}</strong>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer jobs</span>
            <h2>Manage posted jobs</h2>
          </div>
          <p>Use this route to pause or reopen jobs while keeping the Android-compatible document structure.</p>
        </div>

        {loading ? (
          <div className="empty-state">Loading your posted jobs.</div>
        ) : jobs.length === 0 ? (
          <div className="empty-state">No jobs posted yet.</div>
        ) : (
          <div className="section-grid">
            {jobs.map((job) => (
              <EmployerJobCard
                key={job.id}
                action={() => handleToggleActive(job)}
                actionLabel={job.isActive ? "Pause job" : "Reopen job"}
                applicantsHref={`/app/employer/jobs/${job.id}/applications`}
                busy={busyJobId === job.id}
                editHref={canEditEmployerJob(job) ? `/app/employer/jobs/${job.id}` : undefined}
                job={job}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function nextStatusNote(status: ProductApplicationStatus) {
  switch (status) {
    case "UNDER_REVIEW":
      return "Application reviewed in web employer flow";
    case "ACCEPTED":
      return "Application accepted in web employer flow";
    case "IN_PROGRESS":
      return "Work verified and started in web employer flow";
    case "COMPLETED":
      return "Job completed in web employer flow";
    case "REJECTED":
      return "Application rejected in web employer flow";
    default:
      return "Status updated in web employer flow";
  }
}

export function EmployerApplicationsClient({
  backHref = "/app/employer/jobs",
  jobId,
  jobTitle,
  session
}: EmployerApplicationsClientProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [applications, setApplications] = useState<ProductApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [scopedJob, setScopedJob] = useState<ProductJob | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [busyApplicationId, setBusyApplicationId] = useState<string | null>(null);

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadApplications() {
      try {
        setLoading(true);
        setError(null);

        if (jobId) {
          const jobSnapshot = await getDoc(doc(activeServices.db, "jobs", jobId));

          if (!jobSnapshot.exists()) {
            throw new Error("This job could not be found.");
          }

          const nextJob = normalizeProductJob(
            jobSnapshot.id,
            jobSnapshot.data() as Record<string, unknown>
          );

          if (nextJob.employerId !== activeUser.uid) {
            throw new Error("You can only review applicants for your own employer jobs.");
          }

          if (!cancelled) {
            setScopedJob(nextJob);
          }
        } else if (!cancelled) {
          setScopedJob(null);
        }

        const snapshot = await getDocs(
          query(
            collection(activeServices.db, "applications"),
            where("employerId", "==", activeUser.uid)
          )
        );

        if (cancelled) {
          return;
        }

        setApplications(
          sortByTimestampDesc(
            snapshot.docs.map((item) =>
              normalizeProductApplication(item.id, item.data() as Record<string, unknown>)
            ),
            "appliedAt"
          ).filter((application) => !jobId || application.jobId === jobId)
        );
      } catch (loadError) {
        if (!cancelled) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load applications.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadApplications();

    return () => {
      cancelled = true;
    };
  }, [jobId, services, session.user]);

  const filteredApplications = applications.filter(
    (application) => {
      const matchesStatus = filterStatus === "ALL" || application.status === filterStatus;
      const searchValue = searchQuery.trim().toLowerCase();

      if (!searchValue) {
        return matchesStatus;
      }

      const matchesSearch =
        (application.workerName ?? "").toLowerCase().includes(searchValue) ||
        (application.jobTitle ?? "").toLowerCase().includes(searchValue) ||
        (application.coverLetter ?? "").toLowerCase().includes(searchValue);

      return matchesStatus && matchesSearch;
    }
  );

  async function handleOpenConversation(_application: ProductApplication) {
    setError("Direct messaging is no longer available. Please contact the worker via the phone number on their application.");
  }

  void handleOpenConversation;

  async function handleStatusUpdate(
    application: ProductApplication,
    nextStatus: ProductApplicationStatus
  ) {
    if (!services || application.status === nextStatus) {
      return;
    }

    try {
      setBusyApplicationId(application.id);

      const currentTime = Date.now();
      const previousHistory = Array.isArray(application.statusHistory)
        ? application.statusHistory.filter(
            (entry) => entry !== null && typeof entry === "object"
          )
        : [];
      const nextHistory = [
        ...previousHistory,
        {
          notes: nextStatusNote(nextStatus),
          status: nextStatus,
          systemUpdate: false,
          timestamp: currentTime,
          updatedAt: currentTime,
          updatedBy: session.user?.uid ?? ""
        }
      ];
      const storedHistory = nextHistory.map((entry) => {
        const rawStatus =
          typeof entry.status === "string" && entry.status.trim().length > 0
            ? entry.status.toUpperCase()
            : nextStatus;

        return {
          ...entry,
          status: toStorageApplicationStatus(rawStatus as ProductApplicationStatus)
        };
      });
      const updatePayload: Record<string, unknown> = {
        status: toStorageApplicationStatus(nextStatus),
        statusHistory: storedHistory,
        updatedAt: currentTime
      };

      if (nextStatus === "ACCEPTED") {
        const verification = await createPendingWorkVerification({
          applicationId: application.id,
          db: services.db,
          employerId: application.employerId,
          employerName:
            session.profile?.companyName ||
            "DutyPe employer",
          jobId: application.jobId,
          jobTitle: application.jobTitle || "Job",
          workerId: application.workerId,
          workerName: application.workerName || "Worker"
        });

        updatePayload.verification = verification;
        updatePayload.verificationCode = verification.verificationCode;
        updatePayload.verificationId = verification.verificationId;
        updatePayload.verificationStatus = verification.status;

        const jobSnapshot = await getDoc(doc(services.db, "jobs", application.jobId));
        if (jobSnapshot.exists()) {
          const job = normalizeProductJob(
            jobSnapshot.id,
            jobSnapshot.data() as Record<string, unknown>
          );
          const acceptedCount = (job.acceptedCount ?? 0) + 1;
          const isFilled = acceptedCount >= (job.vacancies ?? 1);

          await updateDoc(doc(services.db, "jobs", application.jobId), {
            acceptedCount,
            status: isFilled ? "closed" : "open",
            updatedAt: currentTime,
            vacancyStatus: isFilled ? "FILLED" : "OPEN"
          });
        }
      }

      await updateDoc(doc(services.db, "applications", application.id), updatePayload);

      setApplications((current) =>
        current.map((item) =>
          item.id === application.id
            ? {
                ...item,
                status: nextStatus,
                statusHistory: nextHistory,
                updatedAt: currentTime
              }
            : item
        )
      );
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Failed to update application status.");
    } finally {
      setBusyApplicationId(null);
    }
  }

  const viewTitle = scopedJob?.title || jobTitle || "Employer applications";
  const pendingCount = applications.filter(
    (application) => application.status === "PENDING" || application.status === "UNDER_REVIEW"
  ).length;
  const acceptedCount = applications.filter(
    (application) => application.status === "ACCEPTED" || application.status === "IN_PROGRESS"
  ).length;

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Applications error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>{jobId ? "Selected job" : "Total applications"}</span>
            <strong>{jobId ? viewTitle : applications.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>{jobId ? "Pending review" : "Pending"}</span>
            <strong>{pendingCount}</strong>
          </div>
          <div className="product-summary-card">
            <span>Accepted / live</span>
            <strong>{acceptedCount}</strong>
          </div>
        </div>

        {jobId ? (
          <div className="button-row">
            <Link href={backHref} className="button ghost">
              Back to my jobs
            </Link>
            <Link href={`/app/employer/jobs/${jobId}`} className="button ghost">
              Edit this job
            </Link>
          </div>
        ) : null}

        <div className="product-top-grid">
          <div className="filter-row">
            <label className="inline-field">
              <span>Search applicants</span>
              <input
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Worker name, job title, or cover note"
              />
            </label>
            <label className="inline-field">
              <span>Status filter</span>
              <select value={filterStatus} onChange={(event) => setFilterStatus(event.target.value)}>
                <option value="ALL">All statuses</option>
                <option value="PENDING">Pending</option>
                <option value="UNDER_REVIEW">Under review</option>
                <option value="ACCEPTED">Accepted</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="REJECTED">Rejected</option>
                <option value="WITHDRAWN">Withdrawn</option>
              </select>
            </label>
          </div>

          <div className="pill-row">
            <span className="pill">{applications.length} total applications</span>
            <span className="pill">
              {pendingCount} pending
            </span>
            <span className="pill">
              {acceptedCount} accepted or live
            </span>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">{jobId ? "Job applicants" : "Application review"}</span>
            <h2>{jobId ? `Applicants for ${viewTitle}` : "Review worker applications"}</h2>
          </div>
          <p>
            Status changes here write back to `applications`, update job fill state, and
            generate the worker start-code flow when an application is accepted.
          </p>
        </div>

        {loading ? (
          <div className="empty-state">Loading applications.</div>
        ) : filteredApplications.length === 0 ? (
          <div className="empty-state">
            {jobId
              ? "No applicants matched the current search or status filter."
              : "No applications matched the current filter."}
          </div>
        ) : (
          <div className="application-card-grid">
            {filteredApplications.map((application) => (
              <EmployerApplicationCard
                key={application.id}
                application={application}
                busy={busyApplicationId === application.id}
                detailHref={`/app/employer/applications/${application.id}`}
                onMessageWorker={() => handleOpenConversation(application)}
                onStatusUpdate={(nextStatus) => handleStatusUpdate(application, nextStatus)}
                showActions
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
