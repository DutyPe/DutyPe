'use strict';

const fs = require('fs');
const path = require('path');

const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

function parseArgs(argv) {
  const args = {
    review: path.join(__dirname, 'output', 'local-service-hyd-khm-vja-wgl-krm-knl-strict-including-existing-review.json')
  };
  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) throw new Error(`Missing value after ${token}`);
      return argv[index];
    };
    if (token === '--review') args.review = next();
    else throw new Error(`Unknown option: ${token}`);
  }
  return args;
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(resolvePath(filePath), 'utf8').replace(/^\uFEFF/, ''));
}

function salaryNumbers(value) {
  return (String(value || '').match(/\d[\d,]*/g) || [])
    .map((item) => Number(item.replace(/,/g, '')))
    .filter(Number.isFinite);
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const review = readJson(options.review);
  const admin = require('firebase-admin');
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }
  const db = admin.firestore();

  let verifiedOk = 0;
  const rows = [];
  for (const job of review.jobs || []) {
    const [metaDoc, detailsDoc] = await Promise.all([
      db.collection('jobmetadata').doc(job.jobId).get(),
      db.collection('job_details').doc(job.jobId).get()
    ]);
    const meta = metaDoc.data() || {};
    const details = detailsDoc.data() || {};
    const numbers = salaryNumbers(meta.salary);
    const checks = {
      exists: metaDoc.exists && detailsDoc.exists,
      status: meta.status === 'open',
      salaryOk: numbers.length > 0 && Math.max(...numbers) <= 30000,
      phoneOnlyDetails: !Object.prototype.hasOwnProperty.call(meta, 'contactNumber') && Boolean(details.contactNumber),
      fieldsOk: Boolean(
        meta.title &&
        meta.companyName &&
        meta.salary &&
        meta.vacancies &&
        details.description &&
        details.gender &&
        details.educationRequired &&
        details.shiftTiming &&
        details.experienceRequired &&
        details.companyCity
      )
    };
    if (Object.values(checks).every(Boolean)) verifiedOk += 1;
    rows.push({
      id: job.jobId,
      title: meta.title || '',
      city: details.companyCity || '',
      salary: meta.salary || '',
      vacancies: meta.vacancies || '',
      gender: details.gender || '',
      education: details.educationRequired || '',
      shift: details.shiftTiming || '',
      experience: details.experienceRequired || '',
      checks
    });
  }

  const firstPage = await db.collection('jobmetadata').orderBy('createdAt', 'desc').limit(30).get();
  const firstPageOpen = firstPage.docs
    .filter((doc) => doc.get('status') === 'open')
    .map((doc) => ({
      id: doc.id,
      title: doc.get('title') || '',
      salary: doc.get('salary') || '',
      address: doc.get('addressText') || ''
    }));

  console.log(JSON.stringify({
    verifiedOk,
    total: (review.jobs || []).length,
    rows,
    firstPageOpenCount: firstPageOpen.length,
    firstPageOpen
  }, null, 2));

  if (verifiedOk !== (review.jobs || []).length || firstPageOpen.length === 0) {
    process.exit(1);
  }
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
