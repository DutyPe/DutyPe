const { before, after, test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const { createRequire } = require('node:module');
const requireBackend = createRequire(path.resolve(__dirname, '../functions/package.json'));
const projectId = 'demo-dutype-android-fixes';
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8185';
process.env.GCLOUD_PROJECT = projectId;
process.env.FIREBASE_CONFIG = JSON.stringify({ projectId });
const typescript = requireBackend('typescript');
require.extensions['.ts'] = (module, filename) => {
  const result = typescript.transpileModule(fs.readFileSync(filename, 'utf8'), {
    compilerOptions: { module: typescript.ModuleKind.CommonJS, target: typescript.ScriptTarget.ES2020 }
  });
  module._compile(result.outputText, filename);
};
const admin = requireBackend('firebase-admin');
const app = admin.initializeApp({ projectId });
const db = admin.firestore();
const { requestWithdrawal } = require('../functions/src/referral-system.ts');
const { submitRating } = require('../functions/src/ratings.ts');
const { publicProfile, refreshPublicProfile, getPublicProfile, getApplicationContact } = require('../functions/src/profiles.ts');
const { emitApplicationNotifications, requestNotification, notifyApplicationEvent } = require('../functions/src/notification-events.ts');
const { createJobWithIdempotency, batchUpdateVacancyStatus } = require('../functions/src/job-posting.ts');
const { backfillPublicProfilePage, applyPublicProfileItem, reconciliationPage, applyReconciliationItem } = require('../functions/src/migration.ts');

before(async () => {
  const reset = await fetch(`http://127.0.0.1:8185/emulator/v1/projects/${projectId}/databases/(default)/documents`, {
    method: 'DELETE', redirect: 'error', signal: AbortSignal.timeout(20000)
  });
  assert.equal(reset.ok, true, 'The isolated demo emulator must be available');
});
after(async () => { await db.terminate(); await app.delete(); });

test('profile migration dry run is read-only and apply preserves private source data', async () => {
  const privateData = { fullName: 'Migration Name', phone: 'private', fcmToken: 'private-token', referralStats: { availableBalance: 100 } };
  await db.doc('users/migration-profile').set(privateData);
  await db.doc('public_profiles/migration-profile').set({ id: 'migration-profile', fullName: 'Old name', phone: 'old-leak' });
  const options = { afterId: 'migration-profil', pageSize: 1 };
  const dryRun = await backfillPublicProfilePage(db, options);
  assert.equal(dryRun.items[0].action, 'replace');
  assert.equal(dryRun.items[0].applied, false);
  assert.equal((await db.doc('public_profiles/migration-profile').get()).get('phone'), 'old-leak');
  await db.doc('users/migration-profile').update({ fullName: 'Latest Name' });
  const applied = await backfillPublicProfilePage(db, { ...options, apply: true });
  assert.equal(applied.items[0].applied, true);
  assert.deepEqual((await db.doc('public_profiles/migration-profile').get()).data(), { id: 'migration-profile', fullName: 'Latest Name' });
  assert.deepEqual((await db.doc('users/migration-profile').get()).data(), { ...privateData, fullName: 'Latest Name' });
  assert.equal((await backfillPublicProfilePage(db, { ...options, apply: true })).items[0].action, 'unchanged');
});

test('profile migration pages resume and clean disabled or orphaned public projections', async () => {
  for (const userId of ['migration-page-a', 'migration-page-b', 'migration-page-c']) {
    await db.doc(`users/${userId}`).set({ fullName: userId });
  }
  const first = await backfillPublicProfilePage(db, { afterId: 'migration-page-', pageSize: 2, apply: true });
  assert.deepEqual(first.items.map(item => item.userId), ['migration-page-a', 'migration-page-b']);
  assert.equal(first.nextCursor, 'migration-page-b');
  const second = await backfillPublicProfilePage(db, { afterId: first.nextCursor, pageSize: 1, apply: true });
  assert.equal(second.items[0].userId, 'migration-page-c');
  await db.doc('users/migration-page-a').update({ isActive: false });
  await db.doc('users/migration-page-b').delete();
  const cleanup = await backfillPublicProfilePage(db, { collection: 'public_profiles', afterId: 'migration-page-', pageSize: 2, apply: true });
  assert.deepEqual(cleanup.items.map(item => item.action), ['delete', 'delete']);
  assert.equal((await db.doc('public_profiles/migration-page-c').get()).exists, true);
});

test('profile migration rejects unbounded page sizes before database access', async () => {
  await assert.rejects(backfillPublicProfilePage(db, { pageSize: 0 }), /Page size/);
  await assert.rejects(backfillPublicProfilePage(db, { pageSize: 101 }), /Page size/);
});

async function account(userId, balance = 1000, extra = {}) {
  await db.doc(`users/${userId}`).set({ activeRole: 'WORKER', referralStats: { availableBalance: 999999, successfulReferrals: 100, canWithdraw: true } });
  await db.doc(`referral_stats/${userId}`).set({ availableBalance: balance, successfulReferrals: 5, withdrawnAmount: 0, totalWithdrawals: 0, ...extra });
}

function withdraw(userId, requestId, amount = 100, extra = {}) {
  return requestWithdrawal.run({ requestId, amount, paymentMethod: 'UPI', upiId: 'synthetic@invalid', ...extra }, { auth: { uid: userId, token: {} } });
}

test('concurrent withdrawals cannot overspend a balance or daily limit', { timeout: 30000 }, async () => {
  await account('concurrent');
  const results = await Promise.all([
    withdraw('concurrent', 'concurrent-request-01', 600),
    withdraw('concurrent', 'concurrent-request-02', 600)
  ]);
  assert.equal(results.filter(result => result.success).length, 1);
  assert.equal((await db.doc('referral_stats/concurrent').get()).get('availableBalance'), 400);
  assert.equal((await db.collection('withdrawal_requests').where('userId', '==', 'concurrent').get()).size, 1);
});

test('concurrent and repeated same-ID retries create one withdrawal and deduction', { timeout: 30000 }, async () => {
  await account('retry');
  const results = await Promise.all(Array.from({ length: 4 }, () => withdraw('retry', 'idempotent-request-01', 200)));
  assert.ok(results.every(result => result.success));
  assert.equal(new Set(results.map(result => result.withdrawalId)).size, 1);
  for (let attempt = 0; attempt < 6; attempt += 1) {
    assert.equal((await withdraw('retry', 'idempotent-request-01', 200)).withdrawalId, results[0].withdrawalId);
  }
  const stats = await db.doc('referral_stats/retry').get();
  assert.equal(stats.get('availableBalance'), 800);
  assert.equal(stats.get('totalWithdrawals'), 1);
  assert.equal((await db.collection('referral_events').where('userId', '==', 'retry').get()).size, 1);
});

test('same request ID cannot be reused for a different amount or payment destination', async () => {
  await account('changed');
  assert.equal((await withdraw('changed', 'request-details-01', 100)).success, true);
  await assert.rejects(withdraw('changed', 'request-details-01', 200), error => error.code === 'already-exists');
  await assert.rejects(withdraw('changed', 'request-details-01', 100, { upiId: 'different@invalid' }), error => error.code === 'already-exists');
  assert.equal((await db.doc('referral_stats/changed').get()).get('availableBalance'), 900);
});

test('client profile mirror cannot authorize a withdrawal without authoritative funds', async () => {
  await account('unfunded', 0);
  assert.equal((await withdraw('unfunded', 'unfunded-request-01')).success, false);
  await db.doc('referral_stats/unfunded').delete();
  assert.equal((await withdraw('unfunded', 'missing-ledger-01')).success, false);
  assert.equal((await db.collection('withdrawal_requests').where('userId', '==', 'unfunded').get()).size, 0);
});

test('blocked or ineligible accounts leave no partial withdrawal records', async () => {
  await account('blocked', 1000, { isBlocked: true });
  await account('ineligible', 1000, { successfulReferrals: 0, canWithdraw: true });
  for (const userId of ['blocked', 'ineligible']) {
    assert.equal((await withdraw(userId, `${userId}-request-01`)).success, false);
    assert.equal((await db.doc(`referral_stats/${userId}`).get()).get('availableBalance'), 1000);
    assert.equal((await db.collection('withdrawal_requests').where('userId', '==', userId).get()).size, 0);
  }
});

test('daily reservation includes requests created before the daily counter existed', async () => {
  await account('legacy-day', 1000);
  await db.doc('withdrawal_requests/legacy-day-existing').set({ userId: 'legacy-day', amount: 800, createdAt: admin.firestore.Timestamp.now() });
  assert.equal((await withdraw('legacy-day', 'legacy-request-01', 300)).success, false);
  assert.equal((await db.doc('referral_stats/legacy-day').get()).get('availableBalance'), 1000);
});

test('withdrawal rejects malformed amounts, missing IDs, invalid payments, and unauthenticated calls', async () => {
  await account('invalid');
  for (const amount of [NaN, Infinity, -100, 0, 49, 100.001, '100']) {
    await assert.rejects(withdraw('invalid', 'invalid-request-01', amount), error => error.code === 'invalid-argument');
  }
  await assert.rejects(withdraw('invalid', ''), error => error.code === 'invalid-argument');
  await assert.rejects(withdraw('invalid', 'payment-request-01', 100, { upiId: '' }), error => error.code === 'invalid-argument');
  await assert.rejects(requestWithdrawal.run({}, {}), error => error.code === 'unauthenticated');
  await assert.rejects(withdraw('invalid', 'account-changed-01', 100, { userId: 'other' }), error => error.code === 'permission-denied');
  assert.equal((await db.doc('referral_stats/invalid').get()).get('availableBalance'), 1000);
});

async function completedWork(applicationId, workerId, employerId) {
  await db.doc(`users/${workerId}`).set({ fullName: 'Synthetic worker' }, { merge: true });
  await db.doc(`users/${employerId}`).set({ fullName: 'Synthetic employer' }, { merge: true });
  await db.doc(`job_applications/${applicationId}`).set({ workerId, employerId, jobId: 'synthetic-job', status: 'COMPLETED' });
}

function rate(userId, applicationId, rating = 5, extra = {}) {
  return submitRating.run({ applicationId, rating, review: 'Synthetic review', tags: ['Reliable'], ...extra }, { auth: { uid: userId, token: {} } });
}

test('ratings derive participants from completed work and preserve both concurrent aggregates', { timeout: 30000 }, async () => {
  await completedWork('rated-work-1', 'rater-one', 'rated-employer');
  await completedWork('rated-work-2', 'rater-two', 'rated-employer');
  await Promise.all([
    rate('rater-one', 'rated-work-1', 5, { targetUserId: 'unrelated', targetRole: 'WORKER' }),
    rate('rater-two', 'rated-work-2', 1)
  ]);
  const target = await db.doc('users/rated-employer').get();
  assert.equal(target.get('totalRatings'), 2);
  assert.equal(target.get('averageRating'), 3);
  const ratings = await db.collection('ratings').where('targetUserId', '==', 'rated-employer').get();
  assert.equal(ratings.size, 2);
  assert.ok(ratings.docs.every(document => document.get('targetRole') === 'EMPLOYER'));
});

test('same-application rating retries are idempotent and cannot overwrite a prior review', { timeout: 30000 }, async () => {
  await completedWork('rated-retry', 'retry-rater', 'retry-target');
  const results = await Promise.all([rate('retry-rater', 'rated-retry'), rate('retry-rater', 'rated-retry')]);
  assert.equal(new Set(results.map(result => result.ratingId)).size, 1);
  assert.equal((await db.doc('users/retry-target').get()).get('totalRatings'), 1);
  await assert.rejects(rate('retry-rater', 'rated-retry', 1), error => error.code === 'already-exists');
  await rate('retry-target', 'rated-retry', 4);
  assert.equal((await db.doc('users/retry-rater').get()).get('workerAverageRating'), 4);
});

test('rating rejects unrelated users, incomplete work, self-rating and invalid stars', async () => {
  await completedWork('invalid-rating-work', 'rating-worker', 'rating-employer');
  await assert.rejects(rate('outsider', 'invalid-rating-work'), error => error.code === 'permission-denied');
  for (const rating of [0, 6, 1.5, Infinity, '5']) {
    await assert.rejects(rate('rating-worker', 'invalid-rating-work', rating), error => error.code === 'invalid-argument');
  }
  await db.doc('job_applications/invalid-rating-work').update({ status: 'IN_PROGRESS' });
  await assert.rejects(rate('rating-worker', 'invalid-rating-work'), error => error.code === 'failed-precondition');
  await completedWork('self-rating-work', 'self-rater', 'self-rater');
  await assert.rejects(rate('self-rater', 'self-rating-work'), error => error.code === 'failed-precondition');
  assert.equal((await db.doc('users/rating-employer').get()).get('totalRatings'), undefined);
});

test('a legacy randomly identified review also prevents another rating', async () => {
  await completedWork('legacy-rating-work', 'legacy-rater', 'legacy-target');
  await db.doc('ratings/legacy-id').set({ applicationId: 'legacy-rating-work', raterId: 'legacy-rater', rating: 4 });
  await assert.rejects(rate('legacy-rater', 'legacy-rating-work'), error => error.code === 'already-exists');
  assert.equal((await db.doc('users/legacy-target').get()).get('totalRatings'), undefined);
});

test('public profile allowlist excludes contact, location, tokens, balances and nested private fields', () => {
  const profile = publicProfile('profile-user', {
    fullName: 'Public Name', companyName: 'Public Company', roles: ['WORKER', 'ADMIN'],
    skills: ['Cooking', { phone: 'private' }], averageRating: 4,
    phone: 'private-phone', email: 'private-email', address: 'private-address',
    latitude: 10, longitude: 20, fcmToken: 'private-token', dateOfBirth: 'private-date',
    referralStats: { availableBalance: 1000 }, workLocations: [{ address: 'home' }],
    experience: { documents: 'private' }, unknownFutureField: 'private'
  });
  assert.deepEqual(profile, { id: 'profile-user', fullName: 'Public Name', companyName: 'Public Company', skills: 'Cooking', averageRating: 4, roles: ['WORKER'] });
});

test('public projection reads current source and removes obsolete/private fields or deleted profiles', async () => {
  await db.doc('users/projected').set({ fullName: 'Current', phone: 'private', fcmToken: 'private' });
  await db.doc('public_profiles/projected').set({ fullName: 'Stale', phone: 'leaked-before' });
  await refreshPublicProfile('projected');
  assert.deepEqual((await db.doc('public_profiles/projected').get()).data(), { id: 'projected', fullName: 'Current' });
  await db.doc('users/projected').delete();
  await refreshPublicProfile('projected');
  assert.equal((await db.doc('public_profiles/projected').get()).exists, false);
});

test('contact callable discloses only the other participant contact and denies unrelated or ended work', async () => {
  await completedWork('contact-work', 'contact-worker', 'contact-employer');
  await db.doc('users/contact-worker').update({ phone: 'private-worker-phone', email: 'worker@example.invalid', fcmToken: 'secret', address: 'home' });
  const result = await getApplicationContact.run({ applicationId: 'contact-work', userId: 'someone-else' }, { auth: { uid: 'contact-employer', token: {} } });
  assert.equal(result.profile.id, 'contact-worker');
  assert.equal(result.profile.phone, 'private-worker-phone');
  assert.equal(result.profile.fcmToken, undefined);
  assert.equal(result.profile.address, undefined);
  await assert.rejects(getApplicationContact.run({ applicationId: 'contact-work' }, { auth: { uid: 'outsider', token: {} } }), error => error.code === 'permission-denied');
  await db.doc('job_applications/contact-work').update({ status: 'WITHDRAWN' });
  await assert.rejects(getApplicationContact.run({ applicationId: 'contact-work' }, { auth: { uid: 'contact-employer', token: {} } }), error => error.code === 'permission-denied');
  await assert.rejects(getPublicProfile.run({ userId: 'contact-worker' }, {}), error => error.code === 'unauthenticated');
});

test('notification content and recipients are derived from the actual application, not client text', async () => {
  await completedWork('notify-work', 'notify-worker', 'notify-employer');
  await db.doc('jobs/synthetic-job').set({ title: 'Actual job', employerId: 'notify-employer', isActive: true });
  await db.doc('job_applications/notify-work').update({ status: 'ACCEPTED' });
  const result = await requestNotification.run({
    type: 'APPLICATION_STATUS', recipientId: 'notify-worker', title: 'Forged title', message: 'Forged body',
    data: { applicationId: 'notify-work', status: 'REJECTED', deepLink: 'https://evil.invalid' }
  }, { auth: { uid: 'notify-employer', token: {} } });
  assert.equal(result.success, true);
  const messages = await db.collection('notifications').where('recipientId', '==', 'notify-worker').get();
  assert.equal(messages.size, 1);
  assert.equal(messages.docs[0].get('title'), 'Application Accepted');
  assert.equal(messages.docs[0].get('data.status'), 'ACCEPTED');
  assert.equal(messages.docs[0].get('data.deepLink'), 'dutype://worker/applications/notify-work');
  await assert.rejects(requestNotification.run({ type: 'APPLICATION_STATUS', recipientId: 'outsider', data: { applicationId: 'notify-work' } }, { auth: { uid: 'notify-employer', token: {} } }), error => error.code === 'permission-denied');
  await assert.rejects(requestNotification.run({ type: 'APPLICATION_STATUS', recipientId: 'notify-worker', data: { applicationId: 'notify-work' } }, { auth: { uid: 'outsider', token: {} } }), error => error.code === 'permission-denied');
});

test('duplicate application events and deleted notifications cannot be recreated by client retries', { timeout: 30000 }, async () => {
  const results = await Promise.all([emitApplicationNotifications('notify-work'), emitApplicationNotifications('notify-work')]);
  assert.deepEqual(results, [0, 0]);
  const messages = await db.collection('notifications').where('recipientId', '==', 'notify-worker').get();
  await messages.docs[0].ref.delete();
  assert.equal(await emitApplicationNotifications('notify-work'), 0);
  assert.equal((await db.collection('notifications').where('recipientId', '==', 'notify-worker').get()).size, 0);
});

test('self events cannot spoof other recipients or privileged notification types', async () => {
  await db.doc('users/self-notifier').set({ fullName: 'Self', profileCompleted: true });
  const context = { auth: { uid: 'self-notifier', token: {} } };
  await assert.rejects(requestNotification.run({ type: 'PROFILE_COMPLETE', recipientId: 'victim' }, context), error => error.code === 'permission-denied');
  await assert.rejects(requestNotification.run({ type: 'SYSTEM_UPDATE', recipientId: 'self-notifier' }, context), error => error.code === 'invalid-argument');
  const result = await requestNotification.run({ type: 'PROFILE_COMPLETE', recipientId: 'self-notifier', title: 'Fake' }, context);
  assert.equal(result.created, 1);
  assert.equal((await requestNotification.run({ type: 'PROFILE_COMPLETE', recipientId: 'self-notifier' }, context)).created, 0);
});

test('job callable cannot create on behalf of another employer and scopes retries to the real owner', { timeout: 30000 }, async () => {
  const payload = { idempotencyKey: 'same-job-request', employerId: 'job-owner', title: 'Test job', category: 'HELPER', vacancies: 2 };
  await assert.rejects(createJobWithIdempotency.run(payload, { auth: { uid: 'attacker', token: {} } }), error => error.code === 'permission-denied');
  const context = { auth: { uid: 'job-owner', token: {} } };
  const results = await Promise.all([createJobWithIdempotency.run(payload, context), createJobWithIdempotency.run(payload, context)]);
  assert.equal(results[0].jobId, results[1].jobId);
  const another = await createJobWithIdempotency.run({ ...payload, employerId: 'other-owner' }, { auth: { uid: 'other-owner', token: {} } });
  assert.notEqual(another.jobId, results[0].jobId);
  assert.equal((await db.doc(`jobs/${results[0].jobId}`).get()).get('employerId'), 'job-owner');
});

test('batch vacancy callable rejects foreign jobs without committing owned-job changes', async () => {
  await db.doc('jobs/batch-own').set({ employerId: 'batch-owner', vacancies: 1, acceptedCount: 1, isFilled: false });
  await db.doc('jobs/batch-foreign').set({ employerId: 'other-owner', vacancies: 1, acceptedCount: 1, isFilled: false });
  const context = { auth: { uid: 'batch-owner', token: {} } };
  await assert.rejects(batchUpdateVacancyStatus.run({ jobIds: ['batch-own', 'batch-foreign'] }, context), error => error.code === 'permission-denied');
  assert.equal((await db.doc('jobs/batch-own').get()).get('isFilled'), false);
  await batchUpdateVacancyStatus.run({ jobIds: ['batch-own'] }, context);
  assert.equal((await db.doc('jobs/batch-own').get()).get('isFilled'), true);
});

test('daily withdrawal limit is enforced with excess funds and request count persists across handler calls', { timeout: 30000 }, async () => {
  await account('daily-limit', 5000);
  const results = await Promise.all([
    withdraw('daily-limit', 'daily-limit-request-01', 600),
    withdraw('daily-limit', 'daily-limit-request-02', 600)
  ]);
  assert.equal(results.filter(result => result.success).length, 1);
  assert.equal((await db.doc('referral_stats/daily-limit').get()).get('availableBalance'), 4400);
  await account('request-limit', 5000);
  for (let index = 0; index < 5; index += 1) {
    assert.equal((await withdraw('request-limit', `request-count-limit-${index}`, 50)).success, true);
  }
  assert.equal((await withdraw('request-limit', 'request-count-limit-extra', 50)).success, false);
  assert.equal((await db.doc('referral_stats/request-limit').get()).get('availableBalance'), 4750);
});

test('application trigger retries derive current state and emit each logical event once', async () => {
  await completedWork('trigger-work', 'trigger-worker', 'trigger-employer');
  await db.doc('jobs/synthetic-job').set({ employerId: 'trigger-employer', title: 'Trigger job' });
  const reference = db.doc('job_applications/trigger-work');
  await reference.update({ status: 'PENDING' });
  const before = await reference.get();
  await reference.update({ status: 'ACCEPTED' });
  const after = await reference.get();
  const change = { before, after };
  const context = { params: { applicationId: 'trigger-work' } };
  await notifyApplicationEvent.run(change, context);
  await notifyApplicationEvent.run(change, context);
  const notifications = await db.collection('notifications').where('recipientId', '==', 'trigger-worker').get();
  assert.equal(notifications.size, 1);
  assert.equal(notifications.docs[0].get('data.status'), 'ACCEPTED');
  await reference.update({ updatedAt: Date.now() });
  await notifyApplicationEvent.run({ before: after, after: await reference.get() }, context);
  assert.equal((await db.collection('notifications').where('recipientId', '==', 'trigger-worker').get()).size, 1);
});

test('data reconciliation repairs reviewed derived fields and never changes authoritative money', async () => {
  await db.doc('users/reconcile-user').set({ fullName: 'Keep Name', phone: 'keep-private', averageRating: 5, totalRatings: 99, referralStats: { availableBalance: 999 } });
  const ledger = { availableBalance: 100, totalEarnings: 150, withdrawnAmount: 50, successfulReferrals: 5 };
  await db.doc('referral_stats/reconcile-user').set(ledger);
  await db.doc('users/reconcile-rater').set({ fullName: 'Rater' });
  await db.doc('job_applications/reconcile-work').set({ employerId: 'reconcile-user', workerId: 'reconcile-rater', status: 'COMPLETED', jobId: 'reconcile-job' });
  await db.doc('ratings/reconcile-review').set({ applicationId: 'reconcile-work', jobId: 'reconcile-job', raterId: 'reconcile-rater', targetUserId: 'reconcile-user', targetRole: 'EMPLOYER', rating: 3 });
  const [plan] = (await reconciliationPage(db, 'users', { afterId: 'reconcile-use', pageSize: 1 })).items;
  assert.deepEqual(plan.issues, []);
  assert.equal(plan.changes['referralStats.availableBalance'], 100);
  assert.equal(plan.changes['referralStats.canWithdraw'], true);
  assert.equal(plan.changes.totalRatings, 1);
  assert.equal((await db.doc('users/reconcile-user').get()).get('totalRatings'), 99);
  const applied = await applyReconciliationItem(db, plan, 'local-reconcile-run');
  assert.equal(applied.status, 'applied');
  assert.deepEqual((await db.doc('referral_stats/reconcile-user').get()).data(), ledger);
  assert.equal((await db.doc('users/reconcile-user').get()).get('phone'), 'keep-private');
  assert.equal((await db.doc('users/reconcile-user').get()).get('averageRating'), 3);
  assert.equal((await db.doc('users/reconcile-user').get()).get('referralStats.canWithdraw'), true);
  assert.equal((await db.doc(`migration_audit/${applied.receiptId}`).get()).get('before.totalRatings'), 99);
  assert.deepEqual((await reconciliationPage(db, 'users', { afterId: 'reconcile-use', pageSize: 1 })).items[0].changes, {});
});

test('data reconciliation rejects stale plans and leaves ambiguous reviews or fraud blocks untouched', async () => {
  const options = { afterId: 'reconcile-use', pageSize: 1 };
  await db.doc('users/reconcile-user').update({ totalRatings: 44 });
  const [plan] = (await reconciliationPage(db, 'users', options)).items;
  await db.doc('users/reconcile-user').update({ bio: 'Changed after review' });
  await assert.rejects(applyReconciliationItem(db, plan, 'stale-review-run'), /Stale reconciliation plan/);
  await db.doc('ratings/reconcile-duplicate').set((await db.doc('ratings/reconcile-review').get()).data());
  await db.doc('users/reconcile-user').update({ 'referralStats.isBlocked': true });
  const [blocked] = (await reconciliationPage(db, 'users', options)).items;
  assert.ok(blocked.issues.some(issue => issue.startsWith('duplicate_rating:')));
  assert.ok(blocked.issues.includes('fraud_block_conflict_requires_review'));
  assert.equal((await applyReconciliationItem(db, blocked, 'blocked-review-run')).status, 'blocked');
  assert.equal((await db.doc('users/reconcile-user').get()).get('totalRatings'), 44);
});

test('data reconciliation counts occupied work, preserves closed jobs, and blocks overbooked data', async () => {
  await db.doc('jobs/reconcile-capacity').set({ employerId: 'reconcile-employer', vacancies: 3, acceptedCount: 9, applicationCount: 0, vacancyStatus: 'CLOSED', isFilled: true });
  for (const [index, status] of ['ACCEPTED', 'IN_PROGRESS', 'WITHDRAWN'].entries()) {
    await db.doc(`job_applications/reconcile-capacity-${index}`).set({ jobId: 'reconcile-capacity', employerId: 'reconcile-employer', workerId: `worker-${index}`, status });
  }
  const options = { afterId: 'reconcile-capacit', pageSize: 1 };
  const [plan] = (await reconciliationPage(db, 'jobs', options)).items;
  assert.deepEqual(plan.issues, []);
  assert.equal(plan.changes.acceptedCount, 2);
  assert.equal(plan.changes.vacancyStatus, undefined);
  await applyReconciliationItem(db, plan, 'capacity-review-run');
  assert.equal((await db.doc('jobs/reconcile-capacity').get()).get('vacancyStatus'), 'CLOSED');
  await db.doc('jobs/reconcile-capacity').update({ vacancies: 1 });
  assert.ok((await reconciliationPage(db, 'jobs', options)).items[0].issues.includes('existing_job_overbooked'));
});

test('profile migration reviewed apply refuses a source changed after approval', async () => {
  await db.doc('users/migration-stale').set({ fullName: 'Original' });
  const [reviewed] = (await backfillPublicProfilePage(db, { afterId: 'migration-stal', pageSize: 1 })).items;
  await db.doc('users/migration-stale').update({ fullName: 'Changed' });
  await assert.rejects(applyPublicProfileItem(db, reviewed, 'profile-review-run'), /Stale profile plan/);
  assert.equal((await db.doc('public_profiles/migration-stale').get()).exists, false);
});

test('migration CLI refuses unsafe targets and unreviewed apply before connecting', () => {
  const { parse } = require('./android-data-migration.cjs');
  assert.throws(() => parse(['apply', '--out', 'unused-report.json']), /Apply requires/);
  assert.throws(() => parse(['plan', '--environment', 'production', '--out', 'unused-report.json']), /Only emulator/);
  assert.throws(() => parse(['plan', '--project', 'arbitrary-project', '--out', 'unused-report.json']), /isolated demo/);
  assert.throws(() => parse(['plan', '--environment', 'staging', '--project', 'dutype-staging', '--out', 'unused-report.json']), /matching --confirm-staging/);
  assert.equal(parse(['plan', '--kind', 'profiles', '--out', 'unused-report.json']).environment, 'emulator');
});

test('migration CLI rehearses reviewed plan apply and rejects report tampering', { timeout: 45000 }, async () => {
  const { spawnSync } = require('node:child_process');
  const reportRoot = path.resolve(__dirname, '../app/build/reports/migration');
  fs.mkdirSync(reportRoot, { recursive: true });
  const directory = fs.mkdtempSync(path.join(reportRoot, 'cli-rehearsal-'));
  await db.doc('users/cli-rehearsal-user').set({ fullName: 'CLI Rehearsal', phone: 'private', referralStats: { availableBalance: 999 } });
  await db.doc('referral_stats/cli-rehearsal-user').set({ availableBalance: 100, totalEarnings: 100 });
  const plan = path.join(directory, 'plan.json');
  const invoke = args => spawnSync(process.execPath, [path.join(__dirname, 'android-data-migration.cjs'), ...args], { encoding: 'utf8', timeout: 20000 });
  const planned = invoke(['plan', '--kind', 'users', '--after', 'cli-rehearsal-use', '--page-size', '1', '--out', plan]);
  assert.equal(planned.status, 0, planned.stderr);
  const reviewed = JSON.parse(fs.readFileSync(plan, 'utf8'));
  assert.equal((await db.doc('users/cli-rehearsal-user').get()).get('referralStats.availableBalance'), 999);
  const tampered = path.join(directory, 'tampered.json');
  fs.writeFileSync(tampered, JSON.stringify({ ...reviewed, project: 'different-project' }));
  const confirmation = ['--review-sha', reviewed.reviewSha, '--confirm-project', projectId, '--maintenance-ack'];
  assert.notEqual(invoke(['apply', '--plan', tampered, ...confirmation, '--out', path.join(directory, 'rejected.jsonl')]).status, 0);
  const applied = invoke(['apply', '--plan', plan, ...confirmation, '--out', path.join(directory, 'applied.jsonl')]);
  assert.equal(applied.status, 0, applied.stderr);
  assert.equal((await db.doc('users/cli-rehearsal-user').get()).get('referralStats.availableBalance'), 100);
  assert.notEqual(invoke(['plan', '--kind', 'users', '--out', plan]).status, 0);
});