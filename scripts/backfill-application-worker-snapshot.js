/**
 * Bug #18 backfill — populate workerSnapshot fields on legacy
 * applications/{jobId}_{workerId} docs.
 *
 * The applications collection now allows the worker to denormalize
 * their identity into the doc on apply (workerName, workerPhone,
 * workerProfileImageUrl, workerSkills) so the employer can render the
 * applicant card without a worker_profiles read (which is owner-only).
 *
 * Pre-existing applications from before that fix exist with no snapshot
 * and render as "Unknown Worker" on the JobApplications screen. This
 * one-shot script reads users + worker_profiles and patches each
 * application doc that is missing workerName.
 *
 * Usage:
 *   node scripts/backfill-application-worker-snapshot.js [--dry-run] [--limit=500]
 *
 * Notes:
 *   • Skips docs that already have workerName populated.
 *   • Logs progress every 100 docs.
 *   • Uses the same admin SDK service-account loader the other backfill
 *     scripts use.
 *   • Idempotent — re-running after a partial run only patches what is
 *     still missing.
 */
const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const args = process.argv.slice(2);
const DRY_RUN = args.includes('--dry-run');
const LIMIT_ARG = args.find((a) => a.startsWith('--limit='));
const LIMIT = LIMIT_ARG ? parseInt(LIMIT_ARG.split('=')[1], 10) : 0;

async function main() {
  const sa = loadServiceAccount();
  if (!admin.apps.length) {
    admin.initializeApp({ credential: admin.credential.cert(sa) });
  }
  const db = admin.firestore();

  console.log(
    `Bug #18 backfill: applications.workerSnapshot — ${
      DRY_RUN ? 'DRY RUN' : 'LIVE'
    }${LIMIT ? `, limit=${LIMIT}` : ''}`
  );

  const profileCache = new Map();

  async function loadWorkerSnapshot(workerId) {
    if (!workerId) return null;
    if (profileCache.has(workerId)) return profileCache.get(workerId);

    const [userSnap, profileSnap] = await Promise.all([
      db.collection('users').doc(workerId).get(),
      db.collection('worker_profiles').doc(workerId).get(),
    ]);
    const user = userSnap.exists ? userSnap.data() : {};
    const profile = profileSnap.exists ? profileSnap.data() : {};

    const snap = {
      workerName:
        (profile && profile.fullName) ||
        (user && user.fullName) ||
        (user && user.displayName) ||
        '',
      workerPhone: (user && user.phone) || (profile && profile.phone) || '',
      workerProfileImageUrl:
        (profile && profile.profileImageUrl) ||
        (user && user.profileImageUrl) ||
        '',
      workerSkills: Array.isArray(profile && profile.skills)
        ? profile.skills.slice(0, 20)
        : [],
    };
    profileCache.set(workerId, snap);
    return snap;
  }

  let processed = 0;
  let updated = 0;
  let skipped = 0;
  let missingWorker = 0;
  let lastDoc = null;
  const PAGE_SIZE = 200;

  // Page through applications. We can't filter on "missing" fields, so
  // we scan everything and skip docs that already have workerName.
  // eslint-disable-next-line no-constant-condition
  while (true) {
    let q = db.collection('applications').orderBy('__name__').limit(PAGE_SIZE);
    if (lastDoc) q = q.startAfter(lastDoc);
    const snap = await q.get();
    if (snap.empty) break;
    lastDoc = snap.docs[snap.docs.length - 1];

    let batch = db.batch();
    let batchCount = 0;

    for (const doc of snap.docs) {
      processed += 1;
      const data = doc.data() || {};
      const existing = (data.workerName || '').toString().trim();
      if (existing.length > 0) {
        skipped += 1;
        continue;
      }
      const workerId = (data.workerId || '').toString();
      const snapData = await loadWorkerSnapshot(workerId);
      if (!snapData || (!snapData.workerName && !snapData.workerPhone)) {
        missingWorker += 1;
        continue;
      }
      const update = {};
      if (!data.workerName && snapData.workerName) update.workerName = snapData.workerName;
      if (!data.workerPhone && snapData.workerPhone) update.workerPhone = snapData.workerPhone;
      if (!data.workerProfileImageUrl && snapData.workerProfileImageUrl)
        update.workerProfileImageUrl = snapData.workerProfileImageUrl;
      if (
        (!Array.isArray(data.workerSkills) || data.workerSkills.length === 0) &&
        snapData.workerSkills.length
      ) {
        update.workerSkills = snapData.workerSkills;
      }
      if (Object.keys(update).length === 0) {
        skipped += 1;
        continue;
      }
      if (!DRY_RUN) {
        batch.update(doc.ref, update);
        batchCount += 1;
        if (batchCount >= 400) {
          await batch.commit();
          batch = db.batch();
          batchCount = 0;
        }
      }
      updated += 1;
      if (LIMIT && updated >= LIMIT) break;
    }

    if (!DRY_RUN && batchCount > 0) {
      await batch.commit();
    }

    if (processed % 1000 === 0 || (LIMIT && updated >= LIMIT)) {
      console.log(
        `progress: processed=${processed} updated=${updated} skipped=${skipped} missingWorker=${missingWorker}`
      );
    }

    if (LIMIT && updated >= LIMIT) break;
    if (snap.size < PAGE_SIZE) break;
  }

  console.log('\nBackfill summary:');
  console.log(`  processed=${processed}`);
  console.log(`  updated=${updated}${DRY_RUN ? ' (dry-run, no writes)' : ''}`);
  console.log(`  skipped (already had snapshot)=${skipped}`);
  console.log(`  missingWorker (no users/worker_profiles doc)=${missingWorker}`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Backfill failed:', err);
    process.exit(1);
  });
