'use strict';

/**
 * Review-first online job importer for DutyPe.
 *
 * Default mode scrapes configured public/authorized sources and writes a local
 * review JSON file only. Use --apply to post valid reviewed jobs to Firestore.
 */

const crypto = require('crypto');
const fs = require('fs');
const http = require('http');
const https = require('https');
const path = require('path');
const { URL } = require('url');

const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const USER_AGENT = 'DutyPeJobImporter/1.0 (+https://dutype.com)';
const DEFAULT_EMPLOYER_ID = process.env.DUTYPE_IMPORT_EMPLOYER_ID || 'admin1';
const DEFAULT_SOURCES_PATH = path.join(__dirname, 'data', 'online-job-sources.json');
const DEFAULT_REVIEW_OUT = path.join(__dirname, 'output', `online-job-review-${compactDate(new Date())}.json`);
const MAX_RESPONSE_BYTES = 5 * 1024 * 1024;

const VALID_JOB_TYPES = new Set([
  'FULL_TIME',
  'PART_TIME',
  'CONTRACT',
  'FREELANCE',
  'INTERNSHIP',
  'TEMPORARY',
  'OTHER'
]);

const VALID_SALARY_TYPES = new Set(['HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'TASK']);

function compactDate(date) {
  return date.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
}

function printHelp() {
  console.log(`DutyPe online job importer

Usage:
  node import-online-jobs.js --sources ./data/online-job-sources.json
  node import-online-jobs.js --from-review ./output/online-job-review-YYYYMMDDTHHMMSSZ.json --apply --approved-only

Options:
  --sources <path>          Source config JSON. Default: data/online-job-sources.json
  --from-review <path>      Import jobs from a generated review file instead of scraping.
  --review-out <path>       Review output path. Default: output/online-job-review-<timestamp>.json
  --apply                   Write valid jobs to Firestore. Omitted means dry-run/review only.
  --approved-only           With --from-review, import only jobs where approved is true.
  --allow-missing-phone     Keep jobs without a public employer phone number.
  --employer-id <id>        Employer/admin uid to attach to imported jobs. Default: ${DEFAULT_EMPLOYER_ID}
  --expires-days <days>     Expiry window when source has no validThrough. Default: 30
  --limit <count>           Maximum valid jobs to process across all sources.
  --max-per-source <count>  Maximum valid jobs per source.
  --no-robots               Skip robots.txt checks. Use only for sources you are authorized to crawl.
  --help                    Show this help.
`);
}

function parseArgs(argv) {
  const args = {
    sources: DEFAULT_SOURCES_PATH,
    fromReview: '',
    reviewOut: DEFAULT_REVIEW_OUT,
    apply: false,
    approvedOnly: false,
    requirePhone: true,
    employerId: DEFAULT_EMPLOYER_ID,
    expiresDays: 30,
    limit: 0,
    maxPerSource: 0,
    respectRobots: true,
    help: false
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) {
        throw new Error(`Missing value after ${token}`);
      }
      return argv[index];
    };

    switch (token) {
      case '--sources':
      case '--source':
        args.sources = next();
        break;
      case '--from-review':
        args.fromReview = next();
        break;
      case '--review-out':
        args.reviewOut = next();
        break;
      case '--apply':
        args.apply = true;
        break;
      case '--approved-only':
        args.approvedOnly = true;
        break;
      case '--allow-missing-phone':
        args.requirePhone = false;
        break;
      case '--employer-id':
        args.employerId = next();
        break;
      case '--expires-days':
        args.expiresDays = parsePositiveInt(next(), '--expires-days');
        break;
      case '--limit':
        args.limit = parsePositiveInt(next(), '--limit');
        break;
      case '--max-per-source':
        args.maxPerSource = parsePositiveInt(next(), '--max-per-source');
        break;
      case '--no-robots':
        args.respectRobots = false;
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

function readJsonFile(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`File not found: ${filePath}`);
  }
  return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
}

function writeJsonFile(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

function sha1(input) {
  return crypto.createHash('sha1').update(input).digest('hex');
}

function asArray(value) {
  if (Array.isArray(value)) return value;
  if (value == null) return [];
  return [value];
}

function firstString(...values) {
  for (const value of values.flatMap(asArray)) {
    if (typeof value === 'string' && value.trim()) return value.trim();
    if (typeof value === 'number' && Number.isFinite(value)) return String(value);
  }
  return '';
}

function truncate(value, max) {
  const text = String(value || '').replace(/\s+/g, ' ').trim();
  if (text.length <= max) return text;
  return text.slice(0, Math.max(0, max - 1)).trimEnd();
}

function decodeHtmlEntities(value) {
  return String(value || '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCharCode(Number.parseInt(code, 16)));
}

function stripHtml(value) {
  return decodeHtmlEntities(
    String(value || '')
      .replace(/<script\b[\s\S]*?<\/script>/gi, ' ')
      .replace(/<style\b[\s\S]*?<\/style>/gi, ' ')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n')
      .replace(/<[^>]+>/g, ' ')
  )
    .replace(/[ \t\r\n]+/g, ' ')
    .trim();
}

function htmlToLines(value) {
  return decodeHtmlEntities(
    String(value || '')
      .replace(/<script\b[\s\S]*?<\/script>/gi, ' ')
      .replace(/<style\b[\s\S]*?<\/style>/gi, ' ')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/(p|div|section|article|li|h[1-6])>/gi, '\n')
      .replace(/<[^>]+>/g, ' ')
  )
    .split(/\r?\n/)
    .map((line) => line.replace(/[ \t]+/g, ' ').trim())
    .filter(Boolean);
}

function fetchText(targetUrl, redirectsLeft = 5) {
  return new Promise((resolve, reject) => {
    let parsed;
    try {
      parsed = new URL(targetUrl);
    } catch (error) {
      reject(new Error(`Invalid URL: ${targetUrl}`));
      return;
    }

    const client = parsed.protocol === 'http:' ? http : https;
    const request = client.request(
      parsed,
      {
        method: 'GET',
        headers: {
          'User-Agent': USER_AGENT,
          Accept: 'text/html,application/xhtml+xml,application/ld+json,application/json;q=0.9,*/*;q=0.8'
        }
      },
      (response) => {
        const statusCode = response.statusCode || 0;
        const location = response.headers.location;
        if ([301, 302, 303, 307, 308].includes(statusCode) && location && redirectsLeft > 0) {
          response.resume();
          const redirectedUrl = new URL(location, parsed).toString();
          fetchText(redirectedUrl, redirectsLeft - 1).then(resolve, reject);
          return;
        }

        if (statusCode < 200 || statusCode >= 300) {
          response.resume();
          reject(new Error(`HTTP ${statusCode} for ${targetUrl}`));
          return;
        }

        const chunks = [];
        let totalBytes = 0;
        response.on('data', (chunk) => {
          totalBytes += chunk.length;
          if (totalBytes > MAX_RESPONSE_BYTES) {
            request.destroy(new Error(`Response too large for ${targetUrl}`));
            return;
          }
          chunks.push(chunk);
        });
        response.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
      }
    );

    request.setTimeout(20000, () => request.destroy(new Error(`Timeout fetching ${targetUrl}`)));
    request.on('error', reject);
    request.end();
  });
}

async function robotsAllows(targetUrl) {
  const parsed = new URL(targetUrl);
  const robotsUrl = `${parsed.origin}/robots.txt`;
  let robotsText = '';
  try {
    robotsText = await fetchText(robotsUrl);
  } catch (_) {
    return true;
  }

  const pathWithQuery = `${parsed.pathname}${parsed.search}`;
  const rules = parseRobotsRules(robotsText);
  const matching = rules.filter((rule) => rule.userAgents.includes('*') || rule.userAgents.some((ua) => USER_AGENT.toLowerCase().startsWith(ua)));
  if (matching.length === 0) return true;

  let strongest = null;
  for (const group of matching) {
    for (const rule of group.rules) {
      if (!rule.path) continue;
      if (pathWithQuery.startsWith(rule.path) && (!strongest || rule.path.length > strongest.path.length)) {
        strongest = rule;
      }
    }
  }
  return !strongest || strongest.type !== 'disallow';
}

function parseRobotsRules(text) {
  const groups = [];
  let current = null;

  for (const rawLine of String(text || '').split(/\r?\n/)) {
    const line = rawLine.replace(/#.*/, '').trim();
    if (!line) continue;
    const separator = line.indexOf(':');
    if (separator === -1) continue;

    const key = line.slice(0, separator).trim().toLowerCase();
    const value = line.slice(separator + 1).trim();
    if (key === 'user-agent') {
      if (!current || current.rules.length > 0) {
        current = { userAgents: [], rules: [] };
        groups.push(current);
      }
      current.userAgents.push(value.toLowerCase());
    } else if ((key === 'allow' || key === 'disallow') && current) {
      current.rules.push({ type: key, path: value });
    }
  }

  return groups;
}

function extractJsonLdBlocks(html) {
  const blocks = [];
  const regex = /<script\b[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = regex.exec(html)) !== null) {
    const body = decodeHtmlEntities(match[1]).replace(/^\s*<!--/, '').replace(/-->\s*$/, '').trim();
    if (!body) continue;
    blocks.push(body);
  }
  return blocks;
}

function parseJsonLd(block, sourceName) {
  try {
    return JSON.parse(block);
  } catch (error) {
    throw new Error(`Invalid JSON-LD in ${sourceName}: ${error.message}`);
  }
}

function collectJobPostingNodes(value, output = []) {
  if (Array.isArray(value)) {
    value.forEach((item) => collectJobPostingNodes(item, output));
    return output;
  }
  if (!value || typeof value !== 'object') return output;

  if (isJobPostingNode(value)) output.push(value);
  if (value['@graph']) collectJobPostingNodes(value['@graph'], output);
  return output;
}

function isJobPostingNode(value) {
  const types = asArray(value['@type']).map((type) => String(type).toLowerCase());
  return types.includes('jobposting');
}

function normalizePhone(rawValue) {
  const raw = String(rawValue || '');
  const matches = raw.match(/(?:\+?91[\s().-]*)?[6-9]\d(?:[\s().-]*\d){8}|\+?\d(?:[\s().-]*\d){8,14}/g) || [];

  for (const match of matches) {
    const digits = match.replace(/\D/g, '');
    if (digits.length === 10 && /^[6-9]/.test(digits)) return digits;
    if (digits.length === 12 && digits.startsWith('91') && /^[6-9]/.test(digits.slice(2))) return digits.slice(2);
    if (digits.length >= 11 && digits.length <= 15) return `+${digits}`;
  }

  return '';
}

function extractPhoneFromCandidates(candidates) {
  for (const candidate of candidates) {
    const phone = normalizePhone(candidate);
    if (phone) return phone;
  }
  return '';
}

function extractPhoneFromCandidateSources(candidates) {
  for (const candidate of candidates) {
    const value = candidate && typeof candidate === 'object' && Object.prototype.hasOwnProperty.call(candidate, 'value')
      ? candidate.value
      : candidate;
    const phone = normalizePhone(value);
    if (phone) {
      return {
        phone,
        source: candidate && typeof candidate === 'object' ? candidate.source : 'public_listing_or_source_config'
      };
    }
  }
  return { phone: '', source: '' };
}

function normalizeJobType(value, fallback = '') {
  const raw = String(value || fallback || '').trim().toLowerCase();
  if (!raw) return '';
  const hasFullTime = /full[\s-]*time/.test(raw) || raw === 'full_time';
  const hasPartTime = /part[\s-]*time/.test(raw) || raw === 'part_time';
  if (hasFullTime && hasPartTime) return 'Full-time / Part-time';
  if (hasPartTime) return 'Part-time';
  if (hasFullTime) return 'Full-time';
  if (/contract/.test(raw)) return 'Contract';
  if (/temporary|temp\b/.test(raw)) return 'Temporary';
  if (/weekend/.test(raw)) return 'Weekend Only';
  if (/student/.test(raw)) return 'Student-friendly';
  const upper = raw.toUpperCase().replace(/[\s-]+/g, '_');
  return VALID_JOB_TYPES.has(upper) ? upper : value;
}

function normalizeSalaryType(value, fallback = 'MONTHLY') {
  const raw = String(value || fallback || '').trim().toUpperCase();
  if (raw.includes('HOUR')) return 'HOURLY';
  if (raw.includes('DAY')) return 'DAILY';
  if (raw.includes('WEEK')) return 'WEEKLY';
  if (raw.includes('TASK') || raw.includes('PIECE')) return 'TASK';
  if (raw.includes('MONTH') || raw.includes('YEAR') || raw.includes('ANNUAL')) return 'MONTHLY';
  return VALID_SALARY_TYPES.has(raw) ? raw : 'MONTHLY';
}

function normalizeSalary(baseSalary, source) {
  const fallbackSalary = truncate(firstString(source.defaultSalary), 60);
  const fallbackType = normalizeSalaryType(source.defaultSalaryType, 'MONTHLY');
  const salaryObjects = asArray(baseSalary).filter(Boolean);

  for (const salaryObject of salaryObjects) {
    if (typeof salaryObject === 'string' || typeof salaryObject === 'number') {
      const text = truncate(salaryObject, 60);
      if (text) return { salary: text, salaryType: fallbackType };
    }

    if (!salaryObject || typeof salaryObject !== 'object') continue;
    const value = salaryObject.value && typeof salaryObject.value === 'object'
      ? salaryObject.value
      : salaryObject;

    const minValue = firstString(value.minValue);
    const maxValue = firstString(value.maxValue);
    const exactValue = firstString(value.value);
    const unitText = firstString(value.unitText, salaryObject.unitText);

    let salary = '';
    if (minValue && maxValue) salary = `${minValue}-${maxValue}`;
    else if (exactValue) salary = exactValue;
    else if (minValue) salary = `${minValue}+`;
    else if (maxValue) salary = `Up to ${maxValue}`;

    salary = truncate(salary, 60);
    if (salary) {
      return {
        salary,
        salaryType: normalizeSalaryType(unitText, fallbackType)
      };
    }
  }

  return { salary: fallbackSalary, salaryType: fallbackSalary ? fallbackType : '' };
}

function resolveAddress(address) {
  if (!address) return { addressText: '', companyCity: '' };
  if (typeof address === 'string') {
    return { addressText: truncate(address, 300), companyCity: extractCity(address) };
  }
  if (typeof address !== 'object') return { addressText: '', companyCity: '' };

  const parts = [
    address.streetAddress,
    address.addressLocality,
    address.addressRegion,
    address.postalCode,
    address.addressCountry && typeof address.addressCountry === 'object'
      ? address.addressCountry.name
      : address.addressCountry
  ]
    .map((part) => firstString(part))
    .filter(Boolean);

  return {
    addressText: truncate(parts.join(', '), 300),
    companyCity: firstString(address.addressLocality, extractCity(parts.join(', ')))
  };
}

function resolveLocation(jobLocation, source) {
  const locationNode = asArray(jobLocation)[0] || {};
  const address = resolveAddress(locationNode.address || locationNode);
  const defaultLocation = source.defaultLocation || {};
  const defaultAddress = resolveAddress(defaultLocation.addressText || defaultLocation.address || source.defaultAddressText);
  const geo = locationNode.geo || defaultLocation.geo || {};

  const lat = parseFiniteNumber(firstString(geo.latitude, geo.lat, locationNode.latitude, defaultLocation.lat, defaultLocation.latitude));
  const lng = parseFiniteNumber(firstString(geo.longitude, geo.lng, locationNode.longitude, defaultLocation.lng, defaultLocation.longitude));

  return {
    addressText: truncate(firstString(address.addressText, defaultLocation.addressText, defaultAddress.addressText, source.defaultAddressText), 300),
    companyCity: truncate(firstString(address.companyCity, defaultLocation.companyCity, defaultAddress.companyCity), 120),
    lat,
    lng
  };
}

function parseFiniteNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function extractCity(addressText) {
  const parts = String(addressText || '')
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean);
  if (parts.length >= 3) return parts[parts.length - 2];
  if (parts.length >= 2) return parts[parts.length - 1];
  return parts[0] || '';
}

const GEOHASH_BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

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

function isValidLatLng(lat, lng) {
  return Number.isFinite(lat) && Number.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 && !(lat === 0 && lng === 0);
}

function parseDateToIso(value, fallbackDate) {
  const raw = firstString(value);
  if (raw) {
    const parsed = Date.parse(raw);
    if (!Number.isNaN(parsed)) return new Date(parsed).toISOString();
  }
  return fallbackDate.toISOString();
}

function addDays(date, days) {
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000);
}

function contactCandidatesFromNode(node, source, description) {
  const hiringOrganization = node.hiringOrganization || {};
  const contactPoint = hiringOrganization.contactPoint || node.contactPoint || {};
  return [
    { source: 'source_config', value: source.contactNumber },
    { source: 'source_config', value: source.defaultContactNumber },
    { source: 'public_contact_field', value: contactPoint.telephone },
    { source: 'public_contact_field', value: contactPoint.phone },
    { source: 'public_contact_field', value: hiringOrganization.telephone },
    { source: 'public_contact_field', value: hiringOrganization.phone },
    { source: 'public_contact_field', value: node.telephone },
    { source: 'public_contact_field', value: node.phone },
    { source: 'public_description_text', value: description }
  ];
}

function normalizeJobPosting(node, source, options) {
  const warnings = [];
  const now = new Date();
  const title = truncate(firstString(node.title, node.name, source.defaultTitle), 120);
  const hiringOrganization = node.hiringOrganization || {};
  const companyName = truncate(firstString(hiringOrganization.name, source.defaultCompanyName), 120);
  const description = truncate(stripHtml(firstString(node.description, source.defaultDescription)), 5000);
  const location = resolveLocation(node.jobLocation, source);
  const contact = extractPhoneFromCandidateSources(contactCandidatesFromNode(node, source, description));
  const contactNumber = contact.phone;
  const salaryInfo = normalizeSalary(node.baseSalary, source);
  const jobType = normalizeJobType(firstString(node.employmentType, source.defaultJobType), source.defaultJobType);
  const createdAt = parseDateToIso(node.datePosted, now);
  const expiresAt = parseDateToIso(node.validThrough, addDays(now, options.expiresDays));
  const vacancies = normalizeVacancies(firstString(node.totalJobOpenings, node.openings, source.defaultVacancies), null);

  if (!title || title.length < 3) warnings.push('missingTitle');
  if (!companyName) warnings.push('missingCompanyName');
  if (!description) warnings.push('missingDescription');
  if (!contactNumber && options.requirePhone) warnings.push('missingPublicPhone');
  if (!location.addressText) warnings.push('missingAddressText');
  if (!isValidLatLng(location.lat, location.lng)) warnings.push('missingValidCoordinates');

  const idSeed = [source.url, title, companyName, location.addressText].join('|').toLowerCase();
  const hash = sha1(idSeed);
  const jobId = `online_${hash.slice(0, 24)}`;
  const geohash = isValidLatLng(location.lat, location.lng) ? encodeGeohash(location.lat, location.lng, 6) : '';

  const meta = {
    employerId: options.employerId,
    companyName,
    title,
    salary: salaryInfo.salary,
    salaryType: salaryInfo.salaryType,
    location: isValidLatLng(location.lat, location.lng) ? { lat: location.lat, lng: location.lng } : { lat: 0, lng: 0 },
    geohash,
    addressText: location.addressText,
    jobType,
    status: 'open',
    createdAt,
    vacancies
  };

  const details = {
    employerId: options.employerId,
    createdAt,
    expiresAt,
    idempotencyKey: `online:${hash.slice(0, 56)}`,
    description,
    contactNumber: contactNumber || '',
    gender: firstString(source.defaultGender),
    experienceRequired: firstString(source.defaultExperienceRequired),
    educationRequired: firstString(source.defaultEducationRequired),
    companyCity: location.companyCity || extractCity(location.addressText),
    shiftTiming: truncate(firstString(source.defaultShiftTiming), 120),
    applicationCount: 0
  };

  const audit = {
    jobId,
    sourceName: source.name || '',
    sourceUrl: source.url || '',
    sourceType: 'json-ld-jobposting',
    importedBy: 'scripts/import-online-jobs.js',
    scrapedAt: now.toISOString(),
    sourceDatePosted: firstString(node.datePosted),
    sourceValidThrough: firstString(node.validThrough),
    contactNumberSource: contactNumber ? contact.source : 'missing',
    reviewRequired: true
  };

  return {
    approved: false,
    jobId,
    sourceName: source.name || '',
    sourceUrl: source.url || '',
    warnings,
    valid: warnings.length === 0,
    meta,
    details,
    audit
  };
}

function normalizeVacancies(value, fallback) {
  const parsed = Number.parseInt(String(value || ''), 10);
  if (Number.isInteger(parsed) && parsed >= 1 && parsed <= 1000) return parsed;
  return fallback;
}

const HYDERABAD_AREA_COORDINATES = {
  'ameerpet': { lat: 17.4375, lng: 78.4483 },
  'banjara hills': { lat: 17.4156, lng: 78.4347 },
  'beeramguda': { lat: 17.5183, lng: 78.2988 },
  'borabanda': { lat: 17.453, lng: 78.413 },
  'chanda nagar': { lat: 17.493, lng: 78.331 },
  'dilsukhnagar': { lat: 17.3688, lng: 78.5247 },
  'gachibowli': { lat: 17.4401, lng: 78.3489 },
  'hyderabad public school': { lat: 17.4432, lng: 78.4672 },
  'ie moulali': { lat: 17.466, lng: 78.56 },
  'kalyan nagar': { lat: 17.4446, lng: 78.4339 },
  'kondapur': { lat: 17.46, lng: 78.3548 },
  'kukatpally': { lat: 17.4849, lng: 78.4138 },
  'kyasaram': { lat: 17.565, lng: 78.645 },
  'l b nagar': { lat: 17.3457, lng: 78.5522 },
  'lb nagar': { lat: 17.3457, lng: 78.5522 },
  'madhapur': { lat: 17.4486, lng: 78.3908 },
  'madina': { lat: 17.3684, lng: 78.4768 },
  'moosaram bagh': { lat: 17.372, lng: 78.533 },
  'pragathi nagar': { lat: 17.521, lng: 78.391 },
  'saroor nagar': { lat: 17.356, lng: 78.533 },
  'secunderabad': { lat: 17.4399, lng: 78.4983 },
  'suraram': { lat: 17.542, lng: 78.434 },
  'uppal': { lat: 17.4065, lng: 78.5593 }
};

function resolveAreaCoordinates(addressText, source) {
  const sourceAreas = source.areaCoordinates && typeof source.areaCoordinates === 'object'
    ? source.areaCoordinates
    : {};
  const combined = { ...HYDERABAD_AREA_COORDINATES, ...sourceAreas };
  const address = String(addressText || '').toLowerCase();

  for (const [area, coords] of Object.entries(combined)) {
    if (address.includes(area.toLowerCase()) && isValidLatLng(Number(coords.lat), Number(coords.lng))) {
      return { lat: Number(coords.lat), lng: Number(coords.lng) };
    }
  }

  const defaultLocation = source.defaultLocation || {};
  if (isValidLatLng(Number(defaultLocation.lat), Number(defaultLocation.lng))) {
    return { lat: Number(defaultLocation.lat), lng: Number(defaultLocation.lng) };
  }

  return { lat: null, lng: null };
}

function extractJobLinksFromHtml(html, baseUrl) {
  const links = [];
  const seen = new Set();
  const regex = /href=["']([^"']*\/jobs\/[^"']+\/?)["']/gi;
  let match;
  while ((match = regex.exec(html)) !== null) {
    const href = match[1];
    const resolved = new URL(href, baseUrl).toString();
    const parsed = new URL(resolved);
    if (!parsed.pathname.startsWith('/jobs/') || parsed.pathname === '/jobs/') continue;
    if (seen.has(resolved)) continue;
    seen.add(resolved);
    links.push(resolved);
  }
  return links;
}

function valueAfterLabel(lines, label) {
  const normalizedLabel = label.toLowerCase();
  const index = lines.findIndex((line) => line.toLowerCase().replace(/\s+/g, ' ').startsWith(normalizedLabel));
  if (index === -1) return '';
  return firstString(lines[index + 1]);
}

function parseWorkIndiaDate(value) {
  const match = String(value || '').match(/(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  if (!match) return '';
  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  if (!day || !month || !year) return '';
  return new Date(Date.UTC(year, month - 1, day, 0, 0, 0)).toISOString();
}

function parseSalaryText(value) {
  const match = String(value || '').match(/Rs\.\s*([0-9,]+)\s*-\s*Rs\.\s*([0-9,]+)/i);
  if (match) {
    return `${match[1].replace(/,/g, '')}-${match[2].replace(/,/g, '')}`;
  }
  const single = String(value || '').match(/Rs\.\s*([0-9,]+)/i);
  if (single) return single[1].replace(/,/g, '');
  return truncate(firstString(value), 60);
}

function parseWorkIndiaDetail(html, detailUrl, source, options) {
  const lines = htmlToLines(html);
  const now = new Date();
  const salaryIndex = lines.findIndex((line) => /^Rs\.\s*\d/i.test(line));
  const postedIndex = lines.findIndex((line) => /^Posted on:/i.test(line));
  const warnings = [];

  const title = truncate(firstString(lines[salaryIndex - 1], source.defaultTitle), 120);
  const salaryLine = firstString(lines[salaryIndex]);
  const locationLine = firstString(
    lines.slice(salaryIndex + 1, postedIndex > salaryIndex ? postedIndex : salaryIndex + 12)
      .find((line) => /,\s*Hyderabad/i.test(line)),
    valueAfterLabel(lines, 'Location :'),
    source.defaultLocation?.addressText
  );
  const companyName = truncate(firstString(lines[postedIndex - 2], source.defaultCompanyName, source.name), 120);
  const educationRequired = firstString(valueAfterLabel(lines, 'Educational Requirement :'), lines[postedIndex - 1], source.defaultEducationRequired);
  const experienceRequired = firstString(valueAfterLabel(lines, 'Experience Requirement :'), source.defaultExperienceRequired);
  const gender = firstString(valueAfterLabel(lines, 'Gender Preference :'), source.defaultGender);
  const shiftTiming = truncate(firstString(valueAfterLabel(lines, 'Working Hours :'), valueAfterLabel(lines, 'Job Timings'), source.defaultShiftTiming), 120);
  const additionalInfo = firstString(valueAfterLabel(lines, 'Additional Info'), source.defaultDescription);
  const employmentType = firstString(valueAfterLabel(lines, 'Employment Type'), source.defaultJobType);
  const addressText = truncate(locationLine, 300);
  const coords = resolveAreaCoordinates(addressText, source);
  const contact = extractPhoneFromCandidateSources([
    { source: 'source_config', value: source.contactNumber },
    { source: 'source_config', value: source.defaultContactNumber },
    { source: 'public_description_text', value: additionalInfo },
    { source: 'public_page_text', value: html }
  ]);
  const contactNumber = contact.phone;
  const postedAt = parseWorkIndiaDate(lines[postedIndex]) || now.toISOString();
  const hash = sha1([detailUrl, title, companyName, addressText].join('|').toLowerCase());
  const jobId = `workindia_${hash.slice(0, 22)}`;
  const geohash = isValidLatLng(coords.lat, coords.lng) ? encodeGeohash(coords.lat, coords.lng, 6) : '';

  if (!title || title.length < 3) warnings.push('missingTitle');
  if (!companyName) warnings.push('missingCompanyName');
  if (!addressText) warnings.push('missingAddressText');
  if (!contactNumber && options.requirePhone) warnings.push('missingPublicPhone');
  if (!isValidLatLng(coords.lat, coords.lng)) warnings.push('missingValidCoordinates');

  const description = truncate(
    [
      additionalInfo,
      salaryLine ? `Salary: ${salaryLine}` : '',
      shiftTiming ? `Working hours: ${shiftTiming}` : '',
      educationRequired ? `Education: ${educationRequired}` : '',
      experienceRequired ? `Experience: ${experienceRequired}` : ''
    ].filter(Boolean).join('\n'),
    5000
  );

  return {
    approved: false,
    jobId,
    sourceName: source.name || 'WorkIndia',
    sourceUrl: detailUrl,
    warnings,
    valid: warnings.length === 0,
    meta: {
      employerId: options.employerId,
      companyName,
      title,
      salary: parseSalaryText(salaryLine),
      salaryType: normalizeSalaryType(source.defaultSalaryType, 'MONTHLY'),
      location: isValidLatLng(coords.lat, coords.lng) ? { lat: coords.lat, lng: coords.lng } : { lat: 0, lng: 0 },
      geohash,
      addressText,
      jobType: normalizeJobType(employmentType, source.defaultJobType),
      status: 'open',
      createdAt: postedAt,
      vacancies: normalizeVacancies(source.defaultVacancies, null)
    },
    details: {
      employerId: options.employerId,
      createdAt: postedAt,
      expiresAt: addDays(now, options.expiresDays).toISOString(),
      idempotencyKey: `workindia:${hash.slice(0, 54)}`,
      description,
      contactNumber: contactNumber || '',
      gender,
      experienceRequired,
      educationRequired,
      companyCity: 'Hyderabad',
      shiftTiming,
      applicationCount: 0
    },
    audit: {
      jobId,
      sourceName: source.name || 'WorkIndia',
      sourceUrl: detailUrl,
      sourceType: 'workindia-public-detail-page',
      importedBy: 'scripts/import-online-jobs.js',
      scrapedAt: now.toISOString(),
      sourceDatePosted: lines[postedIndex] || '',
      sourceValidThrough: '',
      contactNumberSource: contactNumber ? contact.source : 'not_public_on_workindia',
      reviewRequired: true
    }
  };
}

async function scrapeWorkIndiaSource(source, options) {
  const skipped = [];
  const jobs = [];
  const html = await fetchText(source.url);
  const links = extractJobLinksFromHtml(html, source.url);

  if (links.length === 0) {
    return {
      jobs,
      skipped: [{ sourceName: source.name || source.url, sourceUrl: source.url, reason: 'noWorkIndiaJobLinks' }]
    };
  }

  for (const detailUrl of links) {
    try {
      const detailHtml = await fetchText(detailUrl);
      const job = parseWorkIndiaDetail(detailHtml, detailUrl, source, options);
      if (job.valid) jobs.push(job);
      else skipped.push({
        sourceName: source.name || source.url,
        sourceUrl: detailUrl,
        title: job.meta.title,
        reason: job.warnings.join(',')
      });
    } catch (error) {
      skipped.push({ sourceName: source.name || source.url, sourceUrl: detailUrl, reason: error.message });
    }

    if (options.maxPerSource > 0 && jobs.length >= options.maxPerSource) break;
    if (options.limit > 0 && jobs.length >= options.limit) break;
  }

  return { jobs, skipped };
}

async function scrapeSource(source, options) {
  const skipped = [];
  const jobs = [];

  if (source.disabled) {
    skipped.push({ sourceName: source.name || source.url, reason: 'disabled' });
    return { jobs, skipped };
  }

  if (!source.url) {
    skipped.push({ sourceName: source.name || '(unnamed)', reason: 'missingUrl' });
    return { jobs, skipped };
  }

  if (options.respectRobots && source.respectRobots !== false) {
    const allowed = await robotsAllows(source.url);
    if (!allowed) {
      skipped.push({ sourceName: source.name || source.url, sourceUrl: source.url, reason: 'blockedByRobotsTxt' });
      return { jobs, skipped };
    }
  }

  if (source.parser === 'workindia' || /(^|\.)workindia\.in$/i.test(new URL(source.url).hostname)) {
    return scrapeWorkIndiaSource(source, options);
  }

  const html = await fetchText(source.url);
  const blocks = extractJsonLdBlocks(html);
  if (blocks.length === 0) {
    skipped.push({ sourceName: source.name || source.url, sourceUrl: source.url, reason: 'noJsonLdJobPosting' });
    return { jobs, skipped };
  }

  for (const block of blocks) {
    const parsed = parseJsonLd(block, source.name || source.url);
    const nodes = collectJobPostingNodes(parsed);
    for (const node of nodes) {
      const job = normalizeJobPosting(node, source, options);
      if (job.valid) jobs.push(job);
      else skipped.push({
        sourceName: source.name || source.url,
        sourceUrl: source.url,
        title: job.meta.title,
        reason: job.warnings.join(',')
      });

      if (options.maxPerSource > 0 && jobs.length >= options.maxPerSource) return { jobs, skipped };
    }
  }

  if (jobs.length === 0 && skipped.length === 0) {
    skipped.push({ sourceName: source.name || source.url, sourceUrl: source.url, reason: 'noJobPostingNodes' });
  }

  return { jobs, skipped };
}

async function collectJobsFromSources(options) {
  const sourcesPath = resolvePath(options.sources);
  const config = readJsonFile(sourcesPath);
  const sources = Array.isArray(config) ? config : config.sources;
  if (!Array.isArray(sources) || sources.length === 0) {
    throw new Error(`No sources found in ${sourcesPath}`);
  }

  const jobsById = new Map();
  const skipped = [];

  for (const source of sources) {
    try {
      const result = await scrapeSource(source, options);
      result.skipped.forEach((item) => skipped.push(item));
      for (const job of result.jobs) {
        if (!jobsById.has(job.jobId)) jobsById.set(job.jobId, job);
        if (options.limit > 0 && jobsById.size >= options.limit) {
          return { jobs: Array.from(jobsById.values()), skipped, sourcesPath };
        }
      }
    } catch (error) {
      skipped.push({ sourceName: source.name || source.url || '(unnamed)', sourceUrl: source.url || '', reason: error.message });
    }
  }

  return { jobs: Array.from(jobsById.values()), skipped, sourcesPath };
}

function collectJobsFromReview(options) {
  const reviewPath = resolvePath(options.fromReview);
  const review = readJsonFile(reviewPath);
  const allJobs = Array.isArray(review) ? review : review.jobs;
  if (!Array.isArray(allJobs)) {
    throw new Error(`Review file does not contain a jobs array: ${reviewPath}`);
  }

  const jobs = [];
  const skipped = [];

  for (const job of allJobs) {
    if (!job || typeof job !== 'object') {
      skipped.push({ sourceName: '(review)', sourceUrl: '', reason: 'invalidReviewJob' });
      continue;
    }

    if (options.approvedOnly && job.approved !== true) continue;

    const errors = validatePreparedJob(job, options);
    const warnings = Array.from(new Set([...(Array.isArray(job.warnings) ? job.warnings : []), ...errors]));
    const reviewedJob = { ...job, warnings, valid: errors.length === 0 };

    if (errors.length > 0) {
      skipped.push({
        sourceName: job.sourceName || job.audit?.sourceName || '(review)',
        sourceUrl: job.sourceUrl || job.audit?.sourceUrl || '',
        title: job.meta?.title || '',
        reason: errors.join(',')
      });
      continue;
    }

    jobs.push(reviewedJob);
  }

  return { jobs, skipped, sourcesPath: reviewPath };
}

function toReviewDocument(collected, options) {
  return {
    generatedAt: new Date().toISOString(),
    mode: options.apply ? 'apply' : 'review',
    sourcesPath: collected.sourcesPath,
    requirePhone: options.requirePhone,
    employerId: options.employerId,
    totals: {
      validJobs: collected.jobs.length,
      skipped: collected.skipped.length
    },
    jobs: collected.jobs,
    skipped: collected.skipped
  };
}

function convertTimestampFields(admin, value, key = '') {
  if (Array.isArray(value)) return value.map((item) => convertTimestampFields(admin, item));
  if (!value || typeof value !== 'object') {
    if (typeof value === 'string' && ['createdAt', 'expiresAt', 'scrapedAt', 'importedAt'].includes(key)) {
      const parsed = Date.parse(value);
      if (!Number.isNaN(parsed)) return admin.firestore.Timestamp.fromDate(new Date(parsed));
    }
    return value;
  }

  const output = {};
  for (const [childKey, childValue] of Object.entries(value)) {
    output[childKey] = convertTimestampFields(admin, childValue, childKey);
  }
  return output;
}

function validatePreparedJob(job, options = {}) {
  const errors = [];
  if (!job.jobId || typeof job.jobId !== 'string') errors.push('missing jobId');
  if (!job.meta || typeof job.meta !== 'object') errors.push('missing meta');
  if (!job.details || typeof job.details !== 'object') errors.push('missing details');
  if (job.meta) {
    if (!job.meta.title || job.meta.title.length < 3 || job.meta.title.length > 120) errors.push('invalid title');
    if (!job.meta.companyName || job.meta.companyName.length > 120) errors.push('invalid companyName');
    if (!job.meta.salary || String(job.meta.salary).length > 60) errors.push('invalid salary');
    if (!job.meta.salaryType || !VALID_SALARY_TYPES.has(String(job.meta.salaryType).toUpperCase())) errors.push('invalid salaryType');
    if (!isValidLatLng(job.meta.location?.lat, job.meta.location?.lng)) errors.push('invalid location');
    if (!job.meta.geohash) errors.push('missing geohash');
    if (!job.meta.addressText || job.meta.addressText.length > 300) errors.push('invalid addressText');
    if (!job.meta.jobType) errors.push('missing jobType');
    if (job.meta.status !== 'open') errors.push('status must be open');
    const vacancies = Number.parseInt(String(job.meta.vacancies || ''), 10);
    if (!Number.isInteger(vacancies) || vacancies < 1 || vacancies > 50) errors.push('invalid vacancies');
  }
  if (job.details) {
    if (!job.details.employerId) errors.push('missing details.employerId');
    if (!String(job.details.description || '').trim()) errors.push('missing description');
    if (String(job.details.description || '').length > 5000) errors.push('description too long');
    if (String(job.details.contactNumber || '').length > 20) errors.push('contactNumber too long');
    if (options.requirePhone && !normalizePhone(job.details.contactNumber)) errors.push('missing public contactNumber');
  }
  return errors;
}

function buildFirestoreDocuments(job) {
  const cardData = {
    title: job.meta.title,
    companyName: job.meta.companyName,
    salary: job.meta.salary,
    salaryType: job.meta.salaryType,
    location: job.meta.location,
    geohash: job.meta.geohash,
    addressText: job.meta.addressText,
    jobType: job.meta.jobType,
    status: job.meta.status,
    createdAt: job.meta.createdAt,
    employerId: job.meta.employerId,
    vacancies: job.meta.vacancies
  };

  const detailsData = {
    employerId: job.details.employerId,
    expiresAt: job.details.expiresAt,
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

async function applyJobs(jobs, options = {}) {
  const admin = require('firebase-admin');
  const serviceAccount = loadServiceAccount();
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  }
  const db = admin.firestore();

  let created = 0;
  let duplicates = 0;
  let invalid = 0;
  let batch = db.batch();
  let batchOps = 0;

  async function commitIfNeeded(force = false) {
    if (batchOps === 0) return;
    if (!force && batchOps < 450) return;
    await batch.commit();
    batch = db.batch();
    batchOps = 0;
  }

  for (const job of jobs) {
    const errors = validatePreparedJob(job, options);
    if (errors.length > 0) {
      invalid += 1;
      console.warn(`Skipping invalid job ${job.jobId || '(missing id)'}: ${errors.join(', ')}`);
      continue;
    }

    const metaRef = db.collection('jobmetadata').doc(job.jobId);
    const existing = await metaRef.get();
    if (existing.exists) {
      duplicates += 1;
      continue;
    }

    const detailsRef = db.collection('job_details').doc(job.jobId);
    const { cardData, detailsData } = buildFirestoreDocuments(job);

    batch.set(metaRef, convertTimestampFields(admin, cardData));
    batch.set(detailsRef, convertTimestampFields(admin, detailsData));
    batchOps += 2;
    created += 1;
    await commitIfNeeded(false);
  }

  await commitIfNeeded(true);
  return { created, duplicates, invalid };
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    printHelp();
    return;
  }

  const collected = options.fromReview
    ? collectJobsFromReview(options)
    : await collectJobsFromSources(options);

  const review = toReviewDocument(collected, options);
  const reviewOut = resolvePath(options.reviewOut);
  writeJsonFile(reviewOut, review);

  console.log(`Review file: ${reviewOut}`);
  console.log(`Valid jobs: ${review.totals.validJobs}`);
  console.log(`Skipped: ${review.totals.skipped}`);

  if (!options.apply) {
    console.log('Dry-run only. No Firestore writes were made.');
    return;
  }

  if (collected.jobs.length === 0) {
    console.log('No valid jobs to import.');
    return;
  }

  const result = await applyJobs(collected.jobs, options);
  console.log(`Created: ${result.created}`);
  console.log(`Duplicates skipped: ${result.duplicates}`);
  console.log(`Invalid skipped at write time: ${result.invalid}`);
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
