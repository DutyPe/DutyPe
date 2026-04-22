/**
 * migrate-saved-jobs-canonical.js
 *
 * Strips legacy fields from every doc in the `saved_jobs` collection so it
 * matches the canonical schema enforced by firestore.rules:
 *   { userId, jobId, createdAt }
 *
 * Any doc missing userId or jobId is deleted. Extra fields (id, workerId,
 * snapshot payloads, etc.) are removed with FieldValue.delete().
 *
 * USAGE:
 *   cd scripts
 *   node migrate-saved-jobs-canonical.js             # dry-run
 *   node migrate-saved-jobs-canonical.js --commit    # apply changes
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

const COMMIT = process.argv.includes('--commit');
const ALLOWED = new Set(['userId', 'jobId', 'createdAt']);

async function run() {
  console.log(`[migrate-saved-jobs] mode=${COMMIT ? 'COMMIT' : 'DRY-RUN'}`);

  const snap = await db.collection('saved_jobs').get();
  console.log(`[migrate-saved-jobs] scanned ${snap.size} docs`);

  let toDelete = 0;
  let toUpdate = 0;
  let clean = 0;

  let batch = db.batch();
  let batched = 0;

  for (const doc of snap.docs) {
    const data = doc.data() || {};

    if (!data.userId || !data.jobId) {
      toDelete++;
      console.log(`  DELETE  ${doc.id} (missing userId/jobId)`);
      if (COMMIT) {
        batch.delete(doc.ref);
        batched++;
      }
      continue;
    }

    const extras = Object.keys(data).filter((k) => !ALLOWED.has(k));
    if (extras.length === 0) {
      clean++;
      continue;
    }

    toUpdate++;
    console.log(`  STRIP   ${doc.id} -> removing: ${extras.join(', ')}`);
    if (COMMIT) {
      const patch = {};
      for (const k of extras) patch[k] = FieldValue.delete();
      batch.update(doc.ref, patch);
      batched++;
    }

    if (batched >= 400) {
      if (COMMIT) await batch.commit();
      batch = db.batch();
      batched = 0;
    }
  }

  if (COMMIT && batched > 0) await batch.commit();

  console.log('');
  console.log('[migrate-saved-jobs] summary');
  console.log(`  already canonical : ${clean}`);
  console.log(`  to strip          : ${toUpdate}`);
  console.log(`  to delete         : ${toDelete}`);
  console.log(`  committed         : ${COMMIT}`);
}

run()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('[migrate-saved-jobs] FAILED', err);
    process.exit(1);
  });
