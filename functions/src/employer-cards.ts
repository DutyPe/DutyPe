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
 * by Cloud Functions.
 *
 * Current client reality (May 2026): Android reads ONLY
 * `employerId` (query filter) + `applicationCount` (card badge).
 * Keeping extra denormalized fields here increases write size and drift
 * without adding value. This trigger now stores only fields that are read.
 *
 * Schema:
 *   employer_job_cards/{jobId} = {
 *     jobId,
 *     employerId,
 *     applicationCount,
 *     updatedAt
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
}

const UNUSED_CARD_FIELDS: readonly string[] = [
  "title",
  "jobType",
  "status",
  "salary",
  "salaryType",
  "location",
  "addressText",
  "geohash",
  "jobImageUrl",
  "vacancies",
  "shiftTiming",
  "companyName",
  "contactNumber",
  "shortlistedCount",
  "hiredCount",
  "completedCount",
  "rejectedCount",
  "withdrawnCount",
  "appliedCount",
  "lastApplicationAt",
  "createdAt",
];

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
      if (change.after.get("searchKeywords") !== undefined) {
        await change.after.ref.update({ searchKeywords: FIELD.delete() });
      }
      const removeUnused: Record<string, admin.firestore.FieldValue> = {};
      for (const key of UNUSED_CARD_FIELDS) {
        removeUnused[key] = FIELD.delete();
      }

      await cardRef.set(
        {
          ...denorm,
          ...removeUnused,
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

    // applicationCount is the only counter consumed by current clients.
    const deltas: Record<string, FirebaseFirestore.FieldValue> = {};
    const bump = (by: number) => {
      if (by === 0) return;
      deltas["applicationCount"] = FIELD.increment(by);
    };

    if (!before && after) {
      // Created.
      bump(1);
    } else if (before && !after) {
      // Deleted.
      bump(-1);
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
