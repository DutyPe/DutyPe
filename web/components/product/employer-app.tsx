"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  runTransaction,
  setDoc,
  updateDoc,
  where,
  writeBatch
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { FIREBASE_SETUP_ERROR } from "@/lib/firebase/auth-errors";
import { validJobId } from "@/lib/jobs/public-listings";
import { jobDirectoryCities } from "@/lib/public-site";
import {
  getOrCreateConversationId,
  productConversationRoute
} from "@/lib/firebase/chat-actions";
import { formatCurrencyRange, formatDateTime } from "@/lib/firebase/firestore-helpers";
import {
  employerBaseLocationFromProfile,
  hasValidCoordinates
} from "@/lib/product/location";
import {
  getEmployerType,
  isIndividualEmployer,
  isCompanyEmployer,
  type EmployerType,
  type ProductWorkLocation
} from "@/lib/product/profile";
import {
  canEmployerAcceptOrReject,
  canEmployerMoveToUnderReview,
  canEditEmployerJob,
  defaultCompanyName,
  deriveJobCategory,
  employerJobEditRestrictionMessage,
  employerProfileCompletion,
  isLiveJob,
  missingEmployerFields,
  normalizeProductApplication,
  normalizeProductJob,
  productStatusLabel,
  productStatusTone,
  sortByTimestampDesc,
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
  city: string;
  area: string;
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
  city: "",
  area: "",
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

const jobDraftKey = "dutype:employer-job-draft:v1";

type InstantTaskForm = {
  addressText: string;
  area: string;
  category: string;
  city: string;
  contactNumber: string;
  description: string;
  durationText: string;
  latitude: string;
  longitude: string;
  needType: "urgent_now" | "today" | "scheduled";
  perPersonPayment: string;
  scheduledAtLabel: string;
  title: string;
  totalPayment: string;
  urgencyType: "right_now" | "today" | "tomorrow" | "scheduled";
  workersNeeded: string;
};

const initialInstantForm: InstantTaskForm = {
  addressText: "",
  area: "",
  category: "Helper",
  city: "",
  contactNumber: "",
  description: "",
  durationText: "4 hours (Half day)",
  latitude: "",
  longitude: "",
  needType: "urgent_now",
  perPersonPayment: "500",
  scheduledAtLabel: "",
  title: "",
  totalPayment: "500",
  urgencyType: "right_now",
  workersNeeded: "1"
};

const instantDraftKey = "dutype:employer-instant-draft:v1";

const instantCategories = [
  "Helper",
  "Cook",
  "Maid",
  "Electrician",
  "Plumber",
  "Driver",
  "Delivery",
  "Painter",
  "Carpenter",
  "Other"
];

const instantDurations = [
  "2 hours",
  "3 hours",
  "4 hours (Half day)",
  "6 hours",
  "8 hours (Full day)",
  "Task completion"
];

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
  const isInstant = Boolean((job as any).isInstant || job.jobType === "INSTANT" || job.title?.startsWith("[Instant]"));

  return (
    <article className="card market-card">
      <div className="market-card-head">
        <div>
          <span className="card-kicker">{isInstant ? "⚡ INSTANT TASK" : job.category || "JOB POST"}</span>
          <h3>{job.title}</h3>
        </div>
        <div style={{ display: "flex", gap: "0.4rem", alignItems: "center" }}>
          {isInstant ? <span className="status-pill warning">⚡ Instant</span> : null}
          <span className={`status-pill ${job.isActive ? "success" : "neutral"}`}>
            {job.isFilled ? "Filled" : job.isActive ? "Live" : "Paused"}
          </span>
        </div>
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
          <strong>{job.location || "Location pending"}</strong>
        </div>
        <div className="market-meta-item">
          <span>Pay</span>
          <strong>{formatCurrencyRange(job.payAmount, job.payType)}</strong>
        </div>
        <div className="market-meta-item">
          <span>{isInstant ? "Duration" : "Shift"}</span>
          <strong>{job.shiftTiming || (isInstant ? "Immediate" : "Shift timing pending")}</strong>
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
  const latestUpdate =
    application.statusHistory[application.statusHistory.length - 1]?.notes ||
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
            collection(activeServices.db, "job_applications"),
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

  async function handleOpenConversation(application: ProductApplication) {
    try {
      setError(null);

      const conversationId = await getOrCreateConversationId(
        application.workerId,
        application.jobId || application.id
      );

      router.push(productConversationRoute("EMPLOYER", conversationId));
    } catch (conversationError) {
      setError(
        conversationError instanceof Error
          ? conversationError.message
          : "Failed to open worker chat."
      );
    }
  }

  async function handleToggleEmployerType() {
    if (!services || !session.user) return;
    const currentType = getEmployerType(session.profile);
    const nextType = currentType === "INDIVIDUAL" ? "COMPANY" : "INDIVIDUAL";
    try {
      await updateDoc(doc(services.db, "users", session.user.uid), {
        employerType: nextType,
        updatedAt: Date.now()
      });
      await session.refreshProfile();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update profile type.");
    }
  }

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

        <div className="callout" style={{ marginTop: "1rem", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "0.75rem" }}>
          <div>
            <span>Employer Account Type: </span>
            <strong>{isIndividualEmployer(session.profile) ? "👤 Personal / Individual" : "🏢 Company / Business"}</strong>
            <p style={{ margin: "0.25rem 0 0 0", fontSize: "0.85rem", color: "#64748b" }}>
              {isIndividualEmployer(session.profile)
                ? "Personal profile for domestic chores, quick gigs, and instant tasks. Job posting defaults to Instant Tasks."
                : "Company profile for commercial hiring, shops, and enterprises. Job posting defaults to Regular Vacancies."}
            </p>
          </div>
          <button type="button" className="button ghost" style={{ fontSize: "0.85rem", padding: "0.4rem 0.8rem" }} onClick={() => void handleToggleEmployerType()}>
            Switch to {isIndividualEmployer(session.profile) ? "Company Profile" : "Personal Profile"}
          </button>
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
          <p>Applications are read from `job_applications`, the same collection used by the Kotlin services.</p>
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
  const pendingJob = useRef<{ id: string; employerId: string } | null>(null);
  const pendingInstant = useRef<{ id: string; employerId: string } | null>(null);
  const pageActive = useRef(true);
  const posting = useRef(false);
  const userToggledTab = useRef(false);

  const isIndividual = isIndividualEmployer(session.profile);
  const [postingMode, setPostingMode] = useState<"INSTANT" | "REGULAR">("REGULAR");
  const [published, setPublished] = useState(false);
  const [form, setForm] = useState<EmployerJobForm>(initialJobForm);
  const [instantForm, setInstantForm] = useState<InstantTaskForm>(initialInstantForm);
  const [submitting, setSubmitting] = useState(false);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [retryingProfile, setRetryingProfile] = useState(false);
  const inactiveAccount = Boolean(session.user && session.profile?.isActive === false);
  const profileUnavailable = Boolean(session.user && (session.error || !session.profile));
  const canPublish = Boolean(session.user && !session.loading && !profileUnavailable && !inactiveAccount && session.availableRoles.includes("EMPLOYER"));
  const draftOwnerId = session.user?.uid ?? null;
  const savedBusinessLocation = employerBaseLocationFromProfile(session.profile);
  const savedWorkLocations = useMemo(
    () => normalizeWorkLocations(session.profile?.workLocations),
    [session.profile?.workLocations]
  );
  const matchedSavedLocation = useMemo(
    () =>
      savedWorkLocations.find((location) =>
        postingMode === "INSTANT"
          ? location.address?.trim().toLowerCase() === instantForm.addressText.trim().toLowerCase()
          : matchesEmployerLocationForm(form, location)
      ) ?? null,
    [form, instantForm.addressText, postingMode, savedWorkLocations]
  );

  useEffect(() => {
    pageActive.current = true;
    return () => { pageActive.current = false; };
  }, []);

  useEffect(() => {
    if (!userToggledTab.current && session.profile) {
      setPostingMode(isIndividualEmployer(session.profile) ? "INSTANT" : "REGULAR");
    }
  }, [session.profile]);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(jobDraftKey);
      if (!raw) return;
      const draft = JSON.parse(raw);
      if (draft.version !== 1 || typeof draft.savedAt !== "number" || draft.savedAt > Date.now() || Date.now() - draft.savedAt > 3_600_000) {
        sessionStorage.removeItem(jobDraftKey);
        return;
      }
      if (draft.ownerId && draft.ownerId !== draftOwnerId) return;
      const fields = Object.keys(initialJobForm) as (keyof EmployerJobForm)[];
      const savedForm = { ...initialJobForm, ...draft.form };
      if (!draft.form || !fields.every((field) => typeof savedForm[field] === "string" && savedForm[field].length <= 10_000)) return;
      const restored = Object.fromEntries(fields.map((field) => [field, savedForm[field]])) as EmployerJobForm;
      if (draftOwnerId && draft.ownerId === draftOwnerId && validJobId(draft.pendingJobId)) {
        pendingJob.current = { id: draft.pendingJobId, employerId: draftOwnerId };
      }
      setForm(restored);
      if (draftOwnerId && !draft.ownerId) sessionStorage.setItem(jobDraftKey, JSON.stringify({ ...draft, ownerId: draftOwnerId }));
    } catch {
      setError("Your saved draft could not be restored. Please review the job details before posting.");
    }
  }, [draftOwnerId]);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(instantDraftKey);
      if (!raw) return;
      const draft = JSON.parse(raw);
      if (draft.version !== 1 || typeof draft.savedAt !== "number" || draft.savedAt > Date.now() || Date.now() - draft.savedAt > 3_600_000) {
        sessionStorage.removeItem(instantDraftKey);
        return;
      }
      if (draft.ownerId && draft.ownerId !== draftOwnerId) return;
      if (draft.form) {
        setInstantForm((current) => ({ ...current, ...draft.form }));
      }
      if (draftOwnerId && draft.ownerId === draftOwnerId && validJobId(draft.pendingInstantId)) {
        pendingInstant.current = { id: draft.pendingInstantId, employerId: draftOwnerId };
      }
    } catch {
      // ignore
    }
  }, [draftOwnerId]);

  useEffect(() => {
    setForm((current) => ({
      ...current,
      companyName: current.companyName || session.profile?.companyName || (session.user ? defaultCompanyName(session.profile) : ""),
      contactNumber: current.contactNumber || session.profile?.contactPhone || session.profile?.phone || "",
      latitude: current.latitude || toCoordinateText(session.profile?.businessLatitude),
      location: current.location || session.profile?.businessAddress || "",
      longitude: current.longitude || toCoordinateText(session.profile?.businessLongitude)
    }));

    setInstantForm((current) => ({
      ...current,
      contactNumber: current.contactNumber || session.profile?.contactPhone || session.profile?.phone || "",
      city: current.city || session.profile?.currentLocationAddress?.split(",")[0]?.trim() || "",
      addressText: current.addressText || session.profile?.businessAddress || session.profile?.address || "",
      latitude: current.latitude || toCoordinateText(session.profile?.businessLatitude || session.profile?.latitude),
      longitude: current.longitude || toCoordinateText(session.profile?.businessLongitude || session.profile?.longitude)
    }));
  }, [session.profile, session.user]);

  function handleApplySavedLocation(location: ProductWorkLocation) {
    setError(null);
    const latText = toCoordinateText(location.latitude);
    const lngText = toCoordinateText(location.longitude);
    setForm((current) => ({
      ...current,
      latitude: latText,
      location: location.address ?? current.location,
      longitude: lngText
    }));
    setInstantForm((current) => ({
      ...current,
      latitude: latText,
      addressText: location.address ?? current.addressText,
      longitude: lngText
    }));
  }

  async function handleUseCurrentLocation() {
    try {
      setLocating(true);
      setError(null);

      const position = await getBrowserPosition();
      const latText = position.coords.latitude.toFixed(6);
      const lngText = position.coords.longitude.toFixed(6);
      setForm((current) => ({
        ...current,
        latitude: latText,
        longitude: lngText
      }));
      setInstantForm((current) => ({
        ...current,
        latitude: latText,
        longitude: lngText
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

  async function handleInstantSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (posting.current || published || session.loading) return;
    if (inactiveAccount) { setError("This account is inactive. Contact support before posting."); return; }
    if (profileUnavailable) { setError("You are signed in, but your profile could not be loaded. Retry the account check."); return; }

    if (![instantForm.title, instantForm.city, instantForm.addressText, instantForm.contactNumber].every((v) => v.trim())) {
      setError("Please complete the task title, city, address, and contact number.");
      return;
    }
    const workers = Number(instantForm.workersNeeded);
    const pay = Number(instantForm.perPersonPayment);
    if (!Number.isSafeInteger(workers) || workers < 1 || workers > 20 || !Number.isFinite(pay) || pay <= 0) {
      setError("Enter 1-20 helpers needed and a positive payment amount per worker.");
      return;
    }
    const latitude = Number(instantForm.latitude);
    const longitude = Number(instantForm.longitude);
    const hasCoordinates = Boolean(instantForm.latitude.trim() && instantForm.longitude.trim()) &&
      Number.isFinite(latitude) && Number.isFinite(longitude) && hasValidCoordinates(latitude, longitude);
    if ((instantForm.latitude.trim() || instantForm.longitude.trim()) && !hasCoordinates) {
      setError("Enter both valid map coordinates, or leave both blank.");
      return;
    }

    if (!canPublish) {
      try {
        sessionStorage.setItem(instantDraftKey, JSON.stringify({ version: 1, savedAt: Date.now(), ownerId: session.user?.uid ?? null, form: instantForm }));
        router.push("/app/auth?role=EMPLOYER&next=%2Fapp%2Femployer%2Fpost-job");
      } catch {
        setError("Your browser could not keep this draft. Allow tab storage before continuing to sign in.");
      }
      return;
    }

    if (!services || !session.user) { setError(FIREBASE_SETUP_ERROR); return; }

    const activeServices = services;
    const activeUser = session.user;
    const isCurrentSubmission = () => pageActive.current && activeServices.auth.currentUser?.uid === activeUser.uid;

    try {
      posting.current = true;
      setSubmitting(true);
      setError(null);

      const instantRef = pendingInstant.current?.employerId === activeUser.uid
        ? doc(activeServices.db, "instant_requests", pendingInstant.current.id)
        : doc(collection(activeServices.db, "instant_requests"));
      pendingInstant.current = { id: instantRef.id, employerId: activeUser.uid };

      const jobRef = doc(activeServices.db, "jobs", instantRef.id);
      const currentTime = Date.now();
      const expiresAtMillis = instantForm.urgencyType === "right_now"
        ? currentTime + 4 * 3600 * 1000
        : instantForm.urgencyType === "today"
          ? currentTime + 24 * 3600 * 1000
          : currentTime + 48 * 3600 * 1000;

      const employerDisplayName = session.profile?.fullName?.trim() || session.profile?.name?.trim() || session.profile?.companyName?.trim() || "DutyPe Employer";
      const totalPayment = pay * workers;

      await runTransaction(activeServices.db, async (transaction) => {
        const existingInstant = await transaction.get(instantRef);
        if (existingInstant.exists()) {
          if (existingInstant.data()?.employerId !== activeUser.uid) {
            throw new Error("The original posting could not be verified for this account.");
          }
          return;
        }

        const userRef = doc(activeServices.db, "users", activeUser.uid);
        const currentProfile = await transaction.get(userRef);
        const currentWorkLocations = normalizeWorkLocations(currentProfile.data()?.workLocations);

        transaction.update(userRef, {
          businessAddress: instantForm.addressText.trim(),
          businessLatitude: hasCoordinates ? latitude : 0,
          businessLongitude: hasCoordinates ? longitude : 0,
          contactPhone: instantForm.contactNumber.trim(),
          updatedAt: currentTime,
          workLocations: matchedSavedLocation?.id
            ? incrementWorkLocationUsage(currentWorkLocations, matchedSavedLocation.id)
            : currentWorkLocations
        });

        transaction.set(instantRef, {
          requestId: instantRef.id,
          employerId: activeUser.uid,
          employerName: employerDisplayName,
          employerPhone: instantForm.contactNumber.trim(),
          contactNumber: instantForm.contactNumber.trim(),
          title: instantForm.title.trim(),
          description: instantForm.description.trim() || `Instant task: ${instantForm.title.trim()}`,
          category: instantForm.category.trim() || "Helper",
          workersNeeded: workers,
          needType: instantForm.urgencyType === "scheduled" ? "scheduled" : instantForm.urgencyType === "today" ? "today" : "urgent_now",
          status: "open",
          urgency: instantForm.urgencyType === "right_now" ? "urgent" : "today",
          urgencyType: instantForm.urgencyType,
          budgetText: `₹${pay} / person (${instantForm.durationText})`,
          perPersonPayment: pay,
          totalPayment,
          durationText: instantForm.durationText,
          lat: hasCoordinates ? latitude : 0,
          lng: hasCoordinates ? longitude : 0,
          geohash: "",
          addressText: `${instantForm.addressText.trim()}${instantForm.city ? `, ${instantForm.city.trim()}` : ""}`,
          radiusKm: 10,
          createdAt: new Date(currentTime),
          expiresAt: new Date(expiresAtMillis),
          responseCount: 0,
          callCount: 0,
          selectedWorkerId: "",
          selectedWorkerIds: [],
          completedWorkerIds: [],
          failureReason: ""
        });

        transaction.set(jobRef, {
          acceptedCount: 0,
          applicationCount: 0,
          city: instantForm.city.trim(),
          area: instantForm.area.trim(),
          category: instantForm.category.trim().toUpperCase() || "HELPER",
          companyName: employerDisplayName,
          contactNumber: instantForm.contactNumber.trim(),
          createdAt: currentTime,
          description: instantForm.description.trim() || `Instant task: ${instantForm.title.trim()}`,
          employerId: activeUser.uid,
          employerTrustTier: session.profile?.trustTier || "NEW",
          expiryDays: 2,
          expiresAt: expiresAtMillis,
          gender: "ANY",
          isActive: true,
          isFilled: false,
          isInstant: true,
          jobId: jobRef.id,
          jobType: "INSTANT",
          latitude: hasCoordinates ? latitude : 0,
          location: `${instantForm.addressText.trim()}${instantForm.city ? `, ${instantForm.city.trim()}` : ""}`,
          longitude: hasCoordinates ? longitude : 0,
          payAmount: String(pay),
          payType: "DAILY",
          postedAt: currentTime,
          shiftTiming: instantForm.durationText,
          title: `[Instant] ${instantForm.title.trim()}`,
          updatedAt: currentTime,
          vacancies: workers,
          vacancyStatus: "OPEN"
        });
      });

      if (!isCurrentSubmission()) return;
      setPublished(true);
      try { sessionStorage.removeItem(instantDraftKey); } catch {}
      router.push("/app/employer/jobs");
      router.refresh();
    } catch (submitError) {
      if (isCurrentSubmission()) setError(submitError instanceof Error ? submitError.message : "Failed to post instant task.");
    } finally {
      posting.current = false;
      if (isCurrentSubmission()) setSubmitting(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (posting.current || published || session.loading) {
      return;
    }
    if (inactiveAccount) { setError("This account is inactive. Contact support before posting."); return; }
    if (profileUnavailable) { setError("You are signed in, but your profile could not be loaded. Retry the account check."); return; }

    if (![form.title, form.companyName, form.city, form.location, form.contactNumber, form.description].every((value) => value.trim())) {
      setError("Complete the job title, company, city, location, contact number, and description.");
      return;
    }
    const payAmount = Number(form.payAmount);
    const vacancies = Number(form.vacancies);
    if (!Number.isFinite(payAmount) || payAmount <= 0 || !Number.isSafeInteger(vacancies) || vacancies < 1) {
      setError("Enter a positive pay amount and a whole number of vacancies.");
      return;
    }
    const latitude = Number(form.latitude);
    const longitude = Number(form.longitude);
    const hasCoordinates = Boolean(form.latitude.trim() && form.longitude.trim()) &&
      Number.isFinite(latitude) && Number.isFinite(longitude) && hasValidCoordinates(latitude, longitude);
    if ((form.latitude.trim() || form.longitude.trim()) && !hasCoordinates) {
      setError("Enter both valid map coordinates, or leave both blank.");
      return;
    }

    if (!canPublish) {
      try {
        sessionStorage.setItem(jobDraftKey, JSON.stringify({ version: 1, savedAt: Date.now(), ownerId: session.user?.uid ?? null, form }));
        router.push("/app/auth?role=EMPLOYER&next=%2Fapp%2Femployer%2Fpost-job");
      } catch {
        setError("Your browser could not keep this draft. Allow tab storage before continuing to sign in.");
      }
      return;
    }
    if (!services || !session.user) { setError(FIREBASE_SETUP_ERROR); return; }

    const activeServices = services;
    const activeUser = session.user;
    const isCurrentSubmission = () => pageActive.current && activeServices.auth.currentUser?.uid === activeUser.uid;

    try {
      posting.current = true;
      setSubmitting(true);
      setError(null);

      const jobRef = pendingJob.current?.employerId === activeUser.uid
        ? doc(activeServices.db, "jobs", pendingJob.current.id)
        : doc(collection(activeServices.db, "jobs"));
      pendingJob.current = { id: jobRef.id, employerId: activeUser.uid };
      const currentTime = Date.now();
      try {
        sessionStorage.setItem(jobDraftKey, JSON.stringify({
          version: 1, savedAt: currentTime, ownerId: activeUser.uid, form, pendingJobId: jobRef.id
        }));
      } catch {
        throw new Error("Your browser could not keep this posting request. Allow tab storage before publishing.");
      }
      const normalizedCategory =
        form.category.trim().toUpperCase() || deriveJobCategory(form.title, form.description);

      const jobDetails = {
        city: form.city.trim(),
        area: form.area.trim(),
        category: normalizedCategory,
        companyName: form.companyName.trim(),
        contactNumber: form.contactNumber.trim(),
        description: form.description.trim(),
        employerId: activeUser.uid,
        gender: form.gender,
        jobType: form.jobType,
        latitude: hasCoordinates ? latitude : 0,
        location: form.location.trim(),
        longitude: hasCoordinates ? longitude : 0,
        payAmount: form.payAmount.trim(),
        payType: form.payType,
        shiftTiming: form.shiftTiming.trim(),
        title: form.title.trim(),
        vacancies
      };

      await runTransaction(activeServices.db, async (transaction) => {
        const existingJob = await transaction.get(jobRef);
        if (existingJob.exists()) {
          const savedJob = existingJob.data();
          if (savedJob.employerId !== activeUser.uid) {
            throw new Error("The original posting could not be verified for this account.");
          }
          if (Object.entries(jobDetails).some(([field, value]) => savedJob[field] !== value)) {
            throw new Error("This job was already published with different details. Review it in My jobs.");
          }
          return;
        }

        const userRef = doc(activeServices.db, "users", activeUser.uid);
        const currentProfile = await transaction.get(userRef);
        const currentWorkLocations = normalizeWorkLocations(currentProfile.data()?.workLocations);
        transaction.update(userRef, {
          businessAddress: form.location.trim(),
          businessLatitude: hasCoordinates ? latitude : 0,
          businessLongitude: hasCoordinates ? longitude : 0,
          companyName: form.companyName.trim(),
          contactEmail: activeUser.email ?? session.profile?.email ?? "",
          contactPhone: form.contactNumber.trim(),
          updatedAt: currentTime,
          workLocations: matchedSavedLocation?.id
            ? incrementWorkLocationUsage(currentWorkLocations, matchedSavedLocation.id)
            : currentWorkLocations
        });

        transaction.set(jobRef, {
          ...jobDetails,
          acceptedCount: 0,
          applicationCount: 0,
          createdAt: currentTime,
          employerTrustTier: session.profile?.trustTier || "NEW",
          expiryDays: 15,
          expiresAt: currentTime + 15 * 24 * 60 * 60 * 1000,
          isActive: true,
          isFilled: false,
          jobId: jobRef.id,
          postedAt: currentTime,
          updatedAt: currentTime,
          vacancyStatus: "OPEN"
        });
      });

      if (!isCurrentSubmission()) return;
      setPublished(true);
      try {
        const draft = JSON.parse(sessionStorage.getItem(jobDraftKey) ?? "null");
        if (draft?.ownerId === activeUser.uid && draft.pendingJobId === jobRef.id) sessionStorage.removeItem(jobDraftKey);
      } catch {}
      router.push("/app/employer/jobs");
      router.refresh();
    } catch (submitError) {
      if (isCurrentSubmission()) setError(submitError instanceof Error ? submitError.message : "Failed to post job.");
    } finally {
      posting.current = false;
      if (isCurrentSubmission()) setSubmitting(false);
    }
  }

  const tabOrder = isIndividual ? (["INSTANT", "REGULAR"] as const) : (["REGULAR", "INSTANT"] as const);

  return (
    <div className="product-section-stack">
      {error ? <div className="callout" role="alert">{error}</div> : null}
      {session.loading ? <p role="status">Loading your account...</p>
        : inactiveAccount ? <p className="callout" role="alert">This account is inactive. <Link href="/contact">Contact support</Link>.</p>
          : profileUnavailable ? <div className="callout" role="alert"><p>You are signed in, but your profile is unavailable. Your draft is still here.</p><button type="button" className="button ghost" disabled={retryingProfile} onClick={async () => { setRetryingProfile(true); try { await session.refreshProfile(); setError(null); } catch { setError("Your profile is still unavailable. Please try again later."); } finally { setRetryingProfile(false); } }}>{retryingProfile ? "Checking account..." : "Retry account check"}</button></div>
            : !session.user ? <p className="callout">Sign in with Google, email or a mobile verification code before publishing.</p>
              : !canPublish ? <p className="callout">You are signed in. Continue to set up employer access before publishing.</p> : null}

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">{postingMode === "INSTANT" ? "Urgent hiring" : "Standard hiring"}</span>
            <h2>{postingMode === "INSTANT" ? "Post an Instant Task" : "Post a Regular Job Vacancy"}</h2>
          </div>
        </div>

        <div className="product-tab-row post-job-tabs" role="tablist" aria-label="Job Posting Type" style={{ marginBottom: "1rem" }}>
          {tabOrder.map((mode) => {
            const isInstantMode = mode === "INSTANT";
            const isActive = postingMode === mode;
            return (
              <button
                key={mode}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={`product-tab ${isActive ? "active" : ""}`}
                onClick={() => {
                  userToggledTab.current = true;
                  setPostingMode(mode);
                  setError(null);
                }}
                style={{ fontSize: "0.95rem", fontWeight: 600, padding: "0.6rem 1.25rem" }}
              >
                {isInstantMode ? "⚡ Instant Task / Urgent Help" : "📋 Regular Job Vacancy"}
              </button>
            );
          })}
        </div>

        <div className="callout note-banner" style={{ marginBottom: "1.5rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "0.5rem" }}>
            <div>
              <strong>{isIndividual ? "Personal Account" : "Company Account"}</strong> &bull;{" "}
              {postingMode === "INSTANT"
                ? "Instant Tasks are shown first for personal & urgent needs. Local workers are alerted immediately for quick turnaround."
                : "Regular Vacancies are shown for ongoing employment with monthly/daily pay, shifts, and multiple openings."}
            </div>
            <span className="pill" style={{ textTransform: "none", fontSize: "0.75rem" }}>
              {isIndividual ? "👤 Personal Profile" : "🏢 Company Profile"}
            </span>
          </div>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">{postingMode === "INSTANT" ? "Instant task tips" : "Before you publish"}</span>
            <h3>{postingMode === "INSTANT" ? "Get helpers faster" : "What makes a job post convert better"}</h3>
            <ul className="detail-list">
              {postingMode === "INSTANT" ? (
                <>
                  <li>
                    <strong>Immediate payout</strong>
                    <span>Fair hourly or daily pay attracts verified workers in minutes.</span>
                  </li>
                  <li>
                    <strong>Precise landmark</strong>
                    <span>Helpers reach quickly when the exact house/street landmark is given.</span>
                  </li>
                  <li>
                    <strong>Clear timing</strong>
                    <span>State if work starts right now or scheduled for a specific hour.</span>
                  </li>
                </>
              ) : (
                <>
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
                </>
              )}
            </ul>
          </article>

          {postingMode === "INSTANT" ? (
            <article className="detail-panel">
              <span className="card-kicker">⚡ Instant task preview</span>
              <h3>{instantForm.title || "Your instant task"}</h3>
              <p>
                {instantForm.description || "Describe the work clearly so nearby workers can accept immediately."}
              </p>
              <div className="pill-row">
                <span className="pill warning">⚡ Instant Urgent Need</span>
                <span className="pill">{instantForm.category || "Helper"}</span>
                <span className="pill">{instantForm.addressText || instantForm.city || "Location pending"}</span>
                <span className="pill">₹{instantForm.perPersonPayment || "500"}/helper ({instantForm.durationText})</span>
                <span className="pill">{instantForm.workersNeeded} helper{Number(instantForm.workersNeeded) > 1 ? "s" : ""}</span>
                <span className="pill">
                  {hasValidCoordinates(Number(instantForm.latitude), Number(instantForm.longitude))
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
          ) : (
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
          )}
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
                  <strong>{location.label || location.address}</strong>
                  <span>{location.address}</span>
                  {typeof location.usageCount === "number" && location.usageCount > 0 ? (
                    <small>{location.usageCount} previous posts</small>
                  ) : null}
                </button>
              ))}
            </div>
          </article>
        ) : null}

        {postingMode === "INSTANT" ? (
          <form className="product-form editor-form" onSubmit={handleInstantSubmit} aria-busy={submitting || session.loading}>
            <label className="editor-form-wide">
              <span>Task title (What do you need done?)</span>
              <input
                required
                value={instantForm.title}
                onChange={(event) => setInstantForm((current) => ({ ...current, title: event.target.value }))}
                placeholder="e.g. Electrician urgently for fan & switch repair, House cleaning helper today"
              />
            </label>

            <label>
              <span>Category / Skill</span>
              <select
                value={instantForm.category}
                onChange={(event) => setInstantForm((current) => ({ ...current, category: event.target.value }))}
              >
                {instantCategories.map((cat) => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </label>

            <label>
              <span>When is help needed?</span>
              <select
                value={instantForm.urgencyType}
                onChange={(event) => setInstantForm((current) => ({
                  ...current,
                  urgencyType: event.target.value as any,
                  needType: event.target.value === "scheduled" ? "scheduled" : event.target.value === "today" ? "today" : "urgent_now"
                }))}
              >
                <option value="right_now">⚡ Right now (within 2-4 hours)</option>
                <option value="today">📅 Today</option>
                <option value="tomorrow">⏰ Tomorrow</option>
                <option value="scheduled">📆 Scheduled date/time</option>
              </select>
            </label>

            <label>
              <span>Helpers needed</span>
              <input
                required
                type="number"
                min="1"
                max="20"
                step="1"
                value={instantForm.workersNeeded}
                onChange={(event) => setInstantForm((current) => ({
                  ...current,
                  workersNeeded: event.target.value,
                  totalPayment: String((Number(event.target.value) || 1) * (Number(current.perPersonPayment) || 0))
                }))}
              />
            </label>

            <label>
              <span>Pay per helper (₹)</span>
              <input
                required
                type="number"
                min="1"
                step="1"
                value={instantForm.perPersonPayment}
                onChange={(event) => setInstantForm((current) => ({
                  ...current,
                  perPersonPayment: event.target.value,
                  totalPayment: String((Number(event.target.value) || 0) * (Number(current.workersNeeded) || 1))
                }))}
                placeholder="500"
              />
            </label>

            <label>
              <span>Expected duration</span>
              <select
                value={instantForm.durationText}
                onChange={(event) => setInstantForm((current) => ({ ...current, durationText: event.target.value }))}
              >
                {instantDurations.map((dur) => (
                  <option key={dur} value={dur}>{dur}</option>
                ))}
              </select>
            </label>

            <label>
              <span>Total payout (Estimated)</span>
              <input
                disabled
                value={`₹${(Number(instantForm.perPersonPayment) || 0) * (Number(instantForm.workersNeeded) || 1)} total`}
              />
            </label>

            <label>
              <span>City</span>
              <input
                required
                list="city-suggestions"
                value={instantForm.city}
                onChange={(event) => setInstantForm((current) => ({ ...current, city: event.target.value }))}
                placeholder="e.g. Hyderabad"
              />
            </label>

            <label>
              <span>Area or locality</span>
              <input
                value={instantForm.area}
                onChange={(event) => setInstantForm((current) => ({ ...current, area: event.target.value }))}
                placeholder="e.g. Madhapur, Kukatpally, Banjara Hills"
              />
            </label>

            <label className="editor-form-wide">
              <span>Exact task address or landmark</span>
              <input
                required
                value={instantForm.addressText}
                onChange={(event) => setInstantForm((current) => ({ ...current, addressText: event.target.value }))}
                placeholder="House/flat no, building name, street, landmark"
              />
            </label>

            <label>
              <span>Latitude</span>
              <input
                type="number"
                min="-90"
                max="90"
                step="any"
                value={instantForm.latitude}
                onChange={(event) => setInstantForm((current) => ({ ...current, latitude: event.target.value }))}
                placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.latitude) : "17.448294"}
              />
            </label>

            <label>
              <span>Longitude</span>
              <input
                type="number"
                min="-180"
                max="180"
                step="any"
                value={instantForm.longitude}
                onChange={(event) => setInstantForm((current) => ({ ...current, longitude: event.target.value }))}
                placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.longitude) : "78.391487"}
              />
            </label>

            <label>
              <span>Contact number</span>
              <input
                required
                type="tel"
                autoComplete="tel"
                value={instantForm.contactNumber}
                onChange={(event) => setInstantForm((current) => ({ ...current, contactNumber: event.target.value }))}
                placeholder="9876543210"
              />
            </label>

            <label className="editor-form-wide">
              <span>Task instructions & details</span>
              <textarea
                required
                rows={4}
                value={instantForm.description}
                onChange={(event) => setInstantForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="Explain the work clearly: tools provided, any specific requirements, timing, landmark directions..."
              />
            </label>

            <div className="editor-form-actions button-row">
              <button
                type="submit"
                className="button"
                disabled={submitting || published || session.loading || inactiveAccount || profileUnavailable}
              >
                {published ? "Published" : submitting ? "Posting..." : session.loading ? "Loading account..." : canPublish ? "Post Instant Task" : session.user ? "Continue as employer" : "Sign in to post task"}
              </button>
            </div>
          </form>
        ) : (
          <form className="product-form editor-form" onSubmit={handleSubmit} aria-busy={submitting || session.loading}>
            <label className="editor-form-wide">
              <span>Job title</span>
              <input
                required
                value={form.title}
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                placeholder="Retail associate, warehouse supervisor, delivery executive"
              />
            </label>

            <label>
              <span>Company name</span>
              <input
                required
                autoComplete="organization"
                value={form.companyName}
                onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))}
                placeholder="Acme Enterprises"
              />
            </label>

            <label>
              <span>City</span>
              <input
                required
                list="city-suggestions"
                value={form.city}
                onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))}
                placeholder="Hyderabad"
              />
            </label>

            <label>
              <span>Area or locality</span>
              <input
                value={form.area}
                onChange={(event) => setForm((current) => ({ ...current, area: event.target.value }))}
                placeholder="Madhapur"
              />
            </label>

            <label className="editor-form-wide">
              <span>Job location address</span>
              <input
                required
                value={form.location}
                onChange={(event) => setForm((current) => ({ ...current, location: event.target.value }))}
                placeholder="Plot 12, Hitec City Main Road"
              />
            </label>

            <label>
              <span>Latitude</span>
              <input
                type="number"
                min="-90"
                max="90"
                step="any"
                value={form.latitude}
                onChange={(event) => setForm((current) => ({ ...current, latitude: event.target.value }))}
                placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.latitude) : "17.448294"}
              />
            </label>

            <label>
              <span>Longitude</span>
              <input
                type="number"
                min="-180"
                max="180"
                step="any"
                value={form.longitude}
                onChange={(event) => setForm((current) => ({ ...current, longitude: event.target.value }))}
                placeholder={savedBusinessLocation ? toCoordinateText(savedBusinessLocation.longitude) : "78.391487"}
              />
            </label>

            <label>
              <span>Contact number</span>
              <input
                required
                type="tel"
                autoComplete="tel"
                value={form.contactNumber}
                onChange={(event) => setForm((current) => ({ ...current, contactNumber: event.target.value }))}
              />
            </label>

            <label>
              <span>Pay amount</span>
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
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
                required
                type="number"
                min="1"
                step="1"
                value={form.vacancies}
                onChange={(event) => setForm((current) => ({ ...current, vacancies: event.target.value }))}
              />
            </label>

            <label className="editor-form-wide">
              <span>Description</span>
              <textarea
                required
                rows={6}
                value={form.description}
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="Role details, responsibilities, and candidate expectations"
              />
            </label>

            <div className="editor-form-actions button-row">
              <button type="submit" className="button" disabled={submitting || published || session.loading || inactiveAccount || profileUnavailable}>
                {published ? "Published" : submitting ? "Posting..." : session.loading ? "Loading account..." : canPublish ? "Post job" : session.user ? "Continue as employer" : "Sign in to post job"}
              </button>
            </div>
          </form>
        )}
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
          city: nextJob.city,
          area: nextJob.area,
          category: nextJob.category,
          companyName:
            nextJob.companyName ||
            session.profile?.companyName ||
            defaultCompanyName(session.profile),
          contactNumber:
            nextJob.contactNumber ||
            session.profile?.contactPhone ||
            session.profile?.phone ||
            "",
          description: nextJob.description,
          gender: nextJob.gender || "ANY",
          jobType: nextJob.jobType || "FULL_TIME",
          latitude: toCoordinateText(nextJob.latitude),
          location: nextJob.location,
          longitude: toCoordinateText(nextJob.longitude),
          payAmount: nextJob.payAmount,
          payType: nextJob.payType || "MONTHLY",
          shiftTiming: nextJob.shiftTiming,
          title: nextJob.title,
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

    if (!form.city.trim()) { setError("Enter the job's city before saving."); return; }

    try {
      setSaving(true);
      setError(null);

      const currentTime = Date.now();
      const batch = writeBatch(services.db);
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

      batch.update(doc(services.db, "users", session.user.uid), {
        businessAddress: form.location.trim(),
        businessLatitude: finalLatitude,
        businessLongitude: finalLongitude,
        companyName: form.companyName.trim(),
        contactEmail: session.user.email ?? session.profile?.email ?? "",
        contactPhone: form.contactNumber.trim(),
        updatedAt: currentTime,
        workLocations: nextWorkLocations
      });

      batch.update(doc(services.db, "jobs", job.id), {
        city: form.city.trim(),
        area: form.area.trim(),
        category: form.category.trim().toUpperCase() || deriveJobCategory(form.title, form.description),
        companyName: form.companyName.trim(),
        contactNumber: form.contactNumber.trim(),
        description: form.description.trim(),
        gender: form.gender,
        jobType: form.jobType,
        latitude: finalLatitude,
        location: form.location.trim(),
        longitude: finalLongitude,
        payAmount: form.payAmount.trim(),
        payType: form.payType,
        shiftTiming: form.shiftTiming.trim(),
        title: form.title.trim(),
        updatedAt: currentTime,
        vacancies: Math.max(1, Number(form.vacancies || "1"))
      });

      await batch.commit();
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
            <p>{job.location || "Location not available."}</p>
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
          <label><span>City</span><input disabled={!canEdit} maxLength={80} value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} /></label>
          <label><span>Area or locality</span><input disabled={!canEdit} maxLength={100} value={form.area} onChange={(event) => setForm((current) => ({ ...current, area: event.target.value }))} /></label>
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
  const [filter, setFilter] = useState<"ALL" | "REGULAR" | "INSTANT">("ALL");

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
      await updateDoc(doc(services.db, "jobs", job.id), {
        isActive: !job.isActive,
        updatedAt: Date.now()
      });
      setJobs((current) =>
        current.map((item) =>
          item.id === job.id
            ? {
                ...item,
                isActive: !item.isActive
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

  const regularJobs = useMemo(
    () => jobs.filter((job) => job.jobType !== "INSTANT" && !(job as { isInstant?: boolean }).isInstant),
    [jobs]
  );
  const instantJobs = useMemo(
    () => jobs.filter((job) => job.jobType === "INSTANT" || (job as { isInstant?: boolean }).isInstant),
    [jobs]
  );

  const displayedJobs = useMemo(() => {
    if (filter === "REGULAR") return regularJobs;
    if (filter === "INSTANT") return instantJobs;
    return jobs;
  }, [filter, jobs, regularJobs, instantJobs]);

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Jobs error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Total posts</span>
            <strong>{jobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>📋 Regular vacancies</span>
            <strong>{regularJobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>⚡ Instant tasks</span>
            <strong>{instantJobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Applications</span>
            <strong>{jobs.reduce((sum, job) => sum + job.applicationCount, 0)}</strong>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer listings</span>
            <h2>Manage posted jobs & tasks</h2>
          </div>
          <div className="button-row compact">
            <Link href="/app/employer/post-job" className="button">
              + Post new job / task
            </Link>
          </div>
        </div>

        {/* Tab Filters */}
        <div className="button-row compact" style={{ marginBottom: "1rem" }}>
          <button
            type="button"
            className={`button ${filter === "ALL" ? "" : "ghost"}`}
            onClick={() => setFilter("ALL")}
          >
            All Listings ({jobs.length})
          </button>
          <button
            type="button"
            className={`button ${filter === "REGULAR" ? "" : "ghost"}`}
            onClick={() => setFilter("REGULAR")}
          >
            📋 Regular Vacancies ({regularJobs.length})
          </button>
          <button
            type="button"
            className={`button ${filter === "INSTANT" ? "" : "ghost"}`}
            onClick={() => setFilter("INSTANT")}
          >
            ⚡ Instant Tasks ({instantJobs.length})
          </button>
        </div>

        {loading ? (
          <div className="empty-state">Loading your posted listings...</div>
        ) : displayedJobs.length === 0 ? (
          <div className="empty-state">
            <p>
              {filter === "REGULAR"
                ? "No regular job vacancies posted yet."
                : filter === "INSTANT"
                ? "No instant tasks posted yet."
                : "No listings posted yet."}
            </p>
            <div className="button-row compact" style={{ marginTop: "0.75rem", justifyContent: "center" }}>
              <Link href="/app/employer/post-job" className="button">
                Post {filter === "INSTANT" ? "an instant task" : "a job"} now
              </Link>
            </div>
          </div>
        ) : (
          <div className="section-grid">
            {displayedJobs.map((job) => (
              <EmployerJobCard
                key={job.id}
                action={() => handleToggleActive(job)}
                actionLabel={job.isActive ? "Pause post" : "Reopen post"}
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
            collection(activeServices.db, "job_applications"),
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
        application.workerName.toLowerCase().includes(searchValue) ||
        application.jobTitle.toLowerCase().includes(searchValue) ||
        application.companyName.toLowerCase().includes(searchValue) ||
        application.coverLetter.toLowerCase().includes(searchValue);

      return matchesStatus && matchesSearch;
    }
  );

  async function handleOpenConversation(application: ProductApplication) {
    try {
      setError(null);

      const conversationId = await getOrCreateConversationId(
        application.workerId,
        application.jobId || application.id
      );

      router.push(productConversationRoute("EMPLOYER", conversationId));
    } catch (conversationError) {
      setError(
        conversationError instanceof Error
          ? conversationError.message
          : "Failed to open worker chat."
      );
    }
  }

  async function handleStatusUpdate(
    application: ProductApplication,
    nextStatus: ProductApplicationStatus
  ) {
    if (!services || !session.user || application.status === nextStatus || busyApplicationId) {
      return;
    }

    const employerId = session.user.uid;
    try {
      setBusyApplicationId(application.id);
      setError(null);
      const verification = nextStatus === "ACCEPTED"
        ? await createPendingWorkVerification({
          applicationId: application.id,
          db: services.db,
          employerId,
          employerName: application.companyName || session.profile?.companyName || "DutyPe employer",
          jobId: application.jobId,
          jobTitle: application.jobTitle,
          workerId: application.workerId,
          workerName: application.workerName
        })
        : null;

      const applicationRef = doc(services.db, "job_applications", application.id);
      const updatedApplication = await runTransaction(services.db, async (transaction) => {
        const snapshot = await transaction.get(applicationRef);
        if (!snapshot.exists()) throw new Error("This application is no longer available.");
        const current = normalizeProductApplication(snapshot.id, snapshot.data());
        if (current.employerId !== employerId) throw new Error("You don't have access to this application.");
        if (current.status === nextStatus) return current;

        const allowed = nextStatus === "UNDER_REVIEW"
          ? canEmployerMoveToUnderReview(current.status)
          : (nextStatus === "ACCEPTED" || nextStatus === "REJECTED") && canEmployerAcceptOrReject(current.status);
        if (!allowed) throw new Error("This application's status has changed. Refresh before trying again.");

        const currentTime = Date.now();
        const nextHistory = [...current.statusHistory, {
          notes: nextStatusNote(nextStatus),
          status: nextStatus,
          systemUpdate: false,
          timestamp: currentTime,
          updatedAt: currentTime,
          updatedBy: employerId
        }];
        const updatePayload: Record<string, unknown> = {
          status: nextStatus,
          statusHistory: nextHistory,
          updatedAt: currentTime
        };

        if (verification) {
          if (current.jobId !== application.jobId || current.workerId !== application.workerId) {
            throw new Error("This application has changed. Refresh before trying again.");
          }
          const jobRef = doc(services.db, "jobs", current.jobId);
          const jobSnapshot = await transaction.get(jobRef);
          const job = normalizeProductJob(jobSnapshot.id, jobSnapshot.data());
          if (!jobSnapshot.exists() || job.employerId !== employerId || !isLiveJob(job) || job.acceptedCount >= job.vacancies) {
            throw new Error("This job has no open vacancies.");
          }
          const acceptedCount = job.acceptedCount + 1;
          const isFilled = acceptedCount >= job.vacancies;
          transaction.update(jobRef, {
            acceptedCount,
            isFilled,
            updatedAt: currentTime,
            vacancyStatus: isFilled ? "FILLED" : "OPEN"
          });
          Object.assign(updatePayload, {
            verification,
            verificationCode: verification.verificationCode,
            verificationId: verification.verificationId,
            verificationStatus: verification.status
          });
        }

        transaction.update(applicationRef, updatePayload);
        return { ...current, status: nextStatus, statusHistory: nextHistory, updatedAt: currentTime };
      });
      setApplications((current) => current.map((item) => item.id === application.id ? updatedApplication : item));
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
      {error ? <div className="callout" role="alert">{error}</div> : null}

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
            Status changes here write back to `job_applications`, update job fill state, and
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
