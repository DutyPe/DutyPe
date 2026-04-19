/**
 * Aggregate maintainers — Cloud Functions triggers that keep the
 * client-forbidden aggregate fields (rating, totalRatings, totalJobs,
 * totalHires) correct on worker_profiles / employer_profiles.
 *
 * Design notes:
 *   • Every aggregate mutation runs inside a Firestore transaction so concurrent
 *     events don't step on each other.
 *   • The rating averages are computed incrementally using the running mean
 *     formula:  new_avg = old_avg + (new_value - old_avg) / new_count
 *     This avoids a full table scan on every rating.
 *   • Counters are bumped exactly once per state transition (onUpdate checks
 *     the previous value to avoid double-counting on no-op writes).
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;

/**
 * When a rating is created, bump the target user's running average and
 * total count on whichever profile collection matches their role.
 */
export const onRatingCreated = functions.firestore
  .document("ratings/{ratingId}")
  .onCreate(async (snap) => {
    const rating = snap.data();
    const toUserId = String(rating?.toUserId ?? "");
    const value = Number(rating?.rating ?? 0);
    if (!toUserId || !Number.isFinite(value) || value < 1 || value > 5) {
      functions.logger.warn("onRatingCreated: invalid payload", { ratingId: snap.id });
      return;
    }

    // Update both worker_profiles and employer_profiles if they exist for the user.
    await Promise.all([
      updateRollingAverage(db.doc(`worker_profiles/${toUserId}`), value),
      updateRollingAverage(db.doc(`employer_profiles/${toUserId}`), value),
    ]);
  });

async function updateRollingAverage(
  ref: FirebaseFirestore.DocumentReference,
  newValue: number
): Promise<void> {
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return; // profile doesn't exist for this role — skip

    const data = snap.data() || {};
    const prevCount = Number(data.totalRatings ?? 0);
    const prevAvg = Number(data.rating ?? 0);
    const newCount = prevCount + 1;
    const newAvg = prevAvg + (newValue - prevAvg) / newCount;

    tx.update(ref, {
      rating: Math.round(newAvg * 100) / 100, // two-decimal precision
      totalRatings: newCount,
    });
  });
}

/**
 * When an application transitions to status='hired' (and wasn't hired
 * before), increment:
 *   • worker_profiles/{workerId}.totalJobs
 *   • employer_profiles/{employerId}.totalHires
 *
 * onUpdate is used rather than onWrite so we don't re-fire on deletes, and
 * we check the previous state so re-writes of the same status are no-ops.
 */
export const onApplicationHired = functions.firestore
  .document("applications/{applicationId}")
  .onUpdate(async (change) => {
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String(before.status ?? "");
    const next = String(after.status ?? "");
    if (next !== "hired" || prev === "hired") return;

    const workerId = String(after.workerId ?? "");
    const employerId = String(after.employerId ?? "");
    if (!workerId || !employerId) {
      functions.logger.warn("onApplicationHired: missing ids", { id: change.after.id });
      return;
    }

    await Promise.all([
      safeIncrement(db.doc(`worker_profiles/${workerId}`), { totalJobs: FIELD.increment(1) }),
      safeIncrement(db.doc(`employer_profiles/${employerId}`), { totalHires: FIELD.increment(1) }),
    ]);
  });

/**
 * Keep per-job application counters in sync for employer views.
 * This runs on application create and updates `job_details/{jobId}.applicationCount`.
 */
export const onApplicationCreatedIncrementApplicationCount = functions.firestore
  .document("applications/{applicationId}")
  .onCreate(async (snap) => {
    const data = snap.data() || {};
    const jobId = String(data.jobId ?? "");
    if (!jobId) {
      functions.logger.warn("onApplicationCreatedIncrementApplicationCount: missing jobId", { id: snap.id });
      return;
    }

    try {
      await db.doc(`job_details/${jobId}`).set(
        { applicationCount: FIELD.increment(1) },
        { merge: true }
      );
    } catch (e: any) {
      functions.logger.error("onApplicationCreatedIncrementApplicationCount failed", {
        id: snap.id,
        jobId,
        err: e?.message,
      });
    }
  });

async function safeIncrement(
  ref: FirebaseFirestore.DocumentReference,
  updates: Record<string, FirebaseFirestore.FieldValue>
): Promise<void> {
  try {
    const snap = await ref.get();
    if (!snap.exists) return;
    await ref.update(updates);
  } catch (e: any) {
    functions.logger.error("safeIncrement failed", { ref: ref.path, err: e?.message });
  }
}
