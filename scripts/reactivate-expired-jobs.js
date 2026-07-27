/**
 * Reactivate inactive/expired job postings.
 *
 * Usage:
 *   node scripts/reactivate-expired-jobs.js --dry-run
 *   node scripts/reactivate-expired-jobs.js
 *
 * By default, this reopens only jobs that are inactive because they are
 * expired/paused/inactive or have a past expiresAt. It intentionally skips
 * closed, filled, and deleted jobs.
 */
const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const args = process.argv.slice(2);
const DRY_RUN = args.includes('--dry-run');
const LIMIT_ARG = args.find((a) => a.startsWith('--limit='));
const LIMIT = LIMIT_ARG ? parseInt(LIMIT_ARG.split('=')[1], 10) : 0;

const JOBS_COLLECTION = 'jobmetadata';
const JOB_DETAILS_COLLECTION = 'job_details';
const PAGE_SIZE = 200;
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

function toMillis(value) {
  if (!value) return 0;
  if (value instanceof admin.firestore.Timestamp) return value.toMillis();
  if (value instanceof Date) return value.getTime();
  if (typeof value === 'number') {
    if (value <= 0) return 0;
    if (value < 100_000_000_000) return value * 1000;
    if (value > 9_999_999_999_999) return Math.floor(value / 1000);
    return value;
  }
  return 0;
}

function shouldReactivate(card, details, now) {
  const status = String(card.status || details.status || '').trim().toLowerCase();
  if (['closed', 'filled', 'deleted'].includes(status)) return false;
  if (['expired', 'paused', 'inactive'].includes(status)) return true;

  const expiresAt = toMillis(details.expiresAt) || toMillis(card.expiresAt);
  return expiresAt > 0 && expiresAt <= now;
}

async function main() {
  const sa = loadServiceAccount();
  if (!admin.apps.length) {
    admin.initializeApp({ credential: admin.credential.cert(sa) });
  }
  const db = admin.firestore();
  const now = Date.now();
  const newExpiry = admin.firestore.Timestamp.fromMillis(now + THIRTY_DAYS_MS);

  console.log(
    `Reactivate expired jobs: ${DRY_RUN ? 'DRY RUN' : 'LIVE'}${LIMIT ? `, limit=${LIMIT}` : ''}`
  );

  let processed = 0;
  let wouldUpdate = 0;
  let updated = 0;
  let skipped = 0;
  let lastDoc = null;

  while (true) {
    let query = db.collection(JOBS_COLLECTION).orderBy('__name__').limit(PAGE_SIZE);
    if (lastDoc) query = query.startAfter(lastDoc);

    const snapshot = await query.get();
    if (snapshot.empty) break;
    lastDoc = snapshot.docs[snapshot.docs.length - 1];

    let batch = db.batch();
    let batchCount = 0;

    for (const doc of snapshot.docs) {
      if (LIMIT && processed >= LIMIT) break;
      processed += 1;

      const card = doc.data() || {};
      const detailsRef = db.collection(JOB_DETAILS_COLLECTION).doc(doc.id);
      const detailsSnap = await detailsRef.get();
      const details = detailsSnap.exists ? detailsSnap.data() || {} : {};

      if (!shouldReactivate(card, details, now)) {
        skipped += 1;
        continue;
      }

      wouldUpdate += 1;
      const cardUpdate = {
        status: 'open',
        expiresAt: newExpiry,
        isActive: true,
        isFilled: false,
      };
      const detailsUpdate = {
        status: 'open',
        expiresAt: newExpiry,
        isActive: true,
        isFilled: false,
      };

      console.log(
        `${DRY_RUN ? 'Would update' : 'Updating'} ${doc.id}: ` +
          `status=${card.status || details.status || '(blank)'}, ` +
          `expiresAt=${toMillis(details.expiresAt) || toMillis(card.expiresAt) || '(none)'}`
      );

      if (!DRY_RUN) {
        batch.set(doc.ref, cardUpdate, { merge: true });
        batch.set(detailsRef, detailsUpdate, { merge: true });
        batchCount += 2;
        updated += 1;
      }

      if (batchCount >= 400) {
        await batch.commit();
        batch = db.batch();
        batchCount = 0;
      }
    }

    if (!DRY_RUN && batchCount > 0) {
      await batch.commit();
    }
    if (LIMIT && processed >= LIMIT) break;
  }

  console.log('Done.');
  console.log(`Processed: ${processed}`);
  console.log(`Skipped: ${skipped}`);
  console.log(`${DRY_RUN ? 'Would update' : 'Updated'}: ${DRY_RUN ? wouldUpdate : updated}`);
  console.log(`New expiry: ${new Date(now + THIRTY_DAYS_MS).toISOString()}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
