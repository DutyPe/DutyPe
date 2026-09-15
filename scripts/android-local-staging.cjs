const path = require('node:path');
const { createRequire } = require('node:module');
const assert = require('node:assert/strict');
const projectId = 'demo-dutype-android-fixes';
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8185';
process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9199';
process.env.GCLOUD_PROJECT = projectId;
const requireBackend = createRequire(path.resolve(__dirname, '../functions/package.json'));

async function main() {
  if (!['seed', 'verify'].includes(process.argv[2])) throw new Error('Use seed or verify for the fixed local demo project.');
  const admin = requireBackend('firebase-admin');
  const app = admin.initializeApp({ projectId });
  const db = app.firestore();
  const auth = app.auth();
  try {
    if (process.argv[2] === 'seed') {
      for (const role of ['worker', 'employer']) {
        const uid = `device-${role}`;
        try { await auth.getUser(uid); }
        catch (error) {
          if (error.code !== 'auth/user-not-found') throw error;
          await auth.createUser({ uid, email: `${uid}@example.invalid`, password: 'local-emulator-only-password', emailVerified: true });
        }
        await db.doc(`users/${uid}`).set({
          fullName: role === 'worker' ? 'Staging Worker' : 'Staging Employer',
          roles: [role.toUpperCase()], activeRole: role.toUpperCase(), role: role.toUpperCase(),
          phone: role === 'worker' ? '+919000000000' : '+919000000001', email: `${uid}@example.invalid`,
          isActive: true, profileCompleted: false,
          referralStats: { availableBalance: 999, successfulReferrals: 5 }
        });
        await db.doc(`referral_stats/${uid}`).set({ availableBalance: 500, totalEarnings: 500, withdrawnAmount: 0,
          successfulReferrals: 5, totalWithdrawals: 0, isBlocked: false });
      }
      await db.doc('jobs/device-job').set({ employerId: 'device-employer', title: 'Local Staging Job', category: 'HELPER',
        isActive: true, createdAt: Date.now(), expiresAt: Date.now() + 86400000, vacancies: 2, acceptedCount: 9, applicationCount: 0 });
      await db.doc('job_applications/device-completed-work').set({ id: 'device-completed-work', jobId: 'device-job',
        workerId: 'device-worker', employerId: 'device-employer', status: 'COMPLETED', active: true, appliedAt: Date.now() });
      console.log('Seeded only device-* synthetic records and emulator identities. Run reconciliation and profile backfill before device tests.');
    } else {
      const profile = await db.doc('public_profiles/device-worker').get();
      assert.equal(profile.get('fullName'), 'Staging Worker');
      assert.equal(profile.get('phone'), undefined);
      const worker = await db.doc('referral_stats/device-worker').get();
      const withdrawals = await db.collection('withdrawal_requests').where('userId', '==', 'device-worker').get();
      assert.equal(withdrawals.size, 1);
      assert.equal(worker.get('availableBalance'), 400);
      assert.equal(worker.get('totalWithdrawals'), 1);
      const ratings = await db.collection('ratings').where('applicationId', '==', 'device-completed-work').get();
      assert.equal(ratings.size, 1);
      assert.equal((await db.doc('users/device-employer').get()).get('totalRatings'), 1);
      assert.equal((await db.doc('jobs/device-job').get()).get('acceptedCount'), 1);
      console.log('Verified device writes: one withdrawal/debit, one rating/aggregate, reconciled capacity, and public profile without private contact.');
    }
  } finally {
    await db.terminate();
    await app.delete();
  }
}

main().catch(error => { console.error(error.message); process.exitCode = 1; });