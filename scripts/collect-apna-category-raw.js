'use strict';

const fs = require('fs');
const path = require('path');

const DEFAULT_CURRENT_DATE = '2026-04-29';
const USER_AGENT = 'DutyPeApnaCategoryCollector/1.0 (+https://dutype.com)';

const CITY_CONFIGS = [
  { slug: 'hyderabad', label: 'Hyderabad', state: 'Telangana', allowedPathSlugs: ['hyderabad', 'secunderabad', 'hyderabad-region'] },
  { slug: 'khammam', label: 'Khammam', state: 'Telangana', allowedPathSlugs: ['khammam'] },
  { slug: 'vijayawada', label: 'Vijayawada', state: 'Andhra Pradesh', allowedPathSlugs: ['vijayawada'] },
  { slug: 'warangal', label: 'Warangal', state: 'Telangana', allowedPathSlugs: ['warangal', 'hanamkonda'] },
  { slug: 'karimnagar', label: 'Karimnagar', state: 'Telangana', allowedPathSlugs: ['karimnagar'] },
  { slug: 'kurnool', label: 'Kurnool', state: 'Andhra Pradesh', allowedPathSlugs: ['kurnool'] }
];

const DEFAULT_CATEGORIES = [
  'delivery_person',
  'field_sales',
  'telecalling_bpo_telesales',
  'retail',
  'business_operations',
  'driver',
  'office_help',
  'security_guard',
  'technician',
  'cook_chef',
  'housekeeping',
  'cashier',
  'warehouse_logistics'
];

function parseArgs(argv) {
  const args = {
    cities: CITY_CONFIGS.map((city) => city.slug),
    categories: DEFAULT_CATEGORIES,
    maxPages: 10,
    currentDate: DEFAULT_CURRENT_DATE,
    out: path.join(__dirname, 'output', 'apna-local-service-category-live-raw.json')
  };

  for (let index = 0; index < argv.length; index += 1) {
    const token = argv[index];
    const next = () => {
      index += 1;
      if (index >= argv.length) throw new Error(`Missing value after ${token}`);
      return argv[index];
    };
    switch (token) {
      case '--cities':
        args.cities = next().split(',').map((item) => item.trim().toLowerCase()).filter(Boolean);
        break;
      case '--categories':
        args.categories = next().split(',').map((item) => item.trim()).filter(Boolean);
        break;
      case '--max-pages':
        args.maxPages = Number.parseInt(next(), 10);
        break;
      case '--current-date':
        args.currentDate = next();
        break;
      case '--out':
        args.out = next();
        break;
      default:
        throw new Error(`Unknown option: ${token}`);
    }
  }

  if (!Number.isInteger(args.maxPages) || args.maxPages < 1) throw new Error('--max-pages must be a positive integer');
  return args;
}

function resolveCities(slugs) {
  const bySlug = new Map(CITY_CONFIGS.map((city) => [city.slug, city]));
  return slugs.map((slug) => {
    const city = bySlug.get(slug);
    if (!city) throw new Error(`Unknown city: ${slug}`);
    return city;
  });
}

function resolvePath(inputPath) {
  return path.isAbsolute(inputPath) ? inputPath : path.resolve(process.cwd(), inputPath);
}

function writeJson(filePath, value) {
  const outPath = resolvePath(filePath);
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  fs.writeFileSync(outPath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
  return outPath;
}

function pageUrl(city, category, pageNumber) {
  const url = new URL(`https://apna.co/jobs/jobs-in-${city.slug}`);
  url.searchParams.set('category', category);
  if (pageNumber > 1) url.searchParams.set('page', String(pageNumber));
  return url.toString();
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
    .replace(/&gt;/gi, '>');
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

function extractJobs(data) {
  const pageProps = data.props && data.props.pageProps ? data.props.pageProps : {};
  return {
    totalPages: Number.parseInt(String(pageProps.totalPages || 0), 10) || 0,
    jobs: (pageProps.jobs || []).map((item) => item.data || item).filter(Boolean)
  };
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const cities = resolveCities(options.cities);
  const pages = [];
  const errors = [];

  for (const city of cities) {
    for (const category of options.categories) {
      let consecutiveEmpty = 0;
      for (let pageNumber = 1; pageNumber <= options.maxPages; pageNumber += 1) {
        const url = pageUrl(city, category, pageNumber);
        try {
          const html = await fetchText(url);
          const data = extractNextData(html, url);
          const result = extractJobs(data);
          pages.push({ city, category, pageNumber, pageUrl: url, totalPages: result.totalPages, jobs: result.jobs });
          consecutiveEmpty = result.jobs.length ? 0 : consecutiveEmpty + 1;
          if (pageNumber > 2 && consecutiveEmpty >= 2) break;
        } catch (error) {
          errors.push({ city: city.slug, category, pageNumber, url, error: error.message });
          if (pageNumber === 1 || pageNumber > 2) break;
        }
      }
    }
  }

  const output = {
    generatedAt: new Date(`${options.currentDate}T12:00:00.000Z`).toISOString(),
    sourceType: 'apna-live-category-next-data',
    currentDate: options.currentDate,
    freshDays: 7,
    pages,
    errors
  };
  const outPath = writeJson(options.out, output);
  const totalJobs = pages.reduce((sum, page) => sum + page.jobs.length, 0);
  console.log(`Raw file: ${outPath}`);
  console.log(`Pages: ${pages.length}`);
  console.log(`Jobs in pages: ${totalJobs}`);
  console.log(`Errors: ${errors.length}`);
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
