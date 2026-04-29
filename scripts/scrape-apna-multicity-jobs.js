'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const DEFAULT_CURRENT_DATE = '2026-04-29';
const DEFAULT_FRESH_DAYS = 7;
const DEFAULT_TARGET = 500;
const DEFAULT_PAGES_PER_CITY = 120;
const DEFAULT_CONCURRENCY = 3;
const DEFAULT_EMPLOYER_ID = process.env.DUTYPE_IMPORT_EMPLOYER_ID || 'admin1';
const DEFAULT_REVIEW_OUT = path.join(__dirname, 'output', 'telangana-ap-fresh-phone-verified-apna-review.json');
const MAX_DESCRIPTION_LENGTH = 5000;

const USER_AGENT = 'DutyPeApnaReviewScraper/1.1 (+https://dutype.com)';
const PHONE_LINE_HINT = /call|phone|whats|contact|mobile|hr|resume|cv|apply|send|share|reach|interested/i;
const PHONE_TOKEN = /(?<!\d)(?:\+?91[\s().-]*)?[6-9](?:[\s().-]*\d){9}(?!\d)/g;
const GEOHASH_BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

const CITY_CONFIGS = [
  { slug: 'hyderabad', label: 'Hyderabad', state: 'Telangana', allowedPathSlugs: ['hyderabad', 'secunderabad', 'hyderabad-region'] },
  { slug: 'vijayawada', label: 'Vijayawada', state: 'Andhra Pradesh', allowedPathSlugs: ['vijayawada'] },
  { slug: 'khammam', label: 'Khammam', state: 'Telangana', allowedPathSlugs: ['khammam'] },
  { slug: 'warangal', label: 'Warangal', state: 'Telangana', allowedPathSlugs: ['warangal', 'hanamkonda'] },
  { slug: 'visakhapatnam', label: 'Visakhapatnam', state: 'Andhra Pradesh', allowedPathSlugs: ['visakhapatnam', 'vizag'] },
  { slug: 'guntur', label: 'Guntur', state: 'Andhra Pradesh', allowedPathSlugs: ['guntur'] },
  { slug: 'rajahmundry', label: 'Rajahmundry', state: 'Andhra Pradesh', allowedPathSlugs: ['rajahmundry'] },
  { slug: 'kakinada', label: 'Kakinada', state: 'Andhra Pradesh', allowedPathSlugs: ['kakinada'] },
  { slug: 'nellore', label: 'Nellore', state: 'Andhra Pradesh', allowedPathSlugs: ['nellore'] },
  { slug: 'kurnool', label: 'Kurnool', state: 'Andhra Pradesh', allowedPathSlugs: ['kurnool'] },
  { slug: 'tirupati', label: 'Tirupati', state: 'Andhra Pradesh', allowedPathSlugs: ['tirupati'] },
  { slug: 'karimnagar', label: 'Karimnagar', state: 'Telangana', allowedPathSlugs: ['karimnagar'] },
  { slug: 'nizamabad', label: 'Nizamabad', state: 'Telangana', allowedPathSlugs: ['nizamabad'] },
  { slug: 'nalgonda', label: 'Nalgonda', state: 'Telangana', allowedPathSlugs: ['nalgonda'] },
  { slug: 'eluru', label: 'Eluru', state: 'Andhra Pradesh', allowedPathSlugs: ['eluru'] },
  { slug: 'ongole', label: 'Ongole', state: 'Andhra Pradesh', allowedPathSlugs: ['ongole'] },
  { slug: 'anantapur', label: 'Anantapur', state: 'Andhra Pradesh', allowedPathSlugs: ['anantapur', 'ananthapur'] },
  { slug: 'kadapa', label: 'Kadapa', state: 'Andhra Pradesh', allowedPathSlugs: ['kadapa'] }
];

function printHelp() {
  console.log(`Apna Telangana/AP fresh phone-visible scraper

Usage:
  node scrape-apna-multicity-jobs.js --target 500

Options:
  --review-out <path>       Output review JSON path.
  --from-browser-raw <path> Process raw page data collected from a browser instead of fetching in Node.
  --cities <csv>            City slugs to scan. Default: all configured Telangana/AP cities.
  --pages-per-city <count>  Maximum Apna pages per city. Default: ${DEFAULT_PAGES_PER_CITY}
  --target <count>          Stop output at this many unique valid jobs. Default: ${DEFAULT_TARGET}
  --current-date <date>     Freshness reference date. Default: ${DEFAULT_CURRENT_DATE}
  --fresh-days <count>      Include jobs created within this many days. Default: ${DEFAULT_FRESH_DAYS}
  --concurrency <count>     List pages fetched in parallel per city. Default: ${DEFAULT_CONCURRENCY}
  --employer-id <id>        Employer/admin uid for review rows. Default: ${DEFAULT_EMPLOYER_ID}
  --help                    Show this help.
`);
}

function parseArgs(argv) {
  const args = {
    reviewOut: DEFAULT_REVIEW_OUT,
    fromBrowserRaw: '',
    citySlugs: [],
    pagesPerCity: DEFAULT_PAGES_PER_CITY,
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
      case '--from-browser-raw':
        args.fromBrowserRaw = next();
        break;
      case '--cities':
        args.citySlugs = next().split(',').map((item) => item.trim().toLowerCase()).filter(Boolean);
        break;
      case '--pages-per-city':
        args.pagesPerCity = parsePositiveInt(next(), '--pages-per-city');
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

function resolveCities(citySlugs) {
  if (!citySlugs.length) return CITY_CONFIGS;
  const bySlug = new Map(CITY_CONFIGS.map((city) => [city.slug, city]));
  return citySlugs.map((slug) => {
    const city = bySlug.get(slug);
    if (!city) throw new Error(`Unknown city slug: ${slug}`);
    return city;
  });
}

function cityUrl(city) {
  return `https://apna.co/jobs/jobs-in-${city.slug}`;
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(resolvePath(filePath), 'utf8').replace(/^\uFEFF/, ''));
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
    await sleep(600 * attempt);
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

function firstString(...values) {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) return value.trim();
    if (typeof value === 'number' && Number.isFinite(value)) return String(value);
  }
  return '';
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
    address.mid_area && address.mid_area.latitude
  );
  const lng = firstFiniteNumber(
    geometry.google_longitude,
    area.longitude,
    address.longitude,
    address.mid_area && address.mid_area.longitude
  );

  return isValidLatLng(lat, lng) ? { lat, lng } : null;
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

function parseInteger(value) {
  if (typeof value === 'number' && Number.isFinite(value)) return Math.trunc(value);
  const parsed = Number.parseInt(String(value || '').replace(/,/g, ''), 10);
  return Number.isInteger(parsed) ? parsed : null;
}

function normalizeSalary(job) {
  const min = parseInteger(job.fixed_min_salary ?? job.min_salary);
  const max = parseInteger(job.fixed_max_salary ?? job.max_salary);
  if (min && max) return { salary: `${min}-${max}`, salaryType: 'MONTHLY', source: 'source_salary_fields' };
  if (min) return { salary: `${min}+`, salaryType: 'MONTHLY', source: 'source_salary_fields' };
  if (max) return { salary: `Up to ${max}`, salaryType: 'MONTHLY', source: 'source_salary_fields' };

  const fromText = parseSalaryFromText(job.description || '');
  return fromText || null;
}

function parseSalaryFromText(text) {
  const lines = String(text || '')
    .split(/\r?\n+/)
    .map((line) => line.replace(/\s+/g, ' ').trim())
    .filter(Boolean);

  for (const line of lines) {
    if (!/(salary|pay|earning|earnings|income|ctc|stipend|wage|\u20b9|rs\.?|inr)/i.test(line)) continue;
    const range = line.match(/(?:\u20b9|rs\.?|inr)?\s*([0-9][0-9,]{2,})\s*(?:-|to|\u2013|\u2014)\s*(?:\u20b9|rs\.?|inr)?\s*([0-9][0-9,]{2,})/i);
    const upto = line.match(/(?:upto|up to)\s*(?:\u20b9|rs\.?|inr)?\s*([0-9][0-9,]{2,})/i);
    const single = line.match(/(?:\u20b9|rs\.?|inr)\s*([0-9][0-9,]{2,})/i);
    const salaryType = inferSalaryType(line);

    if (range && salaryType) {
      return {
        salary: `${range[1].replace(/,/g, '')}-${range[2].replace(/,/g, '')}`,
        salaryType,
        source: 'public_description_text'
      };
    }
    if (upto && salaryType) {
      return { salary: `Up to ${upto[1].replace(/,/g, '')}`, salaryType, source: 'public_description_text' };
    }
    if (single && salaryType) {
      return { salary: single[1].replace(/,/g, ''), salaryType, source: 'public_description_text' };
    }
  }

  return null;
}

function inferSalaryType(text) {
  const raw = String(text || '').toLowerCase();
  if (/per\s*hour|\/\s*hour|hourly/.test(raw)) return 'HOURLY';
  if (/per\s*day|\/\s*day|daily|day/.test(raw)) return 'DAILY';
  if (/per\s*week|\/\s*week|weekly/.test(raw)) return 'WEEKLY';
  if (/per\s*task|per\s*order|piece/.test(raw)) return 'TASK';
  if (/per\s*month|\/\s*month|monthly|month|pm\b|ctc|lpa/.test(raw)) return 'MONTHLY';
  return '';
}

function normalizeVacancies(job) {
  const sourceValue = parseInteger(job.no_of_openings ?? job.openings ?? job.total_openings);
  if (sourceValue && sourceValue >= 1 && sourceValue <= 50) return { vacancies: sourceValue, source: 'source_openings_field' };

  const textValue = parseVacanciesFromText(job.description || '');
  if (textValue && textValue >= 1 && textValue <= 50) return { vacancies: textValue, source: 'public_description_text' };
  return null;
}

function parseVacanciesFromText(text) {
  const patterns = [
    /(?:vacanc(?:y|ies)|openings?|positions?|slots?)\D{0,24}(\d{1,2})/i,
    /(\d{1,2})\s*(?:vacanc(?:y|ies)|openings?|positions?|slots?)/i
  ];
  for (const pattern of patterns) {
    const match = String(text || '').match(pattern);
    if (match) return parseInteger(match[1]);
  }
  return null;
}

function normalizeWorkType(job) {
  const sourceText = [job.employment_type, job.type, job.title, job.description].filter(Boolean).join('\n');
  if (job.is_part_time === true) return { jobType: 'Part-time', source: 'source_is_part_time_field' };
  if (job.is_part_time === false) return { jobType: 'Full-time', source: 'source_is_part_time_field' };
  const raw = sourceText.toLowerCase();
  const hasFullTime = /full[\s-]*time/.test(raw);
  const hasPartTime = /part[\s-]*time/.test(raw);
  if (hasFullTime && hasPartTime) return { jobType: 'Full-time / Part-time', source: 'public_description_text' };
  if (hasPartTime) return { jobType: 'Part-time', source: 'public_description_text' };
  if (hasFullTime) return { jobType: 'Full-time', source: 'public_description_text' };
  if (/contract/.test(raw)) return { jobType: 'Contract', source: 'public_description_text' };
  if (/temporary|temp\b/.test(raw)) return { jobType: 'Temporary', source: 'public_description_text' };
  if (/weekend/.test(raw)) return { jobType: 'Weekend Only', source: 'public_description_text' };
  if (/student[\s-]*friendly/.test(raw)) return { jobType: 'Student-friendly', source: 'public_description_text' };
  return null;
}

function normalizeGender(job) {
  const fromField = firstString(job.gender, job.gender_preference, job.genderPreference);
  if (fromField) return mapGender(fromField);
  return mapGender(job.description || '');
}

function mapGender(value) {
  const raw = String(value || '').toLowerCase();
  if (/female\s+only|only\s+female|women\s+only|girls\s+only/.test(raw)) return 'Female';
  if (/male\s+only|only\s+male|men\s+only|boys\s+only/.test(raw)) return 'Male';
  if (/female/.test(raw) && !/male/.test(raw.replace(/female/g, ''))) return 'Female';
  if (/\bmale\b/.test(raw) && !/female/.test(raw)) return 'Male';
  if (/both|any gender|male\/female|male or female|all gender/.test(raw)) return 'Both';
  if (/\bany\b/.test(raw)) return 'Both';
  return '';
}

function normalizeShift(job) {
  const source = firstString(job.shift, job.shift_timing, job.working_hours);
  const mapped = mapShift(source);
  if (mapped) return { shiftTiming: mapped, source: 'source_shift_field' };
  const fromText = mapShift(job.description || '');
  return fromText ? { shiftTiming: fromText, source: 'public_description_text' } : null;
}

function mapShift(value) {
  const raw = String(value || '').toLowerCase();
  if (!raw) return '';
  if (/night|evening/.test(raw)) return 'Night shift';
  if (/day|morning|afternoon/.test(raw)) return 'Day shift';
  if (/rotational|both\s+shift|day\s*\/\s*night|night\s*\/\s*day/.test(raw)) return 'Both shift';
  if (/flexible|any\s+shift/.test(raw)) return 'Any shift';
  const timeRange = String(value || '').match(/\b\d{1,2}(?::\d{2})?\s*(?:am|pm)?\s*(?:-|to|\u2013|\u2014)\s*\d{1,2}(?::\d{2})?\s*(?:am|pm)?\b/i);
  return timeRange ? timeRange[0] : '';
}

function normalizeExperience(job) {
  const source = firstString(job.experience_in_years, job.experience, job.min_experience);
  if (source) return truncate(source, 120);
  const match = String(job.description || '').match(/(?:experience|exp)\D{0,24}([0-9]+\s*(?:-|to)?\s*[0-9]*\s*(?:year|yr|years|yrs)|freshers? can apply|fresher)/i);
  return match ? truncate(match[1], 120) : '';
}

function normalizeEducation(job) {
  const source = firstString(job.education, job.minimum_education, job.qualification);
  if (source) return truncate(source, 120);
  const match = String(job.description || '').match(/(?:education|qualification|qualified)\D{0,24}(10th|12th|intermediate|iti|diploma|graduate|degree|any qualification|no qualification)/i);
  return match ? truncate(match[1], 120) : '';
}

function resolveAddressText(job) {
  const address = job.address || {};
  const companyAddress = job.company_address || {};
  return truncate(
    firstString(
      job.location_name,
      companyAddress.line_1,
      address.line_1,
      address.area && address.area.name,
      address.mid_area && address.mid_area.name,
      address.city && address.city.name
    ),
    300
  );
}

function publicUrlForJob(job, city) {
  const sourceUrl = firstString(job.public_url, job.public_url_v2, job.share_url);
  if (sourceUrl) return sourceUrl;
  return `https://apna.co/job/${city.slug}/${job.slug || job.id}`;
}

function sourcePathMatchesCity(sourceUrl, city) {
  try {
    const parsed = new URL(sourceUrl);
    const pathName = parsed.pathname.toLowerCase();
    return city.allowedPathSlugs.some((slug) => pathName.includes(`/job/${slug.toLowerCase()}/`));
  } catch (_) {
    return false;
  }
}

function buildReviewJob(job, city, options, scrapedAt) {
  const warnings = ['phoneFoundInPublicDescriptionText'];
  const description = truncate(job.description || '', MAX_DESCRIPTION_LENGTH);
  if (!description) return { skippedReason: 'missingSourceDescription' };
  if (!createdWithinFreshWindow(job.created_on, options.currentDate, options.freshDays)) return { skippedReason: 'notFresh' };

  const sourceUrl = publicUrlForJob(job, city);
  if (!sourcePathMatchesCity(sourceUrl, city)) return { skippedReason: 'sourceCityMismatch', sourceUrl };

  const contact = extractPublicPhones(description);
  if (contact.phones.length === 0) return { skippedReason: 'missingPublicPhone', sourceUrl };

  const salary = normalizeSalary(job);
  if (!salary) return { skippedReason: 'missingSourceSalary', sourceUrl };

  const vacancies = normalizeVacancies(job);
  if (!vacancies) return { skippedReason: 'missingSourceVacancies', sourceUrl };

  const workType = normalizeWorkType(job);
  if (!workType) return { skippedReason: 'missingSourceWorkType', sourceUrl };

  const location = resolveLocation(job);
  if (!location) return { skippedReason: 'missingSourceCoordinates', sourceUrl };

  const addressText = resolveAddressText(job);
  if (!addressText) return { skippedReason: 'missingSourceAddressText', sourceUrl };

  const companyName = truncate(firstString(job.organization && job.organization.name, job.company_name, job.company), 120);
  if (!companyName) return { skippedReason: 'missingCompanyName', sourceUrl };

  const title = truncate(firstString(job.title, job.type), 120);
  if (!title || title.length < 3) return { skippedReason: 'missingTitle', sourceUrl };

  const gender = normalizeGender(job);
  const shift = normalizeShift(job);
  const experienceRequired = normalizeExperience(job);
  const educationRequired = normalizeEducation(job);
  if (!gender) warnings.push('sourceFieldMissing:gender');
  if (!shift) warnings.push('sourceFieldMissing:shiftTiming');
  if (!experienceRequired) warnings.push('sourceFieldMissing:experienceRequired');
  if (!educationRequired) warnings.push('sourceFieldMissing:educationRequired');

  const createdAt = parseDateIso(job.created_on, new Date(`${options.currentDate}T00:00:00.000Z`));
  const expiresAt = parseDateIso(job.expiry, addDays(new Date(createdAt), 30));
  const jobId = `apna_${job.id}`;
  const idempotencyHash = sha1([sourceUrl, title, companyName, job.created_on].join('|').toLowerCase());

  return {
    approved: false,
    jobId,
    sourceName: `Apna - ${companyName}`,
    sourceUrl,
    warnings,
    valid: true,
    meta: {
      employerId: options.employerId,
      companyName,
      title,
      salary: salary.salary,
      salaryType: salary.salaryType,
      location,
      geohash: encodeGeohash(location.lat, location.lng, 6),
      addressText,
      jobType: workType.jobType,
      status: 'open',
      createdAt,
      vacancies: vacancies.vacancies
    },
    details: {
      employerId: options.employerId,
      createdAt,
      expiresAt,
      idempotencyKey: `apna:${job.id}:${idempotencyHash.slice(0, 48)}`,
      description,
      contactNumber: contact.phones[0],
      gender,
      experienceRequired,
      educationRequired,
      companyCity: city.label,
      shiftTiming: shift ? shift.shiftTiming : '',
      applicationCount: 0
    },
    audit: {
      jobId,
      sourceName: `Apna - ${companyName}`,
      sourceUrl,
      sourceType: 'apna-public-next-data-city-feed',
      importedBy: 'scripts/scrape-apna-multicity-jobs.js',
      scrapedAt,
      sourceDatePosted: job.created_on || '',
      sourceFreshness: `within_${options.freshDays}_days_of_${options.currentDate}`,
      sourceValidThrough: job.expiry || '',
      sourceCity: city.label,
      sourceState: city.state,
      salarySource: salary.source,
      vacanciesSource: vacancies.source,
      workTypeSource: workType.source,
      genderSource: gender ? (firstString(job.gender, job.gender_preference, job.genderPreference) ? 'source_gender_field' : 'public_description_text') : 'missing',
      shiftTimingSource: shift ? shift.source : 'missing',
      contactNumberSource: 'public_description_text',
      contactEvidence: truncate(contact.evidence, 500),
      publicContactNumbers: contact.phones,
      reviewRequired: true
    }
  };
}

async function getPageJobs(city, pageNumber) {
  const sourceUrl = cityUrl(city);
  const pageUrl = pageNumber === 1 ? sourceUrl : addQueryParam(sourceUrl, 'page', pageNumber);
  const html = await fetchText(pageUrl);
  const data = extractNextData(html, pageUrl);
  const pageProps = data.props && data.props.pageProps ? data.props.pageProps : {};
  return {
    pageUrl,
    totalPages: Number.parseInt(String(pageProps.totalPages || 0), 10) || 0,
    jobs: (pageProps.jobs || []).map((item) => item.data || item).filter(Boolean)
  };
}

async function crawlCity(city, options, jobsById, skipped, scrapedAt) {
  const firstPage = await getPageJobs(city, 1);
  const totalPages = Math.min(options.pagesPerCity, firstPage.totalPages || options.pagesPerCity);
  const pageStats = [];
  let nextPage = 1;

  async function processPage(pageNumber) {
    const result = pageNumber === 1 ? firstPage : await getPageJobs(city, pageNumber);
    let freshCount = 0;
    let phoneVisibleCount = 0;
    let validCount = 0;

    for (const sourceJob of result.jobs) {
      const isFresh = createdWithinFreshWindow(sourceJob.created_on, options.currentDate, options.freshDays);
      const contact = extractPublicPhones(sourceJob.description);
      if (isFresh) freshCount += 1;
      if (contact.phones.length > 0) phoneVisibleCount += 1;

      const reviewJob = buildReviewJob(sourceJob, city, options, scrapedAt);
      if (!reviewJob || reviewJob.skippedReason) {
        skipped.push({
          sourceName: `Apna - ${city.label}`,
          sourceUrl: reviewJob && reviewJob.sourceUrl ? reviewJob.sourceUrl : publicUrlForJob(sourceJob, city),
          title: sourceJob.title || '',
          reason: reviewJob ? reviewJob.skippedReason : 'invalidJob'
        });
        continue;
      }

      validCount += 1;
      if (!jobsById.has(reviewJob.jobId)) jobsById.set(reviewJob.jobId, reviewJob);
    }

    pageStats.push({
      city: city.label,
      pageNumber,
      jobs: result.jobs.length,
      fresh: freshCount,
      phoneVisible: phoneVisibleCount,
      freshPhoneVisibleValid: validCount,
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
        skipped.push({ sourceName: `Apna - ${city.label}`, sourceUrl: addQueryParam(cityUrl(city), 'page', pageNumber), reason: error.message });
      }
    }
  }

  await Promise.all(Array.from({ length: Math.min(options.concurrency, totalPages) }, () => worker()));
  pageStats.sort((a, b) => a.pageNumber - b.pageNumber);
  return { city: city.label, sourceTotalPages: firstPage.totalPages || 0, pagesScanned: pageStats.length, pageStats };
}

async function crawl(options) {
  const cities = resolveCities(options.citySlugs);
  const scrapedAt = new Date(`${options.currentDate}T12:00:00.000Z`).toISOString();
  const jobsById = new Map();
  const skipped = [];
  const cityStats = [];
  const pageStats = [];

  for (const city of cities) {
    if (options.target > 0 && jobsById.size >= options.target) break;
    try {
      const stats = await crawlCity(city, options, jobsById, skipped, scrapedAt);
      cityStats.push({ city: stats.city, sourceTotalPages: stats.sourceTotalPages, pagesScanned: stats.pagesScanned });
      pageStats.push(...stats.pageStats);
    } catch (error) {
      skipped.push({ sourceName: `Apna - ${city.label}`, sourceUrl: cityUrl(city), reason: error.message });
      cityStats.push({ city: city.label, sourceTotalPages: 0, pagesScanned: 0, error: error.message });
    }
  }

  const jobs = Array.from(jobsById.values()).slice(0, options.target > 0 ? options.target : undefined);
  return {
    generatedAt: scrapedAt,
    mode: 'review',
    sourcesPath: cities.map(cityUrl).join(', '),
    requirePhone: true,
    freshnessRule: `Only Apna Telangana/AP jobs with public created_on/datePosted from ${options.freshDays} days before ${options.currentDate}, visible contact number in description, source salary, source coordinates, source vacancy count, and source work type are included. Optional fields stay blank when Apna does not expose them.`,
    employerId: options.employerId,
    totals: {
      validJobs: jobs.length,
      skipped: skipped.length,
      citiesScanned: cityStats.length,
      pagesScanned: pageStats.length,
      uniqueFreshPhoneVisibleFound: jobsById.size
    },
    cityStats,
    jobs,
    skipped,
    pageStats
  };
}

function resolveRawCity(rawCity) {
  const configured = CITY_CONFIGS.find((city) => city.slug === rawCity.slug) || {};
  return {
    slug: rawCity.slug || configured.slug,
    label: rawCity.label || configured.label || rawCity.slug,
    state: rawCity.state || configured.state || '',
    allowedPathSlugs: rawCity.allowedPathSlugs || configured.allowedPathSlugs || [rawCity.slug]
  };
}

function crawlFromBrowserRaw(options) {
  const rawPath = resolvePath(options.fromBrowserRaw);
  const raw = readJson(rawPath);
  const scrapedAt = raw.generatedAt || new Date(`${options.currentDate}T12:00:00.000Z`).toISOString();
  const pages = Array.isArray(raw.pages) ? raw.pages : [];
  const jobsById = new Map();
  const skipped = [];
  const pageStats = [];
  const cityStatsMap = new Map();

  for (const page of pages) {
    if (options.target > 0 && jobsById.size >= options.target) break;
    const city = resolveRawCity(page.city || {});
    const sourceJobs = Array.isArray(page.jobs) ? page.jobs : [];
    let freshCount = 0;
    let phoneVisibleCount = 0;
    let validCount = 0;

    for (const sourceJob of sourceJobs) {
      const isFresh = createdWithinFreshWindow(sourceJob.created_on, options.currentDate, options.freshDays);
      const contact = extractPublicPhones(sourceJob.description);
      if (isFresh) freshCount += 1;
      if (contact.phones.length > 0) phoneVisibleCount += 1;

      const reviewJob = buildReviewJob(sourceJob, city, options, scrapedAt);
      if (!reviewJob || reviewJob.skippedReason) {
        skipped.push({
          sourceName: `Apna - ${city.label}`,
          sourceUrl: reviewJob && reviewJob.sourceUrl ? reviewJob.sourceUrl : publicUrlForJob(sourceJob, city),
          title: sourceJob.title || '',
          reason: reviewJob ? reviewJob.skippedReason : 'invalidJob'
        });
        continue;
      }

      validCount += 1;
      if (!jobsById.has(reviewJob.jobId)) jobsById.set(reviewJob.jobId, reviewJob);
    }

    const cityStats = cityStatsMap.get(city.label) || { city: city.label, sourceTotalPages: page.totalPages || 0, pagesScanned: 0 };
    cityStats.sourceTotalPages = Math.max(cityStats.sourceTotalPages, page.totalPages || 0);
    cityStats.pagesScanned += 1;
    cityStatsMap.set(city.label, cityStats);

    pageStats.push({
      city: city.label,
      pageNumber: page.pageNumber || 0,
      jobs: sourceJobs.length,
      fresh: freshCount,
      phoneVisible: phoneVisibleCount,
      freshPhoneVisibleValid: validCount,
      uniqueValidTotal: jobsById.size
    });
  }

  const jobs = Array.from(jobsById.values()).slice(0, options.target > 0 ? options.target : undefined);
  return {
    generatedAt: scrapedAt,
    mode: 'review',
    sourcesPath: rawPath,
    requirePhone: true,
    freshnessRule: `Only Apna Telangana/AP jobs from browser-collected public page data with public created_on/datePosted from ${options.freshDays} days before ${options.currentDate}, visible contact number in description, source salary, source coordinates, source vacancy count, and source work type are included. Optional fields stay blank when Apna does not expose them.`,
    employerId: options.employerId,
    totals: {
      validJobs: jobs.length,
      skipped: skipped.length,
      citiesScanned: cityStatsMap.size,
      pagesScanned: pageStats.length,
      uniqueFreshPhoneVisibleFound: jobsById.size
    },
    cityStats: Array.from(cityStatsMap.values()),
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

  const review = options.fromBrowserRaw ? crawlFromBrowserRaw(options) : await crawl(options);
  const outPath = resolvePath(options.reviewOut);
  writeJson(outPath, review);

  console.log(`Review file: ${outPath}`);
  console.log(`Valid fresh phone-visible exact-source jobs: ${review.totals.validJobs}`);
  console.log(`Unique found before output limit: ${review.totals.uniqueFreshPhoneVisibleFound}`);
  console.log(`Cities scanned: ${review.totals.citiesScanned}`);
  console.log(`Pages scanned: ${review.totals.pagesScanned}`);
  console.log(`Skipped rows/pages: ${review.totals.skipped}`);
  console.log('Dry-run only. No Firestore writes were made.');
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});