"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  getOrCreateConversationId,
  productConversationRoute
} from "@/lib/firebase/chat-actions";
import {
  formatCurrencyRange,
  formatDate,
  formatDateTime,
  readTimestamp
} from "@/lib/firebase/firestore-helpers";
import {
  buildEmployerReviewWorkerProfile,
  mergeEmployerReviewApplicationWithProfile,
  normalizeEmployerReviewApplication,
  type EmployerReviewApplication,
  type EmployerReviewWorkerProfile
} from "@/lib/product/employer-review";
import {
  canEmployerAcceptOrReject,
  canEmployerMarkWorkComplete,
  canEmployerMoveToUnderReview,
  canEmployerRateWorker,
  canEmployerVerifyWork,
  normalizeProductJob,
  productStatusLabel,
  productStatusTone,
  toStorageApplicationStatus,
  type ProductApplicationStatus,
  type ProductJob
} from "@/lib/product/marketplace";
import {
  createPendingWorkVerification,
  verificationExpiryText,
  verificationIsExpired,
  type ProductVerificationLocation
} from "@/lib/product/work-verification";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type EmployerApplicationDetailClientProps = SharedProps & {
  applicationId: string;
};

type EmployerWorkerProfileClientProps = SharedProps & {
  applicationId?: string | null;
  workerId: string;
};

type EmployerWorkerRating = {
  applicationId: string;
  communicationRating: number;
  createdAt: unknown;
  feedback: string;
  id: string;
  overallRating: number;
  professionalismRating: number;
  punctualityRating: number;
  qualityRating: number;
  tags: string[];
};

type EmployerWorkerRatingForm = {
  communicationRating: number;
  feedback: string;
  overallRating: number;
  professionalismRating: number;
  punctualityRating: number;
  qualityRating: number;
  tags: string[];
};

const initialEmployerWorkerRatingForm: EmployerWorkerRatingForm = {
  communicationRating: 0,
  feedback: "",
  overallRating: 0,
  professionalismRating: 0,
  punctualityRating: 0,
  qualityRating: 0,
  tags: []
};

const workerRatingTags = [
  "On Time",
  "Hard Working",
  "Professional",
  "Good Communication",
  "Skilled",
  "Reliable",
  "Friendly",
  "Quick Learner",
  "Late",
  "Unprofessional",
  "Poor Communication",
  "Needs Improvement"
];

function stringList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((item): item is string => typeof item === "string" && item.trim().length > 0);
}

function numberValue(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

function normalizeEmployerWorkerRating(
  id: string,
  value: Record<string, unknown> | undefined
): EmployerWorkerRating {
  const payload = value ?? {};

  return {
    applicationId: typeof payload.applicationId === "string" ? payload.applicationId : "",
    communicationRating: numberValue(payload.communicationRating),
    createdAt: payload.createdAt ?? Date.now(),
    feedback: typeof payload.feedback === "string" ? payload.feedback : "",
    id,
    overallRating: numberValue(payload.overallRating),
    professionalismRating: numberValue(payload.professionalismRating),
    punctualityRating: numberValue(payload.punctualityRating),
    qualityRating: numberValue(payload.qualityRating),
    tags: stringList(payload.tags)
  };
}

function employerWorkerRatingDocId(applicationId: string, employerId: string) {
  return `${applicationId}_${employerId}`;
}

async function readBrowserVerificationLocation(): Promise<ProductVerificationLocation | null> {
  if (typeof navigator === "undefined" || !navigator.geolocation) {
    return null;
  }

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) =>
        resolve({
          lat: position.coords.latitude,
          lng: position.coords.longitude
        }),
      () => resolve(null),
      {
        enableHighAccuracy: true,
        maximumAge: 60_000,
        timeout: 10_000
      }
    );
  });
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

function initialsForName(name: string) {
  const initials = name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  return initials || "DP";
}

async function loadWorkerSourceData(workerId: string) {
  const services = getFirebaseServices();

  if (!services) {
    throw new Error("Firebase is not configured for employer review.");
  }

  const [userSnapshot, workerProfileSnapshot] = await Promise.all([
    getDoc(doc(services.db, "users", workerId)),
    getDoc(doc(services.db, "worker_profiles", workerId))
  ]);

  return {
    userProfile: userSnapshot.exists()
      ? (userSnapshot.data() as Record<string, unknown>)
      : null,
    workerProfile: workerProfileSnapshot.exists()
      ? (workerProfileSnapshot.data() as Record<string, unknown>)
      : null
  };
}

function ReviewAvatar({
  imageUrl,
  name
}: {
  imageUrl?: string | null;
  name: string;
}) {
  if (imageUrl?.trim()) {
    return (
      <img
        alt={name}
        className="review-avatar-image"
        src={imageUrl}
      />
    );
  }

  return <span>{initialsForName(name)}</span>;
}

function TimelineList({ application }: { application: EmployerReviewApplication }) {
  const history = application.statusHistory.length > 0
    ? [...application.statusHistory].sort((left, right) => {
        const leftTime = readTimestamp(left.timestamp)?.getTime() ?? 0;
        const rightTime = readTimestamp(right.timestamp)?.getTime() ?? 0;
        return rightTime - leftTime;
      })
    : [];

  return (
    <article className="detail-panel">
      <span className="card-kicker">Application timeline</span>
      <h3>Status history</h3>
      {history.length === 0 ? (
        <p>No status history has been recorded yet.</p>
      ) : (
        <div className="timeline-stack">
          {history.map((entry, index) => (
            <div key={`${entry.status}-${index}`} className="timeline-item">
              <span className={`status-pill ${productStatusTone(entry.status)}`}>
                {productStatusLabel(entry.status)}
              </span>
              <strong>{formatDateTime(entry.timestamp)}</strong>
              <p>{entry.notes || "No notes added for this update."}</p>
            </div>
          ))}
        </div>
      )}
    </article>
  );
}

function verificationTone(application: EmployerReviewApplication): string {
  if (application.verificationStatus === "VERIFIED") {
    return "success";
  }

  if (verificationIsExpired(application.verification)) {
    return "danger";
  }

  if (application.verificationStatus === "PENDING" || application.status === "ACCEPTED") {
    return "warning";
  }

  return "neutral";
}

function RatingScaleField({
  label,
  onChange,
  value
}: {
  label: string;
  onChange: (nextValue: number) => void;
  value: number;
}) {
  return (
    <div className="rating-field">
      <span>{label}</span>
      <div className="score-picker">
        {[1, 2, 3, 4, 5].map((score) => (
          <button
            key={score}
            type="button"
            className={`score-option${value === score ? " active" : ""}`}
            onClick={() => onChange(score)}
          >
            {score}
          </button>
        ))}
      </div>
    </div>
  );
}

function WorkerProfilePanel({
  application,
  profile
}: {
  application?: EmployerReviewApplication | null;
  profile: EmployerReviewWorkerProfile;
}) {
  return (
    <>
      <article className="detail-panel">
        <div className="review-hero">
          <div className="review-avatar">
            <ReviewAvatar imageUrl={profile.profileImageUrl} name={profile.fullName} />
          </div>
          <div className="review-hero-copy">
            <span className="card-kicker">Worker snapshot</span>
            <h3>{profile.fullName}</h3>
            <p>
              {profile.location || "Location not set yet."}
              {profile.expectedSalary ? ` Expected salary: ${profile.expectedSalary}.` : ""}
            </p>
          </div>
        </div>

        <div className="pill-row">
          {profile.skills.slice(0, 4).map((skill) => (
            <span key={skill} className="pill">
              {skill}
            </span>
          ))}
          {application ? (
            <span className={`status-pill ${productStatusTone(application.status)}`}>
              {productStatusLabel(application.status)}
            </span>
          ) : null}
        </div>
      </article>

      <div className="detail-grid">
        <article className="detail-panel">
          <span className="card-kicker">Contact</span>
          <h3>Direct worker information</h3>
          <ul className="detail-list">
            <li>
              <strong>Email</strong>
              <span>{profile.email || "Not available"}</span>
            </li>
            <li>
              <strong>Phone</strong>
              <span>{profile.phone || "Not available"}</span>
            </li>
            <li>
              <strong>Location</strong>
              <span>{profile.location || "Not available"}</span>
            </li>
            <li>
              <strong>Gender</strong>
              <span>{profile.gender || "Not set"}</span>
            </li>
            <li>
              <strong>Date of birth</strong>
              <span>{profile.dateOfBirth || "Not set"}</span>
            </li>
            <li>
              <strong>Availability</strong>
              <span>{profile.availability || "Not set"}</span>
            </li>
          </ul>
        </article>

        <article className="detail-panel">
          <span className="card-kicker">Skills</span>
          <h3>Readiness and role fit</h3>
          {profile.skills.length > 0 ? (
            <div className="pill-row">
              {profile.skills.map((skill) => (
                <span key={skill} className="pill">
                  {skill}
                </span>
              ))}
            </div>
          ) : (
            <p>No structured skills are available yet.</p>
          )}

          {profile.certifications.length > 0 ? (
            <>
              <h3>Certifications</h3>
              <div className="pill-row">
                {profile.certifications.map((certification) => (
                  <span key={certification} className="pill">
                    {certification}
                  </span>
                ))}
              </div>
            </>
          ) : null}

          {profile.languages.length > 0 ? (
            <p>Languages: {profile.languages.join(", ")}</p>
          ) : null}
        </article>
      </div>

      <div className="detail-grid">
        <article className="detail-panel">
          <span className="card-kicker">Work experience</span>
          <h3>Past work context</h3>
          {profile.workExperience.length > 0 ? (
            <div className="timeline-stack">
              {profile.workExperience.map((experience) => (
                <div key={experience.id} className="timeline-item">
                  <strong>{experience.position || "Role not set"}</strong>
                  <p>{experience.company || "Company not set"}</p>
                  <small>
                    {[experience.startDate, experience.endDate].filter(Boolean).join(" - ") || "Dates not set"}
                  </small>
                  {experience.description ? <p>{experience.description}</p> : null}
                </div>
              ))}
            </div>
          ) : profile.workExperienceText ? (
            <p>{profile.workExperienceText}</p>
          ) : (
            <p>No work experience has been added yet.</p>
          )}
        </article>

        <article className="detail-panel">
          <span className="card-kicker">Education and resume</span>
          <h3>Extra qualification context</h3>
          {profile.education.length > 0 ? (
            <div className="timeline-stack">
              {profile.education.map((education) => (
                <div key={education.id} className="timeline-item">
                  <strong>{education.degree || "Qualification"}</strong>
                  <p>{education.institution || "Institution not set"}</p>
                  <small>
                    {[education.startDate, education.endDate].filter(Boolean).join(" - ") || "Dates not set"}
                  </small>
                </div>
              ))}
            </div>
          ) : (
            <p>No education records are available.</p>
          )}

          {profile.resumeUrl ? (
            <div className="button-row compact">
              <a
                className="button ghost"
                href={profile.resumeUrl}
                rel="noreferrer"
                target="_blank"
              >
                Open resume
              </a>
            </div>
          ) : null}
        </article>
      </div>
    </>
  );
}

export function EmployerApplicationDetailClient({
  applicationId,
  session
}: EmployerApplicationDetailClientProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [application, setApplication] = useState<EmployerReviewApplication | null>(null);
  const [existingRating, setExistingRating] = useState<EmployerWorkerRating | null>(null);
  const [job, setJob] = useState<ProductJob | null>(null);
  const [workerProfile, setWorkerProfile] = useState<EmployerReviewWorkerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [ratingBusy, setRatingBusy] = useState(false);
  const [ratingForm, setRatingForm] = useState<EmployerWorkerRatingForm>(
    initialEmployerWorkerRatingForm
  );
  const [error, setError] = useState<string | null>(null);
  const [verificationCodeInput, setVerificationCodeInput] = useState("");

  useEffect(() => {
    if (!services || !session.user) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    const activeUser = session.user;
    let cancelled = false;

    async function loadDetail() {
      try {
        setLoading(true);
        setError(null);

        const applicationSnapshot = await getDoc(
          doc(activeServices.db, "applications", applicationId)
        );

        if (!applicationSnapshot.exists()) {
          throw new Error("This application could not be found.");
        }

        const rawApplication = normalizeEmployerReviewApplication(
          applicationSnapshot.id,
          applicationSnapshot.data() as Record<string, unknown>
        );

        if (rawApplication.employerId !== activeUser.uid) {
          throw new Error("You can only review applications for your own employer account.");
        }

        const [{ userProfile, workerProfile: workerProfileDoc }, jobSnapshot] = await Promise.all([
          loadWorkerSourceData(rawApplication.workerId),
          getDoc(doc(activeServices.db, "jobs", rawApplication.jobId))
        ]);

        const nextApplication = mergeEmployerReviewApplicationWithProfile(
          rawApplication,
          userProfile,
          workerProfileDoc
        );
        const nextWorkerProfile = buildEmployerReviewWorkerProfile(
          rawApplication.workerId,
          userProfile,
          workerProfileDoc,
          nextApplication
        );
        const nextJob = jobSnapshot.exists()
          ? normalizeProductJob(jobSnapshot.id, jobSnapshot.data() as Record<string, unknown>)
          : null;

        if (cancelled) {
          return;
        }

        setApplication(nextApplication);
        setWorkerProfile(nextWorkerProfile);
        setJob(nextJob);
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Failed to load application review details."
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadDetail();

    return () => {
      cancelled = true;
    };
  }, [applicationId, services, session.user]);

  useEffect(() => {
    if (!services || !session.user) {
      setExistingRating(null);
      return;
    }

    const activeServices = services;
    const ratingId = employerWorkerRatingDocId(applicationId, session.user.uid);
    let cancelled = false;

    async function loadExistingRating() {
      try {
        const snapshot = await getDoc(doc(activeServices.db, "ratings", ratingId));

        if (cancelled) {
          return;
        }

        setExistingRating(
          snapshot.exists()
            ? normalizeEmployerWorkerRating(
                snapshot.id,
                snapshot.data() as Record<string, unknown>
              )
            : null
        );
      } catch {
        if (!cancelled) {
          setExistingRating(null);
        }
      }
    }

    void loadExistingRating();

    return () => {
      cancelled = true;
    };
  }, [applicationId, services, session.user]);

  async function handleOpenConversation() {
    if (!application) {
      return;
    }

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

  async function createVerification(
    note?: string
  ) {
    if (!services || !application) {
      return null;
    }

    const currentTime = Date.now();
    const verification = await createPendingWorkVerification({
      applicationId: application.id,
      db: services.db,
      employerId: application.employerId,
      employerName:
        application.companyName ||
        job?.companyName ||
        session.profile?.companyName ||
        "DutyPe employer",
      jobId: application.jobId,
      jobTitle: application.jobTitle,
      workerId: application.workerId,
      workerName: application.workerName
    });

    const nextHistory = note
      ? [
          ...application.statusHistory,
          {
            notes: note,
            status: application.status,
            systemUpdate: false,
            timestamp: currentTime,
            updatedAt: currentTime,
            updatedBy: session.user?.uid ?? ""
          }
        ]
      : application.statusHistory;

    await updateDoc(doc(services.db, "applications", application.id), {
      statusHistory: nextHistory,
      updatedAt: currentTime,
      verification,
      verificationCode: verification.verificationCode,
      verificationId: verification.verificationId,
      verificationStatus: verification.status
    });

    setApplication((current) =>
      current
        ? {
            ...current,
            statusHistory: nextHistory,
            updatedAt: currentTime,
            verification,
            verificationCode: verification.verificationCode,
            verificationId: verification.verificationId,
            verificationStatus: verification.status
          }
        : current
    );

    return verification;
  }

  async function handleGenerateVerification() {
    if (!application || application.status !== "ACCEPTED") {
      return;
    }

    try {
      setBusy(true);
      setError(null);
      await createVerification(
        application.verification
          ? "Work verification code regenerated in web employer flow"
          : "Work verification code generated in web employer flow"
      );
      setVerificationCodeInput("");
    } catch (verificationError) {
      setError(
        verificationError instanceof Error
          ? verificationError.message
          : "Failed to generate a work verification code."
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleStatusUpdate(nextStatus: ProductApplicationStatus) {
    if (!services || !application || application.status === nextStatus) {
      return;
    }

    if (nextStatus === "UNDER_REVIEW" && !canEmployerMoveToUnderReview(application.status)) {
      return;
    }

    if (
      (nextStatus === "ACCEPTED" || nextStatus === "REJECTED") &&
      !canEmployerAcceptOrReject(application.status)
    ) {
      return;
    }

    try {
      setBusy(true);
      setError(null);

      const currentTime = Date.now();
      const nextHistory = [
        ...application.statusHistory,
        {
          notes: nextStatusNote(nextStatus),
          status: nextStatus,
          systemUpdate: false,
          timestamp: currentTime,
          updatedAt: currentTime,
          updatedBy: session.user?.uid ?? ""
        }
      ];
      const storedHistory = nextHistory.map((entry) => ({
        ...entry,
        status: toStorageApplicationStatus(entry.status)
      }));
      const updatePayload: Record<string, unknown> = {
        status: toStorageApplicationStatus(nextStatus),
        statusHistory: storedHistory,
        updatedAt: currentTime
      };

      let nextVerification = application.verification;

      if (nextStatus === "ACCEPTED") {
        nextVerification = await createPendingWorkVerification({
          applicationId: application.id,
          db: services.db,
          employerId: application.employerId,
          employerName:
            application.companyName ||
            job?.companyName ||
            session.profile?.companyName ||
            "DutyPe employer",
          jobId: application.jobId,
          jobTitle: application.jobTitle,
          workerId: application.workerId,
          workerName: application.workerName
        });

        updatePayload.verification = nextVerification;
        updatePayload.verificationCode = nextVerification.verificationCode;
        updatePayload.verificationId = nextVerification.verificationId;
        updatePayload.verificationStatus = nextVerification.status;
      }

      await updateDoc(doc(services.db, "applications", application.id), updatePayload);

      if (nextStatus === "ACCEPTED" && job) {
        // Note: acceptedCount and vacancies should be queried from applications collection.
        // Writing them redundantly to jobs violates the canonical schema.
        // For now, we close the job to stop new applications.

        await updateDoc(doc(services.db, "jobs", job.id), {
          status: "closed",
          updatedAt: currentTime
        });

        setJob((current) =>
          current
            ? {
                ...current,
                status: "closed",
                updatedAt: currentTime
              }
            : current
        );
      }

      setApplication((current) =>
        current
          ? {
              ...current,
              status: nextStatus,
              statusHistory: nextHistory,
              updatedAt: currentTime,
              verification: nextVerification,
              verificationCode: nextVerification?.verificationCode ?? current.verificationCode,
              verificationId: nextVerification?.verificationId ?? current.verificationId,
              verificationStatus: nextVerification?.status ?? current.verificationStatus
            }
          : current
      );

      if (nextStatus === "ACCEPTED") {
        setVerificationCodeInput("");
      }
    } catch (updateError) {
      setError(
        updateError instanceof Error
          ? updateError.message
          : "Failed to update application status."
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleVerifyWork() {
    if (!services || !application || !session.user || !canEmployerVerifyWork(application.status)) {
      return;
    }

    if (!application.verification) {
      setError("No work-start verification is available for this application yet.");
      return;
    }

    const normalizedCode = verificationCodeInput.trim().toUpperCase();

    if (!normalizedCode) {
      setError("Enter the worker's DutyPe start code to verify work.");
      return;
    }

    if (verificationIsExpired(application.verification)) {
      setError("This verification code has expired. Generate a fresh code before starting work.");
      return;
    }

    if (normalizedCode !== application.verification.verificationCode) {
      setError("The code does not match the worker's active DutyPe start code.");
      return;
    }

    try {
      setBusy(true);
      setError(null);

      const currentTime = Date.now();
      const verifiedLocation = await readBrowserVerificationLocation();
      const nextHistory = [
        ...application.statusHistory,
        {
          notes: nextStatusNote("IN_PROGRESS"),
          status: "IN_PROGRESS" as const,
          systemUpdate: false,
          timestamp: currentTime,
          updatedAt: currentTime,
          updatedBy: session.user.uid
        }
      ];
      const storedHistory = nextHistory.map((entry) => ({
        ...entry,
        status: toStorageApplicationStatus(entry.status)
      }));
      const nextVerification = {
        ...application.verification,
        status: "VERIFIED" as const,
        verifiedAt: currentTime,
        verifiedByEmployerId: session.user.uid,
        verifiedLocation
      };

      await updateDoc(doc(services.db, "applications", application.id), {
        status: toStorageApplicationStatus("IN_PROGRESS"),
        statusHistory: storedHistory,
        updatedAt: currentTime,
        verification: nextVerification,
        verificationStatus: "VERIFIED",
        workStartedAt: currentTime
      });

      if (job) {
        await updateDoc(doc(services.db, "jobs", job.id), {
          hasActiveWorker: true,
          lastWorkerStartedAt: currentTime,
          updatedAt: currentTime
        });
      }

      setApplication((current) =>
        current
          ? {
              ...current,
              status: "IN_PROGRESS",
              statusHistory: nextHistory,
              updatedAt: currentTime,
              verification: nextVerification,
              verificationStatus: "VERIFIED",
              workStartedAt: currentTime
            }
          : current
      );
      setVerificationCodeInput("");
    } catch (verificationError) {
      setError(
        verificationError instanceof Error
          ? verificationError.message
          : "Failed to verify this worker's start code."
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleMarkComplete() {
    if (!services || !application || !canEmployerMarkWorkComplete(application.status)) {
      return;
    }

    try {
      setBusy(true);
      setError(null);

      const currentTime = Date.now();
      const nextHistory = [
        ...application.statusHistory,
        {
          notes: nextStatusNote("COMPLETED"),
          status: "COMPLETED" as const,
          systemUpdate: false,
          timestamp: currentTime,
          updatedAt: currentTime,
          updatedBy: session.user?.uid ?? ""
        }
      ];
      const storedHistory = nextHistory.map((entry) => ({
        ...entry,
        status: toStorageApplicationStatus(entry.status)
      }));

      await updateDoc(doc(services.db, "applications", application.id), {
        completedAt: currentTime,
        status: toStorageApplicationStatus("COMPLETED"),
        statusHistory: storedHistory,
        updatedAt: currentTime
      });

      if (job) {
        await updateDoc(doc(services.db, "jobs", job.id), {
          hasActiveWorker: false,
          updatedAt: currentTime
        });
      }

      setApplication((current) =>
        current
          ? {
              ...current,
              completedAt: currentTime,
              status: "COMPLETED",
              statusHistory: nextHistory,
              updatedAt: currentTime
            }
          : current
      );
    } catch (completionError) {
      setError(
        completionError instanceof Error
          ? completionError.message
          : "Failed to mark this application as completed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleSubmitRating() {
    if (!services || !application || !session.user || existingRating) {
      return;
    }

    if (!canEmployerRateWorker(application.status)) {
      setError("Worker rating unlocks after the application is marked as completed.");
      return;
    }

    if (
      ratingForm.overallRating < 1 ||
      ratingForm.punctualityRating < 1 ||
      ratingForm.qualityRating < 1 ||
      ratingForm.communicationRating < 1 ||
      ratingForm.professionalismRating < 1
    ) {
      setError("Add all rating scores before submitting worker feedback.");
      return;
    }

    try {
      setRatingBusy(true);
      setError(null);

      const ratingId = employerWorkerRatingDocId(application.id, session.user.uid);
      const payload = {
        applicationId: application.id,
        communicationRating: ratingForm.communicationRating,
        companyName: application.companyName || job?.companyName || "DutyPe employer",
        createdAt: Date.now(),
        feedback: ratingForm.feedback.trim(),
        isActive: true,
        jobId: application.id,
        jobTitle: application.jobTitle || "DutyPe job",
        overallRating: ratingForm.overallRating,
        paymentRating: 0,
        professionalismRating: ratingForm.professionalismRating,
        punctualityRating: ratingForm.punctualityRating,
        qualityRating: ratingForm.qualityRating,
        ratedUserId: application.workerId,
        ratedUserRole: "WORKER",
        raterUserId: session.user.uid,
        raterUserRole: "EMPLOYER",
        ratingId,
        tags: ratingForm.tags
      };

      await setDoc(doc(services.db, "ratings", ratingId), payload);
      setExistingRating(normalizeEmployerWorkerRating(ratingId, payload));
    } catch (ratingError) {
      setError(
        ratingError instanceof Error
          ? ratingError.message
          : "Failed to save worker rating."
      );
    } finally {
      setRatingBusy(false);
    }
  }

  if (loading) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="empty-state">Loading employer application detail.</div>
        </section>
      </div>
    );
  }

  if (!application || !workerProfile) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="detail-panel">
            <span className="card-kicker">Application unavailable</span>
            <h3>We could not open this applicant review.</h3>
            <p>{error || "The application may have been removed or is not owned by this employer."}</p>
            <div className="button-row">
              <Link href="/app/employer/applications" className="button">
                Back to applications
              </Link>
            </div>
          </div>
        </section>
      </div>
    );
  }

  const verificationExpired = verificationIsExpired(application.verification);
  const verificationStateText = application.verification
    ? verificationExpiryText(application.verification)
    : "Verification not generated";
  const showPostHirePanel =
    application.status === "ACCEPTED" ||
    application.status === "IN_PROGRESS" ||
    application.status === "COMPLETED";

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Application detail error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Worker</span>
            <strong>{workerProfile.fullName}</strong>
          </div>
          <div className="product-summary-card">
            <span>Status</span>
            <strong>{productStatusLabel(application.status)}</strong>
          </div>
          <div className="product-summary-card">
            <span>Applied</span>
            <strong>{formatDateTime(application.appliedAt)}</strong>
          </div>
        </div>

        <div className="button-row">
          <Link href="/app/employer/applications" className="button ghost">
            Back to applications
          </Link>
          <Link
            href={`/app/employer/workers/${application.workerId}?applicationId=${application.id}`}
            className="button ghost"
          >
            Worker profile
          </Link>
          <button type="button" className="button ghost" disabled={busy} onClick={() => void handleOpenConversation()}>
            Message worker
          </button>
        </div>

        <div className="button-row compact">
          {canEmployerMoveToUnderReview(application.status) ? (
            <button
              type="button"
              className="button ghost"
              disabled={busy}
              onClick={() => void handleStatusUpdate("UNDER_REVIEW")}
            >
              {busy ? "Updating..." : "Mark under review"}
            </button>
          ) : null}
          {canEmployerAcceptOrReject(application.status) ? (
            <>
              <button
                type="button"
                className="button"
                disabled={busy}
                onClick={() => void handleStatusUpdate("ACCEPTED")}
              >
                {busy ? "Updating..." : "Accept worker"}
              </button>
              <button
                type="button"
                className="button ghost"
                disabled={busy}
                onClick={() => void handleStatusUpdate("REJECTED")}
              >
                {busy ? "Updating..." : "Reject"}
              </button>
            </>
          ) : null}
        </div>
      </section>

      <WorkerProfilePanel application={application} profile={workerProfile} />

      <div className="detail-grid">
        <article className="detail-panel">
          <span className="card-kicker">Application overview</span>
          <h3>{application.jobTitle || "Untitled job"}</h3>
          <ul className="detail-list">
            <li>
              <strong>Company</strong>
              <span>{application.companyName || "DutyPe employer"}</span>
            </li>
            <li>
              <strong>Location</strong>
              <span>{application.jobLocation || job?.location || "Not available"}</span>
            </li>
            <li>
              <strong>Pay</strong>
              <span>{job ? formatCurrencyRange(job.payAmount, job.payType) : "Not available"}</span>
            </li>
            <li>
              <strong>Source</strong>
              <span>{application.source || "DutyPe app flow"}</span>
            </li>
            <li>
              <strong>Home-entry check</strong>
              <span>{application.homeEntryJob ? "Required" : "Not flagged"}</span>
            </li>
          </ul>
        </article>

        <article className="detail-panel">
          <span className="card-kicker">Trust and verification</span>
          <h3>Employer confidence signals</h3>
          <div className="pill-row">
            <span className="pill">{application.workerAadhaarVerified ? "Aadhaar verified" : "Aadhaar pending"}</span>
            <span className="pill">{application.workerPhoneVerified ? "Phone verified" : "Phone pending"}</span>
            <span className="pill">{application.workerIdentityVerified ? "Identity verified" : "Identity pending"}</span>
            <span className="pill">
              {application.workerBackgroundCheckPassed ? "Background clear" : "Background pending"}
            </span>
            <span className="pill">{application.workerJobsInArea} area jobs</span>
            <span className="pill">
              {application.workerLocalRating > 0
                ? `${application.workerLocalRating.toFixed(1)} local rating`
                : "No local rating"}
            </span>
          </div>
          {application.workerTotalReviews > 0 ? (
            <p>{application.workerTotalReviews} total reviews recorded for this worker context.</p>
          ) : null}
        </article>
      </div>

      <div className="detail-grid">
        <article className="detail-panel">
          <span className="card-kicker">Work start verification</span>
          <h3>Move accepted hires into live work</h3>
          {showPostHirePanel ? (
            <>
              <div className="pill-row">
                <span className={`status-pill ${verificationTone(application)}`}>
                  {application.verificationStatus || "Not generated"}
                </span>
                <span className="pill">{verificationStateText}</span>
                {application.workStartedAt ? (
                  <span className="pill">Started {formatDateTime(application.workStartedAt)}</span>
                ) : null}
                {application.verification?.verifiedLocation ? (
                  <span className="pill">Geo check recorded</span>
                ) : null}
              </div>

              <p>
                Ask the worker to share their DutyPe start code when they arrive. Verifying it
                marks the job as live work and records the employer-side confirmation timestamp.
              </p>

              {application.status === "ACCEPTED" && !application.verification ? (
                <div className="callout">
                  This accepted application does not have a start code yet. Generate one before
                  the worker begins.
                </div>
              ) : null}

              {application.status === "ACCEPTED" && verificationExpired ? (
                <div className="callout">
                  The current start code has expired. Generate a fresh code before verifying work.
                </div>
              ) : null}

              {canEmployerVerifyWork(application.status) && application.verification ? (
                <div className="review-action-grid">
                  <label className="inline-field">
                    <span>Worker start code</span>
                    <input
                      value={verificationCodeInput}
                      onChange={(event) => setVerificationCodeInput(event.target.value.toUpperCase())}
                      placeholder="DTP-7X9K2M"
                    />
                  </label>
                </div>
              ) : null}

              <div className="button-row compact">
                {application.status === "ACCEPTED" ? (
                  <button
                    type="button"
                    className="button ghost"
                    disabled={busy}
                    onClick={() => void handleGenerateVerification()}
                  >
                    {busy
                      ? "Generating..."
                      : application.verification
                        ? "Regenerate code"
                        : "Generate start code"}
                  </button>
                ) : null}
                {canEmployerVerifyWork(application.status) && application.verification ? (
                  <button
                    type="button"
                    className="button"
                    disabled={busy}
                    onClick={() => void handleVerifyWork()}
                  >
                    {busy ? "Verifying..." : "Verify work start"}
                  </button>
                ) : null}
                {canEmployerMarkWorkComplete(application.status) ? (
                  <button
                    type="button"
                    className="button ghost"
                    disabled={busy}
                    onClick={() => void handleMarkComplete()}
                  >
                    {busy ? "Updating..." : "Mark complete"}
                  </button>
                ) : null}
              </div>
            </>
          ) : (
            <p>Work-start verification becomes available after you accept this worker.</p>
          )}
        </article>

        <article className="detail-panel">
          <span className="card-kicker">Completion and rating</span>
          <h3>Close the job with worker feedback</h3>
          <div className="pill-row">
            <span className={`status-pill ${productStatusTone(application.status)}`}>
              {productStatusLabel(application.status)}
            </span>
            {application.completedAt ? (
              <span className="pill">Completed {formatDateTime(application.completedAt)}</span>
            ) : null}
            {existingRating ? (
              <span className="pill">{existingRating.overallRating}/5 rating submitted</span>
            ) : null}
          </div>

          {canEmployerMarkWorkComplete(application.status) ? (
            <p>
              Mark this application as completed when the shift is finished. Rating unlocks
              immediately after completion.
            </p>
          ) : null}

          {canEmployerRateWorker(application.status) ? (
            existingRating ? (
              <>
                <p>
                  You already rated this worker on {formatDateTime(existingRating.createdAt)} with an
                  overall score of {existingRating.overallRating}/5.
                </p>
                {existingRating.feedback ? <p>{existingRating.feedback}</p> : null}
                {existingRating.tags.length > 0 ? (
                  <div className="pill-row">
                    {existingRating.tags.map((tag) => (
                      <span key={tag} className="pill">
                        {tag}
                      </span>
                    ))}
                  </div>
                ) : null}
              </>
            ) : (
              <>
                <div className="rating-grid">
                  <RatingScaleField
                    label="Overall"
                    value={ratingForm.overallRating}
                    onChange={(value) =>
                      setRatingForm((current) => ({ ...current, overallRating: value }))
                    }
                  />
                  <RatingScaleField
                    label="Punctuality"
                    value={ratingForm.punctualityRating}
                    onChange={(value) =>
                      setRatingForm((current) => ({ ...current, punctualityRating: value }))
                    }
                  />
                  <RatingScaleField
                    label="Quality"
                    value={ratingForm.qualityRating}
                    onChange={(value) =>
                      setRatingForm((current) => ({ ...current, qualityRating: value }))
                    }
                  />
                  <RatingScaleField
                    label="Communication"
                    value={ratingForm.communicationRating}
                    onChange={(value) =>
                      setRatingForm((current) => ({ ...current, communicationRating: value }))
                    }
                  />
                  <RatingScaleField
                    label="Professionalism"
                    value={ratingForm.professionalismRating}
                    onChange={(value) =>
                      setRatingForm((current) => ({ ...current, professionalismRating: value }))
                    }
                  />
                </div>

                <label className="inline-field">
                  <span>Employer feedback</span>
                  <textarea
                    rows={4}
                    value={ratingForm.feedback}
                    onChange={(event) =>
                      setRatingForm((current) => ({ ...current, feedback: event.target.value }))
                    }
                    placeholder="What stood out about this worker on the job?"
                  />
                </label>

                <div className="tag-toggle-row">
                  {workerRatingTags.map((tag) => {
                    const selected = ratingForm.tags.includes(tag);

                    return (
                      <button
                        key={tag}
                        type="button"
                        className={`tag-toggle${selected ? " active" : ""}`}
                        onClick={() =>
                          setRatingForm((current) => ({
                            ...current,
                            tags: selected
                              ? current.tags.filter((item) => item !== tag)
                              : [...current.tags, tag]
                          }))
                        }
                      >
                        {tag}
                      </button>
                    );
                  })}
                </div>

                <div className="button-row compact">
                  <button
                    type="button"
                    className="button"
                    disabled={ratingBusy}
                    onClick={() => void handleSubmitRating()}
                  >
                    {ratingBusy ? "Saving rating..." : "Submit worker rating"}
                  </button>
                </div>
              </>
            )
          ) : (
            <p>Worker rating becomes available once this application is marked as completed.</p>
          )}
        </article>
      </div>

      {application.coverLetter ? (
        <article className="detail-panel">
          <span className="card-kicker">Cover letter</span>
          <h3>Worker note to employer</h3>
          <p>{application.coverLetter}</p>
        </article>
      ) : null}

      {(application.additionalDocuments.length > 0 || application.resumeUrl) ? (
        <article className="detail-panel">
          <span className="card-kicker">Documents</span>
          <h3>Attachments and resume</h3>
          <div className="detail-link-grid">
            {application.resumeUrl ? (
              <a className="button ghost" href={application.resumeUrl} rel="noreferrer" target="_blank">
                Open resume
              </a>
            ) : null}
            {application.additionalDocuments.map((document) => (
              <a
                key={document.id}
                className="button ghost"
                href={document.fileUrl}
                rel="noreferrer"
                target="_blank"
              >
                {document.fileName || document.name || "Open document"}
              </a>
            ))}
          </div>
        </article>
      ) : null}

      <TimelineList application={application} />
    </div>
  );
}

export function EmployerWorkerProfileClient({
  applicationId,
  session,
  workerId
}: EmployerWorkerProfileClientProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [application, setApplication] = useState<EmployerReviewApplication | null>(null);
  const [profile, setProfile] = useState<EmployerReviewWorkerProfile | null>(null);
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

    async function loadProfile() {
      try {
        setLoading(true);
        setError(null);

        const { userProfile, workerProfile } = await loadWorkerSourceData(workerId);
        let nextApplication: EmployerReviewApplication | null = null;

        if (applicationId) {
          const applicationSnapshot = await getDoc(
            doc(activeServices.db, "applications", applicationId)
          );

          if (!applicationSnapshot.exists()) {
            throw new Error("The related application could not be found.");
          }

          const rawApplication = normalizeEmployerReviewApplication(
            applicationSnapshot.id,
            applicationSnapshot.data() as Record<string, unknown>
          );

          if (rawApplication.employerId !== activeUser.uid || rawApplication.workerId !== workerId) {
            throw new Error("This worker review is not available for your employer account.");
          }

          nextApplication = mergeEmployerReviewApplicationWithProfile(
            rawApplication,
            userProfile,
            workerProfile
          );
        }

        const nextProfile = buildEmployerReviewWorkerProfile(
          workerId,
          userProfile,
          workerProfile,
          nextApplication
        );

        if (cancelled) {
          return;
        }

        setApplication(nextApplication);
        setProfile(nextProfile);
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Failed to load worker profile review."
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadProfile();

    return () => {
      cancelled = true;
    };
  }, [applicationId, services, session.user, workerId]);

  async function handleOpenConversation() {
    try {
      setError(null);
      const conversationId = await getOrCreateConversationId(
        workerId,
        application?.jobId || application?.id || workerId
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

  if (loading) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="empty-state">Loading worker review profile.</div>
        </section>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="product-section-stack">
        <section className="section">
          <div className="detail-panel">
            <span className="card-kicker">Worker profile unavailable</span>
            <h3>We could not open this worker review profile.</h3>
            <p>{error || "The worker document may not exist yet."}</p>
            <div className="button-row">
              <Link href="/app/employer/applications" className="button">
                Back to employer review
              </Link>
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="product-section-stack">
      {error ? <div className="callout">Worker profile error: {error}</div> : null}

      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Worker</span>
            <strong>{profile.fullName}</strong>
          </div>
          <div className="product-summary-card">
            <span>Skills</span>
            <strong>{profile.skills.length > 0 ? profile.skills.length : "No skill tags"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Availability</span>
            <strong>{profile.availability || "Not set"}</strong>
          </div>
        </div>

        <div className="button-row">
          <Link
            href={application ? `/app/employer/applications/${application.id}` : "/app/employer/applications"}
            className="button ghost"
          >
            {application ? "Back to application review" : "Back to applications"}
          </Link>
          <button type="button" className="button ghost" onClick={() => void handleOpenConversation()}>
            Message worker
          </button>
          <Link href={`/worker/${workerId}`} className="button ghost">
            Public worker page
          </Link>
        </div>
      </section>

      <WorkerProfilePanel application={application} profile={profile} />

      {application ? (
        <>
          <article className="detail-panel">
            <span className="card-kicker">Current application</span>
            <h3>{application.jobTitle || "Untitled job"}</h3>
            <p>{application.coverLetter || "No cover letter was included in this application."}</p>
            <div className="pill-row">
              <span className={`status-pill ${productStatusTone(application.status)}`}>
                {productStatusLabel(application.status)}
              </span>
              <span className="pill">{formatDate(application.appliedAt)}</span>
              <span className="pill">{application.companyName || "DutyPe employer"}</span>
            </div>
          </article>
          <TimelineList application={application} />
        </>
      ) : null}
    </div>
  );
}
