/**
 * Final clean-schema migration for DutyPe.
 *
 * Target collections:
 * users, worker_profiles, employer_profiles, jobs, job_details,
 * applications, saved_jobs, ratings, referrals, notifications
 *
 * Usage:
 * node scripts/final-schema-cleanup.js --dry-run
 * node scripts/final-schema-cleanup.js
 * node scripts/final-schema-cleanup.js --purge
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const DRY_RUN = process.argv.includes('--dry-run');
const PURGE = process.argv.includes('--purge');

function resolveServiceAccount() {
  const candidates = [
    path.join(__dirname, 'serviceAccountKey.json'),
    path.join(__dirname, 'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'),
    path.join(__dirname, '..', 'serviceAccountKey.json'),
    path.join(__dirname, '..', 'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json')
  ];

  for (const keyPath of candidates) {
    if (fs.existsSync(keyPath)) {
      return keyPath;
    }
  }

  return null;
}

const serviceAccountPath = resolveServiceAccount();
if (!serviceAccountPath) {
  console.error('Service account key not found.');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath))
});

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;
const Timestamp = admin.firestore.Timestamp;

const stats = {
  users: 0,
  workerProfiles: 0,
  employerProfiles: 0,
  jobs: 0,
  jobDetails: 0,
  applications: 0,
  savedJobs: 0,
  ratings: 0,
  referrals: 0,
  notifications: 0,
  skippedInvalidUsers: 0,
  purgedDocs: 0
};

function toTimestamp(value) {
  if (!value) return null;
  if (value instanceof Timestamp) return value;
  if (value && typeof value.toDate === 'function') return Timestamp.fromDate(value.toDate());

  if (typeof value === 'number') {
    const d = new Date(value);
    if (!isNaN(d.getTime())) return Timestamp.fromDate(d);
  }

  if (typeof value === 'string') {
    const d = new Date(value);
    if (!isNaN(d.getTime())) return Timestamp.fromDate(d);
  }

  return null;
}

function asNumber(value, fallback = 0) {
  if (typeof value === 'number' && !Number.isNaN(value)) return value;
  if (typeof value === 'string' && value.trim().length > 0) {
    const parsed = Number(value.replace(/[^0-9.-]/g, ''));
    if (!Number.isNaN(parsed)) return parsed;
  }
  return fallback;
}

function extractRoles(user) {
  if (Array.isArray(user.roles) && user.roles.length > 0) {
    const normalized = [...new Set(user.roles.filter(Boolean))]
      .map((r) => String(r).toUpperCase())
      .filter((r) => r === 'WORKER' || r === 'EMPLOYER');
    if (normalized.length > 0) return normalized;
  }

  const legacyRole = String(user.role || '').toUpperCase();
  if (legacyRole === 'WORKER' || legacyRole === 'EMPLOYER') return [legacyRole];
  return ['WORKER'];
}

function extractLocation(data) {
  const lat =
    data?.location?.lat ??
    data?.lat ??
    data?.latitude ??
    null;

  const lng =
    data?.location?.lng ??
    data?.lng ??
    data?.longitude ??
    null;

  if (typeof lat !== 'number' || typeof lng !== 'number') return null;
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;

  return { lat, lng };
}

function buildUserDoc(userId, data) {
  const phone = data.phone || data.phoneNumber || null;
  const fullName = data.fullName || data.name || null;
  const roles = extractRoles(data);
  const activeRole = String(data.activeRole || roles[0]).toUpperCase();
  const location = extractLocation(data);
  const geohash = data.geohash || data.geoHash || null;

  if (!phone || !fullName || !location || !geohash) {
    return { valid: false, reason: 'missing_required' };
  }

  const createdAt = toTimestamp(data.createdAt) || Timestamp.now();
  const lastActiveAt = toTimestamp(data.lastActiveAt || data.updatedAt) || Timestamp.now();

  return {
    valid: true,
    doc: {
      phone: String(phone),
      fullName: String(fullName),
      profileImageUrl: String(data.profileImageUrl || data.photoUrl || ''),
      roles,
      activeRole: roles.includes(activeRole) ? activeRole : roles[0],
      location,
      geohash: String(geohash),
      isVerified: Boolean(data.isVerified || data.phoneVerified || false),
      isActive: data.isActive === false ? false : true,
      fcmToken: String(data.fcmToken || ''),
      createdAt,
      lastActiveAt
    }
  };
}

function buildWorkerProfile(userId, user) {
  const rawJobTypes = Array.isArray(user.jobTypes)
    ? user.jobTypes
    : typeof user.skills === 'string'
      ? user.skills.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

  return {
    userId,
    jobTypes: rawJobTypes,
    isAvailable: user.isAvailable === undefined ? true : Boolean(user.isAvailable),
    rating: asNumber(user.ratingSummary?.average || user.rating || 0, 0),
    totalRatings: Math.max(0, Math.floor(asNumber(user.ratingSummary?.count || user.totalRatings || 0, 0))),
    totalJobs: Math.max(0, Math.floor(asNumber(user.totalJobs || user.completedJobs || 0, 0))),
    lastActiveAt: toTimestamp(user.lastActiveAt || user.updatedAt) || Timestamp.now()
  };
}

function buildEmployerProfile(userId, user) {
  return {
    userId,
    companyName: String(user.companyName || `${user.fullName || user.name || 'Employer'}`),
    rating: asNumber(user.ratingSummary?.average || user.rating || 0, 0),
    totalRatings: Math.max(0, Math.floor(asNumber(user.ratingSummary?.count || user.totalRatings || 0, 0))),
    totalHires: Math.max(0, Math.floor(asNumber(user.totalHires || 0, 0)))
  };
}

async function commitBatch(ops) {
  if (DRY_RUN || ops.length === 0) return;
  const batch = db.batch();
  for (const op of ops) {
    if (op.type === 'set') {
      batch.set(op.ref, op.data, op.options || {});
    } else if (op.type === 'delete') {
      batch.delete(op.ref);
    }
  }
  await batch.commit();
}

async function migrateUsersAndProfiles() {
  const snap = await db.collection('users').get();
  const ops = [];

  for (const doc of snap.docs) {
    const userId = doc.id;
    const data = doc.data();
    const userBuild = buildUserDoc(userId, data);

    if (!userBuild.valid) {
      stats.skippedInvalidUsers += 1;
      continue;
    }

    ops.push({
      type: 'set',
      ref: db.collection('users').doc(userId),
      data: userBuild.doc
    });
    stats.users += 1;

    const roles = userBuild.doc.roles;
    if (roles.includes('WORKER')) {
      ops.push({
        type: 'set',
        ref: db.collection('worker_profiles').doc(userId),
        data: buildWorkerProfile(userId, data)
      });
      stats.workerProfiles += 1;
    }

    if (roles.includes('EMPLOYER')) {
      ops.push({
        type: 'set',
        ref: db.collection('employer_profiles').doc(userId),
        data: buildEmployerProfile(userId, data)
      });
      stats.employerProfiles += 1;
    }

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

function mapJobStatus(job) {
  if (job.status) return String(job.status).toLowerCase();
  if (job.isActive === false) return 'closed';
  if (job.isFilled === true) return 'closed';
  return 'open';
}

async function migrateJobsAndDetails() {
  const snap = await db.collection('jobs').get();
  const ops = [];

  for (const doc of snap.docs) {
    const jobId = doc.id;
    const data = doc.data();
    const location = extractLocation(data);
    if (!location) continue;

    const core = {
      employerId: String(data.employerId || ''),
      title: String(data.title || ''),
      jobType: String(data.jobType || data.category || ''),
      salary: asNumber(data.salary ?? data.payAmount, 0),
      salaryType: String(data.salaryType || data.payType || 'DAILY').toUpperCase(),
      location,
      geohash: String(data.geohash || data.geoHash || ''),
      urgency: String(data.urgency || 'MEDIUM').toUpperCase(),
      status: mapJobStatus(data),
      createdAt: toTimestamp(data.createdAt || data.postedAt) || Timestamp.now(),
      expiresAt: toTimestamp(data.expiresAt) || Timestamp.fromMillis(Date.now() + 7 * 24 * 60 * 60 * 1000)
    };

    if (!core.employerId || !core.title || !core.jobType || !core.geohash) {
      continue;
    }

    const details = {
      description: String(data.description || ''),
      contactNumber: String(data.contactNumber || data.contactPhone || ''),
      addressText: String(data.addressText || data.location || '')
    };

    ops.push({ type: 'set', ref: db.collection('jobs').doc(jobId), data: core });
    ops.push({ type: 'set', ref: db.collection('job_details').doc(jobId), data: details });
    stats.jobs += 1;
    stats.jobDetails += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function migrateApplications() {
  const sources = ['applications', 'job_applications'];
  const merged = new Map();

  for (const source of sources) {
    const snap = await db.collection(source).get();
    for (const doc of snap.docs) {
      const d = doc.data();
      const jobId = String(d.jobId || '');
      const workerId = String(d.workerId || '');
      const employerId = String(d.employerId || '');
      if (!jobId || !workerId || !employerId) continue;

      const id = `${jobId}_${workerId}`;
      const createdAt = toTimestamp(d.createdAt || d.appliedAt) || Timestamp.now();
      const status = String(d.status || 'applied').toLowerCase();

      if (!merged.has(id) || merged.get(id).createdAt.toMillis() > createdAt.toMillis()) {
        merged.set(id, {
          jobId,
          workerId,
          employerId,
          status,
          createdAt
        });
      }
    }
  }

  const ops = [];
  for (const [id, data] of merged.entries()) {
    ops.push({ type: 'set', ref: db.collection('applications').doc(id), data });
    stats.applications += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function migrateSavedJobs() {
  const merged = new Map();

  const userSnap = await db.collection('users').get();
  for (const doc of userSnap.docs) {
    const userId = doc.id;
    const saved = doc.data().savedJobs;
    if (!Array.isArray(saved)) continue;

    for (const jobIdRaw of saved) {
      const jobId = String(jobIdRaw || '');
      if (!jobId) continue;
      const id = `${userId}_${jobId}`;
      if (!merged.has(id)) {
        merged.set(id, {
          userId,
          jobId,
          createdAt: Timestamp.now()
        });
      }
    }
  }

  const savedSnap = await db.collection('saved_jobs').get();
  for (const doc of savedSnap.docs) {
    const d = doc.data();
    const userId = String(d.userId || d.workerId || '');
    const jobId = String(d.jobId || '');
    if (!userId || !jobId) continue;

    const id = `${userId}_${jobId}`;
    if (!merged.has(id)) {
      merged.set(id, {
        userId,
        jobId,
        createdAt: toTimestamp(d.createdAt) || Timestamp.now()
      });
    }
  }

  const ops = [];
  for (const [id, data] of merged.entries()) {
    ops.push({ type: 'set', ref: db.collection('saved_jobs').doc(id), data });
    stats.savedJobs += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function migrateRatings() {
  const snap = await db.collection('ratings').get();
  const ops = [];

  for (const doc of snap.docs) {
    const d = doc.data();
    const mapped = {
      jobId: String(d.jobId || d.applicationId || ''),
      fromUserId: String(d.fromUserId || d.raterId || ''),
      toUserId: String(d.toUserId || d.ratedUserId || ''),
      rating: asNumber(d.rating, 0),
      review: String(d.review || d.comment || ''),
      createdAt: toTimestamp(d.createdAt || d.updatedAt) || Timestamp.now()
    };

    if (!mapped.jobId || !mapped.fromUserId || !mapped.toUserId || mapped.rating <= 0) continue;

    ops.push({ type: 'set', ref: db.collection('ratings').doc(doc.id), data: mapped });
    stats.ratings += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function migrateReferrals() {
  const snap = await db.collection('referrals').get();
  const ops = [];

  for (const doc of snap.docs) {
    const d = doc.data();
    const mapped = {
      referrerId: String(d.referrerId || d.referrerUserId || ''),
      referredUserId: String(d.referredUserId || ''),
      status: String(d.status || 'pending').toLowerCase(),
      reward: asNumber(d.reward || d.rewardAmount || 0, 0),
      createdAt: toTimestamp(d.createdAt) || Timestamp.now()
    };

    if (!mapped.referrerId || !mapped.referredUserId) continue;

    ops.push({ type: 'set', ref: db.collection('referrals').doc(doc.id), data: mapped });
    stats.referrals += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function migrateNotifications() {
  const snap = await db.collection('notifications').get();
  const ops = [];

  for (const doc of snap.docs) {
    const d = doc.data();
    const mapped = {
      recipientId: String(d.recipientId || ''),
      title: String(d.title || ''),
      message: String(d.message || ''),
      type: String(d.type || 'generic'),
      data: d.data && typeof d.data === 'object' ? d.data : {},
      isRead: Boolean(d.isRead),
      createdAt: toTimestamp(d.createdAt) || Timestamp.now()
    };

    if (!mapped.recipientId || !mapped.title || !mapped.message) continue;

    ops.push({ type: 'set', ref: db.collection('notifications').doc(doc.id), data: mapped });
    stats.notifications += 1;

    if (ops.length >= 450) {
      await commitBatch(ops.splice(0, ops.length));
    }
  }

  await commitBatch(ops);
}

async function purgeLegacyCollections() {
  const legacyCollections = [
    'job_applications',
    'referral_codes',
    'referral_stats',
    'moderation_queue',
    'employer_blocked_attempts',
    'metadata',
    'phone_roles',
    'announcements',
    'app_feedback',
    'notification_tracking',
    'withdrawal_requests'
  ];

  for (const collectionName of legacyCollections) {
    const snap = await db.collection(collectionName).limit(1000).get();
    if (snap.empty) continue;

    let page = snap;
    while (!page.empty) {
      const ops = [];
      for (const doc of page.docs) {
        ops.push({ type: 'delete', ref: doc.ref });
        stats.purgedDocs += 1;
      }
      await commitBatch(ops);
      page = await db.collection(collectionName).limit(1000).get();
    }
  }
}

async function run() {
  const started = Date.now();

  console.log('Final clean-schema migration started');
  console.log(`Dry run: ${DRY_RUN ? 'yes' : 'no'}`);
  console.log(`Purge legacy: ${PURGE ? 'yes' : 'no'}`);

  await migrateUsersAndProfiles();
  await migrateJobsAndDetails();
  await migrateApplications();
  await migrateSavedJobs();
  await migrateRatings();
  await migrateReferrals();
  await migrateNotifications();

  if (PURGE) {
    await purgeLegacyCollections();
  }

  const tookMs = Date.now() - started;
  console.log('Migration complete');
  console.log(JSON.stringify({
    ...stats,
    dryRun: DRY_RUN,
    purge: PURGE,
    tookMs
  }, null, 2));
}

run().catch((error) => {
  console.error('Migration failed:', error);
  process.exit(1);
});
