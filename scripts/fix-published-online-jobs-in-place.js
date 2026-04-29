'use strict';

const fs = require('fs');
const path = require('path');

const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const OLD_REVIEW_PATH = path.join(__dirname, 'output', 'india-fresh-phone-verified-merged-strict-validated.json');
const CLEAN_REVIEW_PATH = path.join(__dirname, 'output', 'telangana-ap-strict-cleaned-salary-gender-vacancy-validated.json');
const CLOSED_IMPORT_CREATED_AT = new Date('2000-01-01T00:00:00.000Z');

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function parseArgs(argv) {
  return {
    apply: argv.includes('--apply')
  };
}

function timestamp(admin, value) {
  if (!value) return value;
  const parsed = Date.parse(value);
  if (Number.isNaN(parsed)) return value;
  return admin.firestore.Timestamp.fromDate(new Date(parsed));
}

function buildDocuments(admin, job) {
  const cardData = {
    title: job.meta.title,
    companyName: job.meta.companyName,
    salary: job.meta.salary,
    salaryType: job.meta.salaryType,
    location: job.meta.location,
    geohash: job.meta.geohash,
    addressText: job.meta.addressText,
    jobType: job.meta.jobType,
    status: 'open',
    createdAt: timestamp(admin, job.meta.createdAt),
    employerId: job.meta.employerId,
    vacancies: job.meta.vacancies
  };

  const detailsData = {
    employerId: job.details.employerId,
    expiresAt: timestamp(admin, job.details.expiresAt),
    contactNumber: job.details.contactNumber,
    description: job.details.description,
    gender: job.details.gender,
    experienceRequired: job.details.experienceRequired,
    educationRequired: job.details.educationRequired,
    shiftTiming: job.details.shiftTiming,
    companyCity: job.details.companyCity,
    applicationCount: job.details.applicationCount || 0
  };

  return { cardData, detailsData };
}

async function commitWhenReady(db, state, force = false) {
  if (state.ops === 0) return;
  if (!force && state.ops < 450) return;
  await state.batch.commit();
  state.batch = db.batch();
  state.ops = 0;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const oldJobs = readJson(OLD_REVIEW_PATH).jobs || [];
  const cleanJobs = readJson(CLEAN_REVIEW_PATH).jobs || [];
  const cleanById = new Map(cleanJobs.map((job) => [job.jobId, job]));
  const closeJobs = oldJobs.filter((job) => !cleanById.has(job.jobId));

  console.log(`Imported jobs to close: ${closeJobs.length}`);
  console.log(`Clean jobs to keep open/update: ${cleanJobs.length}`);

  if (!options.apply) {
    console.log('Dry-run only. Add --apply to update Firestore.');
    return;
  }

  const admin = require('firebase-admin');
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }
  const db = admin.firestore();
  const state = { batch: db.batch(), ops: 0 };

  let closed = 0;
  let reopened = 0;
  const staleCreatedAt = admin.firestore.Timestamp.fromDate(CLOSED_IMPORT_CREATED_AT);

  for (const job of closeJobs) {
    state.batch.update(db.collection('jobmetadata').doc(job.jobId), {
      status: 'closed',
      createdAt: staleCreatedAt
    });
    state.ops += 1;
    closed += 1;
    await commitWhenReady(db, state);
  }

  for (const job of cleanJobs) {
    const { cardData, detailsData } = buildDocuments(admin, job);
    state.batch.set(db.collection('jobmetadata').doc(job.jobId), cardData);
    state.batch.set(db.collection('job_details').doc(job.jobId), detailsData);
    state.ops += 2;
    reopened += 1;
    await commitWhenReady(db, state);
  }

  await commitWhenReady(db, state, true);
  console.log(`Closed imported jobs: ${closed}`);
  console.log(`Updated clean open jobs: ${reopened}`);
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
