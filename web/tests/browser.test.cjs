const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { before, after, test } = require("node:test");
const { build } = require("esbuild");
const { chromium } = require("playwright");
const { loadSource } = require("./load-source.cjs");

const { resolveLegacyPage, SUPPORT_EMAIL, FEEDBACK_EMAIL, PLAY_STORE_URL } = loadSource("lib/public-site.ts");
const { matchingJobSummary, parseJobSearch, publicJobSummary, queryLiveJobPage } = loadSource("lib/jobs/public-listings.ts");
const resourceSlugs = ["safety", "contact", "faq", "privacy", "terms", "refund", "delivery-jobs", "driver-jobs", "maid-jobs", "jobs-in-hyderabad"];

const root = path.resolve(__dirname, "..");
let browser;
let source;
let styles;
const resultsDirectory = path.join(root, "test-results");
const pageErrors = new WeakMap();
const testJob = {
  title: "Test delivery role", companyName: "Test Company", description: "Deliver local orders during the morning shift.",
  city: "Hyderabad", area: "Madhapur",
  employerId: "test-employer", category: "DELIVERY", location: "Test district", payAmount: 18000,
  payType: "MONTHLY", shiftTiming: "Morning", vacancies: 2, isActive: true, isFilled: false,
  createdAt: 1000, jobId: "job-test", applicationCount: 0
};

const stubs = {
  "@/components/jobs/live-jobs-section": `
    import React from 'react';
    import { LiveJobResults } from './components/jobs/live-job-results';
    import { matchingJobSummary } from './lib/jobs/public-listings';
    export function LiveJobsSection({ search, heading, showFilters = false }) {
      const jobs = Object.entries(globalThis.fixture.documents).filter(([key]) => key.startsWith('jobs/')).map(([key, data]) => matchingJobSummary({ id: key.slice(5), data }, search)).filter(Boolean);
      const initial = globalThis.fixture.jobApiUnavailable || globalThis.fixture.jobSetupRequired ? { status: 'unavailable', unavailableReason: globalThis.fixture.jobSetupRequired ? 'setup-required' : 'temporary', jobs: [], nextCursor: null, scanned: 0 } : { status: 'ready', jobs: jobs.slice(0, 20), nextCursor: jobs.length > 20 ? jobs[19].id : null, scanned: jobs.length };
      return <LiveJobResults search={search} initial={initial} heading={heading} showFilters={showFilters} />;
    }
  `,
  "next/link": `import React from 'react'; export default function Link({ href, children, ...props }) { return <a href={href} {...props}>{children}</a>; }`,
  "next/image": `import React from 'react'; export default function Image({ src, alt, width, height, fill, className, style }) { return <img src={typeof src === 'string' ? src : src.src} alt={alt} width={width} height={height} className={className} style={fill ? { position: 'absolute', inset: 0, width: '100%', height: '100%', ...style } : style} />; }`,
  "next/navigation": `export const useRouter = () => ({ replace: value => globalThis.fixture.redirects.push(value), push: value => globalThis.fixture.redirects.push(value), refresh() {} }); export const useSearchParams = () => new URLSearchParams(globalThis.fixture.query); export const usePathname = () => globalThis.fixture.pathname; export const notFound = () => { throw new Error('Fixture page not found'); };`,
  "@/lib/firebase/client": `const services = { auth: { get currentUser() { return globalThis.fixture.guest ? null : globalThis.fixture.user; } }, db: {} }; export const getFirebaseServices = () => globalThis.fixture.missingFirebaseConfig ? null : services;`,
  "firebase/auth": `
        export class GoogleAuthProvider { setCustomParameters(values) { this.parameters = values; } }
        export class RecaptchaVerifier {
          constructor(auth, container, options) { globalThis.fixture.captchas = (globalThis.fixture.captchas || 0) + 1; this.options = options; }
          clear() { globalThis.fixture.captchaClears = (globalThis.fixture.captchaClears || 0) + 1; }
        }
        function authenticate(user) {
          user.getIdToken = async () => 'synthetic-valid-token';
          globalThis.fixture.user = user;
          globalThis.fixture.guest = false;
          globalThis.fixture.authListener?.(user);
          return { user };
        }
        export async function signInWithPopup(auth, provider) {
          globalThis.fixture.googleRequests = (globalThis.fixture.googleRequests || 0) + 1;
          if (globalThis.fixture.googleError) throw Object.assign(new Error('Synthetic Google failure'), { code: globalThis.fixture.googleError });
          return authenticate(globalThis.fixture.providerUser || { uid: 'google-employer', email: 'google@example.invalid', displayName: 'Google Employer', photoURL: '' });
        }
        export async function signInWithPhoneNumber(auth, phone, verifier) {
          (globalThis.fixture.smsRequests ||= []).push(phone);
          if (globalThis.fixture.smsError) throw Object.assign(new Error('Synthetic SMS failure'), { code: globalThis.fixture.smsError });
          return { async confirm(code) {
            globalThis.fixture.otpAttempts = (globalThis.fixture.otpAttempts || 0) + 1;
            if (globalThis.fixture.otpError) throw Object.assign(new Error('Synthetic OTP failure'), { code: globalThis.fixture.otpError });
            if (code !== '123456') throw Object.assign(new Error('Synthetic invalid code'), { code: 'auth/invalid-verification-code' });
            return authenticate(globalThis.fixture.providerUser || { uid: 'phone-employer', email: null, displayName: null, phoneNumber: phone, photoURL: null });
          } };
        }
    export function onAuthStateChanged(auth, onChange) {
      globalThis.fixture.authSubscriptions = (globalThis.fixture.authSubscriptions || 0) + 1;
      const listener = nextUser => {
        globalThis.fixture.guest = !nextUser;
        if (nextUser) globalThis.fixture.user = nextUser;
        onChange(nextUser);
      };
      globalThis.fixture.authListener = listener;
      queueMicrotask(() => { if (!globalThis.fixture.holdAuthCheck && globalThis.fixture.authListener === listener) listener(auth.currentUser); });
      return () => { if (globalThis.fixture.authListener === listener) globalThis.fixture.authListener = null; };
    }
    export async function signInWithEmailAndPassword(auth, email, password) {
      globalThis.fixture.signIns.push({ email, password });
      if (globalThis.fixture.signInError) throw Object.assign(new Error('Synthetic internal diagnostic'), { code: globalThis.fixture.signInError });
      return { user: { uid: 'test-user' } };
    }
    export async function sendPasswordResetEmail(auth, email) {
      globalThis.fixture.passwordResets.push(email);
      if (globalThis.fixture.holdPasswordReset) {
        await new Promise((resolve) => { globalThis.fixture.releasePasswordReset = resolve; });
      }
      if (globalThis.fixture.passwordResetError) {
        throw Object.assign(new Error('Synthetic Firebase detail'), { code: globalThis.fixture.passwordResetError });
      }
    }
    export async function createUserWithEmailAndPassword(auth, email, password) {
      globalThis.fixture.signUps.push({ email, password });
      return { user: { uid: 'test-user' } };
    }
    export async function updateProfile() {}
    export async function signOut() {}
  `,
  "@/lib/firebase/chat-actions": `export async function getOrCreateConversationId(otherUserId, jobId) { globalThis.fixture.conversations.push({ otherUserId, jobId }); return 'test-conversation'; } export const productConversationRoute = role => '/app/' + role.toLowerCase() + '/messages?conversation=test-conversation';`,
  "./use-product-session": `
    import { useState } from 'react';
    export function useProductSession() {
      const [role, setRole] = useState(globalThis.fixture.initialRole || 'WORKER');
      const [profile, setProfile] = useState(globalThis.fixture.profile);
      const [error, setError] = useState(globalThis.fixture.sessionError || null);
      return {
        user: globalThis.fixture.guest ? null : globalThis.fixture.user,
        profile, loading: Boolean(globalThis.fixture.sessionLoading), error,
        availableRoles: globalThis.fixture.availableRoles || ['WORKER', 'EMPLOYER'], currentRole: role,
        async refreshProfile() {
          globalThis.fixture.profileRefreshes = (globalThis.fixture.profileRefreshes || 0) + 1;
          if (globalThis.fixture.refreshProfileError) throw new Error('Synthetic profile failure');
          setProfile(structuredClone(globalThis.fixture.documents['users/' + globalThis.fixture.user.uid]));
          setError(null);
        },
        async setActiveRole(nextRole) {
          await new Promise((resolve, reject) => globalThis.fixture.roleRequests.push({ resolve, reject }));
          setRole(nextRole);
        }
      };
    }
  `
};

before(async () => {
  const bundled = await build({
    absWorkingDir: root,
    stdin: {
      contents: `
        import React, { useState } from 'react';
        import { createRoot } from 'react-dom/client';
        import { ProductAuthClient } from './components/product/product-auth-client';
        import { ProductEntryClient } from './components/product/product-entry-client';
        import WorkerMessagesPage from './app/app/worker/messages/page';
        import { ProductRoleBoundary, ProductAppShell } from './components/product/product-shell';
        import { JobDescription } from './components/product/job-description';
        import WorkerJobsPage from './app/app/worker/jobs/page';
        import WorkerJobDetailPage from './app/app/worker/jobs/[jobId]/page';
        import EmployerPostJobPage from './app/app/employer/post-job/page';
        import EmployerEditJobPage from './app/app/employer/jobs/[jobId]/page';
        import { EmployerApplicationsClient } from './components/product/employer-app';
        import { useProductSession } from './components/product/use-product-session';
        import { ProductSessionProvider, useProductSession as useRealProductSession } from './components/product/use-product-session.ts';
        import HomePage from './app/page';
        import JobsHubPage from './app/jobs/page';
        import LegacyContentPage from './app/[slug]/page';
        function SessionPage() {
          const session = useRealProductSession();
          return <><p role="status">{session.loading ? 'Loading session' : session.profile?.fullName || 'Guest'}</p>{session.error ? <p role="alert">{session.error}</p> : null}<button onClick={() => void session.refreshProfile()}>Retry session</button></>;
        }
        function SessionFixture() {
          const [page, setPage] = useState(0);
          return <ProductSessionProvider><button onClick={() => setPage(page + 1)}>Next product page</button><SessionPage key={page} /></ProductSessionProvider>;
        }
        function WorkerFixture() {
          return globalThis.fixture.view === 'worker-jobs'
            ? <WorkerJobsPage />
            : <WorkerJobDetailPage params={{ jobId: globalThis.fixture.jobId || 'job-test' }} />;
        }
        function EmployerFixture() {
          const session = useProductSession();
          if (globalThis.fixture.view === 'employer-post') return <EmployerPostJobPage />;
          if (globalThis.fixture.view === 'employer-edit') return <EmployerEditJobPage params={{ jobId: globalThis.fixture.jobId || 'job-test' }} />;
          return <ProductAppShell role="EMPLOYER" session={session} currentPath="/app/employer/post-job" title="Post a job" description="">
            <EmployerApplicationsClient session={session} />
          </ProductAppShell>;
        }
        const container = createRoot(document.getElementById('root'));
        function renderFixture() {
        container.render(globalThis.fixture.view === 'session'
          ? <SessionFixture />
          : globalThis.fixture.view === 'entry'
          ? <ProductEntryClient />
          : globalThis.fixture.view === 'worker-messages'
          ? <WorkerMessagesPage />
          : globalThis.fixture.view.startsWith('employer-')
          ? <EmployerFixture />
          : globalThis.fixture.view.startsWith('worker-')
          ? <WorkerFixture />
          : globalThis.fixture.view === 'home'
          ? <HomePage />
          : globalThis.fixture.view === 'jobs'
          ? <JobsHubPage searchParams={globalThis.fixture.searchParams || {}} />
          : globalThis.fixture.view === 'resource'
          ? <LegacyContentPage params={{ slug: globalThis.fixture.slug }} />
          : globalThis.fixture.view === 'description'
          ? <div style={{ maxWidth: '480px', padding: '16px' }}><JobDescription text={globalThis.fixture.description} /></div>
          : globalThis.fixture.view === 'auth'
          ? <ProductAuthClient />
          : <ProductRoleBoundary requiredRole="EMPLOYER" currentPath="/app/employer" title="Hiring" description="Test workspace">{() => <p>Protected hiring content</p>}</ProductRoleBoundary>);
        }
        globalThis.fixture.showView = (view) => { globalThis.fixture.view = view; renderFixture(); };
        renderFixture();
      `,
      resolveDir: root,
      loader: "tsx"
    },
    bundle: true,
    write: false,
    format: "iife",
    platform: "browser",
    jsx: "automatic",
    define: { "process.env.NODE_ENV": '"development"', "process.env.NEXT_PUBLIC_SITE_URL": '"https://dutype.in"' },
    plugins: [{
      name: "offline-services",
      setup(builder) {
        builder.onResolve({ filter: /.*/ }, (args) => {
          if (args.path === "firebase/firestore") return { path: path.join(root, "tests/firestore-fixture.mjs") };
          if (args.path.endsWith("/use-product-session")) return { path: "./use-product-session", namespace: "fixture" };
          return Object.hasOwn(stubs, args.path) ? { path: args.path, namespace: "fixture" } : undefined;
        });
        builder.onLoad({ filter: /.*/, namespace: "fixture" }, (args) => ({ contents: stubs[args.path], loader: "tsx", resolveDir: root }));
      }
    }]
  });
  source = bundled.outputFiles[0].text;
  const fontStyles = [
    ["IBM Plex Sans", "ibm_plex_sans_regular.ttf", 400],
    ["IBM Plex Sans", "ibm_plex_sans_medium.ttf", 500],
    ["IBM Plex Sans", "ibm_plex_sans_semibold.ttf", 600],
    ["IBM Plex Sans", "ibm_plex_sans_bold.ttf", 700],
    ["Sora", "sora_regular.ttf", 400],
    ["Sora", "sora_medium.ttf", 500],
    ["Sora", "sora_semibold.ttf", 600],
    ["Sora", "sora_bold.ttf", 700]
  ].map(([family, filename, weight]) => {
    const data = fs.readFileSync(path.join(root, "../app/src/main/res/font", filename)).toString("base64");
    return `@font-face { font-family: '${family}'; font-weight: ${weight}; src: url(data:font/ttf;base64,${data}) format('truetype'); }`;
  }).join("\n");
  styles = `${fontStyles}\n${fs.readFileSync(path.join(root, "app/globals.css"), "utf8")}\n:root { --font-body: 'IBM Plex Sans'; --font-display: 'Sora'; }`;
  fs.mkdirSync(resultsDirectory, { recursive: true });
  browser = await chromium.launch({
    headless: true,
    channel: process.env.PLAYWRIGHT_CHANNEL || (process.platform === "win32" ? "msedge" : undefined)
  });
});

after(async () => { await browser?.close(); });

async function openFixture(view = "auth", extra = {}, viewport = { width: 1440, height: 1000 }) {
  const page = await browser.newPage({ viewport, reducedMotion: "reduce" });
  if (extra.fakeClock) await page.clock.install();
  const errors = [];
  pageErrors.set(page, errors);
  page.on("pageerror", (error) => errors.push(error.stack || error.message));
  page.setDefaultTimeout(10000);
  await page.route("**/*", async (route) => {
    const url = new URL(route.request().url());
    if (url.origin === "https://dutype-fixture.invalid") {
      if (url.pathname === "/") return route.fulfill({ contentType: "text/html", body: '<html lang="en"><body><div id="root"></div></body></html>' });
      if (url.pathname === "/api/jobs") {
        const input = route.request().method() === "POST" ? route.request().postDataJSON() : Object.fromEntries(url.searchParams);
        await page.evaluate((request) => { (fixture.jobSearchRequests ||= []).push(request); }, { method: route.request().method(), input });
        const state = await page.evaluate(() => ({ documents: fixture.documents, unavailable: fixture.jobApiUnavailable, setupRequired: fixture.jobSetupRequired }));
        if (state.unavailable || state.setupRequired) return route.fulfill({ status: 503, json: { status: "unavailable", unavailableReason: state.setupRequired ? "setup-required" : "temporary", jobs: [], nextCursor: null, scanned: 0 } });
        const records = Object.entries(state.documents).filter(([key, data]) => key.startsWith("jobs/") && data.isActive === true).map(([key, data]) => ({ id: key.slice(5), data })).sort((left, right) => left.id.localeCompare(right.id));
        const result = await queryLiveJobPage(async (after, limit) => records.filter((record) => !after || record.id > after).slice(0, limit), parseJobSearch(input), input.cursor || null);
        return route.fulfill({ json: result });
      }
      if (url.pathname.startsWith("/api/jobs/")) {
        const id = decodeURIComponent(url.pathname.slice("/api/jobs/".length));
        const signedIn = route.request().headers().authorization === "Bearer synthetic-valid-token";
        await page.evaluate((signedIn) => { (fixture.detailRequests ||= []).push({ signedIn }); }, signedIn);
        const state = await page.evaluate(() => ({ documents: fixture.documents, unavailable: fixture.jobApiUnavailable }));
        if (!signedIn) return route.fulfill({ status: 401, json: { error: "Sign in required" } });
        if (state.unavailable) return route.fulfill({ status: 503, json: { error: "Unavailable" } });
        const match = Object.entries(state.documents).find(([key, data]) => key === `jobs/${id}` || key.startsWith("jobs/") && data.jobId === id);
        const summary = match ? publicJobSummary({ id: match[0].slice(5), data: match[1] }) : null;
        if (!match || !summary) return route.fulfill({ status: 410, json: { error: "Closed" } });
        return route.fulfill({ json: { job: { ...summary, description: match[1].description || "", location: match[1].location || "", shiftTiming: match[1].shiftTiming || "", vacancies: match[1].vacancies || null } } });
      }
      const assets = { "/dutype-logo.webp": "image/webp", "/icons.svg": "image/svg+xml" };
      if (Object.hasOwn(assets, url.pathname)) return route.fulfill({ contentType: assets[url.pathname], body: fs.readFileSync(path.join(root, "public", url.pathname.slice(1))) });
    }
    return route.abort("blockedbyclient");
  });
  await page.goto("https://dutype-fixture.invalid/");
  await page.addStyleTag({ content: styles });
  await page.evaluate((options) => {
    const user = options.extra.user || { uid: "test-user", email: "test@example.invalid" };
    user.getIdToken = async () => {
      if (globalThis.fixture?.holdJobToken) return new Promise(() => {});
      return "synthetic-valid-token";
    };
    const profile = {
      fullName: "Test Account", email: "test@example.invalid", roles: ["WORKER"], activeRole: "WORKER", role: "WORKER", savedJobs: [],
      phone: "9000000000", address: "Test district", skills: "Delivery", experience: "1 year", ...options.extra.profile
    };
    globalThis.fixture = {
      view: options.view, pathname: options.view === "jobs" ? "/jobs" : "/", query: "", guest: false, signIns: [], signUps: [], passwordResets: [], writes: [], redirects: [], roleRequests: [], conversations: [], sequence: 0,
      deniedCollections: ["worker_profiles", "employer_profiles"],
      ...options.extra, user, profile, documents: { ["users/" + user.uid]: profile, ...options.extra.documents }
    };
    if (options.extra.geolocation) Object.defineProperty(navigator, "geolocation", { configurable: true, value: {
      getCurrentPosition(success, failure) {
        fixture.locationRequests = (fixture.locationRequests || 0) + 1;
        if (fixture.geolocation === "granted") queueMicrotask(() => success({ coords: { latitude: 17.44, longitude: 78.39, accuracy: 50 } }));
        else queueMicrotask(() => failure({ code: fixture.geolocation === "denied" ? 1 : 2 }));
      }
    } });
  }, { view, extra });
  await page.addScriptTag({ content: source });
  await page.waitForFunction(() => document.getElementById("root")?.childElementCount > 0, undefined, { timeout: 3000 }).catch(() => {
    throw new Error(`Offline fixture failed to mount: ${errors.join("; ") || "No rendered content"}`);
  });
  return page;
}

for (const width of [390, 1440]) {
  test(`browser: jobs contrast meets WCAG AA at ${width}px`, async () => {
    const page = await openFixture("jobs", { searchParams: { city: "Anantapur" }, documents: { "jobs/anantapur-job": { ...testJob, city: "Anantapur", area: "Local market" } } }, { width, height: 900 });
    try {
      await page.getByRole("heading", { name: "Jobs in Anantapur", level: 1 }).waitFor();
      await page.evaluate(() => document.fonts.ready);
      await page.addScriptTag({ content: require("axe-core").source });
      const result = await page.evaluate(async () => {
        const report = await axe.run(document, { runOnly: { type: "rule", values: ["color-contrast"] } });
        const details = (items) => items.flatMap((item) => item.nodes.map((node) => ({
          target: node.target,
          html: node.html,
          summary: node.failureSummary,
          checks: node.any.map((check) => check.data)
        })));
        return {
          violations: details(report.violations),
          incomplete: details(report.incomplete),
          checked: report.passes.some((item) => item.id === "color-contrast")
        };
      });
      await page.screenshot({ path: path.join(resultsDirectory, `jobs-contrast-${width}.png`), fullPage: true, animations: "disabled" });
      assert.deepEqual(result.violations, [], "Visible text must meet WCAG AA contrast");
      assert.deepEqual(result.incomplete, [], "Unresolved contrast needs manual inspection");
      assert.equal(result.checked, true, "The contrast rule must inspect visible text");
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
  });
}

test("browser: guests see actual city job summaries but no private details or location prompt", async () => {
  const page = await openFixture("jobs", { guest: true, geolocation: "granted", searchParams: { city: "Hyderabad" }, documents: {
    "jobs/hyderabad-job": { ...testJob, city: "Hyderabad", area: "Madhapur", description: "PRIVATE FULL DESCRIPTION" },
    "jobs/delhi-job": { ...testJob, city: "Delhi", title: "Delhi role" },
    "jobs/closed-job": { ...testJob, city: "Hyderabad", title: "Closed role", isFilled: true }
  } });
  try {
    await page.getByRole("heading", { name: "Live jobs in Hyderabad" }).waitFor();
    assert.equal(await page.locator(".public-job-card").count(), 1);
    assert.match(await page.locator(".public-job-card").textContent(), /Madhapur, Hyderabad/);
    assert.doesNotMatch(await page.locator("main").textContent(), /PRIVATE FULL DESCRIPTION/);
    assert.equal(await page.evaluate(() => fixture.locationRequests || 0), 0);
    assert.equal(await page.locator(".public-job-card").getByRole("link", { name: "View details" }).getAttribute("href"), "/jobs/hyderabad-job");
    assert.equal(await page.evaluate(() => fixture.signIns.length + fixture.writes.length), 0);
  } finally { await page.close(); }
});

test("browser: city and area searches query real jobs and permission denial keeps manual search usable", async () => {
  const page = await openFixture("jobs", { guest: true, geolocation: "denied", documents: {
    "jobs/near": { ...testJob, city: "Hyderabad", area: "Madhapur" },
    "jobs/other": { ...testJob, title: "Other area", city: "Hyderabad", area: "Kukatpally" }
  } });
  try {
    await page.getByRole("button", { name: "Use my location", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "permission was denied" }).waitFor();
    await page.getByLabel("City", { exact: true }).fill("Hyderabad");
    await page.getByLabel("Area or locality", { exact: true }).fill("Madhapur");
    await page.getByRole("button", { name: "Find jobs", exact: true }).click();
    await page.waitForFunction(() => fixture.jobSearchRequests?.length === 1);
    await page.getByRole("status").filter({ hasText: "1 opening shown" }).waitFor();
    assert.equal(await page.locator(".public-job-card").count(), 1);
    assert.equal(await page.getByRole("heading", { level: 1 }).textContent(), "Jobs in Hyderabad");
    assert.match(page.url(), /city=Hyderabad.*area=Madhapur/);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: nearby search asks on click and keeps coordinates out of URLs and storage", async () => {
  const page = await openFixture("jobs", { guest: true, geolocation: "granted", documents: {
    "jobs/near": { ...testJob, city: "Hyderabad", latitude: 17.441, longitude: 78.391 },
    "jobs/far": { ...testJob, city: "Delhi", title: "Far job", latitude: 28.6, longitude: 77.2 }
  } });
  try {
    assert.equal(await page.evaluate(() => fixture.locationRequests || 0), 0);
    await page.getByRole("button", { name: "Use my location", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "1 opening shown" }).waitFor();
    const request = await page.evaluate(() => fixture.jobSearchRequests[0]);
    assert.equal(request.method, "POST");
    assert.equal(request.input.location.radiusKm, 5);
    assert.equal(await page.locator(".public-job-card").count(), 1);
    assert.doesNotMatch(page.url(), /17\.44|78\.39|latitude|longitude/);
    assert.equal(await page.evaluate(() => sessionStorage.length + localStorage.length), 0);
    await page.getByRole("button", { name: "Clear location" }).click();
    await page.getByRole("status").filter({ hasText: "2 openings shown" }).waitFor();
  } finally { await page.close(); }
});

test("browser: nearby filtering on a city page preserves the selected city", async () => {
  const page = await openFixture("resource", { slug: "jobs-in-hyderabad", geolocation: "granted", documents: {
    "jobs/near": { ...testJob, latitude: 17.44, longitude: 78.39 }
  } });
  try {
    await page.getByRole("button", { name: "Use my location", exact: true }).click();
    await page.waitForFunction(() => fixture.jobSearchRequests?.length === 1);
    assert.equal(await page.evaluate(() => fixture.jobSearchRequests[0].input.city), "Hyderabad");
    await page.getByRole("status").filter({ hasText: "1 opening shown in Hyderabad" }).waitFor();
    assert.equal(await page.getByRole("heading", { level: 1 }).textContent(), "Jobs in Hyderabad");
  } finally { await page.close(); }
});

test("browser: unavailable job data is not presented as zero openings and retry recovers", async () => {
  const page = await openFixture("jobs", { guest: true, jobApiUnavailable: true, documents: { "jobs/job": testJob } });
  try {
    await page.getByRole("alert").filter({ hasText: "temporarily unavailable" }).waitFor();
    assert.equal(await page.getByRole("alert").count(), 1);
    assert.equal(await page.locator(".live-job-count").count(), 0);
    assert.doesNotMatch(await page.locator("#live-jobs").textContent(), /No current openings|Listings could not be checked/);
    const message = await page.getByRole("alert").locator("p").boundingBox();
    const retry = await page.getByRole("button", { name: "Try again", exact: true }).boundingBox();
    assert.ok(retry.y >= message.y + message.height + 8, "Retry must be separated from the message");
    await page.screenshot({ path: path.join(resultsDirectory, "jobs-temporary-unavailable-1440.png"), fullPage: true });
    await page.evaluate(() => { fixture.jobApiUnavailable = false; });
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "1 opening shown" }).waitFor();
    assert.equal(await page.getByRole("alert").count(), 0);
  } finally { await page.close(); }
});

test("browser: missing live-job setup shows one message and does not keep retrying", async () => {
  const page = await openFixture("jobs", { guest: true, jobSetupRequired: true, fakeClock: true }, { width: 390, height: 844 });
  try {
    await page.getByRole("alert").filter({ hasText: "aren't connected yet" }).waitFor();
    assert.equal(await page.getByRole("alert").count(), 1);
    assert.equal(await page.getByRole("link", { name: "Contact support", exact: true }).getAttribute("href"), "/contact");
    assert.equal(await page.getByRole("button", { name: "Try again", exact: true }).count(), 0);
    assert.equal(await page.getByRole("button", { name: "Refresh jobs", exact: true }).count(), 0);
    assert.equal(await page.getByRole("button", { name: "Find jobs", exact: true }).isDisabled(), true);
    assert.equal(await page.getByRole("button", { name: "Use my location", exact: true }).isDisabled(), true);
    assert.equal(await page.locator(".live-job-count").count(), 0);
    assert.equal(await page.getByRole("alert").evaluate((element) => element.scrollWidth <= element.clientWidth + 1), true);
    await page.evaluate(() => document.fonts.ready);
    await page.addScriptTag({ content: require("axe-core").source });
    const contrast = await page.evaluate(async () => {
      const report = await axe.run(document.querySelector(".live-job-alert"), { runOnly: { type: "rule", values: ["color-contrast"] } });
      return { violations: report.violations.map((item) => item.nodes.map((node) => node.failureSummary)), incomplete: report.incomplete.length };
    });
    assert.deepEqual(contrast, { violations: [], incomplete: 0 });
    await page.screenshot({ path: path.join(resultsDirectory, "jobs-setup-required-390.png"), fullPage: true });
    await page.clock.fastForward(61_000);
    assert.equal(await page.evaluate(() => fixture.jobSearchRequests?.length || 0), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: retry stops when the server reports setup is required", async () => {
  const page = await openFixture("jobs", { jobApiUnavailable: true, fakeClock: true });
  try {
    await page.getByRole("button", { name: "Try again", exact: true }).waitFor();
    await page.evaluate(() => { fixture.jobSetupRequired = true; });
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await page.getByRole("alert").filter({ hasText: "aren't connected yet" }).waitFor();
    await page.clock.fastForward(61_000);
    assert.equal(await page.evaluate(() => fixture.jobSearchRequests.length), 1);
    assert.equal(await page.getByRole("button", { name: "Try again", exact: true }).count(), 0);
    assert.equal(await page.locator(".live-job-count").count(), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: a guest cannot fetch full details and login returns to the selected real job", async () => {
  const page = await openFixture("worker-detail", { guest: true, documents: { "jobs/job-test": { ...testJob, description: "PRIVATE FULL DESCRIPTION" } } });
  try {
    await page.getByRole("heading", { name: "Sign in for full job details" }).waitFor();
    assert.doesNotMatch(await page.locator("main").textContent(), /PRIVATE FULL DESCRIPTION/);
    assert.equal(await page.evaluate(() => fixture.detailRequests?.length || 0), 0);
    const link = page.getByRole("link", { name: "Sign in to view details", exact: true });
    assert.equal(await link.getAttribute("href"), "/app/auth?role=WORKER&next=%2Fjobs%2Fjob-test");
    await page.evaluate(() => fixture.authListener({ uid: "test-user", email: "test@example.invalid", getIdToken: async () => "synthetic-valid-token" }));
    await page.getByText("PRIVATE FULL DESCRIPTION", { exact: true }).waitFor();
    assert.equal(await page.getByRole("link", { name: "Apply in the app", exact: true }).getAttribute("href"), PLAY_STORE_URL);
    assert.deepEqual(await page.evaluate(() => fixture.detailRequests), [{ signedIn: true }]);
    await page.evaluate(() => fixture.authListener(null));
    await page.getByRole("heading", { name: "Sign in for full job details" }).waitFor();
    assert.equal(await page.getByText("PRIVATE FULL DESCRIPTION", { exact: true }).count(), 0);
  } finally { await page.close(); }
});

test("browser: password recovery validates email and never signs in or writes profiles", async () => {
  const page = await openFixture("auth", {
    query: "role=EMPLOYER&method=email&next=%2Fapp%2Femployer%2Fpost-job",
    profile: { role: "EMPLOYER", activeRole: "EMPLOYER", roles: ["EMPLOYER"] }
  });
  try {
    await page.getByLabel("Email", { exact: true }).fill("employer@example.invalid");
    await page.getByRole("button", { name: "Forgot password?", exact: true }).click();
    assert.equal(await page.getByRole("heading", { name: "Reset your password" }).evaluate((element) => element === document.activeElement), true);
    assert.equal(await page.getByLabel("Email", { exact: true }).inputValue(), "employer@example.invalid");
    assert.equal(await page.getByLabel("Password", { exact: true }).count(), 0);
    assert.equal(await page.getByRole("combobox").count(), 0);
    await page.getByLabel("Email", { exact: true }).fill("not-an-email");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    assert.equal(await page.getByLabel("Email", { exact: true }).evaluate((element) => element.validity.typeMismatch), true);
    assert.deepEqual(await page.evaluate(() => fixture.passwordResets), []);
    await page.getByLabel("Email", { exact: true }).fill("  employer@example.invalid  ");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("heading", { name: "Check your email" }).waitFor();
    assert.equal(await page.getByRole("heading", { name: "Check your email" }).evaluate((element) => element === document.activeElement), true);
    assert.deepEqual(await page.evaluate(() => fixture.passwordResets), ["employer@example.invalid"]);
    assert.deepEqual(await page.evaluate(() => ({ signIns: fixture.signIns, signUps: fixture.signUps, writes: fixture.writes, redirects: fixture.redirects })), { signIns: [], signUps: [], writes: [], redirects: [] });
    assert.match(await page.getByRole("status").textContent(), /If an account exists/);
    await page.getByRole("button", { name: "Back to sign in", exact: true }).click();
    assert.equal(await page.getByRole("combobox").inputValue(), "EMPLOYER");
    await page.getByLabel("Password", { exact: true }).fill("synthetic-test-password");
    await page.getByRole("button", { name: "Sign in", exact: true }).last().click();
    await page.waitForFunction(() => fixture.redirects.length === 1);
    assert.deepEqual(await page.evaluate(() => fixture.redirects), ["/app/employer/post-job"]);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: password recovery confirms missing accounts without exposing Firebase details", async () => {
  const page = await openFixture("auth", { passwordResetError: "auth/user-not-found" });
  try {
    await page.getByRole("button", { name: "Forgot password?", exact: true }).click();
    await page.getByLabel("Email", { exact: true }).fill("unknown@example.invalid");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("heading", { name: "Check your email" }).waitFor();
    assert.match(await page.getByRole("status").textContent(), /If an account exists/);
    assert.equal(await page.getByRole("alert").count(), 0);
    assert.equal(await page.getByText("Synthetic Firebase detail").count(), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: password recovery prevents duplicate requests and retries network failures", async () => {
  const page = await openFixture("auth", { holdPasswordReset: true, passwordResetError: "auth/network-request-failed" });
  try {
    await page.getByRole("button", { name: "Forgot password?", exact: true }).click();
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.waitForFunction(() => typeof fixture.releasePasswordReset === "function");
    assert.equal(await page.getByRole("button", { name: "Working...", exact: true }).isDisabled(), true);
    assert.equal(await page.getByRole("button", { name: "Back to sign in", exact: true }).isDisabled(), true);
    assert.equal(await page.getByLabel("Email", { exact: true }).isDisabled(), true);
    await page.locator("form.product-form").evaluate((form) => form.dispatchEvent(new Event("submit", { bubbles: true, cancelable: true })));
    assert.equal(await page.evaluate(() => fixture.passwordResets.length), 1);
    await page.evaluate(() => fixture.releasePasswordReset());
    await page.getByRole("alert").waitFor();
    assert.equal(await page.getByRole("alert").textContent(), "Unable to reach the account service. Check your connection and try again.");
    await page.evaluate(() => { fixture.holdPasswordReset = false; fixture.passwordResetError = null; });
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("heading", { name: "Check your email" }).waitFor();
    assert.equal(await page.evaluate(() => fixture.passwordResets.length), 2);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: password recovery reports rate limits and lets users change email", async () => {
  const page = await openFixture("auth", { passwordResetError: "auth/too-many-requests" });
  try {
    await page.getByRole("button", { name: "Forgot password?", exact: true }).click();
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    assert.equal(await page.getByLabel("Email", { exact: true }).evaluate((element) => element.validity.valueMissing), true);
    assert.equal(await page.evaluate(() => fixture.passwordResets.length), 0);
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("alert").waitFor();
    assert.equal(await page.getByRole("alert").textContent(), "Too many reset requests. Please try again later.");
    await page.evaluate(() => { fixture.passwordResetError = null; });
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("heading", { name: "Check your email" }).waitFor();
    await page.getByRole("button", { name: "Use a different email", exact: true }).click();
    await page.getByLabel("Email", { exact: true }).fill("another@example.invalid");
    await page.getByRole("button", { name: "Send reset link", exact: true }).click();
    await page.getByRole("heading", { name: "Check your email" }).waitFor();
    assert.match(await page.getByRole("status").textContent(), /another@example.invalid/);
    assert.equal(await page.getByRole("alert").count(), 0);
    assert.deepEqual(await page.evaluate(() => fixture.passwordResets), ["worker@example.invalid", "worker@example.invalid", "another@example.invalid"]);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: app entry routes guests to the selected role without account writes", async () => {
  const page = await openFixture("entry", { guest: true });
  try {
    await page.getByRole("heading", { name: "What brings you to DutyPe?" }).waitFor();
    assert.equal(await page.getByRole("link", { name: "Continue as Worker", exact: true }).getAttribute("href"), "/app/auth?role=WORKER");
    assert.equal(await page.getByRole("link", { name: "Continue as Employer", exact: true }).getAttribute("href"), "/app/auth?role=EMPLOYER");
    assert.equal(await page.getByRole("link", { name: "Sign in", exact: true }).getAttribute("href"), "/app/auth");
    assert.doesNotMatch(await page.locator("main").textContent(), /Firestore|Firebase|parity|route tree|React surface/);
    assert.deepEqual(await page.evaluate(() => ({ writes: fixture.writes, roles: fixture.roleRequests, redirects: fixture.redirects })), { writes: [], roles: [], redirects: [] });
  } finally { await page.close(); }
});

test("browser: app entry marks the active workspace and respects unavailable roles", async () => {
  for (const role of ["WORKER", "EMPLOYER"]) {
    const otherRole = role === "WORKER" ? "Employer" : "Worker";
    const label = role === "WORKER" ? "Worker" : "Employer";
    const page = await openFixture("entry", { initialRole: role, availableRoles: [role] });
    try {
      await page.getByRole("heading", { name: "Choose your workspace" }).waitFor();
      assert.equal(await page.locator(".entry-role.is-current").count(), 1);
      assert.equal(await page.locator(".entry-role.is-current").getByRole("link", { name: `Continue as ${label}` }).getAttribute("href"), `/app/${role.toLowerCase()}`);
      assert.equal(await page.getByRole("link", { name: `Sign in as ${otherRole}` }).getAttribute("href"), `/app/auth?role=${otherRole.toUpperCase()}`);
      assert.equal(await page.locator(`a[href='/app/${otherRole.toLowerCase()}']`).count(), 0);
      assert.equal(await page.getByRole("link", { name: "Use another account" }).getAttribute("href"), "/app/auth");
      assert.deepEqual(await page.evaluate(() => fixture.roleRequests), []);
    } finally { await page.close(); }
  }
  const dualRole = await openFixture("entry", { initialRole: "EMPLOYER", availableRoles: ["WORKER", "EMPLOYER"] });
  try {
    assert.equal(await dualRole.getByRole("link", { name: "Continue as Employer" }).getAttribute("href"), "/app/employer");
    assert.equal(await dualRole.getByRole("link", { name: "Open Worker" }).getAttribute("href"), "/app/worker");
    assert.deepEqual(await dualRole.evaluate(() => fixture.writes), []);
  } finally { await dualRole.close(); }
});

test("browser: app entry handles loading, missing roles and profile errors without exposing workspaces", async () => {
  const loading = await openFixture("entry", { sessionLoading: true });
  try {
    assert.match(await loading.getByRole("status").textContent(), /Loading your account/);
    assert.equal(await loading.locator(".entry-role").count(), 0);
  } finally { await loading.close(); }

  const unassigned = await openFixture("entry", { availableRoles: [] });
  try {
    await unassigned.getByRole("heading", { name: "No workspace is available" }).waitFor();
    assert.equal(await unassigned.locator("a[href='/app/worker'], a[href='/app/employer']").count(), 0);
    assert.equal(await unassigned.getByRole("link", { name: "Contact support" }).getAttribute("href"), "/contact");
  } finally { await unassigned.close(); }

  const failed = await openFixture("entry", { sessionError: "Synthetic backend details", refreshProfileError: true });
  try {
    await failed.getByRole("alert").waitFor();
    assert.equal(await failed.locator(".entry-role").count(), 0);
    assert.equal(await failed.getByText("Synthetic backend details").count(), 0);
    await failed.getByRole("button", { name: "Try again", exact: true }).click();
    await failed.waitForFunction(() => fixture.profileRefreshes === 1);
    assert.equal(await failed.locator(".entry-role").count(), 0);
    await failed.evaluate(() => { fixture.refreshProfileError = false; });
    await failed.getByRole("button", { name: "Try again", exact: true }).click();
    await failed.getByRole("link", { name: "Continue as Worker" }).waitFor();
    assert.equal(await failed.getByRole("alert").count(), 0);
    assert.deepEqual(pageErrors.get(failed), []);
  } finally { await failed.close(); }
});

test("browser: product navigation reuses the authenticated profile instead of fetching it again", async () => {
  const page = await openFixture("session");
  try {
    await page.getByRole("status").filter({ hasText: "Test Account" }).waitFor();
    assert.equal(await page.evaluate(() => fixture.profileReads), 1);
    await page.getByRole("button", { name: "Next product page", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "Test Account" }).waitFor();
    assert.equal(await page.evaluate(() => fixture.profileReads), 1);
    assert.equal(await page.evaluate(() => fixture.authSubscriptions), 1);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: delayed old-account reads cannot replace the next account or signed-out state", async () => {
  const page = await openFixture("session", { deferProfileReads: true, documents: { "users/second-user": { fullName: "Second Account", roles: ["EMPLOYER"] } } });
  try {
    await page.waitForFunction(() => fixture.pendingProfileReads?.length === 1);
    await page.evaluate(() => fixture.authListener({ uid: "second-user", email: "second@example.invalid" }));
    await page.waitForFunction(() => fixture.pendingProfileReads.length === 2);
    await page.evaluate(() => fixture.pendingProfileReads[1].resolve());
    await page.getByRole("status").filter({ hasText: "Second Account" }).waitFor();
    await page.evaluate(async () => { fixture.pendingProfileReads[0].resolve(); await Promise.resolve(); });
    assert.equal(await page.getByRole("status").textContent(), "Second Account");
    await page.evaluate(() => fixture.authListener(null));
    await page.getByRole("status").filter({ hasText: "Guest" }).waitFor();
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: slow profile loading times out visibly and can be retried", async () => {
  const page = await openFixture("session", { deferProfileReads: true, fakeClock: true });
  try {
    await page.waitForFunction(() => fixture.pendingProfileReads?.length === 1);
    await page.clock.fastForward(10_001);
    await page.getByRole("alert").filter({ hasText: "timed out" }).waitFor();
    assert.notEqual(await page.getByRole("status").textContent(), "Loading session");
    await page.evaluate(() => { fixture.deferProfileReads = false; });
    await page.getByRole("button", { name: "Retry session", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "Test Account" }).waitFor();
    assert.equal(await page.getByRole("alert").count(), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: signin navigation does not wait for login metadata writes", async () => {
  const page = await openFixture("auth", { deferLoginMetadata: true });
  try {
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByLabel("Password", { exact: true }).fill("synthetic-password");
    await page.getByRole("button", { name: "Sign in", exact: true }).last().click();
    await page.waitForFunction(() => fixture.redirects.length === 1);
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    assert.equal(await page.evaluate(() => typeof fixture.finishLoginMetadata), "function");
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: profile refresh keeps loaded content visible while waiting for the server", async () => {
  const page = await openFixture("session");
  try {
    await page.getByRole("status").filter({ hasText: "Test Account" }).waitFor();
    await page.evaluate(() => { fixture.deferProfileReads = true; });
    await page.getByRole("button", { name: "Retry session", exact: true }).click();
    await page.waitForFunction(() => fixture.pendingProfileReads?.length === 1);
    assert.equal(await page.getByRole("status").textContent(), "Test Account");
    await page.evaluate(() => fixture.pendingProfileReads[0].resolve());
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: missing Firebase configuration disables account requests before submission", async () => {
  const page = await openFixture("auth", { missingFirebaseConfig: true });
  try {
    await page.getByRole("alert").filter({ hasText: "configuration is incomplete" }).waitFor();
    assert.equal(await page.locator("button[type=submit]").isDisabled(), true);
    assert.equal(await page.evaluate(() => fixture.signIns.length + fixture.signUps.length + fixture.passwordResets.length), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: rejected Firebase keys show a configuration error and do not redirect", async () => {
  const page = await openFixture("auth", { signInError: "auth/api-key-not-valid.-please-pass-a-valid-api-key." });
  try {
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByLabel("Password", { exact: true }).fill("synthetic-password");
    await page.locator("button[type=submit]").click();
    await page.getByRole("alert").filter({ hasText: "configuration" }).waitFor();
    assert.doesNotMatch(await page.getByRole("alert").textContent(), /Synthetic internal diagnostic/);
    assert.deepEqual(await page.evaluate(() => fixture.redirects), []);
    assert.equal(await page.evaluate(() => fixture.signIns.length), 1);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: signin profile reads have a deadline rather than an endless working state", async () => {
  const page = await openFixture("auth", { deferProfileReads: true, fakeClock: true });
  try {
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByLabel("Password", { exact: true }).fill("synthetic-password");
    await page.locator("button[type=submit]").click();
    await page.waitForFunction(() => fixture.pendingProfileReads?.length === 1);
    await page.clock.fastForward(10_001);
    await page.getByRole("alert").filter({ hasText: "timed out" }).waitFor();
    assert.equal(await page.locator("button[type=submit]").isEnabled(), true);
    assert.deepEqual(await page.evaluate(() => fixture.redirects), []);
    await page.evaluate(() => fixture.pendingProfileReads[0].resolve());
    assert.deepEqual(await page.evaluate(() => fixture.redirects), []);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: safety sections have a single heading and readable mobile content", async () => {
  const page = await openFixture("resource", { slug: "safety", pathname: "/safety" }, { width: 390, height: 844 });
  try {
    await page.getByRole("heading", { name: "Safety and Security", level: 1 }).waitFor();
    assert.equal(await page.locator(".resource-section").count(), 8);
    assert.equal(await page.locator(".resource-section .card-kicker").count(), 0);
    assert.equal(await page.locator("#most-important-rule").getByRole("heading", { name: "Most important rule", level: 2 }).count(), 1);
    assert.equal(await page.locator("#before-you-meet-anyone").evaluate((element) => element.scrollWidth <= element.clientWidth + 1), true);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: resource pages retain policy content and valid section destinations", async () => {
  for (const slug of ["safety", "contact", "faq", "privacy", "terms", "refund"]) {
    const page = await openFixture("resource", { slug, pathname: `/${slug}` });
    try {
      const descriptor = resolveLegacyPage(slug);
      const rendered = (await page.locator(".resource-body").textContent()).replace(/\s+/g, " ");
      for (const block of descriptor.blocks) {
        assert.equal(await page.locator(".resource-body").getByRole("heading", { name: block.title, exact: true, level: 2 }).count(), 1, `${slug}: ${block.title}`);
        const content = block.kind === "copy" ? block.paragraphs
          : block.kind === "list" ? block.items
          : block.kind === "faq" ? block.items.flatMap((item) => [item.question, item.answer])
          : block.kind === "table" ? [...block.columns, ...block.rows.flat()]
          : block.items.flatMap((item) => [item.label, item.value, item.note]);
        for (const text of content) assert.ok(rendered.includes(text.replace(/\s+/g, " ")), `${slug}: missing ${text}`);
      }
      assert.deepEqual(await page.locator(".resource-toc a[href^='#']").evaluateAll((links) => links.filter((link) => !document.getElementById(link.hash.slice(1))).map((link) => link.hash)), []);
      const ids = await page.locator("main [id]").evaluateAll((elements) => elements.map((element) => element.id));
      assert.equal(new Set(ids).size, ids.length, `${slug}: duplicate section IDs`);
      assert.equal(await page.getByRole("heading", { level: 1 }).count(), 1);
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
  }
});

test("browser: resource FAQ filters topics, searches answers and resets accessibly", async () => {
  const page = await openFixture("resource", { slug: "faq", pathname: "/faq" }, { width: 390, height: 844 });
  try {
    assert.equal(await page.locator(".resource-faq-item").count(), 18);
    const firstQuestion = page.locator("summary").filter({ hasText: "Is DutyPe free for workers?" });
    await firstQuestion.focus();
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("details[open]").count(), 1);
    assert.equal(await page.getByText("Yes. DutyPe is intended to be 100 percent free for job seekers.", { exact: true }).isVisible(), true);
    await page.getByRole("button", { name: "For employers", exact: true }).click();
    assert.equal(await page.locator(".resource-faq-item").count(), 5);
    assert.equal(await page.getByRole("button", { name: "For employers", exact: true }).getAttribute("aria-pressed"), "true");
    await page.getByRole("searchbox", { name: "Search questions" }).fill("applicants");
    assert.equal(await page.locator(".resource-faq-item").count(), 1);
    await page.getByRole("button", { name: "Clear search", exact: true }).click();
    assert.equal(await page.locator(".resource-faq-item").count(), 5);
    assert.equal(await page.getByRole("searchbox").evaluate((element) => element === document.activeElement), true);
    await page.getByRole("searchbox", { name: "Search questions" }).fill("no-matching-question");
    await page.getByRole("heading", { name: "No matching answers" }).waitFor();
    await page.getByRole("button", { name: "Reset filters", exact: true }).click();
    assert.equal(await page.locator(".resource-faq-item").count(), 18);
    assert.equal(await page.getByRole("searchbox").inputValue(), "");
    assert.equal(await page.getByRole("searchbox").evaluate((element) => element === document.activeElement), true);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: resource safety index reaches emergency contacts and exposes dial links", async () => {
  const page = await openFixture("resource", { slug: "safety", pathname: "/safety" }, { width: 390, height: 844 });
  try {
    await page.locator(".resource-mobile-index > summary").click();
    await page.locator('.resource-mobile-index a[href="#emergency-contacts"]').click();
    assert.ok(page.url().endsWith("#emergency-contacts"));
    const bounds = await page.locator("#emergency-contacts").boundingBox();
    assert.ok(bounds.y >= 72 && bounds.y < 200, `Section must be visible below the header: ${bounds.y}`);
    assert.equal(await page.getByRole("link", { name: "Police: 100", exact: true }).getAttribute("href"), "tel:100");
    assert.equal(await page.getByRole("link", { name: "Women helpline: 1091", exact: true }).getAttribute("href"), "tel:1091");
    assert.equal(await page.getByRole("link", { name: "Cyber crime: 1930", exact: true }).getAttribute("href"), "tel:1930");
    assert.equal(await page.locator(".resource-header").getByRole("link", { name: "Report an issue" }).getAttribute("href"), `mailto:${SUPPORT_EMAIL}`);
  } finally { await page.close(); }
});

test("browser: resource contact channels and footer email retain the correct inboxes", async () => {
  const page = await openFixture("resource", { slug: "contact", pathname: "/contact" });
  try {
    assert.equal(await page.getByRole("link", { name: `General support: ${SUPPORT_EMAIL}` }).getAttribute("href"), `mailto:${SUPPORT_EMAIL}`);
    assert.equal(await page.getByRole("link", { name: `Feedback: ${FEEDBACK_EMAIL}` }).getAttribute("href"), `mailto:${FEEDBACK_EMAIL}`);
    assert.equal(await page.locator("footer").getByRole("link", { name: "Email support", exact: true }).getAttribute("href"), `mailto:${SUPPORT_EMAIL}`);
    assert.equal(await page.locator(".resource-sidebar-help").getByRole("link", { name: "Email support", exact: true }).getAttribute("href"), `mailto:${SUPPORT_EMAIL}`);
    assert.equal(await page.locator(".resource-contact-action").count(), 6);
  } finally { await page.close(); }
});

test("browser: category guides preserve city context and point to their actual live listings", async () => {
  for (const category of ["delivery", "driver", "maid"]) {
    const slug = `${category}-jobs-hyderabad`;
    const page = await openFixture("resource", { slug, pathname: `/${slug}` }, { width: 390, height: 844 });
    try {
      assert.equal(await page.getByRole("heading", { level: 1 }).textContent(), resolveLegacyPage(slug).title);
      assert.equal(await page.getByRole("searchbox").inputValue(), category);
      assert.equal(await page.getByRole("combobox").inputValue(), "Hyderabad");
      assert.equal(await page.locator(".city-directory").getByRole("link", { name: "Bangalore", exact: true }).getAttribute("href"), `/${category}-jobs-bangalore`);
      assert.equal(await page.locator(".city-directory").getByRole("link", { name: "Hyderabad", exact: true }).getAttribute("aria-current"), "true");
      assert.equal(await page.getByRole("link", { name: "View live jobs", exact: true }).getAttribute("href"), "#live-jobs");
      assert.equal(await page.locator("#live-jobs").count(), 1);
      assert.equal(await page.locator(".category-role-list li").count(), 4);
      const cityLinks = await page.locator(".city-directory a").evaluateAll((links) => links.map((link) => link.getAttribute("href")));
      for (const href of cityLinks) assert.ok(resolveLegacyPage(href.slice(1)), href);
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
  }
});

test("browser: Google sign-in creates an employer profile and returns to the posting page", async () => {
    const page = await openFixture("auth", { guest: true, query: "role=EMPLOYER&next=%2Fapp%2Femployer%2Fpost-job" });
    try {
      await page.getByRole("button", { name: "Continue with Google", exact: true }).click();
      await page.waitForFunction(() => fixture.redirects.includes("/app/employer/post-job"));
      const account = await page.evaluate(() => fixture.documents["users/google-employer"]);
      assert.equal(account.activeRole, "EMPLOYER");
      assert.equal(account.email, "google@example.invalid");
      assert.deepEqual(account.roles, ["EMPLOYER"]);
      assert.equal(await page.evaluate(() => Object.hasOwn(fixture.documents, "employer_profiles/google-employer")), false);
      assert.equal(await page.evaluate(() => fixture.googleRequests), 1);
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
  });

  test("browser: existing Google accounts preserve referral data and both marketplace roles", async () => {
    const existing = { fullName: "Existing Worker", roles: ["WORKER"], role: "WORKER", savedJobs: ["keep-job"], referralCode: "KEEP-CODE", referralStats: { availableBalance: 100 }, companyName: "Keep Company" };
    const page = await openFixture("auth", { query: "role=EMPLOYER", providerUser: { uid: "existing", email: "existing@example.invalid" }, documents: {
      "users/existing": existing, "employer_profiles/existing": { userId: "existing", companyName: "Keep Company", contactPhone: "9000000000" }
    } });
    try {
      await page.getByRole("button", { name: "Continue with Google", exact: true }).click();
      await page.waitForFunction(() => fixture.redirects.length === 1);
      const updated = await page.evaluate(() => fixture.documents["users/existing"]);
      assert.deepEqual(updated.roles, ["WORKER", "EMPLOYER"]);
      assert.equal(updated.activeRole, "EMPLOYER");
      assert.deepEqual(updated.referralStats, existing.referralStats);
      assert.equal(updated.referralCode, existing.referralCode);
      assert.deepEqual(updated.savedJobs, existing.savedJobs);
      assert.equal(await page.evaluate(() => fixture.documents["employer_profiles/existing"].companyName), "Keep Company");
    } finally { await page.close(); }
  });

  test("browser: cancelled Google sign-in makes no profile writes", async () => {
    const page = await openFixture("auth", { query: "role=EMPLOYER", googleError: "auth/popup-closed-by-user" });
    try {
      await page.getByRole("button", { name: "Continue with Google", exact: true }).click();
      await page.getByRole("alert").filter({ hasText: "cancelled" }).waitFor();
      assert.equal(await page.evaluate(() => fixture.writes.length), 0);
      assert.deepEqual(await page.evaluate(() => fixture.redirects), []);
      assert.equal(await page.getByRole("button", { name: "Continue with Google", exact: true }).isEnabled(), true);
    } finally { await page.close(); }
  });

  test("browser: phone OTP requires consent, rejects wrong codes and signs in without a password", async () => {
    const page = await openFixture("auth", { guest: true, query: "role=EMPLOYER&next=%2Fapp%2Femployer%2Fpost-job" });
    try {
      await page.getByLabel("Mobile number", { exact: true }).fill("90000 00000");
      await page.getByRole("button", { name: "Send verification code", exact: true }).click();
      assert.equal(await page.evaluate(() => fixture.smsRequests?.length || 0), 0);
      await page.getByRole("checkbox").check();
      await page.getByRole("button", { name: "Send verification code", exact: true }).click();
      await page.getByLabel("Verification code", { exact: true }).waitFor();
      assert.deepEqual(await page.evaluate(() => fixture.smsRequests), ["+919000000000"]);
      assert.equal(await page.evaluate(() => fixture.captchas), 1);
      assert.equal(await page.evaluate(() => fixture.captchaClears), 1);
      assert.equal(await page.getByRole("button", { name: /^Resend in/ }).isDisabled(), true);
      await page.getByLabel("Verification code", { exact: true }).fill("654321");
      await page.getByRole("button", { name: "Verify and continue", exact: true }).click();
      await page.getByRole("alert").filter({ hasText: "incorrect" }).waitFor();
      assert.equal(await page.evaluate(() => fixture.writes.length), 0);
      await page.getByLabel("Verification code", { exact: true }).fill("123456");
      await page.getByRole("button", { name: "Verify and continue", exact: true }).click();
      await page.waitForFunction(() => fixture.redirects.includes("/app/employer/post-job"));
      assert.equal(await page.evaluate(() => fixture.documents["users/phone-employer"].phone), "+919000000000");
      assert.equal(await page.evaluate(() => fixture.documents["users/phone-employer"].email), "");
      assert.equal(await page.getByLabel("Password", { exact: true }).count(), 0);
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
  });

  test("browser: OTP resend honors cooldown and expired codes remain retryable", async () => {
    const page = await openFixture("auth", { query: "role=EMPLOYER", fakeClock: true, otpError: "auth/code-expired" });
    try {
      await page.getByLabel("Mobile number", { exact: true }).fill("9000000000");
      await page.getByRole("checkbox").check();
      await page.getByRole("button", { name: "Send verification code", exact: true }).click();
      await page.getByLabel("Verification code", { exact: true }).fill("123456");
      await page.getByRole("button", { name: "Verify and continue", exact: true }).click();
      await page.getByRole("alert").filter({ hasText: "expired" }).waitFor();
      await page.clock.fastForward(61_000);
      await page.getByRole("button", { name: "Resend code", exact: true }).click();
      assert.equal(await page.evaluate(() => fixture.smsRequests.length), 2);
      await page.getByRole("button", { name: "Change number", exact: true }).click();
      await page.getByLabel("Mobile number", { exact: true }).waitFor();
      assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    } finally { await page.close(); }
  });

test("browser: signup validates before calling Firebase and saves the selected employer profile", async () => {
  const page = await openFixture("auth", { query: "role=EMPLOYER&method=email" });
  try {
    await page.getByRole("button", { name: "Create account", exact: true }).click();
    assert.equal(await page.getByRole("combobox").inputValue(), "EMPLOYER");
    await page.getByLabel("Full name").fill("Test Employer");
    await page.getByLabel("Company name").fill("Test Company");
    await page.getByLabel("Email", { exact: true }).fill("employer@example.invalid");
    await page.getByLabel("Password", { exact: true }).pressSequentially("12345");
    await page.getByRole("button", { name: "Create Employer account", exact: true }).click();
    assert.equal(await page.getByLabel("Password", { exact: true }).evaluate((input) => input.validity.tooShort), true);
    assert.equal(await page.evaluate(() => fixture.signUps.length), 0);
    await page.getByLabel("Password", { exact: true }).fill("synthetic-test-password");
    await page.getByRole("button", { name: "Create Employer account", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.length === 1);
    assert.deepEqual(await page.evaluate(() => fixture.redirects), ["/app/employer"]);
    assert.deepEqual(await page.evaluate(() => fixture.writes.map((entry) => entry.path)), ["users/test-user"]);
    assert.equal(await page.evaluate(() => fixture.writes[0].data.activeRole), "EMPLOYER");
  } finally { await page.close(); }
});

test("browser: signin supports existing passwords and password-manager autocomplete", async () => {
  const page = await openFixture("auth", { query: "next=%2Fapp%2Fworker%2Fjobs%2Fjob-test" });
  try {
    await page.getByLabel("Email", { exact: true }).fill("worker@example.invalid");
    await page.getByLabel("Password", { exact: true }).fill("short");
    assert.equal(await page.getByLabel("Password", { exact: true }).getAttribute("autocomplete"), "current-password");
    await page.getByRole("button", { name: "Sign in", exact: true }).last().click();
    await page.waitForFunction(() => fixture.redirects.length === 1);
    assert.deepEqual(await page.evaluate(() => fixture.redirects), ["/app/worker/jobs/job-test"]);
  } finally { await page.close(); }
});

test("browser: role switching starts one write and does not expose protected content early", async () => {
  const page = await openFixture("role");
  try {
    await page.getByText("Switching into employer mode.", { exact: true }).waitFor();
    assert.equal(await page.evaluate(() => fixture.roleRequests.length), 1);
    assert.equal(await page.getByText("Protected hiring content", { exact: true }).count(), 0);
    await page.evaluate(() => fixture.roleRequests[0].resolve());
    await page.getByText("Protected hiring content", { exact: true }).waitFor();
  } finally { await page.close(); }
});

test("browser: failed role switching stops and offers an explicit retry", async () => {
  const page = await openFixture("role");
  try {
    await page.waitForFunction(() => fixture.roleRequests.length === 1);
    await page.evaluate(() => fixture.roleRequests[0].reject(new Error("Synthetic offline failure")));
    await page.getByRole("alert").waitFor();
    assert.equal(await page.evaluate(() => fixture.roleRequests.length), 1);
    assert.equal(await page.getByText("Protected hiring content", { exact: true }).count(), 0);
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await page.waitForFunction(() => fixture.roleRequests.length === 2);
    await page.evaluate(() => fixture.roleRequests[1].resolve());
    await page.getByText("Protected hiring content", { exact: true }).waitFor();
  } finally { await page.close(); }
});

test("browser: protected routes retain their destination at the sign-in gate", async () => {
  const page = await openFixture("role", { guest: true, pathname: "/app/employer/jobs/job-test/applications" });
  try {
    const signIn = page.getByRole("link", { name: "Sign in", exact: true });
    await signIn.waitFor();
    assert.equal(await signIn.getAttribute("href"), "/app/auth?role=EMPLOYER&next=%2Fapp%2Femployer%2Fjobs%2Fjob-test%2Fapplications");
    assert.equal(await page.getByText("Protected hiring content", { exact: true }).count(), 0);
    assert.equal(await page.evaluate(() => fixture.roleRequests.length), 0);
  } finally { await page.close(); }
});

test("browser: Pretext measures responsive descriptions and preserves all text when expanded", async () => {
  const description = "Local delivery work with clear pay, working hours, and a nearby pickup location. ".repeat(12);
  const page = await openFixture("description", { description });
  try {
    const paragraph = page.locator(".job-description p");
    const expand = page.getByRole("button", { name: "Show more", exact: true });
    await expand.waitFor();
    const wideCount = Number(await paragraph.getAttribute("data-line-count"));
    assert.ok(wideCount > 3);
    const collapsed = await paragraph.boundingBox();
    await expand.click();
    const collapse = page.getByRole("button", { name: "Show less", exact: true });
    assert.equal(await collapse.getAttribute("aria-expanded"), "true");
    assert.equal(await paragraph.textContent(), description);
    assert.ok((await paragraph.boundingBox()).height > collapsed.height);
    await page.setViewportSize({ width: 320, height: 800 });
    await page.waitForFunction((previous) => Number(document.querySelector(".job-description p").dataset.lineCount) > previous, wideCount);
    assert.equal(await paragraph.evaluate((element) => element.scrollWidth <= element.clientWidth + 1), true);
    await collapse.click();
    assert.equal(await expand.getAttribute("aria-expanded"), "false");
  } finally { await page.close(); }
});

test("browser: short descriptions do not get an unnecessary expansion control", async () => {
  const page = await openFixture("description", { description: "Local delivery work." });
  try {
    await page.waitForFunction(() => document.querySelector(".job-description p")?.dataset.lineCount === "1");
    assert.equal(await page.getByRole("button").count(), 0);
  } finally { await page.close(); }
});

for (const view of ["auth", "otp-code", "signup", "recovery", "recovery-sent", "recovery-error", "entry", "entry-signed-in", "home", "jobs", "worker-jobs", "worker-detail", "job-login-gate", "employer-post", ...resourceSlugs]) {
  for (const width of [320, 390, 768, 1440]) {
    test(`browser: ${view} fits a ${width}px viewport with readable controls`, async () => {
      const page = await openFixture(resourceSlugs.includes(view) ? "resource" : view === "job-login-gate" ? "worker-detail" : view === "signup" || view === "otp-code" || view.startsWith("recovery") ? "auth" : view.startsWith("entry") ? "entry" : view, {
        query: `role=EMPLOYER${view === "signup" || view.startsWith("recovery") ? "&method=email" : ""}`, documents: { "jobs/job-test": testJob },
        ...(view === "entry" ? { guest: true } : {}),
        ...(view === "job-login-gate" ? { guest: true } : {}),
        ...(view === "entry-signed-in" ? { initialRole: "EMPLOYER", profile: { fullName: "An Account Holder With A Longer Name" } } : {}),
        ...(view === "recovery-error" ? { passwordResetError: "auth/network-request-failed" } : {}),
        ...(resourceSlugs.includes(view) ? { slug: view, pathname: `/${view}` } : {})
      }, { width, height: 900 });
      try {
        await page.getByRole("heading", { level: 1 }).waitFor();
        if (view === "otp-code") {
          await page.getByLabel("Mobile number", { exact: true }).fill("9000000000");
          await page.getByRole("checkbox").check();
          await page.getByRole("button", { name: "Send verification code", exact: true }).click();
          await page.getByLabel("Verification code", { exact: true }).waitFor();
        }
        if (view === "signup") {
          await page.getByRole("button", { name: "Create account", exact: true }).click();
        }
        if (view.startsWith("recovery")) {
          await page.getByRole("button", { name: "Forgot password?", exact: true }).click();
          if (view !== "recovery") {
            await page.getByLabel("Email", { exact: true }).fill("a-long-synthetic-account-address@example.invalid");
            await page.getByRole("button", { name: "Send reset link", exact: true }).click();
            if (view === "recovery-sent") {
              await page.getByRole("heading", { name: "Check your email", exact: true }).waitFor();
            } else {
              await page.getByRole("alert").waitFor();
            }
          }
        }
        await page.evaluate(() => document.fonts.ready);
        await page.waitForFunction(() => Array.from(document.images).every((image) => image.complete && image.naturalWidth > 0));
        const layout = await page.evaluate(() => {
          const overflow = [];
          const shortControls = [];
          const joinedLabels = [];
          const missingIcons = [];
          for (const element of document.querySelectorAll("h1, h2, h3, p, button, .button, input, select, .resource-page summary, .resource-contact-action, .resource-table")) {
            const rectangle = element.getBoundingClientRect();
            if (rectangle.width === 0 || rectangle.height === 0) continue;
            const label = element.textContent?.trim().slice(0, 70) || element.tagName;
            if (rectangle.left < -1 || rectangle.right > innerWidth + 1) overflow.push(label);
            if (element.matches("h1, h2, h3, p, button, .button") && element.scrollWidth > element.clientWidth + 1) overflow.push(`${label}: clipped text`);
            if (element.matches("button, .button, .resource-page summary, .resource-contact-action") && rectangle.height < 44) shortControls.push(`${label}: ${rectangle.height}px`);
          }
          for (const item of document.querySelectorAll(".detail-list li")) {
            const label = item.querySelector(":scope > strong");
            const value = item.querySelector(":scope > span");
            if (label && value && value.getBoundingClientRect().top < label.getBoundingClientRect().bottom - 1) joinedLabels.push(label.textContent);
          }
          for (const icon of document.querySelectorAll(".site-icon")) {
            const rectangle = icon.getBoundingClientRect();
            if (rectangle.width && rectangle.height && !icon.getBBox().width) missingIcons.push(icon.querySelector("use")?.getAttribute("href"));
          }
          return { overflow, shortControls, joinedLabels, missingIcons, pageOverflow: document.documentElement.scrollWidth > innerWidth + 1 };
        });
        await page.screenshot({ path: path.join(resultsDirectory, `${view}-${width}.png`), fullPage: true, animations: "disabled" });
        assert.equal(layout.pageOverflow, false, "Page scrolls horizontally");
        assert.deepEqual(layout.overflow, [], "Content leaves its container");
        assert.deepEqual(layout.shortControls, [], "Controls should have 44px touch targets");
        assert.deepEqual(layout.joinedLabels, [], "Detail labels and values must be visually separated");
        assert.deepEqual(layout.missingIcons, [], "Icon assets must render");
        if (view === "jobs" || view === "worker-jobs") {
          const firstJob = await page.locator(".public-job-card h3").first().boundingBox();
          assert.ok(firstJob && firstJob.y < 876, "A live job title should be visible in the first viewport");
        }
        assert.deepEqual(pageErrors.get(page), [], "Browser runtime errors");
      } finally { await page.close(); }
    });
  }
}

test("browser: public header exposes worker and employer entry and keyboard skip navigation", async () => {
  const page = await openFixture("home");
  try {
    assert.equal(await page.getByRole("link", { name: "Sign in", exact: true }).getAttribute("href"), "/app/auth?role=EMPLOYER");
    const postJob = page.locator(".site-header-actions").getByRole("link", { name: "Post a job", exact: true });
    assert.equal(await postJob.getAttribute("href"), "/app/employer/post-job");
    await page.keyboard.press("Tab");
    assert.equal(await page.getByRole("link", { name: "Skip to content" }).evaluate((element) => element === document.activeElement), true);
    await page.keyboard.press("Enter");
    assert.equal(await page.locator("#main-content").evaluate((element) => element === document.activeElement), true);
  } finally { await page.close(); }
});

test("browser: mobile menu opens, closes on Escape, and returns focus", async () => {
  const page = await openFixture("home", {}, { width: 390, height: 844 });
  try {
    const menu = page.getByRole("navigation", { name: "Primary", exact: true });
    assert.equal(await menu.isVisible(), false);
    await page.getByRole("button", { name: "Open menu", exact: true }).click();
    assert.equal(await menu.isVisible(), true);
    assert.equal(await page.getByRole("button", { name: "Close menu", exact: true }).getAttribute("aria-expanded"), "true");
    await page.keyboard.press("Escape");
    assert.equal(await menu.isVisible(), false);
    assert.equal(await page.getByRole("button", { name: "Open menu", exact: true }).evaluate((element) => element === document.activeElement), true);
  } finally { await page.close(); }
});

test("browser: Home introduces discovery while Jobs contains only live search results", async () => {
  const home = await openFixture("home");
  try {
    assert.equal(await home.locator(".category-tile").count(), 12);
    assert.equal(await home.locator(".city-directory a").count(), 15);
    assert.equal(await home.locator("#live-jobs").count(), 0);
    assert.equal(await home.locator(".public-job-card").count(), 0);
    assert.equal(await home.locator("#categories-heading").count(), 1);
    assert.equal(await home.locator("#cities-heading").count(), 1);
    await home.evaluate(() => document.fonts.ready);
    for (const anchor of ["#categories-heading", "#cities-heading"]) {
      await home.locator(anchor).evaluate((element) => element.scrollIntoView());
      const heading = await home.locator(anchor).boundingBox();
      const header = await home.locator(".site-header").boundingBox();
      assert.ok(heading.y >= header.y + header.height, `${anchor} must not be covered by navigation`);
    }
    const navigation = home.getByRole("navigation", { name: "Primary", exact: true });
    assert.equal(await navigation.getByRole("link", { name: "Home", exact: true }).getAttribute("aria-current"), "page");
    assert.equal(await navigation.getByRole("link", { name: "Find jobs", exact: true }).getAttribute("aria-current"), null);
    assert.equal(await home.locator(".employer-spotlight").getByRole("link", { name: "Post a job", exact: true }).getAttribute("href"), "/app/employer/post-job");
  } finally { await home.close(); }
  const jobs = await openFixture("jobs", { documents: { "jobs/job-test": testJob } });
  try {
    assert.equal(await jobs.locator("#live-jobs").count(), 1);
    assert.equal(await jobs.locator(".public-job-card").count(), 1);
    assert.equal(await jobs.locator(".category-tile, .city-directory, .employer-spotlight, .app-band").count(), 0);
    const navigation = jobs.getByRole("navigation", { name: "Primary", exact: true });
    assert.equal(await navigation.getByRole("link", { name: "Find jobs", exact: true }).getAttribute("aria-current"), "page");
    assert.equal(await navigation.getByRole("link", { name: "Home", exact: true }).getAttribute("aria-current"), null);
    assert.equal(await jobs.getByRole("link", { name: "Browse city guides", exact: true }).getAttribute("href"), "/#cities-heading");
  } finally { await jobs.close(); }
});

test("browser: Home search uses GET and opens matching live jobs in the selected city", async () => {
  const page = await openFixture("home");
  try {
    await page.getByLabel("Job title or keyword").fill("driver");
    await page.getByLabel("City or location").selectOption("Hyderabad");
    const submitted = await page.getByRole("search").evaluate((form) => ({ action: new URL(form.action).pathname, method: form.method, values: Object.fromEntries(new FormData(form)) }));
    assert.deepEqual(submitted, { action: "/jobs", method: "get", values: { q: "driver", city: "Hyderabad" } });
  } finally { await page.close(); }
  const results = await openFixture("jobs", { searchParams: { q: "driver", city: "Hyderabad" }, documents: {
    "jobs/hyderabad-driver": { ...testJob, title: "Hyderabad driver opening", city: "Hyderabad", category: "DRIVING" },
    "jobs/delhi-driver": { ...testJob, title: "Delhi driver opening", city: "Delhi", category: "DRIVING" }
  } });
  try {
    await results.getByRole("heading", { name: "Hyderabad driver opening", exact: true }).waitFor();
    assert.equal(await results.locator(".public-job-card").count(), 1);
    assert.equal(await results.locator(".public-job-card").getByRole("link", { name: "View details", exact: true }).getAttribute("href"), "/jobs/hyderabad-driver");
    assert.equal(await results.getByRole("heading", { name: "Delhi driver opening", exact: true }).count(), 0);
    assert.equal(await results.getByLabel("Job title or keyword", { exact: true }).inputValue(), "driver");
    assert.equal(await results.getByLabel("City", { exact: true }).inputValue(), "Hyderabad");
    assert.equal(await results.locator(".category-tile").count(), 0);
  } finally { await results.close(); }
});

test("browser: password visibility is reversible without changing its value", async () => {
  const page = await openFixture();
  try {
    await page.getByLabel("Password", { exact: true }).fill("synthetic-test-password");
    await page.getByRole("button", { name: "Show password", exact: true }).click();
    assert.equal(await page.getByLabel("Password", { exact: true }).getAttribute("type"), "text");
    await page.getByRole("button", { name: "Hide password", exact: true }).click();
    assert.equal(await page.getByLabel("Password", { exact: true }).getAttribute("type"), "password");
    assert.equal(await page.getByLabel("Password", { exact: true }).inputValue(), "synthetic-test-password");
  } finally { await page.close(); }
});

test("browser: the public worker list filters real openings and uses canonical detail links", async () => {
  const page = await openFixture("worker-jobs", { documents: {
    "jobs/job-test": testJob,
    "jobs/driver-test": { ...testJob, title: "Test driver role", category: "DRIVING", jobId: "driver-test" },
    "jobs/closed-test": { ...testJob, title: "Closed test job", isActive: false }
  } });
  try {
    await page.getByRole("heading", { name: "Test delivery role", exact: true }).waitFor();
    assert.equal(await page.locator(".public-job-card").count(), 2);
    assert.equal(await page.getByRole("heading", { name: "Closed test job", exact: true }).count(), 0);
    await page.getByLabel("Job title or keyword", { exact: true }).fill("delivery");
    await page.getByRole("button", { name: "Find jobs", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "1 opening shown" }).waitFor();
    assert.equal(await page.locator(".public-job-card").count(), 1);
    assert.equal(await page.locator(".public-job-card").getByText("Rs 18,000 / monthly", { exact: true }).count(), 1);
    assert.equal(await page.getByRole("link", { name: "View details", exact: true }).getAttribute("href"), "/jobs/job-test");
    await page.getByLabel("Job title or keyword", { exact: true }).fill("driver jobs");
    await page.getByRole("button", { name: "Find jobs", exact: true }).click();
    await page.getByRole("heading", { name: "Test driver role", exact: true }).waitFor();
    assert.equal(await page.getByRole("heading", { name: "Test driver role", exact: true }).count(), 1);
    assert.equal(await page.getByRole("heading", { name: "Test delivery role", exact: true }).count(), 0);
    await page.getByRole("button", { name: "Reset filters", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "2 openings shown" }).waitFor();
    await page.getByLabel("Category", { exact: true }).selectOption("delivery");
    await page.getByRole("button", { name: "Find jobs", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "1 opening shown" }).waitFor();
    assert.equal(await page.getByRole("heading", { name: "Test delivery role", exact: true }).count(), 1);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: a failed full-detail request stays retryable without revealing stale content", async () => {
  const page = await openFixture("worker-detail", { documents: { "jobs/job-test": testJob }, jobApiUnavailable: true });
  try {
    await page.getByRole("alert").waitFor();
    assert.equal(await page.getByRole("heading", { name: "About this job" }).count(), 0);
    await page.evaluate(() => { fixture.jobApiUnavailable = false; });
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await page.getByRole("heading", { name: "About this job" }).waitFor();
    assert.equal(await page.getByRole("alert").count(), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: job apply and contact handoffs never create a web application or conversation", async () => {
    const page = await openFixture("worker-detail", { guest: false, documents: { "jobs/job-test": { ...testJob, jobId: "LEGACY-JOB" } }, jobId: "LEGACY-JOB" });
    try {
      await page.getByRole("link", { name: "Apply in the app", exact: true }).waitFor();
      for (const name of ["Apply in the app", "Contact employer"]) {
        const action = page.getByRole("link", { name, exact: true });
        assert.equal(await action.getAttribute("href"), PLAY_STORE_URL);
        await action.evaluate((element) => element.addEventListener("click", (event) => event.preventDefault(), { once: true }));
        await action.click();
      }
      assert.equal(await page.getByLabel("Cover letter").count(), 0);
      assert.equal(await page.getByRole("button", { name: "Apply now", exact: true }).count(), 0);
      assert.equal(await page.evaluate(() => fixture.writes.length + fixture.conversations.length), 0);
      assert.equal(await page.evaluate(() => fixture.documents["jobs/job-test"].applicationCount), 0);
      assert.deepEqual(pageErrors.get(page), []);
    } finally { await page.close(); }
});

test("browser: city landing pages expose canonical category links and actual live jobs", async () => {
  const page = await openFixture("resource", { slug: "jobs-in-hyderabad", pathname: "/jobs-in-hyderabad" });
  try {
    await page.getByRole("heading", { name: "Jobs in Hyderabad", level: 1 }).waitFor();
    assert.equal(await page.locator(".category-tile").first().getAttribute("href"), "/delivery-jobs-hyderabad");
    assert.equal(await page.locator(".city-directory").getByRole("link", { name: "Bangalore", exact: true }).getAttribute("href"), "/jobs-in-bangalore");
    assert.equal(await page.getByRole("link", { name: "View live jobs", exact: true }).getAttribute("href"), "#live-jobs");
    const schema = JSON.parse(await page.locator('script[type="application/ld+json"]').textContent());
    assert.equal(schema["@graph"][0].spatialCoverage.name, "Hyderabad");
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: live listings paginate without duplicates and remove expired jobs on refresh", async () => {
  const documents = Object.fromEntries(Array.from({ length: 26 }, (_, index) => [
    `jobs/job-${String(index).padStart(3, "0")}`, { ...testJob, title: `Delivery opening ${index}`, city: "Hyderabad" }
  ]));
  const page = await openFixture("jobs", { guest: true, documents, fakeClock: true });
  try {
    assert.equal(await page.locator(".public-job-card").count(), 20);
    await page.getByRole("link", { name: "Load more jobs", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "26 openings shown" }).waitFor();
    assert.equal(await page.locator(".public-job-card").count(), 26);
    assert.equal(new Set(await page.locator(".public-job-card h3 a").allTextContents()).size, 26);
    await page.evaluate(() => { fixture.documents["jobs/job-000"].isFilled = true; });
    await page.getByRole("button", { name: "Refresh jobs", exact: true }).click();
    await page.getByRole("status").filter({ hasText: "20 openings shown" }).waitFor();
    assert.equal(await page.getByRole("heading", { name: "Delivery opening 0", exact: true }).count(), 0);
  } finally { await page.close(); }
});

test("browser: full-detail loading has a deadline even when the auth token never resolves", async () => {
  const page = await openFixture("worker-detail", { holdJobToken: true, fakeClock: true });
  try {
    await page.getByRole("status").filter({ hasText: "Loading full job details" }).waitFor();
    await page.clock.fastForward(10_001);
    await page.getByRole("alert").filter({ hasText: "too long" }).waitFor();
    assert.equal(await page.evaluate(() => fixture.detailRequests?.length || 0), 0);
    assert.equal(await page.getByRole("button", { name: "Try again", exact: true }).isEnabled(), true);
  } finally { await page.close(); }
});

test("browser: Google sign-in preserves a public job destination instead of sending users to a dashboard", async () => {
  const page = await openFixture("auth", { guest: true, query: "role=WORKER&next=%2Fjobs%2Fchosen-job" });
  try {
    await page.getByRole("button", { name: "Continue with Google", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.includes("/jobs/chosen-job"));
    assert.deepEqual(await page.evaluate(() => fixture.redirects), ["/jobs/chosen-job"]);
  } finally { await page.close(); }
});

async function fillJobPost(page) {
  for (const [label, value] of Object.entries({
    "Job title": "Test delivery opening", "Company name": "Test Company", Location: "Test district", City: "Hyderabad", "Area or locality": "Madhapur",
    "Contact number": "9000000000", "Pay amount": "18000", Description: "Morning deliveries from the local store.",
    "Shift timing": "Morning", Vacancies: "2"
  })) {
    await page.getByLabel(label, { exact: true }).fill(value);
  }
}

for (const provider of ["Google", "phone OTP"]) {
test(`browser: guest employer draft survives ${provider} login and publishes only after confirmation`, async () => {
  const page = await openFixture("employer-post", { guest: true });
  try {
    await fillJobPost(page);
    await page.getByRole("button", { name: "Sign in to post job", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.length === 1);
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    const draft = await page.evaluate(() => JSON.parse(sessionStorage.getItem("dutype:employer-job-draft:v1")));
    assert.equal(draft.form.title, "Test delivery opening");
    assert.equal(draft.ownerId, null);
    await page.evaluate(() => { fixture.query = fixture.redirects[0].split("?")[1]; fixture.showView("auth"); });
    if (provider === "Google") {
      await page.getByRole("button", { name: "Continue with Google", exact: true }).click();
    } else {
      await page.getByLabel("Mobile number", { exact: true }).fill("9000000000");
      await page.getByRole("checkbox").check();
      await page.getByRole("button", { name: "Send verification code", exact: true }).click();
      await page.getByLabel("Verification code", { exact: true }).fill("123456");
      await page.getByRole("button", { name: "Verify and continue", exact: true }).click();
    }
    await page.waitForFunction(() => fixture.redirects.includes("/app/employer/post-job"));
    await page.evaluate(() => fixture.showView("employer-post"));
    await page.getByLabel("Job title", { exact: true }).waitFor();
    assert.equal(await page.getByLabel("Job title", { exact: true }).inputValue(), "Test delivery opening");
    assert.equal(await page.getByLabel("Pay amount", { exact: true }).inputValue(), "18000");
    assert.equal(await page.evaluate(() => Object.keys(fixture.documents).some((key) => key.startsWith("jobs/"))), false);
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.includes("/app/employer/jobs"));
    const jobs = await page.evaluate(() => Object.entries(fixture.documents).filter(([key]) => key.startsWith("jobs/")));
    assert.equal(jobs.length, 1);
    assert.equal(jobs[0][1].employerId, provider === "Google" ? "google-employer" : "phone-employer");
    assert.equal(await page.evaluate(() => sessionStorage.getItem("dutype:employer-job-draft:v1")), null);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});
}

test("browser: expired and another account's employer drafts are not restored", async () => {
  for (const scenario of ["expired", "another-account"]) {
    const page = await openFixture("employer-post", { guest: true });
    try {
      await fillJobPost(page);
      await page.getByRole("button", { name: "Sign in to post job", exact: true }).click();
      await page.waitForFunction(() => fixture.redirects.length === 1);
      await page.evaluate((scenario) => {
        const key = "dutype:employer-job-draft:v1";
        const draft = JSON.parse(sessionStorage.getItem(key));
        if (scenario === "expired") draft.savedAt = Date.now() - 3_600_001;
        else draft.ownerId = "another-account";
        sessionStorage.setItem(key, JSON.stringify(draft));
        fixture.showView("auth");
      }, scenario);
      await page.getByRole("heading", { level: 1 }).waitFor();
      await page.evaluate(() => fixture.showView("employer-post"));
      await page.getByLabel("Job title", { exact: true }).waitFor();
      assert.equal(await page.getByLabel("Job title", { exact: true }).inputValue(), "");
    } finally { await page.close(); }
  }
});

test("browser: employers cannot publish incomplete or invalid jobs", async () => {
  const page = await openFixture("employer-post", { initialRole: "EMPLOYER" });
  try {
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    assert.equal(await page.getByLabel("City", { exact: true }).evaluate((input) => input.validity.valueMissing), true);
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    await fillJobPost(page);
    await page.getByLabel("Pay amount", { exact: true }).fill("-100");
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    assert.equal(await page.getByLabel("Pay amount", { exact: true }).evaluate((input) => input.validity.rangeUnderflow), true);
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    await page.getByLabel("Pay amount", { exact: true }).fill("18000");
    await page.getByLabel("Vacancies", { exact: true }).fill("1.5");
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    assert.equal(await page.getByLabel("Vacancies", { exact: true }).evaluate((input) => input.validity.stepMismatch), true);
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    await page.getByLabel("Vacancies", { exact: true }).fill("2");
    await page.getByLabel("Latitude", { exact: true }).fill("17.44");
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    await page.getByRole("alert").waitFor();
    assert.equal(await page.evaluate(() => fixture.writes.length), 0);
  } finally { await page.close(); }
});

test("browser: employer posting commits job and profile together with authenticated ownership", async () => {
  const page = await openFixture("employer-post", { initialRole: "EMPLOYER" });
  try {
    await fillJobPost(page);
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.includes("/app/employer/jobs"));
    const documents = await page.evaluate(() => fixture.documents);
    const jobs = Object.entries(documents).filter(([key]) => key.startsWith("jobs/"));
    assert.equal(jobs.length, 1);
    assert.equal(jobs[0][1].employerId, "test-user");
    assert.equal(jobs[0][1].city, "Hyderabad");
    assert.equal(jobs[0][1].area, "Madhapur");
    assert.equal(jobs[0][1].payAmount, "18000");
    assert.equal(jobs[0][1].vacancies, 2);
    assert.equal(jobs[0][1].applicationCount, 0);
    assert.equal(jobs[0][1].isActive, true);
    assert.equal(Object.hasOwn(documents, "employer_profiles/test-user"), false);
    assert.equal(documents["users/test-user"].companyName, "Test Company");
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: failed employer batches preserve the profile and reuse one job on retry", async () => {
  const page = await openFixture("employer-post", { initialRole: "EMPLOYER", failWritesTo: ["jobs/generated-1"] });
  try {
    const before = await page.evaluate(() => fixture.documents);
    await fillJobPost(page);
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    await page.getByRole("alert").waitFor();
    assert.deepEqual(await page.evaluate(() => fixture.documents), before);
    await page.evaluate(() => { fixture.failWritesTo = []; });
    await page.getByRole("button", { name: "Post job", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.includes("/app/employer/jobs"));
    assert.deepEqual(await page.evaluate(() => Object.keys(fixture.documents).filter((key) => key.startsWith("jobs/"))), ["jobs/generated-1"]);
  } finally { await page.close(); }
});

test("browser: employers post and review a seeded app application through acceptance", async () => {
  let documents;
  let jobId;
  const employer = await openFixture("employer-post", { initialRole: "EMPLOYER" });
  try {
    await fillJobPost(employer);
    await employer.getByLabel("Vacancies", { exact: true }).fill("1");
    await employer.getByRole("button", { name: "Post job", exact: true }).click();
    await employer.waitForFunction(() => fixture.redirects.includes("/app/employer/jobs"));
    documents = await employer.evaluate(() => fixture.documents);
    jobId = Object.keys(documents).find((key) => key.startsWith("jobs/")).split("/")[1];
  } finally { await employer.close(); }

  const worker = await openFixture("worker-detail", { documents, jobId, user: { uid: "test-worker", email: "worker@example.invalid" } });
  try {
    await worker.getByRole("link", { name: "Apply in the app", exact: true }).waitFor();
    assert.equal(await worker.getByRole("link", { name: "Apply in the app", exact: true }).getAttribute("href"), PLAY_STORE_URL);
    assert.equal(await worker.evaluate(() => fixture.writes.length), 0);
  } finally { await worker.close(); }

  documents[`jobs/${jobId}`].applicationCount = 1;
  documents[`job_applications/test-worker_${jobId}`] = {
    ...pendingApplication, jobId, source: "ANDROID_APP", statusHistory: [{ status: "PENDING", timestamp: 2000 }]
  };
  const hiring = await openFixture("employer-applications", { documents, initialRole: "EMPLOYER" });
  try {
    await hiring.getByRole("button", { name: "Review", exact: true }).click();
    await hiring.locator(".application-card .status-pill").filter({ hasText: "Under Review" }).waitFor();
    await hiring.getByRole("button", { name: "Accept", exact: true }).click();
    await hiring.locator(".application-card .status-pill").filter({ hasText: "Accepted" }).waitFor();
    documents = await hiring.evaluate(() => fixture.documents);
    const application = documents[`job_applications/test-worker_${jobId}`];
    assert.equal(application.status, "ACCEPTED");
    assert.deepEqual(application.statusHistory.map((entry) => entry.status), ["PENDING", "UNDER_REVIEW", "ACCEPTED"]);
    assert.equal(application.verification.workerId, "test-worker");
    assert.equal(application.verification.employerId, "test-user");
    assert.match(application.verificationCode, /^DTP-[A-Z2-9]{6}$/);
    assert.equal(documents[`jobs/${jobId}`].acceptedCount, 1);
    assert.equal(documents[`jobs/${jobId}`].isFilled, true);
    assert.equal(documents[`jobs/${jobId}`].vacancyStatus, "FILLED");
    assert.deepEqual(pageErrors.get(hiring), []);
  } finally { await hiring.close(); }
});

const pendingApplication = {
  workerId: "test-worker", workerName: "Test Worker", employerId: "test-user", jobId: "job-test",
  jobTitle: "Test delivery role", status: "PENDING", statusHistory: [], appliedAt: 2000, active: true
};

test("browser: failed acceptance cannot consume a vacancy and retry increments once", async () => {
  const page = await openFixture("employer-applications", {
    documents: {
      "jobs/job-test": { ...testJob, employerId: "test-user", vacancies: 1 },
      "job_applications/application-test": pendingApplication
    },
    failWritesTo: ["job_applications/application-test"]
  });
  try {
    await page.getByRole("button", { name: "Accept", exact: true }).click();
    await page.getByRole("alert").waitFor();
    assert.equal(await page.evaluate(() => fixture.documents["jobs/job-test"].acceptedCount || 0), 0);
    assert.equal(await page.evaluate(() => fixture.documents["job_applications/application-test"].status), "PENDING");
    await page.evaluate(() => { fixture.failWritesTo = []; });
    await page.getByRole("button", { name: "Accept", exact: true }).click();
    await page.locator(".application-card .status-pill").filter({ hasText: "Accepted" }).waitFor();
    assert.equal(await page.evaluate(() => fixture.documents["jobs/job-test"].acceptedCount), 1);
  } finally { await page.close(); }
});

test("browser: stale employer actions cannot overfill a job or overwrite withdrawal", async () => {
  for (const change of ["filled", "withdrawn"]) {
    const page = await openFixture("employer-applications", { documents: {
      "jobs/job-test": { ...testJob, employerId: "test-user", vacancies: 1 },
      "job_applications/application-test": pendingApplication
    } });
    try {
      await page.getByRole("button", { name: "Accept", exact: true }).waitFor();
      await page.evaluate((change) => {
        if (change === "filled") Object.assign(fixture.documents["jobs/job-test"], { isFilled: true, acceptedCount: 1 });
        else fixture.documents["job_applications/application-test"].status = "WITHDRAWN";
      }, change);
      await page.getByRole("button", { name: "Accept", exact: true }).click();
      await page.getByRole("alert").waitFor();
      assert.equal(await page.evaluate(() => fixture.writes.length), 0);
    } finally { await page.close(); }
  }
});

test("browser: worker messaging route hands off without requesting a web conversation", async () => {
  const page = await openFixture("worker-messages", { guest: true });
  try {
    await page.getByRole("heading", { name: "Contact employers in the DutyPe app", level: 1 }).waitFor();
    assert.equal(await page.getByRole("link", { name: "Contact employer", exact: true }).getAttribute("href"), PLAY_STORE_URL);
    assert.equal(await page.evaluate(() => fixture.conversations.length + fixture.writes.length), 0);
  } finally { await page.close(); }
});

test("browser: a stalled account check falls back to the job sign-in gate", async () => {
  const page = await openFixture("worker-detail", { holdAuthCheck: true, fakeClock: true });
  try {
    await page.getByRole("status").filter({ hasText: "Checking your account" }).waitFor();
    await page.clock.fastForward(10_001);
    await page.getByRole("link", { name: "Sign in to view details", exact: true }).waitFor();
    assert.equal(await page.evaluate(() => fixture.detailRequests?.length || 0), 0);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});

test("browser: employer edits update searchable city and area without denied mirror writes", async () => {
  const page = await openFixture("employer-edit", { initialRole: "EMPLOYER", documents: { "jobs/job-test": {
    ...testJob, employerId: "test-user", postedAt: Date.now(), city: "Hyderabad", area: "Madhapur"
  } } });
  try {
    await page.getByLabel("City", { exact: true }).fill("Pune");
    await page.getByLabel("Area or locality", { exact: true }).fill("Kothrud");
    await page.getByLabel("Location", { exact: true }).fill("Work address, Pune");
    await page.getByRole("button", { name: "Update job", exact: true }).click();
    await page.waitForFunction(() => fixture.redirects.includes("/app/employer/jobs"));
    const job = await page.evaluate(() => fixture.documents["jobs/job-test"]);
    assert.equal(job.city, "Pune");
    assert.equal(job.area, "Kothrud");
    assert.equal(job.location, "Work address, Pune");
    assert.equal(job.employerId, "test-user");
    assert.deepEqual(await page.evaluate(() => fixture.writes.map((entry) => entry.path)), ["users/test-user", "jobs/job-test"]);
    assert.deepEqual(pageErrors.get(page), []);
  } finally { await page.close(); }
});