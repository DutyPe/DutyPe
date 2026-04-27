# DutyPe — Interview Prep (What we actually built)

> Use this as your single page for the interview. Every claim here is backed by code in this repo. Don't bluff anything not on this page.

---

## 1. One-line pitch

DutyPe is a hyperlocal blue/grey-collar jobs marketplace for India. Workers find nearby jobs and apply with one tap; small employers post jobs and hire fast. Android-first product, Firebase backend, Next.js for SEO + admin.

- **Target user:** Indian hyperlocal workers + small employers (shops, kitchens, sites, drivers).
- **Why it can win:** location-first ranking, phone-only auth, low-data offline-first app, viral referrals.

---

## 2. Product surface (what exists)

- **Android app** (Kotlin + Jetpack Compose) — primary product, two roles: WORKER and EMPLOYER.
  - Worker: browse nearby jobs, search by category, save jobs, apply, my-applications, refer & earn, profile.
  - Employer: post job, edit job, see applicants, mark hired, analytics, refer & earn, public profile.
- **Firebase backend** — Auth (Phone OTP), Firestore (data), Cloud Functions (TS), Cloud Messaging (FCM), App Check, Crashlytics, Performance.
- **Next.js web app** (`web/`) — public SEO pages for jobs/cities/employers, deep-link landing pages, admin console, account-deletion page (Play Store requirement), API routes.
- **Cloud Functions** (`functions/src/`) — referral system, scheduled notifications, job-expiry, landing pages SSR, idempotency, validation, employer/worker/job metadata aggregates.

---

## 3. High-level architecture

```
 ┌──────────────┐            ┌──────────────┐
 │  Android app │            │  Next.js web │
 │  (Compose)   │            │  (SEO+Admin) │
 └──────┬───────┘            └──────┬───────┘
        │ Firestore SDK + Callables │ Firestore Web SDK + REST
        ▼                           ▼
 ┌──────────────────────────────────────────────┐
 │               Firebase platform               │
 │  Auth (Phone OTP)  •  Firestore  •  Storage   │
 │  Cloud Messaging   •  App Check  •  Crashlytics│
 └────────────────────────┬─────────────────────┘
                          │ triggers + callables
                          ▼
                ┌──────────────────────┐
                │  Cloud Functions     │
                │  (TypeScript)        │
                │  - referral system   │
                │  - scheduled jobs    │
                │  - landing SSR       │
                │  - notifications     │
                └──────────────────────┘
```

**Why this shape:** Mobile is the product, so Firestore + Functions gives us realtime sync, offline persistence, and zero ops. Next.js exists only for SEO/landing/admin — it does not own business logic.

---

## 4. Android app — internal architecture

- **Pattern:** MVVM + Repository + Service layers, single-activity Compose app.
- **DI:** Hilt (`DutyPeApplication` is `@HiltAndroidApp`; modules in `di/`, plus `@EntryPoint` for Compose service access via `ComposeServiceEntryPoint`).
- **UI:** Jetpack Compose, Material 3, Navigation Compose.
- **Async:** Kotlin Coroutines + Flow everywhere. Repos return `Flow<Result<T>>`.
- **Local DB:** Room (`DutyPeDatabase`, version 8, exported schemas in `app/schemas/`) for offline cache of jobs, applications, saved jobs.
- **DataStore:** for in-progress job drafts and application form drafts (`data/JobDraftDataStore`, `ApplicationFormDataStore`).
- **Performance:** Baseline Profiles module (`baselineprofile/`) shipped in release AAB, Firebase Performance, custom `PerformanceTracker`, `ANRHandler`, `MainThreadChecker`.
- **Observability:** Timber + Crashlytics, custom `ErrorHandler` in `core/error/`.
- **Background work:** WorkManager (`workers/GuestEngagementWorker`, `PendingApplicationNotificationWorker`).
- **Notifications:** `DutyPeMessagingService` (FCM), `LocalNotificationService`, `InAppNotificationManager` for in-app banner, channel manager for Android 8+ channels, `SmartNotificationManager` for cooldown/dedup.

### Layer responsibilities

| Layer | Example | Owns |
|---|---|---|
| UI (Compose screens) | `worker/screens/WorkerHomeScreen.kt` | rendering + user input |
| ViewModel | `worker/viewmodels/*` | state, ui events, calls repos |
| Repository | `repositories/FirestoreJobRepository.kt`, `OfflineFirstJobRepository.kt` | source-of-truth selection, caching policy |
| Service | `services/JobApplicationService.kt`, `services/AuthFlowService.kt`, `services/firestore/JobFirestoreService.kt` | Firestore reads/writes, transactions |
| Engine / Utils | `engine/NearestJobsEngine.kt`, `utils/GeoUtils.kt` | pure logic, distance math, geohashing |

Why split repo and service: services own raw Firebase calls; repositories combine network + Room cache + de-duplication and decide what the UI gets. Keeps testing and replacement easy.

---

## 5. Data model (Firestore)

Key collections (current, post-April-2026 cleanup — no `users` collection anymore):

- `phoneRoles/{phone}` — identity lookup. Maps phone → uid + role + name. Source of truth for "is this phone registered and as what."
- `worker_profiles/{uid}` — worker's profile + referral fields.
- `employer_profiles/{uid}` — employer's profile + company info + referral fields.
- `jobs/{jobId}` — job postings. Carries `lat`, `lng`, `geohash`, `salary`, `salaryType`, `urgency`, `status`, `createdAt`, `expiresAt`, `addressText`, `category`, `employerId`, `applicationCount`.
- `applications/{jobId_workerId}` — deterministic doc id (prevents duplicate apply, makes rules + idempotency trivial).
- `saved_jobs/{workerId}_{jobId}` — bookmark.
- `referral_codes/{CODE}` — O(1) code → owner lookup.
- `referral_stats/{uid}` — per-user referral aggregates (Cloud-Function-only writes).
- `referral_stats/{uid}/withdrawals/*` — withdrawal log.
- `referrals/{referralId}/audit_logs/*` — event sourcing for referral state changes.
- `notifications/{uid}/items/*` — per-user inbox.
- `app_config/*` — runtime config flags read by app + functions.

**Schema discipline (in `firestore.rules`):**
- Every client write is field-whitelisted with `hasOnly()` + required with `hasAll()`.
- All mutable aggregates (`applicationCount`, `rating`, `referralStats`, balance) are Cloud-Function-only — Admin SDK bypasses rules.
- Ownership is bound to `request.auth.uid`, never to a client-supplied field.
- Deterministic doc IDs (`applications`, `saves`, `ratings`, `referrals`) prevent spoofing.
- Listings that could enumerate user data are closed; access is via callable Cloud Functions.

---

## 6. Auth flow (phone OTP)

File: `viewmodels/OtpViewModel.kt`, `services/AuthFlowService.kt`.

1. User enters phone → `PhoneAuthProvider.verifyPhoneNumber` (Firebase Auth) sends SMS OTP.
2. On verification: `signInWithPhoneAuthCredential` → Firebase uid.
3. App calls `AuthFlowService` which:
   - Reads `phoneRoles/{phone}` to find existing role (with 5s timeout, race-safe).
   - If missing, treats as new registration; writes `phoneRoles` doc + creates the role profile (`worker_profiles` or `employer_profiles`).
   - Routes to mandatory profile setup screen if profile incomplete.
4. FCM token registered + topic subscriptions set per role (`workers` / `employers` / `all_users`).

Why `phoneRoles` exists: previously we read `users` and that needed a wide-open list query; phone-keyed doc gives us O(1) lookup with tight rules.

---

## 7. Nearby jobs — the core algorithm

This is the heart of the product. Two pieces:

### 7a. Server-side: Geohash range queries

File: `services/firestore/JobFirestoreService.kt` → `runCellQuery`, `getNearbyJobsSummary`.

- Every job stores a `geohash` (precision 6, computed via Firebase GeoFire utils — `utils/GeoUtils.kt#encodeGeohash`).
- To find jobs within R km of the user we:
  1. `GeoFireUtils.getGeoHashQueryBounds(center, radiusMeters)` → returns up to 9 geohash cell ranges (center + 8 neighbours).
  2. For each cell range, run a Firestore query: `.orderBy("geohash").startAt(start).endAt(end).limit(N)`.
  3. Merge results, then filter client-side for: status active, not expired, exact-distance ≤ R (because geohash cells over-cover), category match.
- Index needed: just the auto single-field index on `geohash`. No composite index per radius. This is the standard GeoFire pattern.

### 7b. Client-side: Haversine + progressive radius expansion

Files: `engine/NearestJobsEngine.kt`, `utils/GeoUtils.kt`, `repositories/FirestoreJobRepository.kt#buildProgressiveRadii`.

- Distances are Haversine on a sphere (R = 6371 km), inlined for speed (~4 ms for 500 jobs).
- If results are thin at the user's chosen radius, the repository expands progressively: 5,10,15…50, then 75,100,150,200,300,500,1000 km — until we have enough jobs or run out.
- Final list is sorted nearest-first and bucketed into UX tiers: VERY_NEAR (<5km), NEAR (<10), MODERATE (<20), FAR.
- `NearestJobsEngine` is the **single source of truth** for sorting — every screen (home, all jobs, categories, map, etc.) calls it. This was a deliberate refactor to kill duplicate sort logic.

**Talking points:**
- Why geohash and not GeoPoint+inequality? Firestore allows only one range filter per query; geohash trick converts a 2D radius query into a 1D string range, which composes with other filters.
- Why client-side final filter? Geohash cells over-approximate. Cheap CPU on phone vs expensive Firestore reads.
- Why Haversine and not a flat-earth approximation? India spans ~28° latitude; flat-earth has up to 1–2% error which matters for "is this job in your village or the next one."

---

## 8. Offline-first jobs

File: `repositories/OfflineFirstJobRepository.kt`.

Strategy (read path):
1. Emit Room cache immediately if not empty (instant UI, zero network wait).
2. If cache age > 5 min TTL → fetch from Firestore in background, update Room.
3. If cache empty → fetch from network and emit.
4. Stale-while-revalidate window: up to 1 hour we still show cached data while refreshing.

Other caches:
- `cachedSavedJobIds` in `FirestoreJobRepository` — 60s TTL, prevents N+1 reads when marking which list items are saved.
- `JobCacheManager` for hot job objects.
- `RequestDeduplicator` to collapse concurrent identical reads into one network call.

Why: blue-collar users are on patchy 3G/4G. App must feel instant and survive offline.

---

## 9. Apply flow + idempotency

File: `services/JobApplicationService.kt`.

- Doc id is `${jobId}_${workerId}` — deterministic. Same user can't accidentally apply twice; rules enforce uniqueness without a transaction.
- Application create writes are kept **minimal** (job + worker pointers + status + timestamps). Heavier denormalised snapshots are populated by a Cloud Function (`backfill-application-worker-snapshot.js` is the script that historically backfilled them).
- `ApplicationStateManager` keeps in-memory state of user's applications so UI updates instantly.
- Offline: if Firestore write fails with a retryable error (`UNAVAILABLE`, `DEADLINE_EXCEEDED`, IO), Room records the application locally and `PendingApplicationNotificationWorker` retries via WorkManager.
- Notifications (worker→employer "new application", employer→worker "you're hired") are fired from server-side functions, not the client, to avoid duplicates and trust issues.

---

## 10. Referral system (most complex feature)

File: `functions/src/referral-system.ts` (server is source of truth) + `services/ReferralService.kt` (client).

**Why it's interesting:** all economic value flows through this system, so it has to be atomic, idempotent, and fraud-resistant.

- **Code generation:** 8-char codes from a Crockford-style alphabet (no `0/O/1/I/L`) prefixed `DUTY`, stored in `referral_codes/{CODE}` for O(1) lookup.
- **Two-sided rewards:** referrer gets ₹25 per successful referral, new user gets ₹25 signup bonus.
- **Milestones:** 5/10/15/25/50/100 referrals → bonus ₹50/100/150/250/500/1000.
- **Tiers:** BRONZE/SILVER/GOLD/PLATINUM/DIAMOND/ELITE based on count.
- **Atomicity:** all reward writes in one Firestore batch + transaction so balance + stats + audit log can never disagree.
- **Idempotency:** every reward operation carries an idempotency key so a retried Cloud Function call can't double-pay.
- **Fraud prevention:** rate limits (max 50 referrals/user/day, max 5 same-IP/day, 24h same-device cooldown), 30-day referral expiry, server-side validation of codes.
- **Audit trail:** every state change appended to `referrals/{id}/audit_logs/*` (event sourcing — we can replay history).
- **Withdrawals:** min ₹100, max ₹1000/day, recorded in `referral_stats/{uid}/withdrawals/*` with admin approval state.
- **Security rules:** all referral aggregates are Cloud-Function-only writes; client can only read its own stats.

This pattern is inspired by Dropbox, PayPal, Uber and Stripe (fraud + atomicity + idempotency).

---

## 11. Notifications

- **FCM** for push. `DutyPeMessagingService` handles incoming messages → routes to in-app banner if app is foreground (`InAppNotificationManager`) or system tray if background.
- **Topic subscriptions:** `all_users`, `workers`, `employers`, `app_updates` — set by role at login.
- **Localization:** `notification-i18n.ts` on the server provides per-locale title/body keys, so the same trigger sends Hindi/Telugu/English variants.
- **Broadcast notifications:** writing a doc to `broadcast_notifications/*` triggers a Cloud Function fan-out to a topic. Admin-controlled.
- **Scheduled notifications:** `scheduled-notifications.ts` Cloud Functions — daily reminders, job expiry warnings, re-engagement.
- **Self-notifications log:** stored per-user with retention (`SELF_NOTIFICATION_RETENTION_MS = 30 days`) and key/value caps to keep doc sizes safe.
- **Smart cooldown:** `SmartNotificationManager` dedupes and rate-limits per user so we never spam.

---

## 12. Security model (concrete things to mention)

- Firebase Auth Phone OTP with App Check (debug provider in dev, Play Integrity in release).
- Firestore rules: `hasOnly` + `hasAll` whitelisting on every client write; ownership tied to `request.auth.uid`.
- Admin access in rules: matches token claim `admin == true`, role `ADMIN`, or `@dutype.com` email — mirrors `web/lib/firebase/admin-access.ts`.
- All money/aggregate writes are server-only (Admin SDK).
- Callable Cloud Functions assert App Check (`assertAppCheck` in `validation.ts`).
- Input validation helpers (`validateString`, `validateNumber`, `validateUserId`, `validateEnum`) on every callable.
- Deterministic doc IDs prevent ID-spoofing on apply/save/rate.
- No secrets in app source. Local API keys in `local.properties`, server keys in Functions config.
- Storage rules separate from Firestore rules (`storage.rules`).

---

## 13. Performance + scale

- Baseline Profiles → faster cold start (compiled into AAB via the `baselineprofile` module).
- Firebase Performance + custom `PerformanceTracker` (cache hit/miss, lookup ms).
- ANR + main-thread checks (`ANRHandler`, `MainThreadChecker`, `assertBackgroundThread()` calls before any Firestore op).
- N+1 reads killed via cached saved-job IDs and request deduplication.
- Pagination: jobs loaded in pages of 50 from cache, geohash queries limited per cell.
- Room indexes added in v2 migration; schemas exported under `app/schemas/`.
- Crashlytics + Firebase Performance for production observability.

---

## 14. Web app (Next.js)

- App Router (`web/app/`).
- Public SEO pages: `/jobs/*`, `/employer/*`, `/worker/*`, `/refer/*`, `/[slug]` for landing pages, `sitemap.ts` for dynamic sitemap.
- Account-deletion page (`/accountdeletion`) — Play Store policy requirement.
- Admin console (`/admin`) — direct Firestore reads via web SDK gated by admin token claims (rules mirror this).
- Deep-link redirector (`/app-redirect`) for SMS/share links → opens app or falls back to Play Store.
- API routes for landing-page generation work with Cloud Functions (`job-landing.ts`, `employer-landing.ts`, `worker-landing.ts`).
- Middleware (`web/middleware.ts`) for routing/redirects.

---

## 15. Tooling, build, ops

- **Build:** Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), AGP 8.7.3, Kotlin 2.0.21, KSP, Hilt, Compose BOM 2024.09, Crashlytics + Performance plugins, signing config from `keystore.properties`.
- **Min/Target SDK:** 24 / 35.
- **Functions:** Node + TS, deployed to Firebase. `package.json` + `tsconfig.json` in `functions/`.
- **Web:** Next.js, TypeScript strict.
- **Scripts:** large `scripts/` folder with one-off audit + backfill utilities (e.g. `audit-live-schema.js`, `backfill-application-worker-snapshot.js`, `delete-deprecated-collections.js`). Used during the April 2026 schema migration.
- **CI artifacts:** `app/build/`, `web/.next/`.

---

## 16. Trade-offs we made (be honest in the interview)

- **Firebase over a custom backend:** trades flexibility for speed-to-market and ops cost. Acceptable until we hit pricing or query-shape limits.
- **Geohash precision 6 (~1.2 km cell):** good for hyperlocal; for very dense city centres we accept some over-fetch and filter client-side.
- **Client-side final sort:** moves CPU to the device, but saves Firestore reads and gives consistent UX across screens.
- **Deterministic doc IDs:** simpler rules and idempotency, but requires careful migration planning.
- **No `users` collection (April 2026 migration):** removed a single hot collection; identity is now `phoneRoles` + role-specific profiles. Cleaner rules, but required script-driven backfills and rule rewrites.
- **Compose-only UI:** faster iteration; baseline profiles + R8 needed to keep cold start tight.

---

## 17. Likely interview questions + crisp answers

**Q: How do you find nearby jobs at scale?**
A: Each job has a precision-6 geohash. We compute the 9 cell ranges that cover the user's radius (`GeoFireUtils.getGeoHashQueryBounds`), run `orderBy('geohash').startAt/endAt` queries per cell, merge, then filter by exact Haversine distance + status + expiry on the client. Single source of truth for sorting in `NearestJobsEngine`. ~4 ms for 500 jobs.

**Q: Why geohash and not a server-side GIS?**
A: Firestore only allows one range filter per query. Geohash flattens 2D radius search into a 1D string range, no extra service to run, no extra indexes. Trade-off: cells over-cover, so we filter exactly on the client.

**Q: How do you prevent duplicate applications?**
A: Document id is `${jobId}_${workerId}`. Firestore rule disallows overwrite. No race possible.

**Q: How do you keep the app fast offline?**
A: Offline-first repo: Room cache emitted first, network refresh in background, stale-while-revalidate up to 1 hour, 5 min TTL freshness. Drafts in DataStore. WorkManager retries failed apply writes.

**Q: How do you keep referral money safe?**
A: All writes that touch balance are Cloud-Function-only with Admin SDK; rules block direct client writes to `referral_stats`. Each reward op is idempotent (unique key) and atomic (batch + transaction). Rate limits + 30-day expiry + audit logs in `referrals/{id}/audit_logs`.

**Q: How does auth work?**
A: Firebase Phone OTP. After sign-in we look up `phoneRoles/{phone}` to determine WORKER/EMPLOYER and route. New numbers create the `phoneRoles` doc + role profile. App Check is enforced.

**Q: Why two apps (Android + Next.js)?**
A: Mobile is the product. The web app exists for SEO landing pages, deep-links, account deletion, and an admin console — not for end-user job-seeking. This keeps each codebase tight.

**Q: How do you handle schema changes?**
A: Room exports schemas (`app/schemas/`) and we write Migration objects unit-tested with `MigrationTestHelper`. Firestore changes go via scripts in `scripts/` (audit → backfill → cleanup) and Functions are versioned. The April 2026 `users`-removal migration is a recent example.

**Q: What happens when a job posting expires?**
A: A scheduled Cloud Function (`job-expiry.ts`) flips `status` to expired. Clients filter by status; rules block worker writes to expired docs. Cache TTL ensures stale "active" rows clear within minutes.

**Q: How do you ship reliably?**
A: Crashlytics + Firebase Performance + Timber logs; Baseline Profiles for cold start; ANR + main-thread guards; offline-first to mask network issues; deterministic doc ids and idempotent server ops to make retries safe.

**Q: What would you do at 10x scale?**
A: Move hot reads (jobs near a city) to BigQuery + a search service (Algolia / Typesense) for full-text + facets; use Firestore for writes + realtime, search service for browse. Add Redis-style cache in front of callable functions for referral lookups. Shard `applicationCount` aggregates if a single popular job hits the per-doc write rate limit.

**Q: What's the riskiest part of the codebase?**
A: The referral system — it's where money lives. Mitigations: server-only writes, idempotency keys, atomic batches, audit logs, rate limits, fraud signals (device + IP + velocity). I'd add formal reconciliation jobs next.

---

## 18. Things to NOT claim

- Don't claim the app has millions of users — the rules + scripts are *built* for ~5M scale, but that's the design target, not a current metric.
- Don't claim full Nav3 — Nav3 deps are declared but not wired (compileSdk still 35; Nav3 needs 36). We're on Navigation Compose 2.8.
- Don't claim a separate microservices backend — backend is Firebase + TS Cloud Functions, not custom servers.
- Don't claim payments are live — referral rewards have a withdrawal flow with admin approval; payout integration is the next step.

---

## 19. 60-second elevator version (memorize this)

> DutyPe is a hyperlocal jobs marketplace for Indian blue/grey-collar workers. The Android app — Kotlin, Compose, MVVM with Hilt — is the main product; Firebase is the backend (Auth, Firestore, Functions, FCM, App Check) and a Next.js app handles SEO landing pages and admin. The hard part is location: every job has a geohash, and we use GeoFire-style cell-range queries plus Haversine on the client through a single `NearestJobsEngine` so every screen shows nearest-first consistently in about 4 ms for 500 jobs. The app is offline-first with Room + stale-while-revalidate caching. Apply is idempotent because the application doc id is `${jobId}_${workerId}`. The referral system is the most interesting service — money flows through Cloud Functions only, with atomic batches, idempotency keys, rate limits and audit logs, inspired by Dropbox, PayPal and Stripe. Security is enforced at the rules layer with field-whitelisted writes and uid-bound ownership; all aggregates are server-only.
