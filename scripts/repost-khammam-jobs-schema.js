const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const SOURCE_FILE = path.join(__dirname, 'jobs_applications_9390515834_1771398064310.json');

function resolveServiceAccount() {
  const candidates = [
    path.join(__dirname, 'serviceAccountKey.json'),
    path.join(__dirname, 'dutype-860ac-firebase-adminsdk.json'),
    path.join(__dirname, '..', 'serviceAccountKey.json'),
    path.join(__dirname, '..', 'dutype-860ac-firebase-adminsdk.json')
  ];

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) return candidate;
  }

  return null;
}

function parseSalary(payAmount) {
  const raw = String(payAmount || '').trim();
  const numeric = raw.replace(/[^0-9\-]/g, '');

  if (!numeric) return 12000;

  if (numeric.includes('-')) {
    const [min, max] = numeric.split('-').map((v) => Number(v || 0));
    if (min > 0 && max > 0) return Math.round((min + max) / 2);
    return min || max || 12000;
  }

  const value = Number(numeric);
  return Number.isFinite(value) && value > 0 ? value : 12000;
}

function normalizeSalaryType(payType) {
  const value = String(payType || '').trim().toUpperCase();
  if (!value) return 'MONTHLY';
  if (value === 'MONTHLY' || value === 'DAILY' || value === 'HOURLY' || value === 'WEEKLY') {
    return value;
  }
  return 'MONTHLY';
}

function normalizeJobType(jobType) {
  const value = String(jobType || '').trim().toUpperCase().replace(/\s+/g, '_').replace('-', '_');
  if (!value) return 'FULL_TIME';
  return value;
}

async function run() {
  if (!fs.existsSync(SOURCE_FILE)) {
    throw new Error('Source JSON not found: ' + SOURCE_FILE);
  }

  const keyPath = resolveServiceAccount();
  if (!keyPath) {
    throw new Error('Service account key not found.');
  }

  admin.initializeApp({
    credential: admin.credential.cert(require(keyPath)),
    projectId: 'dutype-860ac'
  });

  const db = admin.firestore();
  const now = Date.now();
  const expiresAt = now + 45 * 24 * 60 * 60 * 1000;

  const payload = JSON.parse(fs.readFileSync(SOURCE_FILE, 'utf8'));
  const jobs = Array.isArray(payload.jobs) ? payload.jobs : [];

  if (jobs.length === 0) {
    console.log('No jobs found in source file.');
    return;
  }

  console.log('Reposting/upserting jobs with strict-compatible schema...');

  let upserted = 0;

  for (const entry of jobs) {
    const job = entry.jobData || {};
    const jobId = String(job.jobId || '').trim();
    if (!jobId) continue;

    const lat = Number(job.latitude || 0);
    const lng = Number(job.longitude || 0);

    const update = {
      jobId,
      employerId: String(job.employerId || 'admin-reliance-retail'),
      title: String(job.title || 'Job Opening'),
      companyName: String(job.companyName || 'DutyPe Employer'),
      jobType: normalizeJobType(job.jobType),
      category: String(job.category || 'OTHER').toUpperCase(),
      salary: parseSalary(job.payAmount),
      salaryType: normalizeSalaryType(job.payType),
      payAmount: String(job.payAmount || ''),
      payType: normalizeSalaryType(job.payType),
      location: {
        lat,
        lng
      },
      latitude: lat,
      longitude: lng,
      addressText: String(job.location || 'Khammam, Telangana'),
      locationText: String(job.location || 'Khammam, Telangana'),
      contactNumber: String(job.contactNumber || ''),
      vacancies: Number(job.vacancies || 1),
      urgency: String(job.urgency || 'NORMAL').toUpperCase(),
      status: 'open',
      isActive: true,
      isFilled: false,
      createdAt: now,
      postedAt: now,
      updatedAt: now,
      expiresAt,
      expiryDays: 45
    };

    await db.collection('jobs').doc(jobId).set(update, { merge: true });
    upserted += 1;
    console.log('Upserted:', jobId, '|', update.title);
  }

  console.log('Done. Total upserted:', upserted);
}

run().catch((error) => {
  console.error('Failed:', error.message || error);
  process.exit(1);
});
