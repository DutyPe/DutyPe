const assert = require("node:assert/strict");
const { test } = require("node:test");
const { loadSource } = require("./load-source.cjs");

const marketplace = loadSource("lib/product/marketplace.ts");
const { productReturnPath } = loadSource("lib/product/profile.ts");
const discovery = loadSource("lib/public-site.ts");
const location = loadSource("lib/product/location.ts");
const { middleware } = loadSource("middleware.ts");
const { NextRequest } = require("next/server");
const { firebaseAuthErrorMessage } = loadSource("lib/firebase/auth-errors.ts");
const { normalizeSignInPhone } = loadSource("lib/firebase/account-actions.ts");
const { publicJobSummary, matchingJobSummary, emptyJobSearch, parseJobSearch, jobSearchParams, queryLiveJobPage } = loadSource("lib/jobs/public-listings.ts");

test("Android jobmetadata records appear in public city and nearby search without exposing private fields", () => {
  const record = { id: "android-live-job", data: {
    title: "Delivery partner", status: "open", category: "DELIVERY", companyName: "Test employer",
    salary: 18000, salaryType: "MONTHLY", addressText: "Madhapur, Hyderabad, Telangana",
    location: { lat: 17.44, lng: 78.39 }, createdAt: 1789600000000,
    description: "PRIVATE_DESCRIPTION", contactPhone: "PRIVATE_PHONE", employerId: "PRIVATE_ID"
  } };
  const summary = publicJobSummary(record);
  assert.ok(summary, "Current Android records must not require legacy isActive");
  assert.equal(summary.city, "Hyderabad");
  assert.equal(summary.payAmount, "18000");
  assert.equal(summary.payType, "MONTHLY");
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, city: "Hyderabad", area: "Madhapur" }));
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, location: { latitude: 17.44, longitude: 78.39, radiusKm: 5 } }));
  assert.doesNotMatch(JSON.stringify(summary), /PRIVATE_|17\.44|78\.39|addressText/);
});

test("canonical closed status cannot be overridden by an old active flag", () => {
  for (const status of ["closed", "filled", "expired", "cancelled", "draft", "pending"]) {
    assert.equal(publicJobSummary({ id: "closed-job", data: { title: "Closed role", status, isActive: true } }), null, status);
  }
});

test("canonical listing timestamps support seconds milliseconds and microseconds without false expiry", () => {
  const now = 1789600000000;
  for (const scale of [0.001, 1, 1000]) {
    const record = { id: "time-test", data: { title: "Helper", status: "open", createdAt: (now - 1000) * scale, expiresAt: (now + 1000) * scale } };
    const summary = publicJobSummary(record, now);
    assert.ok(summary);
    assert.equal(summary.postedAt, now - 1000);
    assert.equal(summary.expiresAt, now + 1000);
    assert.equal(publicJobSummary({ ...record, data: { ...record.data, expiresAt: (now - 1000) * scale } }, now), null);
  }
});

test("server reads Android collections, joins safe detail fields and suppresses stale legacy duplicates", async () => {
  const documents = {
    jobmetadata: Object.fromEntries(Array.from({ length: 25 }, (_, index) => [String(index).padStart(3, "0"), {
      title: `Driver ${index}`, status: "open", salary: "18000", salaryType: "MONTHLY",
      location: { lat: 17.44, lng: 78.39 }, addressText: "Madhapur, Hyderabad, Telangana, India", vacancies: 1
    }]).concat([["closed", { title: "Closed Android role", status: "closed" }]])),
    job_details: Object.fromEntries(Array.from({ length: 25 }, (_, index) => [String(index).padStart(3, "0"), {
      companyCity: "Telangana", description: "PRIVATE_FULL_DESCRIPTION", contactNumber: "PRIVATE_PHONE",
      shiftTiming: "Morning", expiresAt: Date.now() + 3600000
    }])),
    jobs: {
      "000": { title: "Stale legacy duplicate", jobId: "old-open-link", isActive: true, city: "Hyderabad" },
      closed: { title: "Do not revive closed Android role", jobId: "old-closed-link", isActive: true, city: "Hyderabad" },
      "legacy-only": { title: "Older website role", jobId: "old-legacy-link", isActive: true, city: "Hyderabad", payAmount: 20000 }
    }
  };
  const queried = [];
  const masks = [];
  const fullReads = [];
  const projectFields = (data, fields) => !data ? undefined : fields ? Object.fromEntries(fields.filter(field => Object.hasOwn(data, field)).map(field => [field, data[field]])) : data;
  const snapshot = (collection, id, fields) => ({
    id, exists: Object.hasOwn(documents[collection] || {}, id),
    data: () => projectFields(documents[collection]?.[id], fields)
  });
  function collection(name) {
    let filter;
    let after;
    let count = 100;
    let fields;
    const query = {
      where(field, operator, value) { assert.equal(operator, "=="); filter = { field, value }; return query; },
      orderBy() { return query; }, limit(value) { count = value; return query; },
      startAfter(value) { after = value; return query; }, select(...selected) { fields = selected; return query; },
      doc(id) { return { collection: name, id, get: async () => { fullReads.push({ collection: name, id }); return snapshot(name, id); } }; },
      async get() {
        queried.push({ collection: name, filter, fields });
        return { docs: Object.entries(documents[name] || {}).filter(([id, data]) => (!after || id > after) && (!filter || data[filter.field] === filter.value)).sort(([left], [right]) => left.localeCompare(right)).slice(0, count).map(([id]) => snapshot(name, id, fields)) };
      }
    };
    return query;
  }
  const server = loadSource("lib/jobs/server.ts", {
    "server-only": {}, "next/cache": { unstable_cache: callback => callback },
    "@/lib/firebase/admin-server": { isFirebaseAdminConfigured: () => true, getFirebaseAdminApp: () => ({}) },
    "firebase-admin/firestore": { FieldPath: { documentId: () => "__name__" }, getFirestore: () => ({ collection, getAll: async (...args) => {
      const options = args.pop();
      masks.push({ collection: args[0]?.collection, fields: options.fieldMask });
      return args.map(reference => snapshot(reference.collection, reference.id, options.fieldMask));
    } }) }
  });
  const first = await server.getLiveJobPage({ ...emptyJobSearch, city: "Hyderabad" });
  assert.equal(first.status, "ready");
  assert.equal(first.jobs.length, 20);
  assert.ok(first.nextCursor.startsWith("v2:"));
  const second = await server.getLiveJobPage({ ...emptyJobSearch, city: "Hyderabad" }, first.nextCursor);
  assert.equal(second.jobs.length, 6);
  assert.equal(second.nextCursor, null);
  const ids = [...first.jobs, ...second.jobs].map(job => job.id);
  assert.equal(new Set(ids).size, 26);
  assert.equal(ids.includes("closed"), false);
  assert.equal(first.jobs[0].city, "Hyderabad");
  assert.equal(first.jobs[0].payAmount, "18000");
  assert.doesNotMatch(JSON.stringify([first, second]), /PRIVATE_/);
  assert.deepEqual(queried[0].filter, { field: "status", value: "open" });
  assert.ok(queried.some(query => query.collection === "jobmetadata"));
  assert.ok(masks.filter(mask => mask.collection === "job_details").every(mask => mask.fields.join(",") === "companyCity,expiresAt"));
  const detail = await server.getJobRecord("000");
  assert.equal(detail.data.description, "PRIVATE_FULL_DESCRIPTION");
  assert.equal(detail.data.status, "open");
  const publicRecord = await server.getPublicJob("000");
  assert.equal(publicRecord.job.id, "000");
  assert.doesNotMatch(JSON.stringify(publicRecord), /PRIVATE_/);
  assert.equal((await server.getPublicJob("closed")).job, null);
  const privateReadsBeforeAliases = fullReads.filter(read => read.collection === "job_details").length;
  const closedAlias = await server.getPublicJob("old-closed-link");
  assert.equal(closedAlias.status, "ready");
  assert.equal(closedAlias.job, null, "A legacy alias must respect its migrated canonical closed record");
  const openAlias = await server.getPublicJob("old-open-link");
  assert.equal(openAlias.status, "ready");
  assert.equal(openAlias.job.id, "000");
  assert.equal(openAlias.job.title, "Driver 0");
  assert.equal(openAlias.job.payAmount, "18000");
  assert.doesNotMatch(JSON.stringify(openAlias), /PRIVATE_/);
  assert.equal(fullReads.filter(read => read.collection === "job_details").length, privateReadsBeforeAliases);
  assert.ok(masks.filter(mask => mask.collection === "job_details").every(mask => mask.fields.join(",") === "companyCity,expiresAt"));
  const aliasDetails = await server.getJobRecord("old-open-link");
  assert.equal(aliasDetails.id, "000");
  assert.equal(aliasDetails.data.description, "PRIVATE_FULL_DESCRIPTION");
  const legacyAlias = await server.getPublicJob("old-legacy-link");
  assert.equal(legacyAlias.status, "ready");
  assert.equal(legacyAlias.job.id, "legacy-only");
  assert.equal(legacyAlias.job.title, "Older website role");
  await assert.rejects(() => server.getLiveJobPage(emptyJobSearch, "v2:invalid"));
});

test("public jobs pagination finds matching cities beyond the first 48 records without gaps", async () => {
  const records = Array.from({ length: 145 }, (_, index) => ({ id: String(index).padStart(4, "0"), data: {
    title: "Delivery associate", isActive: true, city: index < 80 ? "Delhi" : "Hyderabad"
  } }));
  const read = async (after, limit) => records.filter((record) => !after || record.id > after).slice(0, limit);
  let cursor = null;
  const found = [];
  do {
    const page = await queryLiveJobPage(read, { ...emptyJobSearch, city: "Hyderabad" }, cursor);
    found.push(...page.jobs.map((job) => job.id));
    cursor = page.nextCursor;
  } while (cursor);
  assert.equal(found.length, 65);
  assert.equal(new Set(found).size, 65);
  assert.equal(found[0], "0080");
  assert.equal(found.at(-1), "0144");
});

test("bounded job scanning returns continuation instead of claiming no jobs exist", async () => {
  const records = Array.from({ length: 510 }, (_, index) => ({ id: String(index).padStart(4, "0"), data: {
    title: "Driver", isActive: true, city: index < 505 ? "Delhi" : "Hyderabad"
  } }));
  const read = async (after, limit) => records.filter((record) => !after || record.id > after).slice(0, limit);
  const first = await queryLiveJobPage(read, { ...emptyJobSearch, city: "Hyderabad" });
  assert.equal(first.jobs.length, 0);
  assert.equal(first.scanned, 500);
  assert.equal(first.nextCursor, "0499");
  const second = await queryLiveJobPage(read, { ...emptyJobSearch, city: "Hyderabad" }, first.nextCursor);
  assert.equal(second.jobs.length, 5);
  assert.equal(second.nextCursor, null);
});

test("job filters validate coordinates and never put precise location into shareable search URLs", () => {
  const filters = parseJobSearch({ q: " driver ", city: "Hyderabad", location: { latitude: 17.44, longitude: 78.39, radiusKm: 5 } });
  assert.equal(filters.query, "driver");
  assert.equal(jobSearchParams(filters).toString(), "q=driver&city=Hyderabad");
  for (const invalid of [{ q: [] }, { q: "x".repeat(121) }, { location: { latitude: 91, longitude: 78, radiusKm: 5 } }, { location: { latitude: 17, longitude: 78, radiusKm: 1000 } }]) {
    assert.throws(() => parseJobSearch(invalid));
  }
});

test("public live-job summaries exclude closed listings and never expose full detail fields", () => {
  const now = Date.UTC(2026, 8, 16);
  const record = { id: "live-job", data: {
    title: "Delivery associate", isActive: true, vacancies: 2, acceptedCount: 0,
    companyName: "Test Store", location: "Private work address, Hyderabad", category: "DELIVERY",
    description: "Private long description", contactNumber: "private-phone", employerId: "private-id",
    latitude: 17.44, longitude: 78.39, payAmount: 18000, createdAt: now - 1000, expiresAt: now + 1000
  } };
  const summary = publicJobSummary(record, now);
  assert.equal(summary.city, "Hyderabad");
  assert.equal(summary.payAmount, "18000");
  for (const field of ["description", "location", "contactNumber", "employerId", "latitude", "longitude"]) {
    assert.equal(Object.hasOwn(summary, field), false, field);
  }
  for (const fields of [{ isActive: false }, { isActive: undefined }, { isFilled: true }, { vacancies: 0 }, { acceptedCount: 2 }, { expiresAt: now }, { expiresAt: "invalid" }, { vacancyStatus: "closed" }]) {
    assert.equal(publicJobSummary({ ...record, data: { ...record.data, ...fields } }, now), null);
  }
});

test("live-job filtering honors city, area, category and explicit nearby coordinates", () => {
  const record = { id: "job", data: { title: "Delivery associate", companyName: "Test Store", category: "DELIVERY", isActive: true, city: "Hyderabad", area: "Madhapur", latitude: 17.44, longitude: 78.39 } };
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, city: "HYDERABAD", area: "Madhapur", query: "delivery" }));
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, city: "Delhi" }), null);
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, city: "Hydera" }), null);
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, query: "constructor" }), null);
  assert.equal(matchingJobSummary({ ...record, data: { ...record.data, location: "Warangal Road, Hyderabad" } }, { ...emptyJobSearch, city: "Warangal" }), null);
  assert.equal(publicJobSummary({ id: "old", data: { title: "Helper", isActive: true, location: "Delhi Road" } }).city, "");
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, location: { latitude: 17.441, longitude: 78.391, radiusKm: 5 } }));
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, location: { latitude: 28.6, longitude: 77.2, radiusKm: 5 } }), null);
  assert.equal(matchingJobSummary({ ...record, data: { ...record.data, latitude: null } }, { ...emptyJobSearch, location: { latitude: 17.44, longitude: 78.39, radiusKm: 5 } }), null);
});

test("phone sign-in normalizes Indian mobile numbers and explicit international numbers", () => {
  assert.equal(normalizeSignInPhone("90000 00000"), "+919000000000");
  assert.equal(normalizeSignInPhone("91 90000 00000"), "+919000000000");
  assert.equal(normalizeSignInPhone("+91 (90000) 00000"), "+919000000000");
  assert.equal(normalizeSignInPhone("+1 202 555 0123"), "+12025550123");
  for (const invalid of ["", "123", "letters", "+0912345678", "1234567890", "+" + "9".repeat(16)]) {
    assert.equal(normalizeSignInPhone(invalid), null, invalid);
  }
});

test("Next.js metadata routes have no competing public assets", () => {
  const fs = require("node:fs");
  const path = require("node:path");
  const root = path.resolve(__dirname, "..");

  for (const [route, source] of [["icon.svg", "icon.svg"], ["robots.txt", "robots.ts"], ["sitemap.xml", "sitemap.ts"]]) {
    assert.equal(fs.existsSync(path.join(root, "app", source)), true, `Keep the App Router owner for /${route}`);
    assert.equal(fs.existsSync(path.join(root, "public", route)), false, `A public copy conflicts with /${route} and causes HTTP 500`);
  }
});

test("robots metadata preserves public crawling, private route exclusions and sitemap discovery", () => {
  const robots = loadSource("app/robots.ts").default();
  assert.equal(robots.rules.userAgent, "*");
  assert.equal(robots.rules.allow, "/");
  assert.deepEqual(robots.rules.disallow, ["/admin/", "/api/", "/app/", "/app-redirect"]);
  assert.equal(robots.host, discovery.SITE_URL);
  assert.equal(robots.sitemap, `${discovery.SITE_URL}/sitemap.xml`);
});

test("supported location pages are indexable with self canonicals and included once in the sitemap", async () => {
  const sitemap = await loadSource("app/sitemap.ts", { "@/lib/jobs/server": { getDiscoverableJobs: async () => [] } }).default();
  const urls = sitemap.map((entry) => entry.url);
  assert.equal(new Set(urls).size, urls.length);
  for (const slug of discovery.getKnownLegacySlugs()) {
    const metadata = discovery.getPublicPageMetadata(slug);
    const page = discovery.resolveLegacyPage(slug);
    const expectedUrl = `${discovery.SITE_URL}/${slug}`;
    assert.equal(metadata.robots.index, true, slug);
    assert.equal(metadata.alternates.canonical, expectedUrl, slug);
    assert.equal(metadata.title, page.title);
    assert.equal(metadata.description, page.description);
    assert.equal(metadata.openGraph.url, expectedUrl);
    assert.ok(urls.includes(expectedUrl), slug);
  }
  assert.equal(urls.some((url) => url.includes("/app/") || url.includes("?")), false);
  assert.equal(sitemap.some((entry) => entry.lastModified), false, "Do not invent content-update timestamps");
});

test("location guides use truthful page schema and reject unsupported location URLs", () => {
  for (const slug of ["jobs-in-hyderabad", "driver-jobs-hyderabad"]) {
    const data = discovery.getPublicPageStructuredData(slug);
    assert.equal(data["@graph"][0]["@type"], "CollectionPage");
    assert.equal(data["@graph"][0].spatialCoverage.name, "Hyderabad");
    assert.equal(data["@graph"][1]["@type"], "BreadcrumbList");
    assert.equal(data["@graph"][1].itemListElement.at(-1).item, `${discovery.SITE_URL}/${slug}`);
    assert.doesNotMatch(JSON.stringify(data), /JobPosting|baseSalary|totalJobOpenings/);
  }
  for (const slug of ["jobs-in-unknown-city", "driver-jobs-unknown-city", "jobs-in-", "__proto__"]) {
    assert.equal(discovery.resolveLegacyPage(slug), null, slug);
  }
});

test("Firebase defaults match the Android project and explicit overrides require valid core fields", () => {
  const fs = require("node:fs");
  const path = require("node:path");
  const vm = require("node:vm");
  const ts = require("typescript");
  const compiled = ts.transpileModule(fs.readFileSync(path.join(__dirname, "../lib/firebase/config.ts"), "utf8"), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
  }).outputText;
  const evaluate = (env) => {
    const exports = {};
    vm.runInNewContext(compiled, { exports, process: { env } });
    return exports;
  };
  const variables = ["API_KEY", "AUTH_DOMAIN", "PROJECT_ID", "STORAGE_BUCKET", "MESSAGING_SENDER_ID", "APP_ID"];
  const configured = Object.fromEntries(variables.map((name) => [`NEXT_PUBLIC_FIREBASE_${name}`, `test-${name}`]));
  const android = JSON.parse(fs.readFileSync(path.join(__dirname, "../../app/google-services.json"), "utf8"));
  assert.equal(evaluate({}).hasFirebaseConfig, true);
  assert.equal(evaluate({}).firebaseConfig.projectId, android.project_info.project_id);
  assert.equal(typeof evaluate({}).firebaseConfig.apiKey, "string");
  assert.equal(evaluate(configured).hasFirebaseConfig, true);
  assert.equal(evaluate(configured).firebaseConfig.projectId, "test-PROJECT_ID");
  assert.equal(evaluate(configured).firebaseConfig.authDomain, "test-AUTH_DOMAIN");
  for (const variable of ["API_KEY", "AUTH_DOMAIN", "PROJECT_ID", "APP_ID"]) {
    assert.equal(evaluate({ ...configured, [`NEXT_PUBLIC_FIREBASE_${variable}`]: "  " }).hasFirebaseConfig, false, variable);
  }
  assert.equal(evaluate({ ...configured, NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET: "", NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID: "" }).hasFirebaseConfig, true);
});

test("Firebase auth errors distinguish rejected configuration from credentials without leaking response contents", () => {
  for (const code of ["auth/invalid-api-key", "auth/api-key-not-valid.-please-pass-a-valid-api-key.", "API_KEY_INVALID"]) {
    const message = firebaseAuthErrorMessage({ code, message: "sensitive-test-marker" });
    assert.match(message, /configuration/);
    assert.equal(message.includes("sensitive-test-marker"), false);
  }
  assert.match(firebaseAuthErrorMessage({ code: "auth/unauthorized-domain" }), /website address/);
  assert.match(firebaseAuthErrorMessage({ code: "auth/network-request-failed" }), /connection/);
  assert.equal(firebaseAuthErrorMessage({ code: "auth/user-not-found" }), firebaseAuthErrorMessage({ code: "auth/wrong-password" }));
});

test("every category and city combination resolves to an existing public page", () => {
  for (const category of discovery.jobDirectoryCategories) {
    for (const city of ["", ...discovery.jobDirectoryCities]) {
      const href = discovery.jobCategoryHref(category, city);
      assert.ok(discovery.resolveLegacyPage(href.slice(1)), href);
    }
  }
});

test("public job search normalizes city names and preserves honest empty results", () => {
  const results = discovery.getJobDiscovery("  driver jobs  ", " hyderabad ");
  assert.equal(results.city, "Hyderabad");
  assert.deepEqual(results.categories.map((item) => item.slug), ["driver"]);
  assert.equal(discovery.getJobDiscovery("missing-specialty-xyz", "Hyderabad").categories.length, 0);
  assert.equal(discovery.getJobDiscovery("", "Unknown City").city, "");
  assert.equal(discovery.getJobDiscovery("x".repeat(300)).query.length, 200);
});

test("legacy redirects preserve query parameters and do not redirect verification files", () => {
  for (const [legacy, current] of [["/privacy.html", "/privacy"], ["/jobs/index.html", "/jobs"], ["/admin/login.html", "/admin/login"], ["/driver-jobs-hyderabad.html", "/driver-jobs-hyderabad"]]) {
    const response = middleware(new NextRequest(`https://dutype.invalid${legacy}?source=test`));
    assert.equal(response.status, 308);
    assert.equal(response.headers.get("location"), `https://dutype.invalid${current}?source=test`);
  }
  for (const pathname of ["/googlef0148bf44dd14fa3.html", "/app/auth"]) {
    assert.equal(middleware(new NextRequest(`https://dutype.invalid${pathname}`)).headers.has("location"), false);
  }
});

test("location validation rejects missing or malformed coordinate pairs", () => {
  for (const [latitude, longitude] of [[undefined, 78], [17, undefined], ["not-a-number", 78], ["", 78], [NaN, 78], [Infinity, 78], [91, 78], [17, 181], [0, 0]]) {
    assert.equal(location.hasValidCoordinates(latitude, longitude), false, `${latitude},${longitude}`);
    assert.equal(location.directionsUrl(marketplace.normalizeProductJob("invalid-location", { latitude, longitude })), null);
  }
  for (const [latitude, longitude] of [[17.44, 78.39], [0, 78], [17, 0], ["17.44", "78.39"]]) {
    assert.equal(location.hasValidCoordinates(latitude, longitude), true);
  }
});

test("nearby sorting and radius filters exclude unknown locations", () => {
  const base = { label: "Test district", latitude: 17.44, longitude: 78.39 };
  const nearby = marketplace.normalizeProductJob("nearby", { latitude: 17.441, longitude: 78.391 });
  const far = marketplace.normalizeProductJob("far", { latitude: 18.44, longitude: 78.39 });
  const missing = marketplace.normalizeProductJob("missing", {});
  const located = location.attachJobDistances([far, missing, nearby], base);
  assert.deepEqual(located.map((entry) => entry.job.id), ["nearby", "far", "missing"]);
  assert.deepEqual(location.filterJobsByRadius(located, 5).map((entry) => entry.job.id), ["nearby"]);
  assert.equal(location.directionsUrl(missing), null);
});
const { getJobDiscovery, jobCategoryHref, jobDirectoryCategories, resolveLegacyPage } = loadSource("lib/public-site.ts");

test("job discovery matches useful keywords and preserves known cities", () => {
  const result = getJobDiscovery("  delivery jobs  ", " HYDERABAD ");
  assert.equal(result.query, "delivery jobs");
  assert.equal(result.city, "Hyderabad");
  assert.deepEqual(result.categories.map((category) => category.slug), ["delivery"]);
  assert.deepEqual(getJobDiscovery("part time").categories.map((category) => category.slug), ["part-time"]);
  assert.deepEqual(getJobDiscovery("cashier").categories.map((category) => category.slug), ["retail"]);
});

test("job discovery handles empty searches, unknown cities and no results", () => {
  assert.equal(getJobDiscovery("jobs").categories.length, jobDirectoryCategories.length);
  assert.equal(getJobDiscovery().categories.length, jobDirectoryCategories.length);
  assert.equal(getJobDiscovery("", "unknown-city").city, "");
  assert.equal(getJobDiscovery("unlisted-speciality").categories.length, 0);
  assert.equal(getJobDiscovery("a".repeat(250)).query.length, 200);
});

test("every job category links to an existing category and city page", () => {
  for (const category of jobDirectoryCategories) {
    for (const city of ["", "Hyderabad", "Bangalore"]) {
      const href = jobCategoryHref(category, city);
      assert.ok(resolveLegacyPage(href.slice(1)), href);
      if (city) assert.ok(href.endsWith(city.toLowerCase()), href);
    }
    assert.equal(jobCategoryHref(category, "//example.invalid"), `/${category.slug}-jobs`);
  }
});

test("signin return paths stay inside the authenticated role", () => {
  assert.equal(productReturnPath("WORKER", "/jobs/job-test"), "/jobs/job-test");
  assert.equal(productReturnPath("EMPLOYER", "/jobs/job-test"), "/jobs/job-test");
  assert.equal(productReturnPath("WORKER", "/app/worker/jobs/job-test?source=saved#details"), "/app/worker/jobs/job-test?source=saved#details");
  assert.equal(productReturnPath("EMPLOYER", "/app/employer/jobs/job-test/applications"), "/app/employer/jobs/job-test/applications");
  for (const invalid of [null, "", "https://example.invalid/", "//example.invalid/", "/app/employer", "/app/worker-other", "/app/worker/../../admin", "/app/worker/..%2f../admin", "/\\example.invalid/app/worker"]) {
    assert.equal(productReturnPath("WORKER", invalid), "/app/worker", String(invalid));
  }
});

test("job normalization preserves numeric and text pay without exposing invalid numbers", () => {
  for (const [payAmount, expected] of [[18000, "18000"], [0, "0"], [750.5, "750.5"], ["12,000 - 15,000", "12,000 - 15,000"], [undefined, ""], [NaN, ""], [Infinity, ""]]) {
    assert.equal(marketplace.normalizeProductJob("job-test", { payAmount }).payAmount, expected);
  }
});

test("only active, unfilled, unexpired jobs are available", () => {
  const normalize = (fields) => marketplace.normalizeProductJob("job-test", fields);
  assert.equal(marketplace.isLiveJob(normalize({})), true);
  assert.equal(marketplace.isLiveJob(normalize({ isActive: false })), false);
  assert.equal(marketplace.isLiveJob(normalize({ isFilled: true })), false);
  assert.equal(marketplace.isLiveJob(normalize({ vacancyStatus: "FILLED" })), false);
  assert.equal(marketplace.isLiveJob(normalize({ expiresAt: Date.now() - 1000 })), false);
  assert.equal(marketplace.isLiveJob(normalize({ expiresAt: Date.now() + 60000 })), true);
});

test("timestamp ordering does not mutate source results", () => {
  const records = [{ id: "older", createdAt: 1000 }, { id: "newer", createdAt: 2000 }];
  assert.deepEqual(marketplace.sortByTimestampDesc(records, "createdAt").map((record) => record.id), ["newer", "older"]);
  assert.deepEqual(records.map((record) => record.id), ["older", "newer"]);
});

test("employer edit window has a deterministic seven-day boundary", () => {
  const postedAt = Date.UTC(2026, 8, 1);
  const job = marketplace.normalizeProductJob("job-test", { postedAt });
  const deadline = postedAt + 7 * 24 * 60 * 60 * 1000;
  assert.equal(marketplace.canEditEmployerJob(job, deadline), true);
  assert.equal(marketplace.canEditEmployerJob(job, deadline + 1), false);
  assert.match(marketplace.employerJobEditRestrictionMessage(job, deadline + 1), /7 days/);
});

test("application actions follow the hiring lifecycle", () => {
  const statuses = ["PENDING", "UNDER_REVIEW", "ACCEPTED", "IN_PROGRESS", "REJECTED", "COMPLETED", "WITHDRAWN"];
  for (const status of statuses) {
    assert.equal(marketplace.canEmployerMoveToUnderReview(status), status === "PENDING");
    assert.equal(marketplace.canEmployerAcceptOrReject(status), ["PENDING", "UNDER_REVIEW"].includes(status));
    assert.equal(marketplace.canEmployerVerifyWork(status), status === "ACCEPTED");
    assert.equal(marketplace.canEmployerMarkWorkComplete(status), ["ACCEPTED", "IN_PROGRESS"].includes(status));
    assert.equal(marketplace.canEmployerRateWorker(status), status === "COMPLETED");
  }
  assert.equal(marketplace.normalizeApplicationStatus(" under_review "), "UNDER_REVIEW");
  assert.equal(marketplace.normalizeApplicationStatus(null), "PENDING");
});

test("profile completion and required fields agree on complete profiles", () => {
  const worker = {
    fullName: "Test Worker", email: "worker@example.invalid", phone: "9000000000",
    address: "Test location", skills: "Delivery", experience: "1 year", gender: "OTHER",
    dateOfBirth: "2000-01-01", profileImageUrl: "/test-avatar.png"
  };
  assert.equal(marketplace.workerProfileCompletion(worker), 100);
  assert.deepEqual(marketplace.missingWorkerFields(worker), []);
  assert.equal(marketplace.workerProfileCompletion(null), 0);
  assert.ok(marketplace.missingWorkerFields(null).length > 0);

  const employer = {
    companyName: "Test Company", industry: "Retail", contactPhone: "9000000000",
    businessAddress: "Test location", gender: "OTHER", dateOfBirth: "2000-01-01",
    contactEmail: "employer@example.invalid", companySize: "1-10"
  };
  assert.equal(marketplace.employerProfileCompletion(employer), 100);
  assert.deepEqual(marketplace.missingEmployerFields(employer), []);
  assert.equal(marketplace.employerProfileCompletion(null), 0);
});

test("full job details require a verified token before any record is read", async () => {
  let reads = 0;
  let verifies = 0;
  const { GET } = loadSource("app/api/jobs/[jobId]/route.ts", {
    "@/lib/firebase/admin-server": {
      isFirebaseAdminConfigured: () => true,
      getFirebaseAdminAuth: () => ({ verifyIdToken: async (token, revoked) => {
        verifies += 1;
        assert.equal(revoked, true);
        if (token === "anonymous-token") return { uid: "guest", firebase: { sign_in_provider: "anonymous" } };
        if (token === "service-error") throw Object.assign(new Error("PRIVATE_SERVER_ERROR"), { code: "app/invalid-credential" });
        if (token !== "synthetic-valid-token") throw Object.assign(new Error("Rejected"), { code: "auth/id-token-expired" });
        return { uid: "test-worker" };
      } })
    },
    "@/lib/jobs/server": { getJobRecord: async () => {
      reads += 1;
      return { id: "job", data: { title: "Delivery role", isActive: true, description: "Full role details", contactNumber: "never-return-this", location: "Exact work address" } };
    } }
  });
  const request = (authorization) => new NextRequest("https://dutype.invalid/api/jobs/job", { headers: authorization ? { authorization } : {} });
  assert.equal((await GET(request(), { params: { jobId: "job" } })).status, 401);
  assert.equal(reads, 0);
  assert.equal(verifies, 0);
  assert.equal((await GET(request("Bearer rejected"), { params: { jobId: "job" } })).status, 401);
  assert.equal(reads, 0);
  assert.equal((await GET(request("Bearer anonymous-token"), { params: { jobId: "job" } })).status, 401);
  assert.equal(reads, 0);
  const outage = await GET(request("Bearer service-error"), { params: { jobId: "job" } });
  assert.equal(outage.status, 503);
  assert.equal(reads, 0);
  assert.doesNotMatch(JSON.stringify(await outage.json()), /PRIVATE_SERVER_ERROR|expired/);
  const response = await GET(request("Bearer synthetic-valid-token"), { params: { jobId: "job" } });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("cache-control"), "private, no-store");
  const result = await response.json();
  assert.equal(result.job.description, "Full role details");
  assert.equal(Object.hasOwn(result.job, "contactNumber"), false);
  assert.equal(reads, 1);
});

test("live-job server identifies missing setup before reading Firestore and keeps failures distinct", async () => {
  let configured = false;
  let attempts = 0;
  const server = loadSource("lib/jobs/server.ts", {
    "server-only": {},
    "next/cache": { unstable_cache: (callback) => callback },
    "@/lib/firebase/admin-server": { isFirebaseAdminConfigured: () => configured, getFirebaseAdminApp: () => ({}) },
    "firebase-admin/firestore": { getFirestore: () => { attempts += 1; throw new Error("PRIVATE_DIAGNOSTIC"); } }
  });
  const unconfigured = await server.getLiveJobPage();
  assert.equal(unconfigured.status, "unavailable");
  assert.equal(unconfigured.unavailableReason, "setup-required");
  assert.equal(attempts, 0);
  configured = true;
  const temporary = await server.getLiveJobPage();
  assert.equal(temporary.unavailableReason, "temporary");
  assert.equal(attempts, 1);
  assert.doesNotMatch(JSON.stringify(temporary), /PRIVATE_DIAGNOSTIC/);
});

test("public job API distinguishes unavailable service from empty results and validates search input", async () => {
  let calls = 0;
  const route = loadSource("app/api/jobs/route.ts", {
    "@/lib/jobs/server": { getLiveJobPage: async (search) => {
      calls += 1;
      return { status: search.city === "Offline" ? "unavailable" : "ready", jobs: [], nextCursor: null, scanned: 0 };
    } }
  });
  assert.equal((await route.GET(new NextRequest("https://dutype.invalid/api/jobs?city=Offline"))).status, 503);
  assert.equal((await route.GET(new NextRequest("https://dutype.invalid/api/jobs?city=Hyderabad"))).status, 200);
  const invalid = await route.POST(new NextRequest("https://dutype.invalid/api/jobs", { method: "POST", body: JSON.stringify({ location: { latitude: 91, longitude: 78, radiusKm: 5 } }) }));
  assert.equal(invalid.status, 400);
  assert.equal(calls, 2);
});

test("real live job URLs use their actual update dates in the sitemap", async () => {
  const sitemap = await loadSource("app/sitemap.ts", { "@/lib/jobs/server": { getDiscoverableJobs: async () => [
    { id: "real-job", updatedAt: 1720000000000 }, { id: "undated-job", updatedAt: null }
  ] } }).default();
  const job = sitemap.find((item) => item.url.endsWith("/jobs/real-job"));
  assert.equal(job.lastModified.getTime(), 1720000000000);
  assert.equal(Object.hasOwn(sitemap.find((item) => item.url.endsWith("/jobs/undated-job")), "lastModified"), false);
});

test("part-time and daily-wage guides filter job type and pay period instead of fake categories", () => {
  const record = { id: "job", data: { title: "Local driver", category: "DRIVING", isActive: true, jobType: "PART_TIME", payType: "DAILY" } };
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, category: "driver", query: "driver jobs" }));
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, category: "part-time" }));
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, category: "daily-wage" }));
  assert.equal(matchingJobSummary({ ...record, data: { ...record.data, jobType: "FULL_TIME" } }, { ...emptyJobSearch, category: "part-time" }), null);
});

test("public job pages render real summaries and never put gated details into HTML or structured data", async () => {
  const React = require("react");
  const { renderToStaticMarkup } = require("react-dom/server");
  const summary = publicJobSummary({ id: "real-job", data: {
    title: "Driver role", companyName: "Actual Company", isActive: true, city: "Hyderabad", area: "Madhapur", payAmount: 18000,
    description: "PRIVATE_DESCRIPTION_MARKER", contactNumber: "PRIVATE_PHONE_MARKER", location: "PRIVATE_ADDRESS_MARKER"
  } });
  const page = loadSource("app/jobs/[jobId]/page.tsx", {
    react: { ...React, cache: (callback) => callback },
    "@/lib/jobs/server": { getPublicJob: async () => ({ status: "ready", job: summary }) },
    "@/components/site-shell": { SiteShell: ({ children }) => React.createElement("main", null, children) },
    "@/components/jobs/job-details-access": { JobDetailsAccess: () => React.createElement("p", null, "Sign in for full job details") },
    "next/link": ({ href, children, ...props }) => React.createElement("a", { href, ...props }, children),
    "next/navigation": { notFound: () => { throw new Error("NOT_FOUND"); }, permanentRedirect: () => { throw new Error("REDIRECT"); } }
  });
  const metadata = await page.generateMetadata({ params: { jobId: "real-job" } });
  const html = renderToStaticMarkup(await page.default({ params: { jobId: "real-job" } }));
  assert.equal(metadata.alternates.canonical, `${discovery.SITE_URL}/jobs/real-job`);
  assert.match(metadata.title, /Driver role.*Hyderabad.*Actual Company/);
  assert.match(html, /Driver role/);
  assert.match(html, /Madhapur/);
  assert.match(html, /application\/ld\+json/);
  assert.match(html, /Sign in for full job details/);
  assert.doesNotMatch(html + JSON.stringify(metadata), /PRIVATE_.*_MARKER|JobPosting/);
});

test("server job access uses managed credentials only in an identified Google runtime", () => {
  const fields = ["GOOGLE_APPLICATION_CREDENTIALS", "FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH", "FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON", "FIREBASE_ADMIN_PROJECT_ID", "FIREBASE_ADMIN_CLIENT_EMAIL", "FIREBASE_ADMIN_PRIVATE_KEY", "K_SERVICE", "FUNCTION_TARGET", "GAE_ENV", "GOOGLE_CLOUD_PROJECT", "GCLOUD_PROJECT"];
  const previous = Object.fromEntries(fields.map((field) => [field, process.env[field]]));
  try {
    for (const field of fields) delete process.env[field];
    const admin = loadSource("lib/firebase/admin-server.ts", {
      "server-only": {},
      "@/lib/firebase/config": { firebaseConfig: { projectId: "" } },
      "firebase-admin/app": { getApps: () => [], applicationDefault: () => "managed-credential", initializeApp: (options) => options },
      "firebase-admin/auth": { getAuth: () => ({}) }
    });
    assert.equal(admin.isFirebaseAdminConfigured(), false);
    process.env.GOOGLE_CLOUD_PROJECT = "synthetic-project";
    assert.equal(admin.isFirebaseAdminConfigured(), false);
    process.env.K_SERVICE = "synthetic-next-server";
    assert.equal(admin.isFirebaseAdminConfigured(), true);
    assert.deepEqual(admin.getFirebaseAdminApp(), { credential: "managed-credential", projectId: "synthetic-project", storageBucket: undefined });
  } finally {
    for (const field of fields) {
      if (previous[field] === undefined) delete process.env[field];
      else process.env[field] = previous[field];
    }
  }
});

test("natural city job queries match canonical work type and category synonyms", () => {
  const record = { id: "canonical-query", data: { title: "Kitchen assistant", status: "open", category: "COOKING", jobType: "Part-time", salaryType: "DAILY", companyCity: "Hyderabad" } };
  for (const query of ["local jobs in Hyderabad", "part-time jobs Hyderabad", "cook jobs in Hyderabad", "available cooking vacancies near me"]) {
    assert.ok(matchingJobSummary(record, { ...emptyJobSearch, query }), query);
  }
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, query: "driver jobs Hyderabad" }), null);
  assert.equal(matchingJobSummary(record, { ...emptyJobSearch, query: "local jobs Delhi" }), null);
});

test("Android companyCity state values do not hide recognizable address cities", () => {
  const record = { id: "address-city", data: { title: "Helper", status: "open", companyCity: "Telangana", addressText: "Madhapur, Hyderabad, Telangana, India", location: { lat: 17.44, lng: 78.39 } } };
  assert.equal(publicJobSummary(record).city, "Hyderabad");
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, city: "Hyderabad" }));
  assert.ok(matchingJobSummary(record, { ...emptyJobSearch, city: "Hyderabad", location: { latitude: 17.44, longitude: 78.39, radiusKm: 5 } }));
  assert.equal(publicJobSummary({ ...record, data: { ...record.data, city: "Explicit City" } }).city, "Explicit City");
});