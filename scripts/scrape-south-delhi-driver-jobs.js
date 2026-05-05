'use strict';
/**
 * Scrapes JobHai + WorkIndia for DRIVER jobs in South Delhi (Delhi NCR)
 * posted within the last 7 days with a public contact number.
 *
 * Usage:
 *   node scrape-south-delhi-driver-jobs.js             -- scrape + write review JSON (dry-run)
 *   node scrape-south-delhi-driver-jobs.js --apply     -- scrape + write to Firestore
 *   node scrape-south-delhi-driver-jobs.js --fresh-days 14  -- extend freshness window
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const CURRENT_DATE = new Date().toISOString().split('T')[0]; // e.g. "2026-05-05"
const FRESH_DAYS = Number(process.argv.find((a, i) => process.argv[i - 1] === '--fresh-days') || 7);
const TARGET = 30;
const MAX_LIST_PAGES = 15;
const APPLY = process.argv.includes('--apply');
const EMPLOYER_ID = 'admin';

const DEFAULT_REVIEW_OUT = path.join(__dirname, 'output', `south-delhi-driver-review-${CURRENT_DATE}.json`);
const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const PHONE_TOKEN = /(?<!\d)(?:\+?91[\s().-]*)?[6-9](?:[\s().-]*\d){9}(?!\d)/g;
const PHONE_LINE_HINT = /call|phone|contact|mobile|hr|whats|resume|cv|apply|interested|share/i;
const GEOHASH_BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

// ─── City config ────────────────────────────────────────────────────────────────
const DELHI = { slug: 'delhi', label: 'Delhi', state: 'Delhi', lat: 28.6139, lng: 77.2090 };

const SOUTH_DELHI_AREAS = {
  'defence colony':    { lat: 28.5705, lng: 77.2322 },
  'lajpat nagar':      { lat: 28.5686, lng: 77.2432 },
  'greater kailash':   { lat: 28.5458, lng: 77.2410 },
  'greater kailash 1': { lat: 28.5458, lng: 77.2410 },
  'greater kailash 2': { lat: 28.5363, lng: 77.2397 },
  'gk1':               { lat: 28.5458, lng: 77.2410 },
  'gk2':               { lat: 28.5363, lng: 77.2397 },
  'nehru place':       { lat: 28.5494, lng: 77.2512 },
  'saket':             { lat: 28.5253, lng: 77.2070 },
  'malviya nagar':     { lat: 28.5270, lng: 77.1970 },
  'hauz khas':         { lat: 28.5494, lng: 77.1993 },
  'vasant kunj':       { lat: 28.5144, lng: 77.1498 },
  'vasant vihar':      { lat: 28.5735, lng: 77.1519 },
  'green park':        { lat: 28.5565, lng: 77.2050 },
  'haus khas':         { lat: 28.5494, lng: 77.1993 },
  'panchsheel':        { lat: 28.5427, lng: 77.2141 },
  'south extension':   { lat: 28.5714, lng: 77.2249 },
  'kalkaji':           { lat: 28.5369, lng: 77.2590 },
  'govindpuri':        { lat: 28.5315, lng: 77.2587 },
  'tughlakabad':       { lat: 28.4979, lng: 77.2750 },
  'okhla':             { lat: 28.5406, lng: 77.2723 },
  'east of kailash':   { lat: 28.5480, lng: 77.2595 },
  'andrews ganj':      { lat: 28.5736, lng: 77.2224 },
  'moolchand':         { lat: 28.5697, lng: 77.2445 },
  'jangpura':          { lat: 28.5816, lng: 77.2440 },
};

// Driver category slugs for listing pages
const DRIVER_CATEGORIES_WORKINDIA = ['driver', 'delivery'];
const DRIVER_SEARCH_TERMS = ['driver', 'car driver', 'cab driver', 'personal driver', 'delivery driver', 'school van driver', 'office driver'];

// ─── Helpers ────────────────────────────────────────────────────────────────────
function sha1(input) { return crypto.createHash('sha1').update(input).digest('hex'); }

function decodeHtmlEntities(value) {
  return String(value || '')
    .replace(/&nbsp;/gi, ' ').replace(/&amp;/gi, '&').replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'").replace(/&#x27;/gi, "'").replace(/&lt;/gi, '<').replace(/&gt;/gi, '>')
    .replace(/&#(\d+);/g, (_, c) => String.fromCharCode(Number(c)))
    .replace(/&#x([0-9a-f]+);/gi, (_, c) => String.fromCharCode(Number.parseInt(c, 16)));
}

function htmlToLines(html) {
  return decodeHtmlEntities(String(html || '')
    .replace(/<script\b[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style\b[\s\S]*?<\/style>/gi, ' ')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/(p|div|section|article|li|h[1-6])>/gi, '\n')
    .replace(/<[^>]+>/g, ' '))
    .split(/\r?\n/)
    .map((l) => l.replace(/[ \t]+/g, ' ').trim())
    .filter(Boolean);
}

function truncate(value, max) {
  const text = String(value || '').replace(/[ \t\r]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
  return text.length <= max ? text : text.slice(0, max - 1).trimEnd();
}

function normalizePhone(value) {
  const d = String(value || '').replace(/\D/g, '');
  if (d.length === 10 && /^[6-9]/.test(d)) return d;
  if (d.length === 12 && d.startsWith('91') && /^[6-9]/.test(d.slice(2))) return d.slice(2);
  return '';
}

function extractPublicPhone(lines) {
  for (const line of lines) {
    const phones = (String(line).match(PHONE_TOKEN) || []).map(normalizePhone).filter(Boolean);
    if (!phones.length) continue;
    if (PHONE_LINE_HINT.test(line) || line.replace(/\D/g, '').length <= 12) return { phone: phones[0], evidence: line };
  }
  return { phone: '', evidence: '' };
}

function encodeGeohash(lat, lng, precision = 6) {
  let latMin = -90, latMax = 90, lonMin = -180, lonMax = 180;
  let hash = '', isEven = true, bit = 0, ch = 0;
  while (hash.length < precision) {
    if (isEven) { const m = (lonMin + lonMax) / 2; if (lng > m) { ch |= 1 << (4 - bit); lonMin = m; } else lonMax = m; }
    else { const m = (latMin + latMax) / 2; if (lat > m) { ch |= 1 << (4 - bit); latMin = m; } else latMax = m; }
    isEven = !isEven;
    if (bit < 4) bit++; else { hash += GEOHASH_BASE32[ch]; bit = 0; ch = 0; }
  }
  return hash;
}

function parseSalary(value) {
  const text = String(value || '').replace(/,/g, '');
  const range = text.match(/(?:Rs\.?|INR|₹)?\s*(\d{4,6})\s*(?:-|to|–|—)\s*(?:Rs\.?|INR|₹)?\s*(\d{4,6})/i);
  if (range) return { salary: `${range[1]}-${range[2]}`, salaryType: 'MONTHLY' };
  const single = text.match(/(?:Rs\.?|INR|₹)\s*(\d{4,6})/i) || text.match(/\b(\d{4,6})\s*\/month/i);
  if (single) return { salary: single[1], salaryType: 'MONTHLY' };
  return { salary: '', salaryType: '' };
}

function parsePostedWorkindia(lines, currentDate) {
  const joined = lines.join('\n');
  const explicit = joined.match(/Posted\s+on\s*:?\s*(\d{1,2})\/(\d{1,2})\/(\d{4})/i);
  if (explicit) {
    const posted = new Date(Date.UTC(Number(explicit[3]), Number(explicit[2]) - 1, Number(explicit[1])));
    const current = new Date(`${currentDate}T00:00:00.000Z`);
    const daysAgo = Math.floor((current - posted) / 86400000);
    return { iso: posted.toISOString(), daysAgo, label: `Posted on: ${explicit[1]}/${explicit[2]}/${explicit[3]}` };
  }
  const relative = joined.match(/Posted\s+(\d+)\s+days?\s+ago/i);
  if (relative) {
    const daysAgo = Number(relative[1]);
    const posted = new Date(new Date(`${currentDate}T00:00:00.000Z`) - daysAgo * 86400000);
    return { iso: posted.toISOString(), daysAgo, label: relative[0] };
  }
  if (/Posted\s+(today|few hours|\d+\s+hours?)/i.test(joined)) return { iso: `${currentDate}T00:00:00.000Z`, daysAgo: 0, label: 'Posted today' };
  return { iso: '', daysAgo: null, label: '' };
}

function parsePostedJobhai(lines, currentDate) {
  const line = lines.find((l) => /^Posted\s+/i.test(l)) || '';
  if (!line) return { iso: '', daysAgo: null, label: '' };
  if (/hour|minute|today/i.test(line)) return { iso: `${currentDate}T00:00:00.000Z`, daysAgo: 0, label: line };
  const m = line.match(/Posted\s+(\d+)\+?\s+days?\s+ago/i);
  if (m) {
    const daysAgo = Number(m[1]);
    const [y, mo, d] = currentDate.split('-').map(Number);
    const posted = new Date(Date.UTC(y, mo - 1, d - daysAgo));
    return { iso: posted.toISOString(), daysAgo, label: line };
  }
  return { iso: '', daysAgo: null, label: line };
}

function addDays(iso, days) {
  return new Date(new Date(iso || `${CURRENT_DATE}T00:00:00.000Z`).getTime() + days * 86400000).toISOString();
}

function firstLine(lines, regex) { return lines.find((l) => regex.test(l)) || ''; }

function resolveLocation(addressText) {
  const lower = String(addressText || '').toLowerCase();
  for (const [area, coords] of Object.entries(SOUTH_DELHI_AREAS)) {
    if (lower.includes(area)) return { lat: coords.lat, lng: coords.lng, source: 'area_lookup' };
  }
  return { lat: DELHI.lat, lng: DELHI.lng, source: 'city_center' };
}

function parseGender(lines) {
  const t = lines.join('\n');
  if (/female\s+only|only\s+female/i.test(t)) return 'Female';
  if (/male\s+only|only\s+male/i.test(t) && !/female\s+can\s+apply/i.test(t)) return 'Male';
  if (/both\s+male\s+and\s+female|male\s*\/\s*female/i.test(t)) return 'Both';
  return '';
}

function parseJobType(lines) {
  const t = lines.join('\n');
  if (/part\s*time/i.test(t)) return 'Part-time';
  if (/full\s*time/i.test(t)) return 'Full-time';
  if (/contract/i.test(t)) return 'Contract';
  return '';
}

function parseShift(lines) { return truncate(firstLine(lines, /\b\d{1,2}:?\d{0,2}\s*(?:AM|PM)\s*-\s*\d{1,2}:?\d{0,2}\s*(?:AM|PM)\b/i), 120); }
function parseEducation(lines) { return truncate(firstLine(lines, /(10th|12th|Graduate|Diploma|ITI|Below 10th|Pass and above)/i), 120); }
function parseExperience(lines) { return truncate(firstLine(lines, /(Freshers?|\d+\s*-\s*\d+\s+years?|\d+\+?\s+years?)\s+(?:in|Experience)?/i).replace(/\s+in\s+.+$/i, ''), 120); }

// ─── HTTP fetch ─────────────────────────────────────────────────────────────────
async function fetchText(url, attempt = 1) {
  const controller = new AbortController();
  const t = setTimeout(() => controller.abort(), 25000);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: { 'User-Agent': USER_AGENT, Accept: 'text/html,*/*;q=0.8', 'Accept-Language': 'en-IN,en;q=0.9' }
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.text();
  } catch (err) {
    if (attempt >= 2) throw new Error(`${err.message} — ${url}`);
    await new Promise((r) => setTimeout(r, 1500));
    return fetchText(url, attempt + 1);
  } finally { clearTimeout(t); }
}

// ─── WorkIndia parser ───────────────────────────────────────────────────────────
// Title tag format: "CompanyName company has vacancy of JobTitle in Location, City"
function parseWorkIndiaDetail(html, url, scrapedAt) {
  const lines = htmlToLines(html);
  const titleTag = decodeHtmlEntities((String(html).match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1] || '').replace(/\s+/g, ' ').trim();

  // Parse title tag: "Neeraj Travels company has vacancy of Ola / Uber Driver in Sector 50 - Faridabad, delhi"
  const tagMatch = titleTag.match(/^(.+?)\s+company\s+has\s+vacanc(?:y|ies)\s+of\s+(.+?)\s+in\s+(.+?)(?:,|$)/i);
  const companyName = truncate((tagMatch && tagMatch[1].trim()) || '', 120);
  const title = truncate((tagMatch && tagMatch[2].trim()) || '', 120);
  const addrFromTag = (tagMatch && tagMatch[3].trim()) || '';

  // Salary: "Rs. 15000" on page, or range from FAQ answer "provide a salary in the range of X INR to Y INR"
  const rawSalaryLine = firstLine(lines, /Rs\.\s*\d{4,6}/);
  const faqSalaryLine = firstLine(lines, /salary.*?\d{4,6}.*?INR.*?\d{4,6}/i);
  let salary = parseSalary(rawSalaryLine);
  if (!salary.salary) {
    const m = (faqSalaryLine || '').match(/(\d{4,6})\s+INR\s+to\s+(\d{4,6})\s+INR/i);
    if (m) salary = { salary: m[1] === m[2] ? m[1] : `${m[1]}-${m[2]}`, salaryType: 'MONTHLY' };
  }
  if (!salary.salary) {
    const m = rawSalaryLine.match(/Rs\.?\s*(\d{4,6})/i);
    if (m) salary = { salary: m[1], salaryType: 'MONTHLY' };
  }

  const phone = extractPublicPhone(lines); // WorkIndia hides phone behind app; will be empty — that's OK
  const posted = parsePostedWorkindia(lines, CURRENT_DATE);

  // Address: prefer a line containing "Delhi" from page, else from title tag
  const addressText = truncate(firstLine(lines, /\bDelhi\b/i) || addrFromTag || 'Delhi', 300);
  const location = resolveLocation(addressText);

  // Job type: "Full time" line appears after "Employment Type" heading
  const empTypeIdx = lines.findIndex((l) => /^Employment Type$/i.test(l));
  const jobType = parseJobType(lines) || (empTypeIdx >= 0 ? (lines[empTypeIdx + 1] || '') : '') || 'Full-time';

  // Description: "More info about ..." section up to "Employment Type"
  const moreInfoIdx = lines.findIndex((l) => /^More info about/i.test(l));
  const description = moreInfoIdx >= 0
    ? truncate(lines.slice(moreInfoIdx, empTypeIdx > moreInfoIdx ? empTypeIdx : moreInfoIdx + 12).join('\n'), 5000)
    : truncate([title, `Company: ${companyName}`, `Salary: ${salary.salary}`, `Location: ${addressText}`].filter(Boolean).join('\n'), 5000);

  const errors = [];
  if (!title || title.length < 3) errors.push('missingTitle');
  if (!companyName) errors.push('missingCompanyName');
  if (!salary.salary) errors.push('missingSalary');
  // Phone is NOT required — WorkIndia hides it behind their app
  if (!posted.iso || posted.daysAgo == null || posted.daysAgo > FRESH_DAYS) errors.push('notFresh');
  if (errors.length) return { errors, title, sourceUrl: url };

  const id = (url.match(/-(\d+)\/?$/) || [])[1] || sha1(url).slice(0, 16);
  const jobId = `workindia_${id}`;
  const hash = sha1([url, title, companyName, posted.iso].join('|').toLowerCase());
  const vacancies = 1; // WorkIndia doesn't expose vacancy count on public pages
  return buildJob({ jobId, source: 'workindia', title, companyName, salary, phone, posted, vacancies, jobType, addressText, location, description, lines, hash, url, scrapedAt });
}

// ─── JobHai parser (kept for future use — currently JobHai Delhi pages are JS-rendered) ──────────
function parseJobHaiDetail(html, url, scrapedAt) {
  const lines = htmlToLines(html);
  const titleTag = decodeHtmlEntities((String(html).match(/<title[^>]*>([\s\S]*?)<\/title>/i) || [])[1] || '');
  const fromTitle = titleTag.match(/^(.+?)\s+Job\s+in\s+(.+?)\s+in\s+(.+?)(?:\s+-|$)/i);
  const hireIdx = lines.findIndex((l) => /Hire Local Staff/i.test(l));
  const title = truncate((fromTitle && fromTitle[1]) || (hireIdx >= 0 ? lines[hireIdx + 1] : '') || '', 120);
  const companyName = truncate((fromTitle && fromTitle[2]) || '', 120);
  const titleLoc = (fromTitle && fromTitle[3]) || '';
  const addressText = truncate(firstLine(lines, /,\s*Delhi/i) || titleLoc || 'Delhi', 300);
  const salaryLine = firstLine(lines, /(?:Rs\.?|INR|₹|\d)\s*[\d,]+\s*(?:-|to|–|—)\s*(?:Rs\.?|INR|₹)?\s*[\d,]+\s*\/month/i);
  const salary = parseSalary(salaryLine || (lines.find((l) => l === 'Salary') ? lines[lines.findIndex((l) => l === 'Salary') + 1] : ''));
  const phone = extractPublicPhone(lines);
  const posted = parsePostedJobhai(lines, CURRENT_DATE);
  const vacMatch = (lines.join('\n').match(/(\d{1,2})\s+Openings?/i));
  const vacancies = vacMatch ? Math.min(50, Math.max(1, Number(vacMatch[1]))) : 0;
  const jobType = parseJobType(lines);
  const location = resolveLocation(addressText);
  const descStart = lines.findIndex((l) => /^Job Description$/i.test(l));
  const descEnd = lines.findIndex((l, i) => i > descStart && /^Other Details$/i.test(l));
  const description = descStart >= 0 ? truncate(lines.slice(descStart + 1, descEnd > descStart ? descEnd : descStart + 18).join('\n'), 5000) : '';

  const errors = [];
  if (!title || title.length < 3) errors.push('missingTitle');
  if (!companyName) errors.push('missingCompanyName');
  if (!salary.salary) errors.push('missingSalary');
  if (!phone.phone) errors.push('missingPublicPhone');
  if (!posted.iso || posted.daysAgo == null || posted.daysAgo > FRESH_DAYS) errors.push('notFresh');
  if (!vacancies) errors.push('missingVacancies');
  if (!jobType) errors.push('missingWorkType');
  if (!description) errors.push('missingDescription');
  if (errors.length) return { errors, title, sourceUrl: url };

  const id = (url.match(/-(\d+)-jid/) || url.match(/-(\d+)$/) || [])[1] || sha1(url).slice(0, 16);
  const jobId = `jobhai_${id}`;
  const hash = sha1([url, title, companyName, posted.iso].join('|').toLowerCase());
  return buildJob({ jobId, source: 'jobhai', title, companyName, salary, phone, posted, vacancies, jobType, addressText, location, description, lines, hash, url, scrapedAt });
}

function buildJob({ jobId, source, title, companyName, salary, phone, posted, vacancies, jobType, addressText, location, description, lines, hash, url, scrapedAt }) {
  const warnings = [`phoneFoundIn${source}PublicPage`];
  if (location.source !== 'area_lookup') warnings.push('locationDerivedFromCityCenter');
  return {
    approved: false,
    jobId,
    sourceName: `${source === 'workindia' ? 'WorkIndia' : 'JobHai'} – ${companyName}`,
    sourceUrl: url,
    warnings,
    valid: true,
    meta: {
      employerId: EMPLOYER_ID,
      companyName,
      title,
      salary: salary.salary,
      salaryType: salary.salaryType,
      location: { lat: location.lat, lng: location.lng },
      geohash: encodeGeohash(location.lat, location.lng, 6),
      addressText,
      jobType,
      status: 'open',
      createdAt: posted.iso,
      vacancies,
    },
    details: {
      employerId: EMPLOYER_ID,
      createdAt: posted.iso,
      expiresAt: addDays(posted.iso, 30),
      idempotencyKey: `${source}:${jobId}:${hash.slice(0, 48)}`,
      description,
      contactNumber: phone.phone,
      gender: parseGender(lines),
      experienceRequired: parseExperience(lines),
      educationRequired: parseEducation(lines),
      companyCity: 'Delhi',
      shiftTiming: parseShift(lines),
      applicationCount: 0,
    },
    audit: {
      jobId,
      sourceUrl: url,
      sourceType: `${source}-public-detail-page`,
      scrapedAt,
      sourceDatePosted: posted.label,
      freshnessWindow: `within_${FRESH_DAYS}_days_of_${CURRENT_DATE}`,
      contactEvidence: truncate(phone.evidence, 500),
      locationSource: location.source,
    },
  };
}

// ─── Link extraction ─────────────────────────────────────────────────────────────
function extractWorkIndiaLinks(html, base) {
  const links = [];
  const regex = /href=["']([^"']*\/jobs\/[^"']+)["']/gi;
  let m;
  while ((m = regex.exec(html)) !== null) {
    const url = new URL(m[1], base).toString().replace(/[?#].*$/, '');
    if (/\/jobs\/jobs-in-/i.test(url)) continue;
    links.push(url.endsWith('/') ? url : `${url}/`);
  }
  return links;
}

function extractJobHaiLinks(html, base) {
  const links = [];
  const regex = /href=["']([^"']*(?:-job-in-|job-in-)[^"']*-jid[^"']*)["']/gi;
  let m;
  while ((m = regex.exec(html)) !== null) {
    const url = new URL(m[1], base).toString().replace(/[?#].*$/, '');
    if (url.includes('/jobs-in-')) continue;
    links.push(url);
  }
  return links;
}

// ─── Seed URLs ───────────────────────────────────────────────────────────────────
function workIndiaSeedUrls() {
  const urls = [`https://www.workindia.in/jobs-in-${DELHI.slug}/`];
  for (const t of DRIVER_SEARCH_TERMS) urls.push(`https://www.workindia.in/jobs-in-${DELHI.slug}/?search=${encodeURIComponent(t)}`);
  for (const c of DRIVER_CATEGORIES_WORKINDIA) urls.push(`https://www.workindia.in/${c}-jobs-in-${DELHI.slug}/`);
  return urls;
}

function jobHaiSeedUrls() {
  const urls = [`https://www.jobhai.com/jobs-in-${DELHI.slug}`];
  for (const c of DRIVER_CATEGORIES_JOBHAI) {
    urls.push(`https://www.jobhai.com/${c}-jobs-in-${DELHI.slug}`);
    urls.push(`https://www.jobhai.com/${c}-jobs-in-${DELHI.slug}-ccty`);
  }
  return urls;
}

function pagedUrl(base, page) {
  if (page === 1) return base;
  const u = new URL(base);
  u.searchParams.set('page', String(page));
  return u.toString();
}

// ─── Crawler ─────────────────────────────────────────────────────────────────────
async function collectLinks(seedUrls, extractor, sourceName, skipped) {
  const links = [];
  const seen = new Set();
  for (const seed of seedUrls) {
    let empty = 0;
    for (let page = 1; page <= MAX_LIST_PAGES; page++) {
      const url = pagedUrl(seed, page);
      try {
        const html = await fetchText(url);
        const found = extractor(html, url);
        let newCount = 0;
        for (const link of found) {
          if (seen.has(link)) continue;
          seen.add(link);
          links.push(link);
          newCount++;
          if (links.length >= 300) return links;
        }
        empty = newCount === 0 ? empty + 1 : 0;
        if (page > 2 && empty >= 2) break;
      } catch (err) {
        skipped.push({ source: sourceName, url, reason: err.message });
        if (page === 1 || page > 3) break;
      }
    }
  }
  return links;
}

async function crawlSource({ seedUrls, linkExtractor, detailParser, sourceName, skipped, jobsById }) {
  console.log(`\n🔍 Crawling ${sourceName} …`);
  const links = await collectLinks(seedUrls, linkExtractor, sourceName, skipped);
  console.log(`   ${links.length} detail links found`);
  const scrapedAt = new Date().toISOString();
  const DRIVER_RE = /\b(driver|delivery|courier|cab|chauffeur|despatch|dispatch)\b/i;
  let fetched = 0;
  for (const url of links) {
    if (jobsById.size >= TARGET) break;
    try {
      const html = await fetchText(url);
      fetched++;
      const parsed = detailParser(html, url, scrapedAt);
      if (parsed && parsed.valid) {
        if (!DRIVER_RE.test(parsed.meta.title)) {
          skipped.push({ source: sourceName, url, reason: 'notDriverCategory', title: parsed.meta.title });
        } else if (!jobsById.has(parsed.jobId)) {
          jobsById.set(parsed.jobId, parsed);
          process.stdout.write(`   ✅ [${jobsById.size}] ${parsed.meta.title} – ${parsed.meta.companyName} (📞 ${parsed.details.contactNumber || 'see WorkIndia app'})\n`);
        }
      } else if (parsed && parsed.errors) {
        skipped.push({ source: sourceName, url, reason: parsed.errors.join(', '), title: parsed.title });
      }
    } catch (err) {
      skipped.push({ source: sourceName, url, reason: err.message });
    }
  }
  console.log(`   Fetched ${fetched} details, ${jobsById.size} total valid so far`);
}

// ─── Firestore write ─────────────────────────────────────────────────────────────
async function applyToFirestore(jobs) {
  const keyFiles = ['serviceAccountKey.json', 'dutype-860ac-firebase-adminsdk.json'];
  let keyPath = null;
  for (const f of keyFiles) {
    for (const dir of [__dirname, path.join(__dirname, '..')]) {
      const p = path.join(dir, f);
      if (fs.existsSync(p)) { keyPath = p; break; }
    }
    if (keyPath) break;
  }
  if (!keyPath) throw new Error('Service account key not found in scripts/ or project root');

  const admin = require('firebase-admin');
  if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(require(keyPath)), projectId: 'dutype-860ac' });
  const db = admin.firestore();
  const ts = (iso) => admin.firestore.Timestamp.fromDate(new Date(iso));

  let ok = 0, fail = 0;
  for (const job of jobs) {
    try {
      const cardData = {
        ...job.meta,
        createdAt: ts(job.meta.createdAt),
        searchKeywords: [
          'driver', 'car driver', 'cab driver', 'personal driver', 'delivery driver', 'delhi', 'south delhi',
          ...job.meta.title.toLowerCase().split(/\W+/).filter((w) => w.length > 2),
          ...job.meta.companyName.toLowerCase().split(/\W+/).filter((w) => w.length > 2),
        ].filter((v, i, a) => a.indexOf(v) === i),
      };
      const detailsData = {
        ...job.details,
        createdAt: ts(job.details.createdAt),
        expiresAt: ts(job.details.expiresAt),
      };
      await db.collection('jobmetadata').doc(job.jobId).set(cardData);
      await db.collection('job_details').doc(job.jobId).set(detailsData);
      console.log(`  ✅ Written: ${job.jobId}`);
      ok++;
    } catch (err) {
      console.error(`  ❌ Failed ${job.jobId}: ${err.message}`);
      fail++;
    }
  }
  return { ok, fail };
}

// ─── Main ────────────────────────────────────────────────────────────────────────
async function main() {
  console.log(`\n🚗  South Delhi Driver Job Scraper`);
  console.log(`   Date: ${CURRENT_DATE}  |  Fresh window: ${FRESH_DAYS} days  |  Mode: ${APPLY ? '🔴 APPLY' : '🟡 DRY-RUN'}\n`);

  const skipped = [];
  const jobsById = new Map();

  await crawlSource({
    seedUrls: workIndiaSeedUrls(),
    linkExtractor: extractWorkIndiaLinks,
    detailParser: parseWorkIndiaDetail,
    sourceName: 'WorkIndia',
    skipped,
    jobsById,
  });

  // JobHai Delhi pages are JS-rendered and return 404 for static city pages; skipping for now
  console.log('\nℹ️  JobHai Delhi pages are JavaScript-rendered — skipping (no static HTML job links).');

  const jobs = Array.from(jobsById.values());

  // Write review file always
  const reviewPath = DEFAULT_REVIEW_OUT;
  fs.mkdirSync(path.dirname(reviewPath), { recursive: true });
  fs.writeFileSync(reviewPath, JSON.stringify({ generatedAt: new Date().toISOString(), currentDate: CURRENT_DATE, freshDays: FRESH_DAYS, employerId: EMPLOYER_ID, totals: { valid: jobs.length, skipped: skipped.length }, jobs, skipped }, null, 2), 'utf8');

  console.log(`\n📄 Review file: ${reviewPath}`);
  console.log(`✅ Valid jobs found: ${jobs.length}`);
  console.log(`⏭  Skipped: ${skipped.length}`);

  if (jobs.length === 0) {
    console.log('\n⚠️  No valid jobs scraped. WorkIndia/JobHai may have blocked the request or no driver jobs in Delhi posted within the freshness window.');
    console.log('   Try: --fresh-days 14');
    return;
  }

  if (APPLY) {
    console.log('\n📝 Writing to Firestore …');
    const { ok, fail } = await applyToFirestore(jobs);
    console.log(`\n🎉 Done. Written: ${ok}  Failed: ${fail}`);
  } else {
    console.log('\n📌 Dry-run complete. Run with --apply to post to Firestore.');
    console.log('   First review: ' + reviewPath);
  }
}

main().catch((err) => { console.error(err.stack || err.message); process.exit(1); });
