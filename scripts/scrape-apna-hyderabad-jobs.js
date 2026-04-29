'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const DEFAULT_CITY_URL = 'https://apna.co/jobs/jobs-in-hyderabad';
const DEFAULT_REVIEW_OUT = path.join(__dirname, 'output', 'hyderabad-fresh-phone-verified-apna-review.json');
const DEFAULT_EMPLOYER_ID = process.env.DUTYPE_IMPORT_EMPLOYER_ID || 'admin1';
const DEFAULT_CURRENT_DATE = '2026-04-29';
const DEFAULT_FRESH_DAYS = 7;
const DEFAULT_CONCURRENCY = 6;
const DEFAULT_TARGET = 300;
const MAX_DESCRIPTION_LENGTH = 5000;

const USER_AGENT = 'DutyPeApnaReviewScraper/1.0 (+https://dutype.com)';
const PHONE_LINE_HINT = /call|phone|whats|contact|mobile|hr|resume|cv|apply|send|share|reach|interested/i;
const PHONE_TOKEN = /(?<!\d)(?:\+?91[\s().-]*)?[6-9](?:[\s().-]*\d){9}(?!\d)/g;

const GEOHASH_BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

function printHelp() {
  console.log(`Apna Hyderabad fresh phone-visible scraper

Usage:
  node scrape-apna-hyderabad-jobs.js --target 300

Options:
  --review-out <path>       Output review JSON path.
  --city-url <url>          Apna city/listing URL. Default: ${DEFAULT_CITY_URL}
  --pages <count>           Maximum pages to scan. Default: source totalPages
  --target <count>          Stop output at this many unique valid jobs. Default: ${DEFAULT_TARGET}
  --current-date <date>     Freshness reference date. Default: ${DEFAULT_CURRENT_DATE}
  --fresh-days <count>      Include jobs created within this many days. Default: ${DEFAULT_FRESH_DAYS}
  --concurrency <count>     List pages fetched in parallel. Default: ${DEFAULT_CONCURRENCY}
  --employer-id <id>        Employer/admin uid for review rows. Default: ${DEFAULT_EMPLOYER_ID}
  --help                    Show this help.
`);
}

function parseArgs(argv) {
  const args = {
    reviewOut: DEFAULT_REVIEW_OUT,
    cityUrl: DEFAULT_CITY_URL,
    pages: 0,
    target: DEFAULT_TARGET,
    currentDate: DEFAULT_CURRENT_DATE,
    freshDays: DEFAULT_FRESH_DAYS,
    concurrency: DEFAULT_CONCURRENCY,
    employerId: DEFAULT_EMPLOYER_ID,
    help: false
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) throw new Error(`Missing value after ${token}`);
      return argv[index];
    };

    switch (token) {
      case '--review-out':
        args.reviewOut = next();
        break;
      case '--city-url':
        args.cityUrl = next();
        break;
      case '--pages':
        args.pages = parsePositiveInt(next(), '--pages');
        break;
      case '--target':
        args.target = parsePositiveInt(next(), '--target');
        break;
      case '--current-date':
        args.currentDate = next();
        break;
      case '--fresh-days':
        args.freshDays = parsePositiveInt(next(), '--fresh-days');
        break;
      case '--concurrency':
        args.concurrency = Math.max(1, parsePositiveInt(next(), '--concurrency'));
        break;
      case '--employer-id':
        args.employerId = next();
        break;
      case '--help':
      case '-h':
        args.help = true;
        break;
      default:
        throw new Error(`Unknown option: ${token}`);
    }
  }

  if (!args.employerId || args.employerId.trim().length < 2) {
    throw new Error('--employer-id must be a non-empty admin/employer uid');
  }

  return args;
}

function parsePositiveInt(value, optionName) {
  const parsed = Number.parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed < 0) {
    throw new Error(`${optionName} must be a positive integer`);
  }
  return parsed;
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function addQueryParam(url, key, value) {
  const parsed = new URL(url);
  parsed.searchParams.set(key, String(value));
  return parsed.toString();
}

async function fetchText(url, attempt = 1) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30000);

  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': USER_AGENT,
        Accept: 'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8'
      }
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.text();
  } catch (error) {
    if (attempt >= 3) throw new Error(`${error.message} for ${url}`);
    await sleep(500 * attempt);
    return fetchText(url, attempt + 1);
  } finally {
    clearTimeout(timeout);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function extractNextData(html, url) {
  const match = String(html || '').match(/<script id="__NEXT_DATA__" type="application\/json">([\s\S]*?)<\/script>/);
  if (!match) throw new Error(`Missing __NEXT_DATA__ in ${url}`);

  try {
    return JSON.parse(match[1]);
  } catch (_) {
    return JSON.parse(decodeHtmlEntities(match[1]));
  }
}

function decodeHtmlEntities(value) {
  return String(value || '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/gi, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCharCode(Number.parseInt(code, 16)));
}

function truncate(value, max) {
  const text = String(value || '').replace(/[ \t\r]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
  if (text.length <= max) return text;
  return text.slice(0, Math.max(0, max - 1)).trimEnd();
}

function sha1(input) {
  return crypto.createHash('sha1').update(input).digest('hex');
}

function normalizePhone(value) {
  const digits = String(value || '').replace(/\D/g, '');
  if (digits.length === 10 && /^[6-9]/.test(digits)) return digits;
  if (digits.length === 12 && digits.startsWith('91') && /^[6-9]/.test(digits.slice(2))) return digits.slice(2);
  return '';
}

function phonesFromLine(line) {
  const matches = String(line || '').match(PHONE_TOKEN) || [];
  return matches.map(normalizePhone).filter(Boolean);
}

function extractPublicPhones(description) {
  const lines = String(description || '')
    .split(/\r?\n+/)
    .map((line) => line.replace(/\s+/g, ' ').trim())
    .filter(Boolean);
  const phones = [];
  let evidence = '';

  for (const line of lines) {
    if (/^https?:\/\//i.test(line) && !PHONE_LINE_HINT.test(line)) continue;
    const linePhones = phonesFromLine(line);
    if (linePhones.length === 0) continue;

    const looksLikeContactLine = PHONE_LINE_HINT.test(line) || line.replace(/\D/g, '').length <= 12;
    if (!looksLikeContactLine) continue;

    if (!evidence) evidence = line;
    for (const phone of linePhones) {
      if (!phones.includes(phone)) phones.push(phone);
    }
  }

  return { phones, evidence };
}

function createdWithinFreshWindow(value, currentDate, freshDays) {
  const created = new Date(value || 0);
  if (Number.isNaN(created.getTime())) return false;

  const [year, month, day] = currentDate.split('-').map(Number);
  const end = new Date(Date.UTC(year, month - 1, day, 23, 59, 59, 999));
  const start = new Date(Date.UTC(year, month - 1, day - freshDays, 0, 0, 0, 0));
  return created >= start && created <= end;
}

function parseDateIso(value, fallback) {
  const parsed = Date.parse(value || '');
  if (!Number.isNaN(parsed)) return new Date(parsed).toISOString();
  return fallback.toISOString();
}

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

function isValidLatLng(lat, lng) {
  return Number.isFinite(lat) && Number.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 && !(lat === 0 && lng === 0);
}

function firstFiniteNumber(...values) {
  for (const value of values) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
}

function resolveLocation(job) {
  const companyAddress = job.company_address || {};
  const area = companyAddress.area || {};
  const geometry = companyAddress.location_geometry || {};
  const address = job.address || {};
  const lat = firstFiniteNumber(
    geometry.google_latitude,
    area.latitude,
    address.latitude,
    address.mid_area && address.mid_area.latitude,
    17.385044
  );
  const lng = firstFiniteNumber(
    geometry.google_longitude,
    area.longitude,
    address.longitude,
    address.mid_area && address.mid_area.longitude,
    78.486671
  );

  return isValidLatLng(lat, lng)
    ? { lat, lng }
    : { lat: 17.385044, lng: 78.486671 };
}

function encodeGeohash(latitude, longitude, precision = 6) {
  let latMin = -90.0;
  let latMax = 90.0;
  let lonMin = -180.0;
  let lonMax = 180.0;
  let geohash = '';
  let isEven = true;
  let bit = 0;
  let ch = 0;

  while (geohash.length < precision) {
    if (isEven) {
      const mid = (lonMin + lonMax) / 2;
      if (longitude > mid) {
        ch |= 1 << (4 - bit);
        lonMin = mid;
      } else {
        lonMax = mid;
      }
    } else {
      const mid = (latMin + latMax) / 2;
      if (latitude > mid) {
        ch |= 1 << (4 - bit);
        latMin = mid;
      } else {
        latMax = mid;
      }
    }

    isEven = !isEven;
    if (bit < 4) {
      bit += 1;
    } else {
      geohash += GEOHASH_BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }

  return geohash;
}

function normalizeSalary(job) {
  const min = Number.parseInt(String(job.fixed_min_salary || job.min_salary || ''), 10);
  const max = Number.parseInt(String(job.fixed_max_salary || job.max_salary || ''), 10);
  if (Number.isFinite(min) && Number.isFinite(max) && min > 0 && max > 0) return `${min}-${max}`;
  if (Number.isFinite(min) && min > 0) return `${min}+`;
  if (Number.isFinite(max) && max > 0) return `Up to ${max}`;
  return 'Negotiable';
}

function normalizeVacancies(value) {
  const parsed = Number.parseInt(String(value || ''), 10);
  if (Number.isInteger(parsed) && parsed >= 1 && parsed <= 1000) return parsed;
  return 1;
}

function normalizeJobType(job) {
  if (job.is_part_time) return 'PART_TIME';
  if (/intern/i.test(job.title || '') || /intern/i.test(job.type || '')) return 'INTERNSHIP';
  return 'FULL_TIME';
}

function resolveAddressText(job) {
  return truncate(
    job.location_name ||
      job.company_address?.line_1 ||
      job.address?.line_1 ||
      [job.address?.area, job.address?.city?.name].filter(Boolean).join(', ') ||
      'Hyderabad, Telangana',
    300
  );
}

function normalizeGender(value) {
  if (!value) return 'Any';
  const raw = String(value).toLowerCase();
  if (raw.includes('female')) return 'Female';
  if (raw.includes('male')) return 'Male';
  return 'Any';
}

function buildReviewJob(job, options, scrapedAt) {
  const contact = extractPublicPhones(job.description);
  if (contact.phones.length === 0) return null;
  if (!createdWithinFreshWindow(job.created_on, options.currentDate, options.freshDays)) return null;

  const createdAt = parseDateIso(job.created_on, new Date(`${options.currentDate}T00:00:00.000Z`));
  const expiresAt = parseDateIso(job.expiry, addDays(new Date(createdAt), 30));
  const location = resolveLocation(job);
  const addressText = resolveAddressText(job);
  const sourceUrl = job.public_url || job.public_url_v2 || `${DEFAULT_CITY_URL}/${job.id}`;
  const jobId = `apna_${job.id}`;
  const idempotencyHash = sha1([sourceUrl, job.title, job.organization?.name, job.created_on].join('|').toLowerCase());
  const companyName = truncate(job.organization?.name || 'Apna employer', 120);
  const title = truncate(job.title || job.type || 'Job Opening', 120);
  const description = truncate(
    [
      job.description,
      sourceUrl ? `Source: ${sourceUrl}` : ''
    ].filter(Boolean).join('\n\n'),
    MAX_DESCRIPTION_LENGTH
  );

  return {
    approved: false,
    jobId,
    sourceName: `Apna - ${companyName}`,
    sourceUrl,
    warnings: ['phoneFoundInPublicDescriptionText'],
    valid: true,
    meta: {
      employerId: options.employerId,
      companyName,
      title,
      salary: normalizeSalary(job),
      salaryType: 'MONTHLY',
      location,
      geohash: encodeGeohash(location.lat, location.lng, 6),
      addressText,
      jobType: normalizeJobType(job),
      status: 'open',
      createdAt,
      vacancies: normalizeVacancies(job.no_of_openings)
    },
    details: {
      employerId: options.employerId,
      createdAt,
      expiresAt,
      idempotencyKey: `apna:${job.id}:${idempotencyHash.slice(0, 48)}`,
      description,
      contactNumber: contact.phones[0],
      gender: normalizeGender(job.gender),
      experienceRequired: truncate(job.experience_in_years || 'Not specified', 120),
      educationRequired: truncate(job.education || 'Not specified', 120),
      companyCity: 'Hyderabad',
      shiftTiming: truncate(job.shift || 'Flexible', 120),
      applicationCount: 0
    },
    audit: {
      jobId,
      sourceName: `Apna - ${companyName}`,
      sourceUrl,
      sourceType: 'apna-public-next-data-listing',
      importedBy: 'scripts/scrape-apna-hyderabad-jobs.js',
      scrapedAt,
      sourceDatePosted: job.created_on || '',
      sourceFreshness: `within_${options.freshDays}_days_of_${options.currentDate}`,
      sourceValidThrough: job.expiry || '',
      contactNumberSource: 'public_description_text',
      contactEvidence: truncate(contact.evidence, 500),
      publicContactNumbers: contact.phones,
      reviewRequired: true
    }
  };
}

async function getPageJobs(cityUrl, pageNumber) {
  const pageUrl = pageNumber === 1 ? cityUrl : addQueryParam(cityUrl, 'page', pageNumber);
  const html = await fetchText(pageUrl);
  const data = extractNextData(html, pageUrl);
  const pageProps = data.props?.pageProps || {};
  return {
    pageUrl,
    totalPages: Number.parseInt(String(pageProps.totalPages || 0), 10) || 0,
    jobs: (pageProps.jobs || []).map((item) => item.data || item).filter(Boolean)
  };
}

async function crawl(options) {
  const firstPage = await getPageJobs(options.cityUrl, 1);
  const totalPages = options.pages > 0 ? Math.min(options.pages, firstPage.totalPages || options.pages) : firstPage.totalPages;
  const scrapedAt = new Date(`${options.currentDate}T12:00:00.000Z`).toISOString();
  const jobsById = new Map();
  const skipped = [];
  const pageStats = [];
  let nextPage = 1;

  async function processPage(pageNumber) {
    const result = pageNumber === 1 ? firstPage : await getPageJobs(options.cityUrl, pageNumber);
    let freshCount = 0;
    let phoneVisibleCount = 0;
    let validCount = 0;

    for (const sourceJob of result.jobs) {
      const isFresh = createdWithinFreshWindow(sourceJob.created_on, options.currentDate, options.freshDays);
      const contact = extractPublicPhones(sourceJob.description);
      if (isFresh) freshCount += 1;
      if (contact.phones.length > 0) phoneVisibleCount += 1;

      const reviewJob = buildReviewJob(sourceJob, options, scrapedAt);
      if (!reviewJob) continue;
      validCount += 1;
      if (!jobsById.has(reviewJob.jobId)) jobsById.set(reviewJob.jobId, reviewJob);
    }

    pageStats.push({
      pageNumber,
      jobs: result.jobs.length,
      fresh: freshCount,
      phoneVisible: phoneVisibleCount,
      freshPhoneVisible: validCount,
      uniqueValidTotal: jobsById.size
    });
  }

  async function worker() {
    while (nextPage <= totalPages) {
      if (options.target > 0 && jobsById.size >= options.target) return;
      const pageNumber = nextPage;
      nextPage += 1;
      try {
        await processPage(pageNumber);
      } catch (error) {
        skipped.push({ sourceName: 'Apna Hyderabad', sourceUrl: addQueryParam(options.cityUrl, 'page', pageNumber), reason: error.message });
      }
    }
  }

  await Promise.all(Array.from({ length: Math.min(options.concurrency, totalPages) }, () => worker()));

  const jobs = Array.from(jobsById.values()).slice(0, options.target > 0 ? options.target : undefined);
  pageStats.sort((a, b) => a.pageNumber - b.pageNumber);

  return {
    generatedAt: scrapedAt,
    mode: 'review',
    sourcesPath: options.cityUrl,
    requirePhone: true,
    freshnessRule: `Only Apna Hyderabad jobs with public created_on/datePosted from ${options.freshDays} days before ${options.currentDate} and visible contact number in description are included.`,
    employerId: options.employerId,
    totals: {
      validJobs: jobs.length,
      skipped: skipped.length,
      pagesScanned: pageStats.length,
      sourceTotalPages: totalPages,
      uniqueFreshPhoneVisibleFound: jobsById.size
    },
    jobs,
    skipped,
    pageStats
  };
}

function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    printHelp();
    return;
  }

  const review = await crawl(options);
  const outPath = resolvePath(options.reviewOut);
  writeJson(outPath, review);

  console.log(`Review file: ${outPath}`);
  console.log(`Valid fresh phone-visible jobs: ${review.totals.validJobs}`);
  console.log(`Unique found before output limit: ${review.totals.uniqueFreshPhoneVisibleFound}`);
  console.log(`Pages scanned: ${review.totals.pagesScanned}/${review.totals.sourceTotalPages}`);
  console.log(`Skipped pages: ${review.totals.skipped}`);
  console.log('Dry-run only. No Firestore writes were made.');
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});