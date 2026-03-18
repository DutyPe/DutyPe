const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const DRY_RUN = process.argv.includes('--dry-run');

function toMillis(value) {
  if (!value) return Date.now();
  if (typeof value === 'number') return value;
  if (typeof value.toMillis === 'function') return value.toMillis();
  if (typeof value._seconds === 'number') return value._seconds * 1000;
  return Date.now();
}

function normalizeStatus(value) {
  if (!value || typeof value !== 'string') return 'PENDING';
  const upper = value.toUpperCase();
  if (['PENDING', 'APPLIED', 'SHORTLISTED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'COMPLETED'].includes(upper)) {
    return upper;
  }
  return 'PENDING';
}

async function main() {
  const serviceAccount = loadServiceAccount();
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  const db = admin.firestore();

  const source = await db.collection('job_applications').get();
  console.log(`Mode: ${DRY_RUN ? 'DRY_RUN' : 'APPLY'}`);
  console.log(`Found ${source.size} docs in job_applications`);

  let migrated = 0;
  let skipped = 0;
  let invalid = 0;

  let batch = db.batch();
  let ops = 0;

  for (const doc of source.docs) {
    const data = doc.data() || {};

    const jobId = String(data.jobId || '').trim();
    const workerId = String(data.workerId || '').trim();
    const employerId = String(data.employerId || '').trim();

    if (!jobId || !workerId || !employerId) {
      invalid += 1;
      continue;
    }

    const id = `${jobId}_${workerId}`;
    const targetRef = db.collection('applications').doc(id);

    const targetPayload = {
      id,
      applicationId: id,
      jobId,
      workerId,
      employerId,
      status: normalizeStatus(data.status),
      createdAt: toMillis(data.createdAt || data.appliedAt || data.updatedAt),
      updatedAt: toMillis(data.updatedAt || data.createdAt || data.appliedAt),
      appliedAt: toMillis(data.appliedAt || data.createdAt || data.updatedAt),
      active: data.active !== false,
      source: typeof data.source === 'string' ? data.source : 'android_app',
      coverLetter: typeof data.coverLetter === 'string' ? data.coverLetter : '',
      workerName: typeof data.workerName === 'string' ? data.workerName : '',
      workerPhone: typeof data.workerPhone === 'string' ? data.workerPhone : '',
      jobTitle: typeof data.jobTitle === 'string' ? data.jobTitle : '',
      jobLocation: typeof data.jobLocation === 'string' ? data.jobLocation : '',
      companyName: typeof data.companyName === 'string' ? data.companyName : ''
    };

    if (!DRY_RUN) {
      batch.set(targetRef, targetPayload, { merge: true });
      ops += 1;
      if (ops >= 450) {
        await batch.commit();
        batch = db.batch();
        ops = 0;
      }
    }

    migrated += 1;
  }

  if (!DRY_RUN && ops > 0) {
    await batch.commit();
  }

  const targetCount = (await db.collection('applications').count().get()).data().count;
  const sourceCount = (await db.collection('job_applications').count().get()).data().count;

  console.log('Migration summary:');
  console.log(`- Migrated/upserted: ${migrated}`);
  console.log(`- Invalid source docs: ${invalid}`);
  console.log(`- Skipped: ${skipped}`);
  console.log(`- Source count (job_applications): ${sourceCount}`);
  console.log(`- Target count (applications): ${targetCount}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
