'use strict';

const fs = require('fs');
const path = require('path');

const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const TARGET_CITY_ALIASES = new Map([
  ['hyderabad', 'Hyderabad'],
  ['secunderabad', 'Hyderabad'],
  ['khammam', 'Khammam'],
  ['vijayawada', 'Vijayawada'],
  ['warangal', 'Warangal'],
  ['hanamkonda', 'Warangal'],
  ['karimnagar', 'Karimnagar'],
  ['kurnool', 'Kurnool'],
  ['karnool', 'Kurnool']
]);

const LOCAL_JOB_ALLOW = /\b(cook|chef|kitchen|restaurant|hotel|food|delivery|driver|rider|courier|security|guard|office\s*boy|peon|helper|cashier|billing|retail|counter|store|shop|shopkeeper|showroom|mall|sales|field\s*sales|field\s*executive|picker|packer|warehouse|housekeeping|cleaner|attendant|waiter|steward|technician|ac\s*technician|hvac|air\s*condition|electrician|mechanic|fitter|operator|data\s*entry|computer\s*operator|petrol\s*pump|pharmacy|telecaller|tele\s*caller|receptionist|customer\s*care|office\s*staff)\b/i;
const NON_LOCAL_REJECT = /\b(software|developer|programmer|data\s*engineer|advocate|lawyer|relationship\s*manager|business\s*development\s*manager|senior\s*manager|branch\s*manager|spa\s*therapist)\b/i;
const ALLOWED_GENDER = new Set(['Male', 'Female', 'Both']);
const MAX_MONTHLY_SALARY = 30000;

function parseArgs(argv) {
  const args = {
    inputs: [],
    reviewOut: path.join(__dirname, 'output', 'local-service-strict-review.json'),
    excludeExisting: false,
    currentDate: '2026-04-29',
    freshDays: 7
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) throw new Error(`Missing value after ${token}`);
      return argv[index];
    };

    switch (token) {
      case '--input':
        args.inputs.push(next());
        break;
      case '--inputs':
        args.inputs.push(...next().split(',').map((item) => item.trim()).filter(Boolean));
        break;
      case '--review-out':
        args.reviewOut = next();
        break;
      case '--exclude-existing':
        args.excludeExisting = true;
        break;
      case '--current-date':
        args.currentDate = next();
        break;
      case '--fresh-days':
        args.freshDays = Number.parseInt(next(), 10);
        break;
      default:
        throw new Error(`Unknown option: ${token}`);
    }
  }

  if (!args.inputs.length) throw new Error('At least one --input review file is required');
  if (!Number.isInteger(args.freshDays) || args.freshDays < 0) throw new Error('--fresh-days must be a positive integer');
  return args;
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(resolvePath(filePath), 'utf8').replace(/^\uFEFF/, ''));
}

function writeJson(filePath, value) {
  const outPath = resolvePath(filePath);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
  return outPath;
}

function normalize(value) {
  return String(value || '').trim();
}

function normalizeLower(value) {
  return normalize(value).toLowerCase();
}

function cityText(job) {
  return [
    job.details && job.details.companyCity,
    job.audit && job.audit.sourceCity,
    job.meta && job.meta.addressText,
    job.details && job.details.description,
    job.sourceUrl
  ].map(normalizeLower).filter(Boolean).join(' | ');
}

function targetCity(job) {
  const text = cityText(job);
  for (const [alias, canonical] of TARGET_CITY_ALIASES.entries()) {
    const pattern = new RegExp(`(^|[^a-z])${alias.replace(/\s+/g, '\\s+')}([^a-z]|$)`, 'i');
    if (pattern.test(text)) return canonical;
  }
  return '';
}

function salaryNumbers(value) {
  return (String(value || '').match(/\d[\d,]*/g) || [])
    .map((item) => Number(item.replace(/,/g, '')))
    .filter(Number.isFinite);
}

function salaryUnderLimit(job) {
  const numbers = salaryNumbers(job.meta && job.meta.salary);
  if (!numbers.length) return false;
  return Math.max(...numbers) <= MAX_MONTHLY_SALARY;
}

function withinFreshWindow(job, currentDate, freshDays) {
  const createdAt = Date.parse(job.meta && job.meta.createdAt || '');
  if (Number.isNaN(createdAt)) return false;
  const [year, month, day] = currentDate.split('-').map(Number);
  const start = Date.UTC(year, month - 1, day - freshDays, 0, 0, 0, 0);
  const end = Date.UTC(year, month - 1, day, 23, 59, 59, 999);
  return createdAt >= start && createdAt <= end;
}

function hasRequiredFields(job) {
  const meta = job.meta || {};
  const details = job.details || {};
  const vacancies = Number.parseInt(String(meta.vacancies || ''), 10);
  const hasLocation = Number.isFinite(Number(meta.location && meta.location.lat)) && Number.isFinite(Number(meta.location && meta.location.lng));
  const gender = normalize(details.gender);

  return Boolean(
    normalize(meta.title).length >= 3 &&
    normalize(meta.companyName).length >= 2 &&
    normalize(meta.salary) &&
    normalize(meta.salaryType) &&
    normalize(meta.addressText) &&
    normalize(meta.jobType) &&
    normalize(meta.geohash) &&
    hasLocation &&
    Number.isInteger(vacancies) && vacancies >= 1 && vacancies <= 50 &&
    normalize(details.contactNumber) &&
    normalize(details.description).length >= 20 &&
    ALLOWED_GENDER.has(gender) &&
    normalize(details.educationRequired) &&
    normalize(details.shiftTiming) &&
    normalize(details.experienceRequired) &&
    normalize(details.companyCity)
  );
}

function isLocalServiceJob(job) {
  const text = [
    job.meta && job.meta.title,
    job.meta && job.meta.companyName,
    job.meta && job.meta.jobType,
    job.meta && job.meta.addressText,
    job.details && job.details.description
  ].map(normalize).join('\n');

  if (!LOCAL_JOB_ALLOW.test(text)) return false;
  if (!NON_LOCAL_REJECT.test(text)) return true;
  return /\b(service\s*engineer|field\s*service|technician|mechanic|electrician|ac\s*technician|hvac)\b/i.test(text);
}

function skipReason(job, options) {
  if (!job || !job.valid) return 'invalidSourceRow';
  const city = targetCity(job);
  if (!city) return 'outsideRequestedCities';
  if (!withinFreshWindow(job, options.currentDate, options.freshDays)) return 'notFresh';
  if (!hasRequiredFields(job)) return 'missingRequiredField';
  if (!salaryUnderLimit(job)) return 'salaryAbove30000';
  if (!isLocalServiceJob(job)) return 'notRequestedLocalJobType';
  return '';
}

async function existingJobIds(jobIds) {
  if (!jobIds.length) return new Set();
  const admin = require('firebase-admin');
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }
  const db = admin.firestore();
  const existing = new Set();
  for (const id of jobIds) {
    const doc = await db.collection('jobmetadata').doc(id).get();
    if (doc.exists) existing.add(id);
  }
  return existing;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const skipped = [];
  const selectedById = new Map();
  const sourceTotals = [];

  for (const input of options.inputs) {
    const review = readJson(input);
    const jobs = Array.isArray(review.jobs) ? review.jobs : [];
    sourceTotals.push({ input, jobs: jobs.length, skipped: Array.isArray(review.skipped) ? review.skipped.length : 0 });

    for (const job of jobs) {
      const reason = skipReason(job, options);
      if (reason) {
        skipped.push({ jobId: job && job.jobId || '', sourceUrl: job && job.sourceUrl || '', title: job && job.meta && job.meta.title || '', reason });
        continue;
      }
      const city = targetCity(job);
      const prepared = JSON.parse(JSON.stringify(job));
      prepared.approved = true;
      prepared.details.companyCity = city;
      if (!selectedById.has(prepared.jobId)) selectedById.set(prepared.jobId, prepared);
    }
  }

  let jobs = Array.from(selectedById.values());
  const duplicateInputRows = sourceTotals.reduce((sum, item) => sum + item.jobs, 0) - jobs.length - skipped.length;

  if (options.excludeExisting && jobs.length) {
    const existing = await existingJobIds(jobs.map((job) => job.jobId));
    jobs = jobs.filter((job) => {
      if (!existing.has(job.jobId)) return true;
      skipped.push({ jobId: job.jobId, sourceUrl: job.sourceUrl || '', title: job.meta.title, reason: 'alreadyExistsInFirebase' });
      return false;
    });
  }

  const byCity = {};
  const byTitleKeyword = {};
  for (const job of jobs) {
    byCity[job.details.companyCity] = (byCity[job.details.companyCity] || 0) + 1;
    const key = normalizeLower(job.meta.title).split(/\s+/).slice(0, 3).join(' ');
    byTitleKeyword[key] = (byTitleKeyword[key] || 0) + 1;
  }

  const output = {
    generatedAt: new Date(`${options.currentDate}T12:00:00.000Z`).toISOString(),
    mode: 'review',
    sourcesPath: options.inputs.join(', '),
    requirePhone: true,
    freshnessRule: `Only jobs within ${options.freshDays} days of ${options.currentDate}; requested cities only; salary max <= ${MAX_MONTHLY_SALARY}; required fields must be source-backed and non-empty.`,
    employerId: jobs[0] && jobs[0].meta && jobs[0].meta.employerId || 'admin1',
    totals: {
      validJobs: jobs.length,
      skipped: skipped.length,
      sourceTotals,
      duplicateInputRows,
      byCity,
      byTitleKeyword
    },
    jobs,
    skipped
  };

  const outPath = writeJson(options.reviewOut, output);
  console.log(`Review file: ${outPath}`);
  console.log(`Strict local service jobs: ${jobs.length}`);
  console.log(`Skipped: ${skipped.length}`);
  console.log(`By city: ${JSON.stringify(byCity)}`);
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
