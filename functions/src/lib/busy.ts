/**
 * "Busy" rule: a worker on an unfinished job cannot take another one.
 *
 * Busy means any of
 *   - an urgent/instant job they were selected for that is still open or filled,
 *   - a DutyPe Services booking assigned to them that is not finished,
 *   - a regular job they were hired for less than the auto-complete grace period ago.
 * Stale pointers (that job already ended) never block.
 */
import * as admin from "firebase-admin";
import { AUTO_COMPLETE_RULES } from "../auto-complete-rules";
import { Applications, InstantRequests, Values, WorkerProfiles } from "../schema";

export type BusyWith = "urgent" | "job";

export const BUSY_MESSAGE: Record<BusyWith, string> = {
  urgent: "Finish your current urgent job first. You can apply for new jobs after it is completed.",
  job: "You were just hired for a job. You can apply for new jobs after it is completed.",
};

/**
 * [worker] is the worker profile snapshot (already read). Reads go through [tx] when given so
 * the check can run inside a transaction (before any write).
 */
export async function busyWith(
  uid: string,
  worker: admin.firestore.DocumentSnapshot | null,
  tx?: admin.firestore.Transaction,
  ignoreUrgentId = "",
): Promise<BusyWith | null> {
  const db = admin.firestore();
  const read = (ref: admin.firestore.DocumentReference) => tx ? tx.get(ref) : ref.get();

  const urgentId = String(worker?.get(WorkerProfiles.ACTIVE_URGENT_ID) || "");
  if (urgentId && urgentId !== ignoreUrgentId) {
    const r = await read(db.collection(InstantRequests.COLLECTION).doc(urgentId));
    const S = Values.InstantStatus;
    if (r.exists && [S.OPEN, S.FILLED].includes(r.get(InstantRequests.STATUS)) &&
      ((r.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[]).includes(uid)) return "urgent";
  }

  const since = admin.firestore.Timestamp.fromMillis(Date.now() - AUTO_COMPLETE_RULES.STANDARD_GRACE_MS);
  const q = db.collection(Applications.COLLECTION)
    .where(Applications.WORKER_ID, "==", uid)
    .where(Applications.STATUS, "==", Values.ApplicationStatus.HIRED)
    .where(Applications.HIRED_AT, ">", since)
    .limit(1);
  const hired = tx ? await tx.get(q) : await q.get();
  if (!hired.empty) return "job";
  return null;
}
