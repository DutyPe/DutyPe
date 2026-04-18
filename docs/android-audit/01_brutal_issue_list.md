# 01 — Brutal Issue List

Every issue is rated **P0** (fix immediately, blocks scale/security), **P1** (fix this quarter), **P2** (tech debt, schedule it), **P3** (cosmetic).

---

## P0 — Critical

### I-001  `firestore.rules` is triplicated

- **Area:** Firebase / Deploy
- **Evidence:** `firestore.rules` is **1296 lines**. `rules_version = '2'` appears at **L1, L491, L954**. `service cloud.firestore {` appears at **L13, L492, L955**.
- **Current problem:** Only the **last** `service` block is evaluated on deploy. Edits made to L1–490 (the "visible" block most developers open to) are silently ignored.
- **Why it is bad:** Security edits can be applied and look committed but never reach production. A well-intentioned tightening of `/users` rules in the first block is a no-op. This is how you ship a breach.
- **Fix:** Delete L491–1296. Keep only the canonical block (L1–490). Add a CI guard: `grep -c "^rules_version" firestore.rules` must equal 1.
- **Effort:** 15 minutes.

### I-002  `referralStats` dual-written to two collections

- **Area:** Firestore schema
- **Evidence:** [functions/src/referral-system.ts](../../functions/src/referral-system.ts) writes the 19-field `referralStats` object to both `/users/{uid}` (L388, L649) **and** `/referral_stats/{uid}` (L459, L1002). Rules (`firestore.rules` L514–520 first block) permit client read of `referral_stats`.
- **Current problem:** Two sources of truth for the same data, maintained by the same CF, with two separate index costs and two chances for drift on retry.
- **Why it is bad:** At 5M users × N referrals × 2× write cost, referral completion becomes the top CF hotspot. If one write succeeds and the other fails (transient 5xx), the two docs diverge permanently unless a repair job runs.
- **Fix:** Pick one canonical location. Recommendation: keep `/referral_stats/{uid}` (separate read traffic from the hot `/users/{uid}` doc that FCM token updates hit every session). Drop the `referralStats` object from `/users/{uid}` and update the Android `User` model to stop parsing it. Migrate existing users via a one-off backfill.
- **Effort:** 2–3 days (backfill + verification).

### I-003  `FirebaseFirestore.getInstance()` static access bypasses DI

- **Area:** Architecture / Testability
- **Evidence:** Static access sites include:
  - [RegisterScreen.kt](../../app/src/main/java/com/example/dutype/auth/RegisterScreen.kt) L869–877
  - [LoginBottomSheet.kt](../../app/src/main/java/com/example/dutype/components/LoginBottomSheet.kt) L883–1078
  - [EmployerHomeScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerHomeScreen.kt) L148–166
  - [EmployerCompanyDetailsScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerCompanyDetailsScreen.kt) L135
  - [EmployerProfileCache.kt](../../app/src/main/java/com/example/dutype/cache/EmployerProfileCache.kt) L37
  - [GuestEngagementWorker.kt](../../app/src/main/java/com/example/dutype/workers/GuestEngagementWorker.kt) L149
  - [MainNavGraph.kt](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) L126
  - `AuthManager.kt` L185
- **Current problem:** Composables and workers instantiate Firestore services inline (e.g. `RegisterReferralSection` builds an entire `ReferralService`, `SmartNotificationManager`, and `NotificationService` with static Firestore/Auth/Functions accessors on every keystroke tap).
- **Why it is bad:** (a) Impossible to unit-test without Firebase emulator, (b) each call may spin up a fresh client under certain init ordering, (c) App Check and offline settings configured in `AppModule` are bypassed.
- **Fix:** Hilt-inject `firestore: FirebaseFirestore` into a single `ReferralService`/`UserFirestoreService` bound via `@Singleton`, consume via `hiltViewModel()` from Composables. Remove the inline `ReferralService(...)` constructor block in `RegisterScreen.kt` L869.
- **Effort:** 1–2 days of mechanical refactor.

### I-004  Three global state-manager singletons shadow ViewModel state

- **Area:** State ownership
- **Evidence:** [app/src/main/java/com/example/dutype/state/](../../app/src/main/java/com/example/dutype/state/) contains `AppStateManager`, `ApplicationStateManager`, `ProfileSetupStateManager` — all `@Singleton` per [AppModule.kt](../../app/src/main/java/com/example/dutype/di/AppModule.kt) L113–117.
- **Current problem:** `ProfileSetupStateManager` holds the same profile-setup progress as `ProfileCompletionViewModel`. `ApplicationStateManager` overlaps with `SmartJobApplicationViewModel`. Any screen can mutate global state that another screen observes without Compose knowing.
- **Why it is bad:** After role switch (`RoleSwitchManager.switchRole`), the global state is not reset; the next screen reads stale worker state while the ViewModel reports employer state. Source of recurring "refer & earn shows old code" / "my jobs shows other role's jobs" bugs.
- **Fix:** Delete all three. Move profile-setup state into `ProfileCompletionViewModel` scoped to `navigation`. Move application-in-progress into `SmartJobApplicationViewModel` with `SavedStateHandle`. Use `RoleSwitchManager` only to invalidate caches, not to own state.
- **Effort:** 3–5 days.

### I-005  `isVerified` writeable by clients on `employer_profiles`

- **Area:** Security
- **Evidence:** `firestore.rules` (first block) L173 allows `employer_profiles/{userId}.isVerified` in the client write allowlist. Comment at L120 says CF-only.
- **Current problem:** An employer (or anyone who can impersonate one) can POST `isVerified: true` and earn the verified badge. Workers trust this badge.
- **Why it is bad:** Trust signal forgery. If a bad actor runs the app with a modified client and writes `isVerified: true` to their own doc, every worker sees them as verified.
- **Fix:** In the rules allowlist for `employer_profiles` update, drop `isVerified`. Move verification to a CF with admin-claim gating.
- **Effort:** 30 minutes rules change + 1 day CF callable for admin verify/unverify.

### I-006  Client writes aggregate counters (`rating`, `totalRatings`, `totalJobs`, `totalHires`)

- **Area:** Security / Integrity
- **Evidence:** Rules comments at `firestore.rules` L120, L128 say "CF-only aggregates", but these fields are not actually excluded from client write in the current allowlist. Android code under `services/UserFirestoreService.kt` and the referenced CF paths (`referral-system.ts` L1279, L1472) both touch user-level aggregates.
- **Current problem:** Same as I-005 — trust signal forgery. A client can inflate their own rating or job count.
- **Why it is bad:** Ratings drive match quality. Fake 5-star profiles poison the worker/employer feed.
- **Fix:** Enforce CF-only writes in rules for every aggregate. Move to CF-maintained counters already called out in the session's aggregate-maintainers work.
- **Effort:** 1 day.

---

## P1 — High

### I-007  Orphan / mis-spelled Firestore indexes

- **Area:** Firestore indexing
- **Evidence:** [firestore.indexes.json](../../firestore.indexes.json):
  - Index #6 `saved_jobs.workerId + createdAt` — canonical field is `userId`, no code queries `workerId`.
  - Index #10 `referrals.status + completedAt` — no matching query.
  - Index #13 `referrals.referrerUserId + createdAt` — canonical field is `referrerId`, mis-named.
  - Index #15 `users.isBlocked + successfulReferrals` — neither field is in the canonical `users` schema.
  - Index #16 `users.userRole + isBlocked + successfulReferrals` — none of these are canonical fields.
- **Current problem:** Firestore builds and maintains indexes for every write to `users`, `saved_jobs`, `referrals` regardless of whether anyone queries them.
- **Why it is bad:** Every composite index adds write amplification. At 5M users writing `fcmToken`/`lastActiveAt` to `/users/{uid}` every session, five orphan indexes = 5× wasted write cost.
- **Fix:** Delete indexes #6, #10, #15, #16; rename/fix #13 to `referrerId`.
- **Effort:** 30 minutes.

### I-008  `JobListing` is a 50-field god-model for both list and detail

- **Area:** Performance / payload
- **Evidence:** [JobListing.kt](../../app/src/main/java/com/example/dutype/models/JobListing.kt) — ~50 fields including long-form `description`, `benefits[]`, `workingHours`, `educationRequired`, `shiftTiming`, `experienceRequired`, `whatsappNumber`, `contactNumber`, `vacancies`.
- **Current problem:** The Firestore side already splits summary (`jobmetadata`) from detail (`job_details`). Android does not — every list query fetches `jobmetadata` then the ViewModel overlays detail fields the UI card never renders.
- **Why it is bad:** Deserialization cost on every feed scroll. Larger Compose state = more recomposition work. Data bound to UI that is never rendered is pure tax.
- **Fix:** Split into `JobCard` (≈12 fields: id, employerId, companyName, title, jobType, salary, salaryType, geohash, urgency, status, createdAt, distance+isSaved as runtime) and `JobDetail` (the full current set). `WorkerJobCard` consumes `JobCard`. `JobDetailScreen` fetches the `job_details` doc on navigate.
- **Effort:** 2 days (models + ViewModel + card composable migration).

### I-009  Duplicate one-shot + snapshot reads per screen

- **Area:** Performance / cost
- **Evidence:** The pattern fixed in [ReferralViewModel.kt](../../app/src/main/java/com/example/dutype/viewmodels/ReferralViewModel.kt) L52–53 — `loadReferralData()` used to call both `getCurrentUserReferralStats()` **and** `getCurrentUserReferralStatsFlow()`. Snapshot listener already delivers cached+server data; one-shot is pure duplicate read.
- **Current problem:** Same pattern almost certainly exists in: `WorkerHomeViewModel`, `AllJobsViewModel` (initial `load()` + `observeJobs()`), `EmployerApplicationViewModel`, `SavedJobsViewModel`, `ProfileViewModel`.
- **Why it is bad:** Every screen open = 2× reads. At 5M daily actives × 5 screens avg = 25M extra reads/day = ~$37 extra/day of Firestore cost.
- **Fix:** Audit every `fun load*()` — if it sits next to an `observe*()` that opens a snapshot listener, delete the one-shot call.
- **Effort:** 1 day.

### I-010  `addSnapshotListener` sites without verified cleanup

- **Area:** Memory / cost
- **Evidence:**
  - [AnnouncementService.kt](../../app/src/main/java/com/example/dutype/services/AnnouncementService.kt) L72
  - [JobFirestoreService.kt](../../app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt) L699
  - [ReferralService.kt](../../app/src/main/java/com/example/dutype/services/ReferralService.kt) L224, L724
  - [AppConfigRepository.kt](../../app/src/main/java/com/example/dutype/repositories/AppConfigRepository.kt) L47 (correctly uses `callbackFlow` + `stateIn(WhileSubscribed(60_000))` — **this is the reference pattern**)
- **Current problem:** Several services hold `ListenerRegistration` references in properties without explicit `remove()` on scope end.
- **Why it is bad:** Leaks keep Firestore streams open indefinitely, billed as reads while the screen is backgrounded.
- **Fix:** Wrap every `addSnapshotListener` in `callbackFlow { awaitClose { registration.remove() } }` and expose as `StateFlow`/`SharedFlow` with `stateIn(scope, WhileSubscribed(5_000), default)`. Use `AppConfigRepository` as the canonical template.
- **Effort:** 1 day per service × 4 services.

### I-011  Room `fallbackToDestructiveMigration()`

- **Area:** Data integrity
- **Evidence:** [DutyPeDatabase.kt](../../app/src/main/java/com/example/dutype/database/DutyPeDatabase.kt).
- **Current problem:** Any schema change wipes every user's Room cache on first launch after update.
- **Why it is bad:** Users on weak networks rely on Room for offline-first. A schema bump = empty job list on open + forced network fetch.
- **Fix:** Write explicit `Migration(from, to)` for every version bump; remove the destructive fallback. As an interim, add `fallbackToDestructiveMigrationFrom(1)` only for the known-bad initial version.
- **Effort:** 2 hours per schema change.

### I-012  `applicationCount` is a denormalized counter on `job_details`

- **Area:** Firestore schema
- **Evidence:** `firestore.rules` L264–318; incremented on every apply.
- **Current problem:** Denormalized counter maintained on write from CF. Under contention (popular job getting 100 applicants/sec) the counter is the lock hotspot.
- **Why it is bad:** At scale, hot counters become the bottleneck. Each increment is a transaction. 100 applies/sec × 1 write = 100 writes/sec on a single doc.
- **Fix:** Drop `applicationCount`. Compute lazily via `count()` aggregation queries (Firestore supports `COUNT()` server-side since 2023) on demand from the employer's view. Cache at the employer-landing level for 60s via CF.
- **Effort:** 1 day.

### I-013  Navigation ownership scattered

- **Area:** Navigation / startup
- **Evidence:** Deep-link broadcast receiver at [MainNavGraph.kt](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) L52–81; deep-link patterns repeated in [WorkerNavGraph.kt](../../app/src/main/java/com/example/dutype/navigation/WorkerNavGraph.kt) L96–110 and `EmployerNavGraph.kt`.
- **Current problem:** Deep-link route determination, auth gating, role gating, and profile-setup gating all live inside Composables (`MainNavGraph`'s `LaunchedEffect` chain).
- **Why it is bad:** Start destination logic re-executes on every recomposition. Recoverable only via explicit `LaunchedEffect(Unit)` (which it is), but still couples business logic to UI layer.
- **Fix:** Extract a `StartDestinationResolver` with a `suspend fun resolve(): Route` that consumes auth + role + profile-complete flags and returns a route. `MainActivity` awaits it before `setContent`.
- **Effort:** 1 day.

### I-014  `metadata/` package holds runtime Firestore readers disguised as constants

- **Area:** Startup / naming
- **Evidence:** [UserMetadata.kt](../../app/src/main/java/com/example/dutype/metadata/UserMetadata.kt) L340, L359, L384, L402 contain live `.whereEqualTo(...).orderBy(...).limit(...)` queries.
- **Current problem:** Name implies static config. Reality is it opens Firestore queries eagerly. Someone maintaining the codebase will not look here for query hotspots.
- **Why it is bad:** Discoverability and cognitive load. Also the 500-limit and 200-limit queries here load aggressively; `applications` query with `limit(500)` fires once and the result sits in memory.
- **Fix:** Rename package to `aggregations/` or move into `repositories/`. Cap query limits at real UI needs (applications list shows ~20 at a time).
- **Effort:** 1 day.

### I-015  `employer_profiles.isVerified` field double-semantics

- See I-005.

### I-016  Write-amplification on apply() and referral completion

- **Area:** Firestore cost
- **Evidence:** Apply-to-job writes 3–4 docs (application, job_details counter, notification, optional user stats). Referral completion writes **8 docs** (referral + 2×users + 2×referral_stats + 2×notifications + referral_code).
- **Current problem:** Already documented in I-002 and I-012.
- **Why it is bad:** Multiplicative with growth.
- **Fix:** Collapse via I-002 (dual-write dedup) + I-012 (drop applicationCount) + batch notifications via fan-out trigger.
- **Effort:** Rolled into I-002/I-012.

### I-017  `DutyPeMessagingService` reads Firestore in `onMessageReceived`

- **Area:** FCM / battery
- **Evidence:** [DutyPeMessagingService.kt](../../app/src/main/java/com/example/dutype/services/DutyPeMessagingService.kt) L95 uses `FirebaseFirestore.getInstance()` inline while handling an FCM push.
- **Current problem:** Push handler does a Firestore roundtrip before showing the notification. If the device is offline or Firestore slow, the notification is delayed or lost.
- **Why it is bad:** FCM delivery has a budget. Doing I/O here costs delivery latency and battery.
- **Fix:** Put every field the notification renders into the FCM payload directly (title, body, deepLink, channelId, entityId). Remove the Firestore lookup. If enrichment is needed, do it when the user opens the app, not in the push handler.
- **Effort:** 1 day (coordinated with CF payload shape).

---

## P2 — Medium

### I-018  27 ViewModels, many over 400 lines

- [FirestoreJobViewModel.kt](../../app/src/main/java/com/example/dutype/viewmodels/FirestoreJobViewModel.kt) mixes pagination, prefetch, cache, UI state, error mapping. Split into `JobFeedViewModel` (UI) + `JobPaginator` (stateful pager, reusable) + `JobCacheCoordinator` (service).

### I-019  Compose screens over 800 lines (god-composables)

- `WorkerHomeScreen`, `PostJobScreen`, `AllJobsScreen`, `EmployerApplicationManagementScreen`. Each screen file contains header, feed, filters, bottom sheets, dialogs. Extract sections into `@Composable` functions in sibling files; target <300 lines per screen file.

### I-020  `LazyColumn` without stable `key =`

- Confirmed missing in some job lists. Add `key = { it.id }` and `contentType = { ... }` to every `items(...)` over model data.

### I-021  `models/` has `Job`, `JobListing`, `JobListingSummary`, `JobEntity`

- Four near-identical representations. After I-008 fix, collapse to `JobCard`, `JobDetail`, `JobEntity` (Room). Delete `JobListingSummary`.

### I-022  `managers/` + `services/` + `repositories/` boundary is undefined

- `RoleSwitchManager` is in `managers/` but is a service. `FCMTokenManager` is in `services/` but is a manager. Rename package or delete it — pick one naming convention.

### I-023  `notifications/` vs `services/NotificationService.kt` vs `services/SmartNotificationManager.kt`

- Three places own notification state. `SmartNotificationManager` and `NotificationService` overlap (`NotificationService` writes, `SmartNotificationManager` schedules/routes). Merge into one `NotificationRepository` with a clear API: `post(notification)`, `observeForCurrentUser()`, `markRead(id)`.

### I-024  Eager `@Singleton` instantiation in `AppModule`

- `AppMetadata`, `JobMetadata`, `UserMetadata` open snapshot listeners in init (inferred from `metadata/` subagent findings). They are created eagerly for every app launch, authenticated or not. Guard with lazy init or defer until after login.

### I-025  Orphan TODOs and disabled features leaking into production

- `PostJobScreen` L798: "TODO: Send to fraud detection system" — feature half-wired.
- `AIJobPostingViewModel` L313: "TODO: Fetch from Firestore".
- `EmployerHomeScreen` L444: "TODO: Uncomment for future release — AI/Voice features next version".
- Either ship the feature behind a server flag or delete the stubs.

### I-026  `fallbackToDestructiveMigration` — covered in I-011.

---

## P3 — Low / cosmetic

### I-027  `WORKER_ALL_JOBS_FILTERED` duplicates `WORKER_ALL_JOBS?filter=...`

- [Routes.kt](../../app/src/main/java/com/example/dutype/navigation/Routes.kt) L26 — two routes for the same screen. Delete `WORKER_ALL_JOBS_FILTERED`, use query param.

### I-028  `PROFILE_SETUP` and `PROFILE_SETUP_WITH_RETURN` duplication

- Collapse into one route with optional `returnRoute` argument.

### I-029  Unused strings / dead composables in `WorkerReferEarnScreen`

- `HowItWorksSection()`, `RewardsSection()`, `RedemptionInstructionsSection()` are declared but never called. Delete.

### I-030  `cache/` vs `database/` vs `data/` package split

- Three packages, overlapping purpose. Merge `cache/` into `repositories/` as in-memory Guava/LRU coordinators. `database/` stays Room. `data/` holds DataStore only.
