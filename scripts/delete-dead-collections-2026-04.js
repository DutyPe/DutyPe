/*
 * One-shot cleanup runner — 2026-04.
 *
 *   node scripts/delete-dead-collections-2026-04.js [--apply]
 *
 * Without `--apply` this is a dry run (counts only). With `--apply` it
 * permanently deletes the four dead collections and strips redundant fields
 * from `users`, `referrals`, and `worker_profiles` documents.
 *
 * Dead collections (no live writers in code as of 2026-04):
 *   - conversations
 *   - messages
 *   - work_locations
 *   - withdrawal_requests   (real data lives at users/{uid}/withdrawals)
 *
 * Field strips:
 *   - users.role             (legacy single-role field; readers fall back to activeRole/roles)
 *   - users.referralStats    (legacy embedded mirror; canonical is referral_stats/{uid})
 *   - referrals.referrerUserId  (legacy alias of referrerId)
 *
 * Worker profile field migration:
 *   - worker_profiles.jobTypes  ->  worker_profiles.skills (when skills empty), then delete jobTypes
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const APPLY = process.argv.includes('--apply');
const BATCH_SIZE = 400;

admin.initializeApp({
  credential: admin.credential.cert(loadServiceAccount())
});

const db = admin.firestore();

async function deleteCollection(name) {
  let total = 0;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const snap = await db.collection(name).limit(BATCH_SIZE).get();
    if (snap.empty) break;
    if (APPLY) {
      const batch = db.batch();
      snap.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
    }
    total += snap.size;
    process.stdout.write(`  ${name}: processed ${total}...\r`);
    if (!APPLY) break; // dry run only counts the first page
  }
  console.log(`  ${name}: ${APPLY ? 'deleted' : 'would delete (page 1)'} ${total} docs`);
  return total;
}

async function stripFieldFromCollection(name, field) {
  let total = 0;
  let last = null;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    let q = db.collection(name).orderBy('__name__').limit(BATCH_SIZE);
    if (last) q = q.startAfter(last);
    const snap = await q.get();
    if (snap.empty) break;
    last = snap.docs[snap.docs.length - 1];

    const batch = APPLY ? db.batch() : null;
    let touched = 0;
    for (const doc of snap.docs) {
      if (Object.prototype.hasOwnProperty.call(doc.data(), field)) {
        touched += 1;
        if (batch) batch.update(doc.ref, { [field]: admin.firestore.FieldValue.delete() });
      }
    }
    if (batch && touched > 0) await batch.commit();
    total += touched;
    if (!APPLY) break; // dry run: scan only first page
  }
  console.log(`  ${name}.${field}: ${APPLY ? 'cleared' : 'would clear (page 1)'} ${total} docs`);
  return total;
}

async function migrateWorkerJobTypes() {
  let total = 0;
  let migrated = 0;
  let last = null;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    let q = db.collection('worker_profiles').orderBy('__name__').limit(BATCH_SIZE);
    if (last) q = q.startAfter(last);
    const snap = await q.get();
    if (snap.empty) break;
    last = snap.docs[snap.docs.length - 1];

    const batch = APPLY ? db.batch() : null;
    for (const doc of snap.docs) {
      const data = doc.data();
      const jobTypes = Array.isArray(data.jobTypes) ? data.jobTypes : null;
      if (!jobTypes) continue;
      total += 1;
      const skills = Array.isArray(data.skills) ? data.skills : [];
      const update = { jobTypes: admin.firestore.FieldValue.delete() };
      if (skills.length === 0) {
        update.skills = jobTypes;
        migrated += 1;
      }
      if (batch) batch.update(doc.ref, update);
    }
    if (batch) await batch.commit();
    if (!APPLY) break;
  }
  console.log(
    `  worker_profiles.jobTypes: ${APPLY ? 'migrated' : 'would migrate (page 1)'} ${total} docs (skills filled in ${migrated})`
  );
}

(async () => {
  console.log(`\n== Dead-collection cleanup (${APPLY ? 'APPLY' : 'DRY RUN'}) ==\n`);

  console.log('1) Deleting dead collections:');
  for (const name of ['conversations', 'messages', 'work_locations', 'withdrawal_requests']) {
    await deleteCollection(name);
  }

  console.log('\n2) Stripping legacy fields:');
  await stripFieldFromCollection('users', 'role');
  await stripFieldFromCollection('users', 'userId');
  await stripFieldFromCollection('users', 'lastActiveAt');
  await stripFieldFromCollection('users', 'referralStats');
  await stripFieldFromCollection('referrals', 'referrerUserId');
  await stripFieldFromCollection('referrals', 'id');
  await stripFieldFromCollection('referrals', 'reward');
  await stripFieldFromCollection('jobmetadata', 'companyCity');

  console.log('\n3) Stripping redundant doc-ID mirrors:');
  await stripFieldFromCollection('worker_profiles', 'userId');
  await stripFieldFromCollection('worker_profiles', 'lastActiveAt');
  await stripFieldFromCollection('employer_profiles', 'userId');
  await stripFieldFromCollection('employer_profiles', 'lastActiveAt');
  await stripFieldFromCollection('referral_stats', 'userId');
  await stripFieldFromCollection('referral_stats', 'canWithdraw');
  await stripFieldFromCollection('referral_stats', 'nextMilestone');
  await stripFieldFromCollection('referral_stats', 'pendingEarnings');
  await stripFieldFromCollection('referral_codes', 'code');
  await stripFieldFromCollection('notifications', 'id');
  await stripFieldFromCollection('notifications', 'body');

  console.log('\n4) Migrating worker_profiles.jobTypes -> skills:');
  await migrateWorkerJobTypes();

  console.log('\nDone.');
  process.exit(0);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
