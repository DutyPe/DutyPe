# DutyPe Web

This folder contains the new Next.js website for DutyPe.

## What it replaces

The old web layer in the repo was split across:

- static marketing and SEO HTML in `public/`
- app-open bridge pages such as `public/jobs/index.html`, `public/worker/index.html`, and `public/app-redirect.html`
- browser-import Firebase admin pages in `public/admin/`

The new source of truth for the website is now `web/`, not `public/`.

## Current route coverage

Public routes:

- `/`
- `/jobs`
- `/jobs/[jobId]`
- `/worker/[workerId]`
- `/employer/[employerId]`
- `/refer/[[...code]]`
- `/application/[applicationId]`
- `/chat/[chatId]`
- `/profile`
- `/notifications`
- `/app-redirect`
- `/<legacy-slug>` for migrated legacy pages like `/privacy`, `/terms`, `/safety`, `/refund`, `/contact`, `/faq`, `/jobs-near-me`, `/delivery-jobs`, `/driver-jobs`, `/maid-jobs`, `/jobs-in-hyderabad`

Product routes:

- `/app`
- `/app/auth`
- `/app/worker/*`
- `/app/employer/*`

Admin routes:

- `/admin`
- `/admin/login`
- `/admin/users`
- `/admin/jobs`
- `/admin/post-job`
- `/admin/applications`
- `/admin/referrals`
- `/admin/check-and-create-code`
- `/admin/create-test-referral`
- `/admin/test-referral`
- `/admin/announcements`

## Legacy compatibility

The Next app now includes compatibility handling for old `.html` links through `web/middleware.ts`.

Examples:

- `/privacy.html` -> `/privacy`
- `/jobs-near-me.html` -> `/jobs-near-me`
- `/admin/login.html` -> `/admin/login`
- `/admin/referrals.html` -> `/admin/referrals`
- `/admin/post-job.html` -> `/admin/post-job`

This is important because several repo scripts and old bookmarks still reference legacy static URLs.

## Public content system

The migrated public site content now lives in:

- `web/lib/public-site.ts`
- `web/app/[slug]/page.tsx`
- `web/components/public/*`

That file now acts as the replacement for many old standalone HTML documents under `public/`.

## SEO and metadata

The Next app now owns:

- route metadata through App Router pages
- `web/app/robots.ts`
- `web/app/sitemap.ts`
- Google site verification file under `web/public/`
- copied platform files from old Hosting under `web/public/`:
  - `googlef0148bf44dd14fa3.html`
  - `app-ads.txt`
  - `.well-known/assetlinks.json`

## Scripts audit

Not all scripts under `scripts/` are related to the old `public/` website.

Script groups:

- Legacy public-site generation scripts:
  - `generate-city-job-pages.js`
  - `generate-comprehensive-city-pages.js`
  - `enhance-city-pages.js`
  - `update-all-city-pages.sh`
  - `deploy-seo-pages.sh`
- Admin/auth operational scripts:
  - `create-admin-user.js`
  - `set-admin-claims.js`
- Firestore audit, cleanup, seeding, and migration scripts:
  - most of the remaining files in `scripts/`

Important distinction:

- `create-admin-user.js` and `set-admin-claims.js` are not public-folder scripts
- they are Firebase Auth and admin-access scripts used by the new React admin too
- `generate-city-job-pages.js` is currently a zero-byte stale file and should be treated as dead legacy baggage, not an active generator

## Environment

Configure `web/.env.local` using `web/.env.example` and the registered **Web App** SDK configuration from Firebase Console > Project settings > General > Your apps. Use `apiKey`, `authDomain`, `projectId` and `appId` from the same project. Storage and messaging fields are optional unless those services are used. Do not use an Admin private key in a `NEXT_PUBLIC_*` variable.

The embedded fallback key was removed after the 2026-09-15 sign-up capture proved Google rejected that exact key with `API_KEY_INVALID`. Without explicit web configuration, public pages remain available but account requests are disabled and show a configuration message. This prevents known-invalid requests; it does not repair or replace the rejected Google API key.

Restart `next dev` after changing environment values. `NEXT_PUBLIC_*` values are compiled into production JavaScript, so rerun `npm --prefix web run build` before restarting `next start`. After supplying a valid key, verify that Email/Password authentication is enabled and configure permitted web origins/API restrictions without removing unrelated protections. CORS or authorized-domain changes alone do not resolve `API_KEY_INVALID`.

Server-rendered live jobs and verified detail access also require server-side Firebase access. Locally, configure `GOOGLE_APPLICATION_CREDENTIALS`, a `FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH`/JSON value, or the individual `FIREBASE_ADMIN_*` fields. These must never be exposed as `NEXT_PUBLIC_*` values. On an identified Firebase/Cloud Run runtime, the server can use the platform's application-default credentials; its service identity must have the required Firestore/Auth permissions. The existing Admin pages use the same server setup.

If the page says **Live job listings aren't connected yet**, no supported server credential source is configured. The API reports `status: "unavailable"` with `unavailableReason: "setup-required"`. The page shows this once, without a second result-status message or futile automatic retries. It does not mean there are zero jobs.

To connect the local server, use an existing authorized credential file for the DutyPe Firebase project, keep it outside the repository, and set its path in `web/.env.local`:

```dotenv
FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH=C:/secure/dutype-server.json
```

The path above is an example, not a file supplied by the app. Do not paste private-key contents into chat. Restart the server after configuring its credentials; rebuild production to refresh previously generated city pages, then reload the browser. Browser Firebase API-key settings alone do not authorize these server reads. A temporary read failure after configuration remains a separate unavailable state with a spaced **Try again** button.

For an existing Firebase Auth user, you can promote them with:

`node scripts/set-admin-claims.js --email admin@dutype.com`

## Location SEO And Employer Publishing

### Home Versus Jobs

- **Home (`/`)** is the discovery entry point: a quick search that opens Jobs, the complete category and city directories, employer posting entry, safety and Android app links. It does not load the live result feed.
- **Jobs (`/jobs`)** is the working search page: live openings, keyword/city/area/category filters, optional nearby search, pagination and full-detail links. It does not repeat Home's category tiles, city directory or promotional sections. Compact links return to Home's guide sections when needed.
- Navigation identifies Home and Find jobs separately. Home search retains its keyword and city when opening the live results; result counts always refer to jobs rather than category matches.

### Live Discovery Plan

The current implementation follows this flow:

1. A guest opens `/jobs`, a supported city page, or a category/location page and sees actual public job summaries without account creation.
2. Keyword, city, area and category filters use the same server query. **Use my location** requests browser permission only after a click and adds a radius filter; denial leaves manual search usable.
3. Each summary links to `/jobs/{id}`. That page renders a public title, employer name, broad area/city and pay summary. Full description, exact work address and shift data require web sign-in.
4. Google, phone OTP or existing email sign-in returns to the selected job. Apply and Contact remain Android app handoffs after details are unlocked; the web does not submit worker applications.
5. Search engines and AI browsers receive the same public summaries and links as other guests, without special crawler access to hidden detail fields.

### Live Data Contract

- `GET /api/jobs` returns an allowlisted summary page. `POST /api/jobs` accepts nearby coordinates in the request body, keeping them out of search URLs. Browser coordinates are not stored in localStorage/sessionStorage or persisted to the user's profile by this feature.
- `GET /api/jobs/{id}` verifies a Firebase bearer ID token, checks revocation, rejects anonymous Firebase sessions, and returns `private, no-store` responses. It does not read the job before validating authentication. Contact phone numbers are not returned; contact continues in the Android app.
- Only explicitly active, unfilled, non-expired records with available capacity are public. Missing or malformed expiry/capacity information is handled conservatively. Public summaries exclude the full description, exact address, employer UID and precise job coordinates.
- Explicit city fields take priority over address text. Legacy city inference uses recognizable city address segments rather than matching any city name inside a street. Area filtering can search legacy location text on the server. New employer posts collect City and Area/locality; existing documents are not bulk-migrated or assigned guessed locations.
- The feed reads in stable document-ID order, up to 100 candidates per batch and 500 candidates per request, returning at most 20 matches plus a continuation cursor. It is not described as globally newest-first or nearest-first. **Search more listings** remains available even when a scanned batch has no local matches, so older matches are not silently lost behind the old 48-record limit.
- Server candidate batches cache for up to 60 seconds. Visible first-page views refresh each minute; once additional pages are loaded, explicit refresh restarts the search while preserving stable pagination until then. Expired summaries are removed as the local clock advances. Distances are approximate straight-line distances, not travel times.
- Server reads have a deadline; the UI also bounds requests and ignores stale responses. Backend/configuration failure is labelled unavailable, never as zero jobs. Empty results are shown only after a successful query. No sample jobs or invented counts are substituted for a failing service.
- Exact coordinates are required for radius filtering; listings without usable coordinates are still available in city/area searches. Geolocation on a city guide stays constrained to that city, while the main directory can switch to the user's current location.

### Detail Privacy Boundary

The website's public response and authenticated API enforce the new distinction, but the repository's shared `jobs` Firestore rule still permits public reads. **This is not database-wide detail privacy.** A direct Firestore client can bypass the website while that rule remains public. Do not claim confidential job details are protected until the Android guest-list flow is migrated to the summary API or a dedicated public projection and the raw job rule is tightened in a coordinated, tested rollout. No shared Firestore rules or Android behavior were changed by this web feature.

### Search Discovery

- Supported city pages such as `/jobs-in-hyderabad` and category/location pages such as `/driver-jobs-hyderabad` now have indexable metadata, self-referencing canonical URLs, location-specific titles/descriptions, and Open Graph/Twitter metadata. Unknown city slugs return 404 instead of generating arbitrary pages.
- City navigation links directly to permanent city URLs. Each city links to its category pages, and category pages link back to the city and other supported locations.
- `sitemap.xml` lists canonical guides and real live job URLs, using actual job update dates only when present. City/category pages render `CollectionPage`/`BreadcrumbList` data and actual result `ItemList` links; real job pages render matching public `WebPage` and breadcrumb data. Unsupported or closed jobs are not fabricated to keep a route indexable.
- Live job pages and guide results can revalidate after 60 seconds, and the sitemap after 300 seconds. The initial sitemap job scan is bounded to 5,000 active candidates; larger catalogs need sitemap partitioning and an indexed search/geo projection before scaling. Query/continuation variants do not create unlimited indexable doorway pages.
- Google job rich results require compliant public job descriptions and other required fields. Because full descriptions are intentionally login-gated here, the site does not emit `JobPosting` markup containing hidden details. Public pages can still be indexed normally. There is no invented employer, salary, vacancy count, crawler-only full description, or promise of AI recommendations.
- After deployment, verify the HTTPS domain in Google Search Console, submit `https://dutype.in/sitemap.xml`, and use URL Inspection on representative city/category pages. Indexing and search ranking depend on Google, content quality, competition and time; the website cannot force immediate placement when someone searches a location.

### Worker Actions

Workers can browse guides and real job summaries without signing in. Full job details require a web account; the sign-in destination retains the chosen job. Once details are unlocked, Apply and Contact employer links open DutyPe's Google Play listing. The worker messages route also hands off to the app. The store handoff does not automatically install the app, submit an application, or promise a deferred deep link after installation.

### Employer Journey

`/app/employer/post-job` shows the posting form before login. A valid draft is retained when a guest chooses **Sign in to post job**, then the employer authenticates using Google or a phone verification code and returns to that form. Publishing requires a second explicit **Post job** action; authentication alone does not publish.

- Google uses Firebase's `GoogleAuthProvider` and popup sign-in. Phone sign-in uses `signInWithPhoneNumber` with an invisible Firebase `RecaptchaVerifier`, SMS consent, a six-digit code field, a 60-second resend cooldown, and wrong/expired-code recovery. OTPs stay only in component memory and are never written to profile documents or browser storage.
- First-time Google/phone users receive a profile in the canonical `users/{uid}` document. Existing names, saved jobs, referral data and other roles are preserved. The selected marketplace role is added without replacing the Firebase identity. Separate Google and phone identities are not merged automatically; use the existing sign-in method unless a verified account-linking flow is provided.
- The current rules do not expose `worker_profiles` or `employer_profiles`. These new account and publishing flows therefore use `users` directly, without adding permissions or making denied mirror writes. Older role-profile documents are not deleted or rewritten.
- Draft storage is limited to `sessionStorage` in the current tab, saved when continuing to sign-in, with a one-hour expiry and an account owner. Another signed-in account's draft is not restored. It is cleared after successful publication. If tab storage fails, the form stays visible instead of navigating away and losing the draft.
- Jobs and employer profile updates commit together. Jobs use the authenticated UID as `employerId`; a failed commit leaves no partial profile/job update. Previously existing email sign-in, signup and password recovery remain available under **Email**.

### Firebase Release Setup

1. Supply the valid registered Firebase Web App configuration described above. The previously rejected key is not reused or replaced with a guessed key.
2. In Firebase Authentication, enable **Google** and **Phone** providers for the same project; configure the Google provider's support email and the authorized production/staging domains. Keep Email/Password enabled while supporting existing email users.
3. Review Phone Auth's SMS region policy, billing requirements, quotas, and abuse protection for the countries served. Use an authorized HTTPS staging domain for real phone/reCAPTCHA checks; a successful localhost preview or a mocked OTP test does not prove that real SMS works there.
4. Preserve reCAPTCHA and provider protections. Test popup blocking, reCAPTCHA, wrong and expired codes, resend limits, new and existing accounts, and account-switch behavior with approved staging users. Never enable Firebase test-phone bypasses in production.
5. Configure the server identity for the live feed. On staging, verify guest summary responses, rejected/anonymous/expired tokens, actual Google/OTP login, city/area/radius filtering, missing coordinates, live expiry, and a job beyond the first query batch. Publish a staging job and confirm its city/area are searchable and it remains visible to Android.
6. Complete the shared database privacy rollout described above before treating the login gate as a security boundary beyond the website API. These changes do not deploy rules, enable providers, send real SMS, or modify production accounts.

The network policy currently blocks Firebase Console/API and browser origins, so real Google login, SMS delivery and deployment remain release prerequisites. The browser suite uses isolated provider adapters and no real accounts.

## Verification

Run these commands from the repository root:

```powershell
npm --prefix web test
npm --prefix web run type-check
npm --prefix web run lint
npm --prefix web run build
npm --prefix web run dev -- --hostname 127.0.0.1 --port 3100
```

The original UX gate generated 256 production pages. The source suite now covers supported-page SEO metadata, sitemap/schema accuracy and phone normalization in addition to existing contracts. The current browser suite covers both Google and phone-OTP draft-to-publish journeys using actual route components with isolated services. Live authentication remains unverified until the Firebase release setup is completed.

The browser suite uses real React components and Edge on Windows, with Next navigation and Firebase replaced by explicit test adapters. A reserved `.invalid` origin is intercepted entirely in memory. Only the local logo and icon sprite are served; all other requests are blocked. Tests never contact Firebase, production, or localhost. These are isolated browser workflow tests, not evidence that the deployed backend works.

| Area | Checked |
| --- | --- |
| Public discovery | Real public summaries, indexable canonical location/job pages, sitemap inclusion, visible breadcrumbs and accurate JSON-LD; keyword/city/area/category filtering, pagination and error/empty states |
| Account access | Google signup/sign-in, existing profile preservation and cancellation; phone normalization, consent, reCAPTCHA lifecycle, code validation, resend/expiry; existing email/password and recovery regressions |
| Account loading | One persistent session across product navigation; no repeated blocking profile fetch on page changes; nonblocking login metadata; visible 10-second profile-read deadlines, retry, stale-account response rejection and background refresh without hiding loaded content |
| App entry | Guest, single-role and dual-role choices; current workspace; loading, unassigned accounts and profile retry without granting new roles |
| Worker jobs | Guest summaries only, authenticated details, token-before-read API checks, stale/logout response protection, selected-job login return; Apply/Contact open Google Play without web application or conversation writes |
| Location | Explicit permission click, denied/unavailable fallback, radius filtering, private request-body coordinates, city scope, false-positive street-name rejection and filters beyond 48 records |
| Employer posting | Guest draft, both provider logins, restored form, City/Area fields, explicit publish, draft expiry/account isolation, numeric validation, atomic profile/job writes, rollback and retry |
| Hiring | Website posting and employer review of a seeded Android-origin application, acceptance/fill state, failed acceptance rollback and stale-state rejection; Android submission itself is not simulated as a web action |
| Responsive UI | City guides, phone and OTP screens, existing email/recovery, worker list/detail and employer posting at 320, 390, 768 and 1440 pixels; overflow, touch targets, images and icons |
| Supporting logic | Legacy redirects, role-safe return URLs, profile completeness, lifecycle actions, expiry and nearby-coordinate validation |

Generated screenshots are kept in ignored `web/test-results/`. Windows uses installed Edge; set `PLAYWRIGHT_CHANNEL` to use another installed channel. On other platforms install Playwright's matching Chromium browser with `npx playwright install chromium` from `web/` before running tests.

### Password Recovery

The sign-in screen's **Forgot password?** action uses Firebase Auth's `sendPasswordResetEmail` and its configured email template/action handler. The confirmation is the same for a successful request and `auth/user-not-found`; the UI does not confirm whether an address has an account. The selected role and original destination remain available when returning to sign-in.

Automated tests replace the email API and send no real messages. Before release, verify delivery, expiry, password reset and subsequent sign-in with an approved staging account. Also verify the Firebase email template, action-handler domain and project-level email-enumeration protection. Generic UI messaging alone does not establish backend enumeration protection.

### Pretext

Job descriptions use `@chenglou/pretext` for font-ready text measurement and responsive expand/collapse controls. Prepared text is reused on width-only changes. If text measurement is unavailable, the full description remains readable. The official library was built from a pinned GitHub revision because the npm registry was blocked; the local package, license and provenance are documented in [vendor/README.md](vendor/README.md).

### Live Verification Limits

- VS Code's network policy blocked `dutype.in`, `127.0.0.1` and `registry.npmjs.org`. No network policy or TLS settings were changed. The local dev server can run, but its real HTTP/browser flow was not verified through the browser tool.
- Real Firebase authentication, Firestore security rules/indexes, transaction contention/retries, Cloud Functions, admin operations, notification delivery, chat, maps/geolocation permissions, referral payouts and cross-device behavior still need staging/live checks. No production accounts or writes were used in testing.
- The in-memory Firestore adapter models atomic rollback and denies obsolete role-profile collections, not server contention or complete security-rule evaluation. Worker applications are now app-only and require separate Android/backend validation.
- The shared Firestore job collection still allows public reads. The website's summary allowlist and verified details endpoint do not change that underlying policy. Shared-rule and backend financial/security behavior require separate coordinated validation and were not changed or deployed here.
- The employer application's separate full-review screen and its work verification/rating actions are not covered by the acceptance-list journey above.

## Dev troubleshooting

### Lighthouse On The Jobs Directory

The supplied 2026-09-15 audit of `/jobs?city=Anantapur` measured **15,070 ms TBT**, while FCP was **1.3 s**, LCP **1.7 s**, and CLS **0**. Most reported scripting cost was attributed to the development `main-app.js` bundle (about 1.3 MiB transferred), with `scheduler.development.js` and a hot-reload WebSocket also present. This is evidence of an audit against `next dev`, not a production performance baseline. It does not prove that all blocking time disappears in production.

Two reproducible page defects were addressed separately:

- `/icon.svg` and `/robots.txt` each had both a public asset and an App Router metadata owner. The duplicate public files were removed. Regression checks cover icon, robots and sitemap route ownership and preserve the existing crawler rules.
- The shared footer origin text measured **4.08:1** contrast against its background, below WCAG AA's **4.5:1** requirement. It now uses the established muted-text color. Axe contrast checks pass on the Anantapur directory at 390 px and 1440 px with no unresolved contrast nodes.

Focused checks from the repository root:

```powershell
node --test --test-name-pattern="metadata routes|robots metadata" web/tests/product.test.cjs
node --test --test-name-pattern="jobs contrast" web/tests/browser.test.cjs
```

For a comparable performance audit:

1. Build with `npm --prefix web run build` and serve using `npm --prefix web run start -- --hostname 127.0.0.1 --port 3101` on an unused port.
2. Open `http://127.0.0.1:3101/jobs?city=Anantapur` in a fresh incognito window with extensions disabled. Do not clear the user's normal IndexedDB or account data just to improve the score.
3. Let the server finish starting and stop other build/test work before auditing. Use the same Lighthouse device, CPU and network settings for comparisons; record several runs rather than treating one score as a guarantee.
4. Verify that icon and robots requests return their intended content, not an HTML error. The new ownership tests and production build are not substitutes for live HTTP checks.
5. Export the Lighthouse JSON and a Performance trace if blocking remains high. Attribute its long tasks before changing polyfills, fonts, CSS or application behavior. A timer's 10-second deadline is not a 10-second CPU task.

The IndexedDB notice and the detected extension are possible measurement differences, not proven causes of the 15-second blocking time. The report attributed only about 3 ms to that extension. Its 192 ms forced-reflow observation and 349 DOM elements also do not explain the entire TBT result.

Lighthouse's CSP/HSTS/COOP/Trusted Types findings in this capture are unscored security guidance, not the reason for the Performance score of 69. The repository's Firebase Hosting headers are not automatically applied by local `next dev` or `next start`. Verify actual HTTPS deployment headers separately, with Firebase sign-in compatibility; do not enable HSTS on localhost or weaken existing headers to improve a score. No production Lighthouse score or live security-header validation is claimed here because the browser origins remain network-policy blocked.

### Slow Local Pages And Invalid Keys

The reported request on 2026-09-15 was rejected by Firebase in **325 ms**. The `30.6 s` timestamp is when it began relative to the page timeline, not a 30-second Firebase request. Its key matched the removed source fallback; this is separate from page rendering performance.

The running server on port 3000 was `next dev`. Existing local Next traces recorded a roughly **2.05 s** compilation and a **2.18 s** worker-page request. Those are development observations, not a production benchmark. Initial route compilation and hot reload can make local navigation slow even when production bundles are healthy.

For an already-built preview without on-demand development compilation, use a free port:

```powershell
npm --prefix web run build
npm --prefix web run start -- --hostname 127.0.0.1 --port 3101
```

Do not compare speed while a production build or the screenshot suite is also running. The product layout now keeps the authenticated profile across client-side route changes. Firestore profile reads have a 10-second UI deadline and explicit recovery instead of an endless loader. This deadline does not cancel the underlying SDK request; stale results are ignored. Sign-in metadata writes no longer block navigation.

The Firebase Management API and local browser origins were policy-blocked during diagnosis. No new key was guessed, no cloud restrictions were changed, no accounts were created, and no production timing improvement is claimed. A valid Web App configuration and an authorized live authentication check are still required.

If you see errors like these while running `next dev`:

- `GET /_next/static/... 404`
- `Cannot find module './8948.js'`
- a valid migrated route like `/safety` suddenly returns `404`

that usually means the running dev server and the `.next` cache are out of sync.

Recovery steps:

1. Stop the current `npm run dev` process.
2. Run `npm run dev:fresh`.
3. Hard refresh the browser once the new server is ready.

If you only want a clean production rebuild, run `npm run build:fresh`.

## Deployment Status

The repository's `firebase.json` already sets Hosting's `source` to `web`. The deployed version and live app-open handoffs have not been verified in this environment. No deployment was performed during the UX verification work.

## Suggested next steps

1. Allow approved local/live test origins and verify the deployed Next site.
2. Run authenticated staging checks with isolated worker, employer and admin accounts, including real Firestore rules and transaction contention.
3. Review backend access-control and financial risks before treating the site as production-ready.
4. Retire old static-page scripts only after confirming they are no longer used by deployment.
