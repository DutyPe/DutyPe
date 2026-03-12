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

You can run with the fallback Firebase config already present in the repo, or define
your own values in `.env.local` using `.env.example`.

If you want the server-backed `/admin/*` protection to work, you must also provide
Firebase Admin credentials through one of the `FIREBASE_ADMIN_*` variables.

For an existing Firebase Auth user, you can promote them with:

`node scripts/set-admin-claims.js --email admin@dutype.com`

## Dev troubleshooting

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

## Remaining migration gaps

These are still not finished:

- `firebase.json` still serves the old root `public/` website
- the new Next app is not yet the deployed Hosting target
- public app-open bridge routes are now rebuilt, but still need production Hosting migration before they replace `public/`
- public dynamic pages still do browser-side Firestore reads instead of server-safe data loading

## Suggested next steps

1. Migrate Firebase Hosting from `public/` to the Next app in `web/`.
2. Replace browser-side public Firestore reads with server-safe data fetching.
3. Decide whether old admin utility pages should become React admin tools or remain script-only.
4. Retire the old `public/` HTML and generator scripts once production Hosting no longer depends on them.
