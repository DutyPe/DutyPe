/**
 * Scheduled cleanup for the transactional rate-limit collection.
 *
 * Rate-limit docs accumulate one per (uid, action). To keep the collection
 * bounded, delete docs whose `lastActionAt` is older than 24h.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const PAGE = 400;
const TTL_MS = 24 * 60 * 60 * 1000;

export const cleanupRateLimits = functions
  .region("asia-south1")
  .pubsub.schedule("every 6 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const cutoff = Date.now() - TTL_MS;
    let deleted = 0;

    while (true) {
      const snap = await db.collection("_rate_limits")
        .where("lastActionAt", "<", cutoff)
        .limit(PAGE)
        .get();
      if (snap.empty) break;

      const batch = db.batch();
      snap.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
      deleted += snap.size;

      if (snap.size < PAGE) break;
    }

    functions.logger.info(`cleanupRateLimits: deleted ${deleted} stale rate-limit docs`);
    return null;
  });
