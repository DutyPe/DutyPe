/**
 * Scheduled job expiry sweeper.
 *
 * Runs every 15 minutes. Finds expired job_details (expiresAt <= now) whose
 * matching jobmetadata.status == 'open' and flips them to status='expired'.
 * expiresAt lives in job_details (slim jobmetadata schema).
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
    let cursor: FirebaseFirestore.QueryDocumentSnapshot | null = null;

    while (true) {
      let q = db.collection("job_details")
        .where("expiresAt", "<=", now)
        .orderBy("expiresAt", "asc")
        .limit(PAGE);
      if (cursor) q = q.startAfter(cursor);
      const snap = await q.get();

      if (snap.empty) break;

      // Fetch jobmetadata for all candidate ids in parallel and keep only open ones.
      const metaRefs = snap.docs.map((d) => db.collection("jobmetadata").doc(d.id));
      const metaSnaps = await db.getAll(...metaRefs);
      const batch = db.batch();
      let batched = 0;
      for (const metaSnap of metaSnaps) {
        if (!metaSnap.exists) continue;
        if (metaSnap.get("status") !== "open") continue;
        batch.update(metaSnap.ref, { status: "expired" });
        batched += 1;
      }
      if (batched > 0) await batch.commit();
      swept += batched;

      if (snap.size < PAGE) break;
      cursor = snap.docs[snap.docs.length - 1];
    }

    functions.logger.info(`expireOpenJobs: swept ${swept} jobs`);
    return null;
  });
