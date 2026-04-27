/**
 * Employer-card aggregate.
 *
 * Bug #4 fix (April 2026): the employer home screen was showing
 * `applicationsReceived = 0` on every card because the existing
 * `jobmetadata` document is shared with the worker side and only carries
 * the slim public payload — application counts were being written to
 * `job_details.applicationCount` (private, employer-only via callable)
 * and never reached the card list.
 *
 * Solution: a dedicated `employer_job_cards/{jobId}` document maintained
 * by Cloud Functions. It has every field the employer's job-list card
 * needs (title, salary text, location, status, hero image, per-status
 * application counts, last-application timestamp) so the client can
 * render without any JOIN against jobmetadata / job_details / applications.
 *
 * Schema:
 *   employer_job_cards/{jobId} = {
 *     jobId, employerId, title, jobType, status,
 *     salary, salaryType,
 *     location: { lat, lng },
 *     addressText, geohash, jobImageUrl?,
 *     vacancies, urgency, workingHours, shiftTiming,
 *     applicationCount, shortlistedCount, hiredCount,
 *     completedCount, rejectedCount,
 *     lastApplicationAt?,
 *     createdAt, updatedAt
 *   }
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;

const CARDS = "employer_job_cards";

interface DenormPayload {
  jobId: string;
  employerId: string;
  title: string;
  jobType: string;
  status: string;
  salary: string;
  salaryType: string;
  location: { lat: number; lng: number } | null;
  addressText: string;
  geohash: string;
  jobImageUrl: string | null;
  vacancies: number;
  shiftTiming: string;
  companyName: string;
  contactNumber: string;
  createdAt: admin.firestore.Timestamp | null;
}

async function buildDenormFromJob(jobId: string): Promise<DenormPayload | null> {
  const [metaSnap, detailsSnap] = await Promise.all([
    db.collection("jobmetadata").doc(jobId).get(),
    db.collection("job_details").doc(jobId).get(),
  ]);
  if (!metaSnap.exists) return null;
  const meta = metaSnap.data() || {};
  const details = (detailsSnap.exists ? detailsSnap.data() : {}) || {};
  const employerId = String(meta.employerId ?? details.employerId ?? "");
  if (!employerId) {
    functions.logger.warn("buildDenormFromJob: missing employerId", { jobId });
    return null;
  }
  return {
    jobId,
    employerId,
    title: String(meta.title ?? ""),
    jobType: String(meta.jobType ?? ""),
    status: String(meta.status ?? "open"),
    salary: String(meta.salary ?? ""),
    salaryType: String(meta.salaryType ?? ""),
    location: meta.location && typeof meta.location === "object"
      ? {
        lat: Number((meta.location as any).lat ?? 0),
        lng: Number((meta.location as any).lng ?? 0),
      }
      : null,
    addressText: String(meta.addressText ?? ""),
    geohash: String(meta.geohash ?? ""),
    jobImageUrl: meta.jobImageUrl ? String(meta.jobImageUrl) : null,
    vacancies: Number(meta.vacancies ?? details.vacancies ?? 1),
    shiftTiming: String(details.shiftTiming ?? meta.shiftTiming ?? "Flexible"),
    companyName: String(meta.companyName ?? ""),
    contactNumber: String(details.contactNumber ?? ""),
    createdAt: (meta.createdAt as admin.firestore.Timestamp) ?? null,
  };
}

/**
 * On every jobmetadata write, refresh the matching employer_job_cards
 * doc with the latest denormalized snapshot. We deliberately do not
 * touch the per-status counters here — those are owned by the
 * applications trigger below.
 */
export const onJobMetadataWriteSyncEmployerCard = functions.firestore
  .document("jobmetadata/{jobId}")
  .onWrite(async (change, context) => {
    const jobId = context.params.jobId as string;
    const cardRef = db.collection(CARDS).doc(jobId);
    if (!change.after.exists) {
      // jobmetadata deleted — drop the card too.
      await cardRef.delete().catch((e) => {
        functions.logger.warn("delete employer_job_card failed", { jobId, err: e?.message });
      });
      return;
    }
    const denorm = await buildDenormFromJob(jobId);
    if (!denorm) return;
    try {
      await cardRef.set(
        {
          ...denorm,
          updatedAt: FIELD.serverTimestamp(),
        },
        { merge: true }
      );
    } catch (e: any) {
      functions.logger.error("upsert employer_job_card failed", { jobId, err: e?.message });
    }
  });

/**
 * Maintain per-status counters on employer_job_cards from the
 * applications collection. We use `onWrite` so creates, status updates
 * and deletes all flow through one place.
 */
export const onApplicationWriteSyncEmployerCard = functions.firestore
  .document("applications/{applicationId}")
  .onWrite(async (change) => {
    const before = change.before.exists ? change.before.data() || {} : null;
    const after = change.after.exists ? change.after.data() || {} : null;
    const jobId = String((after?.jobId ?? before?.jobId) ?? "");
    if (!jobId) return;

    const prevStatus = String(before?.status ?? "");
    const nextStatus = String(after?.status ?? "");

    // Compute counter deltas for: applicationCount (total non-deleted),
    // shortlistedCount, hiredCount, completedCount, rejectedCount.
    const deltas: Record<string, FirebaseFirestore.FieldValue> = {};
    const bump = (field: string, by: number) => {
      if (by === 0) return;
      deltas[field] = FIELD.increment(by);
    };

    if (!before && after) {
      // Created.
      bump("applicationCount", 1);
      bump(statusCounterField(nextStatus), 1);
      deltas["lastApplicationAt"] = FIELD.serverTimestamp() as any;
    } else if (before && !after) {
      // Deleted.
      bump("applicationCount", -1);
      bump(statusCounterField(prevStatus), -1);
    } else if (before && after && prevStatus !== nextStatus) {
      // Status transitioned.
      bump(statusCounterField(prevStatus), -1);
      bump(statusCounterField(nextStatus), 1);
    }

    if (Object.keys(deltas).length === 0) return;

    const cardRef = db.collection(CARDS).doc(jobId);
    try {
      // Use set+merge so the doc is auto-created if the metadata trigger has not run yet.
      await cardRef.set(deltas, { merge: true });
    } catch (e: any) {
      functions.logger.error("update employer_job_card counters failed", {
        jobId,
        err: e?.message,
      });
    }
  });

function statusCounterField(status: string): string {
  switch (status.toLowerCase()) {
    case "shortlisted":
      return "shortlistedCount";
    case "hired":
      return "hiredCount";
    case "completed":
      return "completedCount";
    case "rejected":
      return "rejectedCount";
    case "withdrawn":
      return "withdrawnCount";
    default:
      // applied / unknown -> only contributes to the total.
      return "appliedCount";
  }
}
