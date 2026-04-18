# 04 — Refactor / Rebuild Plan

Six phases. Do them in order. Each phase is **independently shippable** and each lands behind a remote-config kill switch where practical.

---

## Phase 1 — Stop the bleeding (week 1)

Goal: eliminate footguns that can silently break production.

- [ ] **Delete `firestore.rules` L491–1296.** Single block only. Add CI guard: `test $(grep -c "^rules_version" firestore.rules) -eq 1`. _(I-001)_
- [ ] **Delete orphan indexes** `saved_jobs.workerId`, `referrals.status+completedAt`, `users.isBlocked+successfulReferrals`, `users.userRole+isBlocked+successfulReferrals`. Rename `referrals.referrerUserId` → `referrerId`. _(I-007)_
- [ ] **Tighten client write allowlist** in rules: remove `isVerified`, `isActive`, `rating`, `totalRatings`, `totalJobs`, `totalHires`, `referralStats.*` from `users`, `worker_profiles`, `employer_profiles`. _(I-005, I-006)_
- [ ] **Replace `Room.fallbackToDestructiveMigration()`** with explicit migrations for current schema version + add a guard in CI that fails if `fallbackToDestructiveMigration()` appears in source. _(I-011)_
- [ ] **Gate `DutyPeMessagingService.onMessageReceived` behind a self-contained payload.** No Firestore reads in the push path. Coordinate with CF to include all render fields in data payload. _(I-017)_

**Exit criteria:** CI guard green, smoke test of push + referral + apply flows unchanged.

---

## Phase 2 — Architecture cleanup (weeks 2–3)

Goal: kill hidden global state and undisciplined Firebase access.

- [ ] **Replace every `FirebaseFirestore.getInstance()`** in `RegisterScreen`, `LoginBottomSheet`, `EmployerHomeScreen`, `EmployerCompanyDetailsScreen`, `EmployerProfileCache`, `GuestEngagementWorker`, `MainNavGraph`, `AuthManager` with Hilt-injected `firestore`. _(I-003)_
- [ ] **Delete `state/AppStateManager.kt`, `state/ApplicationStateManager.kt`, `state/ProfileSetupStateManager.kt`.** Move their state into respective `ViewModel`s with `SavedStateHandle`. Update `AppModule` providers. _(I-004)_
- [ ] **Extract `StartDestinationResolver`** as pure suspend function. Call once in `MainActivity.onCreate`. Remove start-destination branching from `MainNavGraph`. _(I-013)_
- [ ] **Rename `metadata/` → `core/aggregations/`.** Cap `UserMetadata` query limits at UI-real needs (≤50). Convert listeners to `callbackFlow + WhileSubscribed`. _(I-014)_
- [ ] **Collapse duplicate one-shot + snapshot calls** in `WorkerHomeViewModel`, `AllJobsViewModel`, `EmployerApplicationViewModel`, `SavedJobsViewModel`, `ProfileViewModel` using the `ReferralViewModel` fix as template. _(I-009)_

**Exit criteria:** No static Firebase access in `git grep -nE 'FirebaseFirestore\.getInstance\(\)'`. No `*StateManager` class under `state/`.

---

## Phase 3 — Firebase schema redesign (weeks 4–6)

Goal: one source of truth per field, minimal docs, targeted indexes.

- [ ] **Drop `referralStats` from `/users/{uid}`.** Migrate reads to `/referral_stats/{uid}` everywhere (Android + CF paths still doing the dual-write). Deploy a one-off backfill CF that copies and then nulls the field for existing users. _(I-002)_
- [ ] **Drop `applicationCount` from `/job_details/{jobId}`.** Replace with `COUNT()` aggregation query cached by CF for 60s on the employer landing endpoint. _(I-012)_
- [ ] **Strip detail fields from `jobmetadata`.** Write a backfill that moves any leftover detail-only fields into `job_details`. _(I-008 groundwork)_
- [ ] **Denormalize `companyName` onto `jobmetadata`** via `employer_profiles` onUpdate CF trigger. _(3.8)_
- [ ] **Denormalize `fullName`/`profileImageUrl` onto `worker_profiles`** via `users` onUpdate CF trigger.
- [ ] **Enforce snapshot-listener allowlist.** Only: `app_config/referral`, own `users/{uid}`, own `referral_stats/{uid}`, the active `jobmetadata/{jobId}` on detail screen. Remove the `announcements` listener or gate it behind a feature flag. _(I-010)_

**Exit criteria:** No dual-written fields. `jobmetadata` doc size <1.5 KB p95.

---

## Phase 4 — Performance hardening (weeks 6–8)

Goal: lean UI, stable Compose, bounded startup.

- [ ] **Split `JobListing` into `JobCard` + `JobDetail`.** `WorkerJobCard` composable consumes `JobCard`. `JobDetailScreen` fetches `job_details`. _(I-008)_
- [ ] **Add `key = { it.id }`** (and `contentType = { ... }` where relevant) to every `LazyColumn`/`LazyRow` over model data. _(I-020)_
- [ ] **Break up god-composables.** `WorkerHomeScreen`, `PostJobScreen`, `AllJobsScreen`, `EmployerApplicationManagementScreen` — extract sections into sibling files. Target: <300 lines per screen file. _(I-019)_
- [ ] **Break up god-ViewModels.** `FirestoreJobViewModel` → `JobFeedViewModel` + `JobPaginator` + `JobCacheCoordinator`. _(I-018)_
- [ ] **Gate eager singletons.** `UserMetadata`, `JobMetadata`, `AppMetadata` must be lazy / post-auth. _(I-024)_
- [ ] **Generate a Baseline Profile.** Include home feed scroll + refer & earn + apply. Add to release build via `androidx.baselineprofile` plugin.

**Exit criteria:** cold start p50 <900 ms on Pixel 4a debug build; Compose recompose count on home feed scroll <2× visible items.

---

## Phase 5 — Scalability hardening (weeks 8–10)

Goal: survive a 10× traffic spike.

- [ ] **Cap all Firestore queries with `limit(...)`.** Audit `UserMetadata.kt` — 500-limit queries are too broad; cut to 50 for UI feeds.
- [ ] **Paginate notifications.** Replace any collection-scoped listener with `orderBy(createdAt, DESC).limit(20)` + pull-to-refresh.
- [ ] **Move hot counters off client.** Any field the client increments (`totalJobs`, `applicationsThisMonth`) becomes CF-only or derived.
- [ ] **FCM data-only messages** for state-change notifications that don't need foreground display; avoid app wake-ups.
- [ ] **App Check token rotation** verified (currently Play Integrity) — add a Crashlytics breadcrumb on token failures so we see silent permission denials.
- [ ] **Firestore read-cost metric** in Crashlytics custom keys per session. Alarm if p95 session reads >100.

**Exit criteria:** load-test scenario (10k synthetic sessions/min) stays under cost and latency budgets.

---

## Phase 6 — Cleanup & deletion (week 10+)

Goal: nothing in the repo that isn't earning its keep.

- [ ] **Delete dead composables** (`HowItWorksSection`, `RewardsSection`, `RedemptionInstructionsSection` in `WorkerReferEarnScreen`). _(I-029)_
- [ ] **Collapse duplicate routes** `WORKER_ALL_JOBS_FILTERED` and `PROFILE_SETUP_WITH_RETURN`. _(I-027, I-028)_
- [ ] **Collapse duplicate models** `JobListing` / `JobListingSummary` after JobCard/JobDetail split lands. _(I-021)_
- [ ] **Merge `NotificationService` + `SmartNotificationManager` → `NotificationRepository`.** _(I-023)_
- [ ] **Delete `managers/` package** — move `RoleSwitchManager` into `feature/auth/data/`. _(I-022)_
- [ ] **Delete `data/` top-level package** — move DataStore files into the feature that owns them.
- [ ] **Delete `cache/` package** — collapse into respective repositories.
- [ ] **Remove incomplete TODOs** (`PostJobScreen:L798`, `AIJobPostingViewModel:L313`, `EmployerHomeScreen:L444`). Either ship or delete. _(I-025)_
- [ ] **Audit `docs/` folder** — 100+ existing audit docs. Archive everything not under `docs/android-audit/` into `docs/archive/`.
- [ ] **Verify rule file, index file, CF exports** match a canonical list — add a CI step that diffs against `docs/android-audit/02_firebase_collection_field_minimization.md` inventory.

**Exit criteria:** `git grep -c TODO` < 30 across production code.

---

## Rollout discipline

- **Every phase lands behind a remote-config flag** where the change could affect user-facing behavior (e.g., schema migrations).
- **Every phase has a rollback note** in the PR description.
- **No phase starts until the previous phase is deployed to production for ≥1 week without Crashlytics regression.**
