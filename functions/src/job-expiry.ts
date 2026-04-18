/**
 * Scheduled job expiry sweeper.
 *
 * Runs every 15 minutes. Finds open jobs whose expiresAt is in the past
 * and flips them to status='expired'. Uses the
 * (status ASC, expiresAt ASC) composite index added in firestore.indexes.json.
 *
 * Batched to 400 docs per commit to stay under Firestore's 500-op batch cap.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const PAGE = 400;

export const expireOpenJobs = functions
  .region("asia-south1")
  .pubsub.schedule("every 15 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = admin.firestore.Timestamp.now();
    let swept = 0;

    while (true) {
      const snap = await db.collection("jobmetadata")
        .where("status", "==", "open")
        .where("expiresAt", "<=", now)
        .orderBy("expiresAt", "asc")
        .limit(PAGE)
        .get();

      if (snap.empty) break;

      const batch = db.batch();
      snap.docs.forEach((d) => batch.update(d.ref, { status: "expired" }));
      await batch.commit();
      swept += snap.size;

      if (snap.size < PAGE) break;
    }

    functions.logger.info(`expireOpenJobs: swept ${swept} jobs`);
    return null;
  });
