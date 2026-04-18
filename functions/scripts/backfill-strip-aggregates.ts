/**
 * One-time migration — strip client-forgeable aggregate fields from existing docs
 * so they comply with the tightened firestore.rules field whitelists.
 *
 * Runs idempotently. Safe to re-run. Invoke via:
 *   ts-node scripts/backfill-strip-aggregates.ts <serviceAccountJson>
 */
import * as admin from "firebase-admin";
import * as path from "path";

async function main() {
  const keyPath = process.argv[2];
  if (!keyPath) {
    console.error("Usage: ts-node scripts/backfill-strip-aggregates.ts <serviceAccountJson>");
    process.exit(1);
  }
  const absPath = path.resolve(keyPath);
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const serviceAccount = require(absPath);

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
  const db = admin.firestore();

  const DELETE = admin.firestore.FieldValue.delete();

  // worker_profiles: remove rating/totalRatings/totalJobs from the doc body;
  // canonical aggregates belong under a server-owned summary (not enforced here).
  await stripCollection(db, "worker_profiles", {
    rating: DELETE,
    totalRatings: DELETE,
    totalJobs: DELETE,
  });

  await stripCollection(db, "employer_profiles", {
    rating: DELETE,
    totalRatings: DELETE,
    totalHires: DELETE,
    isVerified: DELETE,
  });

  // users: aggregates must not live here either.
  await stripCollection(db, "users", {
    rating: DELETE,
    totalRatings: DELETE,
    totalJobs: DELETE,
    totalHires: DELETE,
    isVerified: DELETE,
    isActive: DELETE,
  });

  console.log("✅ backfill complete");
}

async function stripCollection(
  db: FirebaseFirestore.Firestore,
  collection: string,
  updates: Record<string, FirebaseFirestore.FieldValue>
) {
  const pageSize = 400;
  let last: FirebaseFirestore.QueryDocumentSnapshot | undefined;
  let total = 0;

  while (true) {
    let q = db.collection(collection).orderBy(admin.firestore.FieldPath.documentId()).limit(pageSize);
    if (last) q = q.startAfter(last);
    const snap = await q.get();
    if (snap.empty) break;

    const batch = db.batch();
    for (const doc of snap.docs) {
      // Only write if at least one stripped key is actually present — avoids no-op writes.
      const data = doc.data();
      const shouldTouch = Object.keys(updates).some((k) => k in data);
      if (shouldTouch) {
        batch.update(doc.ref, updates);
      }
    }
    await batch.commit();
    total += snap.size;
    last = snap.docs[snap.docs.length - 1];
    console.log(`  ${collection}: processed ${total}`);
    if (snap.size < pageSize) break;
  }
}

main().catch((err) => {
  console.error("backfill failed:", err);
  process.exit(1);
});
