'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const DEFAULT_CURRENT_DATE = '2026-04-29';
const DEFAULT_FRESH_DAYS = 7;
const DEFAULT_TARGET = 500;
const DEFAULT_MAX_LIST_PAGES = 35;
const DEFAULT_MAX_DETAILS = 5000;
const DEFAULT_EMPLOYER_ID = process.env.DUTYPE_IMPORT_EMPLOYER_ID || 'admin1';
const DEFAULT_REVIEW_OUT = path.join(__dirname, 'output', 'telangana-ap-fresh-phone-verified-jobhai-review.json');
const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

const PHONE_TOKEN = /(?<!\d)(?:\+?91[\s().-]*)?[6-9](?:[\s().-]*\d){9}(?!\d)/g;
const PHONE_LINE_HINT = /call|phone|contact|mobile|hr|whats|resume|apply|interested/i;
const GEOHASH_BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

const CITY_CONFIGS = [
  { slug: 'hyderabad', label: 'Hyderabad', state: 'Telangana', lat: 17.385044, lng: 78.486671 },
  { slug: 'vijayawada', label: 'Vijayawada', state: 'Andhra Pradesh', lat: 16.506174, lng: 80.648015 },
  { slug: 'khammam', label: 'Khammam', state: 'Telangana', lat: 17.247253, lng: 80.151445 },
  { slug: 'warangal', label: 'Warangal', state: 'Telangana', lat: 17.968901, lng: 79.594055 },
  { slug: 'visakhapatnam', label: 'Visakhapatnam', state: 'Andhra Pradesh', lat: 17.686816, lng: 83.218482 },
  { slug: 'guntur', label: 'Guntur', state: 'Andhra Pradesh', lat: 16.306652, lng: 80.43654 },
  { slug: 'tirupati', label: 'Tirupati', state: 'Andhra Pradesh', lat: 13.628756, lng: 79.419179 },
  { slug: 'kakinada', label: 'Kakinada', state: 'Andhra Pradesh', lat: 16.989065, lng: 82.247465 },
  { slug: 'karimnagar', label: 'Karimnagar', state: 'Telangana', lat: 18.438555, lng: 79.128841 },
  { slug: 'nizamabad', label: 'Nizamabad', state: 'Telangana', lat: 18.672505, lng: 78.094087 },
  { slug: 'nellore', label: 'Nellore', state: 'Andhra Pradesh', lat: 14.442599, lng: 79.986456 },
  { slug: 'kurnool', label: 'Kurnool', state: 'Andhra Pradesh', lat: 15.828126, lng: 78.037279 }
];

const CATEGORY_SLUGS = [
  'delivery',
  'driver',
  'peon',
  'security-guard',
  'warehouse-logistics',
  'field-sales',
  'customer-support-telecaller',
  'receptionist',
  'back-office',
  'data-entry',
  'housekeeping',
  'retail-counter-sales',
  'cook-chef',
  'teacher',
  'beautician',
  'technician',
  'recruiter-hr-admin'
];

const HYDERABAD_AREA_COORDINATES = {
  'a s raju nagar': { lat: 17.4988, lng: 78.4006 },
  'ag colony': { lat: 17.4523, lng: 78.4307 },
  'ameerpet': { lat: 17.4375, lng: 78.4483 },
  'attapur': { lat: 17.3688, lng: 78.4277 },
  'banjara hills': { lat: 17.4156, lng: 78.4347 },
  'begumpet': { lat: 17.4435, lng: 78.4677 },
  'chanda nagar': { lat: 17.493, lng: 78.331 },
  'dilsukhnagar': { lat: 17.3688, lng: 78.5247 },
  'gachibowli': { lat: 17.4401, lng: 78.3489 },
  'hi-tech city': { lat: 17.4483, lng: 78.3915 },
  'hitech city': { lat: 17.4483, lng: 78.3915 },
  'jubilee hills': { lat: 17.4352, lng: 78.4022 },
  'kukatpally': { lat: 17.4849, lng: 78.4138 },
  'madhapur': { lat: 17.4486, lng: 78.3908 },
  'nanakramguda': { lat: 17.4169, lng: 78.3428 },
  'narayanguda': { lat: 17.3956, lng: 78.4895 },
  'rasoolpura': { lat: 17.4447, lng: 78.4827 },
  'somajiguda': { lat: 17.4239, lng: 78.4582 },
  'uppal': { lat: 17.4065, lng: 78.5593 }
};

function printHelp() {
  console.log(`JobHai public job scraper

Usage:
  node scrape-jobhai-public-jobs.js --target 500

Options:
  --review-out <path>       Output review JSON path.
  --cities <csv>            City slugs to scan. Default: all configured Telangana/AP cities.
  --target <count>          Stop output at this many valid jobs. Default: ${DEFAULT_TARGET}
  --current-date <date>     Freshness reference date. Default: ${DEFAULT_CURRENT_DATE}
  --fresh-days <count>      Include jobs posted within this many days. Default: ${DEFAULT_FRESH_DAYS}
  --max-list-pages <count>  Category pages to scan per seed. Default: ${DEFAULT_MAX_LIST_PAGES}
  --max-details <count>     Maximum unique detail pages to fetch. Default: ${DEFAULT_MAX_DETAILS}
  --employer-id <id>        Employer/admin uid for review rows. Default: ${DEFAULT_EMPLOYER_ID}
  --help                    Show this help.
`);
}

function parseArgs(argv) {
  const args = {
    reviewOut: DEFAULT_REVIEW_OUT,
    citySlugs: [],
    target: DEFAULT_TARGET,
    currentDate: DEFAULT_CURRENT_DATE,
    freshDays: DEFAULT_FRESH_DAYS,
    maxListPages: DEFAULT_MAX_LIST_PAGES,
    maxDetails: DEFAULT_MAX_DETAILS,
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
      case '--cities':
        args.citySlugs = next().split(',').map((item) => item.trim().toLowerCase()).filter(Boolean);
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
      case '--max-list-pages':
        args.maxListPages = parsePositiveInt(next(), '--max-list-pages');
        break;
      case '--max-details':
        args.maxDetails = parsePositiveInt(next(), '--max-details');
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
  if (!Number.isInteger(parsed) || parsed < 0) throw new Error(`${optionName} must be a positive integer`);
  return parsed;
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function writeJson(filePath, value) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
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

async function fetchText(url, attempt = 1) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 25000);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': USER_AGENT,
        Accept: 'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-IN,en;q=0.9'
      }
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.text();
  } catch (error) {
    if (attempt >= 2) throw new Error(`${error.message} for ${url}`);
    return fetchText(url, attempt + 1);
  } finally {
    clearTimeout(timeout);
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

function htmlToLines(html) {
  return decodeHtmlEntities(
    String(html || '')
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
  return (String(line || '').match(PHONE_TOKEN) || []).map(normalizePhone).filter(Boolean);
}

function extractPublicPhone(lines) {
  for (const line of lines) {
    const phones = phonesFromLine(line);
    if (phones.length === 0) continue;
    if (PHONE_LINE_HINT.test(line) || line.replace(/\D/g, '').length <= 12) {
      return { phone: phones[0], evidence: line };
    }
  }
  return { phone: '', evidence: '' };
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
    if (bit < 4) bit += 1;
    else {
      geohash += GEOHASH_BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return geohash;
}

function parseSalary(value) {
  const text = String(value || '').replace(/,/g, '');
  const range = text.match(/(?:Rs\.?|INR|\u20b9)?\s*(\d{4,6})\s*(?:-|to|\u2013|\u2014)\s*(?:Rs\.?|INR|\u20b9)?\s*(\d{4,6})/i);
  if (range) return { salary: `${range[1]}-${range[2]}`, salaryType: 'MONTHLY' };
  const single = text.match(/(?:Rs\.?|INR|\u20b9)\s*(\d{4,6})/i) || text.match(/\b(\d{4,6})\s*\/month/i);
  if (single) return { salary: single[1], salaryType: 'MONTHLY' };
  return { salary: '', salaryType: '' };
}

function parsePosted(lines, currentDate) {
  const line = lines.find((item) => /^Posted\s+/i.test(item)) || '';
  if (!line) return { iso: '', daysAgo: null, label: '' };
  if (/hour|minute|today/i.test(line)) return { iso: `${currentDate}T00:00:00.000Z`, daysAgo: 0, label: line };
  const dayMatch = line.match(/Posted\s+(\d+)\+?\s+days?\s+ago/i);
  if (dayMatch) {
    const daysAgo = Number(dayMatch[1]);
    const [year, month, day] = currentDate.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day - daysAgo, 0, 0, 0));
    return { iso: date.toISOString(), daysAgo, label: line };
  }
  return { iso: '', daysAgo: null, label: line };
}

function addDaysIso(iso, days) {
  const date = new Date(iso || `${DEFAULT_CURRENT_DATE}T00:00:00.000Z`);
  return new Date(date.getTime() + days * 24 * 60 * 60 * 1000).toISOString();
}

function firstLineMatching(lines, regex) {
  return lines.find((line) => regex.test(line)) || '';
}

function valueAfter(lines, label) {
  const index = lines.findIndex((line) => line.toLowerCase() === label.toLowerCase());
  return index >= 0 ? (lines[index + 1] || '') : '';
}

function parseJobHaiTitle(html, lines, url) {
  const titleTag = decodeHtmlEntities((String(html).match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1] || '');
  const fromTitle = titleTag.match(/^(.+?)\s+Job\s+in\s+(.+?)\s+in\s+(.+?)(?:\s+-|$)/i);
  const hireIndex = lines.findIndex((line) => /Hire Local Staff/i.test(line));
  const title = (fromTitle && fromTitle[1]) || (hireIndex >= 0 ? lines[hireIndex + 1] : '') || '';
  const companyName = (fromTitle && fromTitle[2]) || '';
  const titleLocation = (fromTitle && fromTitle[3]) || '';
  return {
    title: truncate(title, 120),
    companyName: truncate(companyName, 120),
    titleLocation: truncate(titleLocation, 180),
    id: (url.match(/-(\d+)-jid(?:[/?#]|$)/) || url.match(/-(\d+)$/) || [])[1] || sha1(url).slice(0, 16)
  };
}

function parseGender(lines) {
  const text = lines.join('\n');
  if (/Females? Only/i.test(text)) return 'Female';
  if (/Males? Only/i.test(text)) return 'Male';
  if (/Both|Male\/Female|Any Gender/i.test(text)) return 'Both';
  return '';
}

function parseVacancies(lines) {
  const line = firstLineMatching(lines, /\b\d+\s+Openings?\b/i);
  const match = line.match(/(\d+)\s+Openings?/i);
  const value = match ? Number(match[1]) : 0;
  return Number.isInteger(value) && value >= 1 && value <= 50 ? value : 0;
}

function parseJobType(lines) {
  const joined = lines.join('\n');
  if (/Part Time/i.test(joined)) return 'Part-time';
  if (/Full Time/i.test(joined)) return 'Full-time';
  if (/Contract Job\s*\n\s*Yes/i.test(joined)) return 'Contract';
  return '';
}

function parseShift(lines) {
  const line = firstLineMatching(lines, /\b\d{1,2}:?\d{0,2}\s*(?:AM|PM)\s*-\s*\d{1,2}:?\d{0,2}\s*(?:AM|PM)\b/i);
  return truncate(line, 120);
}

function parseEducation(lines) {
  return truncate(firstLineMatching(lines, /(10th|12th|Graduate|Diploma|ITI|Below 10th|Pass and above)/i), 120);
}

function parseExperience(lines) {
  const titleLine = firstLineMatching(lines, /(Freshers?|\d+\s*-\s*\d+\s+years?|\d+\+?\s+years?)\s+(?:in|Experience)/i);
  return truncate(titleLine.replace(/\s+in\s+.+$/i, '').trim(), 120);
}

function parseAddressText(parsedTitle, lines, city) {
  const locationLine = firstLineMatching(lines, new RegExp(`,\\s*${city.label}`, 'i')) || parsedTitle.titleLocation;
  return truncate(locationLine || city.label, 300);
}

function resolveLocation(addressText, city) {
  const lower = String(addressText || '').toLowerCase();
  if (city.slug === 'hyderabad') {
    for (const [area, coords] of Object.entries(HYDERABAD_AREA_COORDINATES)) {
      if (lower.includes(area)) return { lat: coords.lat, lng: coords.lng, source: 'area_lookup' };
    }
  }
  return { lat: city.lat, lng: city.lng, source: 'city_center_lookup' };
}

function parseDescription(lines) {
  const start = lines.findIndex((line) => /^Job Description$/i.test(line));
  if (start < 0) return '';
  const end = lines.findIndex((line, index) => index > start && /^Other Details$/i.test(line));
  return truncate(lines.slice(start + 1, end > start ? end : start + 18).join('\n'), 5000);
}

function parseJobHaiDetail(html, url, city, options, scrapedAt) {
  const lines = htmlToLines(html);
  const parsedTitle = parseJobHaiTitle(html, lines, url);
  const salaryLine = firstLineMatching(lines, /(?:Rs\.?|INR|\u20b9|\d)\s*[\d,]+\s*(?:-|to|\u2013|\u2014)\s*(?:Rs\.?|INR|\u20b9)?\s*[\d,]+\s*\/month/i) || firstLineMatching(lines, /^Salary$/i);
  const salary = parseSalary(salaryLine === 'Salary' ? valueAfter(lines, 'Salary') : salaryLine);
  const phone = extractPublicPhone(lines);
  const posted = parsePosted(lines, options.currentDate);
  const vacancies = parseVacancies(lines);
  const jobType = parseJobType(lines);
  const addressText = parseAddressText(parsedTitle, lines, city);
  const location = resolveLocation(addressText, city);
  const description = parseDescription(lines);
  const gender = parseGender(lines);
  const shiftTiming = parseShift(lines);
  const educationRequired = parseEducation(lines);
  const experienceRequired = parseExperience(lines);
  const companyName = parsedTitle.companyName || lines[lines.findIndex((line) => line === salaryLine) + 1] || '';
  const warnings = ['phoneFoundInPublicDescriptionText'];
  if (location.source !== 'area_lookup') warnings.push('locationCoordinatesDerivedFromCity');
  if (!gender) warnings.push('sourceFieldMissing:gender');
  if (!shiftTiming) warnings.push('sourceFieldMissing:shiftTiming');
  if (!educationRequired) warnings.push('sourceFieldMissing:educationRequired');
  if (!experienceRequired) warnings.push('sourceFieldMissing:experienceRequired');

  const errors = [];
  if (!parsedTitle.title || parsedTitle.title.length < 3) errors.push('missingTitle');
  if (!companyName) errors.push('missingCompanyName');
  if (!salary.salary) errors.push('missingSalary');
  if (!phone.phone) errors.push('missingPublicPhone');
  if (!posted.iso || posted.daysAgo == null || posted.daysAgo > options.freshDays) errors.push('notFresh');
  if (!vacancies) errors.push('missingVacancies');
  if (!jobType) errors.push('missingWorkType');
  if (!description) errors.push('missingDescription');

  if (errors.length > 0) {
    return { errors, title: parsedTitle.title, sourceUrl: url };
  }

  const jobId = `jobhai_${parsedTitle.id}`;
  const hash = sha1([url, parsedTitle.title, companyName, posted.iso].join('|').toLowerCase());
  return {
    approved: false,
    jobId,
    sourceName: `JobHai - ${companyName}`,
    sourceUrl: url,
    warnings,
    valid: true,
    meta: {
      employerId: options.employerId,
      companyName: truncate(companyName, 120),
      title: parsedTitle.title,
      salary: salary.salary,
      salaryType: salary.salaryType,
      location: { lat: location.lat, lng: location.lng },
      geohash: encodeGeohash(location.lat, location.lng, 6),
      addressText,
      jobType,
      status: 'open',
      createdAt: posted.iso,
      vacancies
    },
    details: {
      employerId: options.employerId,
      createdAt: posted.iso,
      expiresAt: addDaysIso(posted.iso, 30),
      idempotencyKey: `jobhai:${parsedTitle.id}:${hash.slice(0, 48)}`,
      description,
      contactNumber: phone.phone,
      gender,
      experienceRequired,
      educationRequired,
      companyCity: city.label,
      shiftTiming,
      applicationCount: 0
    },
    audit: {
      jobId,
      sourceName: `JobHai - ${companyName}`,
      sourceUrl: url,
      sourceType: 'jobhai-public-detail-page',
      importedBy: 'scripts/scrape-jobhai-public-jobs.js',
      scrapedAt,
      sourceDatePosted: posted.label,
      sourceFreshness: `within_${options.freshDays}_days_of_${options.currentDate}`,
      sourceCity: city.label,
      sourceState: city.state,
      contactNumberSource: 'public_description_text',
      contactEvidence: truncate(phone.evidence, 500),
      locationSource: location.source,
      reviewRequired: true
    }
  };
}

function seedUrlsForCity(city) {
  const urls = [`https://www.jobhai.com/jobs-in-${city.slug}`];
  for (const category of CATEGORY_SLUGS) {
    urls.push(`https://www.jobhai.com/${category}-jobs-in-${city.slug}`);
    urls.push(`https://www.jobhai.com/${category}-jobs-in-${city.slug}-ccty`);
  }
  return urls;
}

function pagedUrl(seedUrl, pageNumber) {
  if (pageNumber === 1) return seedUrl;
  const parsed = new URL(seedUrl);
  parsed.searchParams.set('page', String(pageNumber));
  return parsed.toString();
}

function extractDetailLinks(html, baseUrl) {
  const links = [];
  const regex = /href=["']([^"']*(?:-job-in-|job-in-)[^"']*-jid[^"']*)["']/gi;
  let match;
  while ((match = regex.exec(html)) !== null) {
    const resolved = new URL(match[1], baseUrl).toString().replace(/[?#].*$/, '');
    if (resolved.includes('/jobs-in-')) continue;
    links.push(resolved);
  }
  return links;
}

async function collectDetailLinks(cities, options, skipped) {
  const links = [];
  const seen = new Set();
  for (const city of cities) {
    for (const seedUrl of seedUrlsForCity(city)) {
      let consecutiveEmpty = 0;
      for (let pageNumber = 1; pageNumber <= options.maxListPages; pageNumber += 1) {
        const listUrl = pagedUrl(seedUrl, pageNumber);
        try {
          const html = await fetchText(listUrl);
          const found = extractDetailLinks(html, listUrl);
          let newLinks = 0;
          for (const link of found) {
            if (seen.has(link)) continue;
            seen.add(link);
            links.push({ url: link, city });
            newLinks += 1;
            if (links.length >= options.maxDetails) return links;
          }
          consecutiveEmpty = newLinks === 0 ? consecutiveEmpty + 1 : 0;
          if (pageNumber > 3 && consecutiveEmpty >= 2) break;
        } catch (error) {
          skipped.push({ sourceName: `JobHai - ${city.label}`, sourceUrl: listUrl, reason: error.message });
          if (pageNumber === 1) break;
          if (pageNumber > 3) break;
        }
      }
    }
  }
  return links;
}

async function crawl(options) {
  const cities = resolveCities(options.citySlugs);
  const scrapedAt = new Date(`${options.currentDate}T12:00:00.000Z`).toISOString();
  const skipped = [];
  const jobsById = new Map();
  const detailLinks = await collectDetailLinks(cities, options, skipped);
  let detailsFetched = 0;

  for (const item of detailLinks) {
    if (options.target > 0 && jobsById.size >= options.target) break;
    try {
      const html = await fetchText(item.url);
      detailsFetched += 1;
      const parsed = parseJobHaiDetail(html, item.url, item.city, options, scrapedAt);
      if (parsed && parsed.valid) {
        if (!jobsById.has(parsed.jobId)) jobsById.set(parsed.jobId, parsed);
      } else {
        skipped.push({ sourceName: `JobHai - ${item.city.label}`, sourceUrl: item.url, title: parsed.title || '', reason: parsed.errors.join(',') });
      }
    } catch (error) {
      skipped.push({ sourceName: `JobHai - ${item.city.label}`, sourceUrl: item.url, reason: error.message });
    }
  }

  const jobs = Array.from(jobsById.values());
  const byCity = {};
  for (const job of jobs) byCity[job.details.companyCity] = (byCity[job.details.companyCity] || 0) + 1;
  return {
    generatedAt: scrapedAt,
    mode: 'review',
    sourcesPath: 'JobHai public detail pages from Telangana/AP category/listing crawls',
    requirePhone: true,
    freshnessRule: `Only JobHai jobs with public posted age within ${options.freshDays} days of ${options.currentDate}, visible contact number in public description text, salary, openings, and work type are included. Coordinates are derived from public city/area text when JobHai does not expose lat/lng.`,
    employerId: options.employerId,
    totals: {
      validJobs: jobs.length,
      skipped: skipped.length,
      detailLinksFound: detailLinks.length,
      detailsFetched,
      byCity
    },
    jobs,
    skipped
  };
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
  console.log(`Valid fresh phone-visible JobHai jobs: ${review.totals.validJobs}`);
  console.log(`Detail links found: ${review.totals.detailLinksFound}`);
  console.log(`Details fetched: ${review.totals.detailsFetched}`);
  console.log(`Skipped rows/pages: ${review.totals.skipped}`);
  console.log(`By city: ${JSON.stringify(review.totals.byCity)}`);
  console.log('Dry-run only. No Firestore writes were made.');
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});