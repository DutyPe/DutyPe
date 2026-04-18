# 00 — Master Summary (Principal Architect Audit)

> **Verdict:** The app is **shipped, not architected**. It works today but its Firestore schema, state ownership, and static Firebase access will break under real growth. Below is the unvarnished view.

_Last updated: 2026-04-17. Scope: `app/`, `functions/`, `firestore.rules`, `firestore.indexes.json`._

> **Reality-check delta (2026-04-18, verified by grep on `main`):**
>
> Several P0/P1 items in the table below are **already resolved** in the current tree — the audit was written against an older snapshot. Status column reflects today's reality:
> - **#1 (rules triplication):** ✅ RESOLVED — `firestore.rules` is now 488 lines, exactly 1 `rules_version` and 1 `service` block.
> - **#3 (`FirebaseFirestore.getInstance()` static access):** ✅ RESOLVED — 0 occurrences in `screens/`, `auth/`, `workers/`, `cache/`. Remaining 7 hits are all in `di/AppModule.kt` (Hilt provider — correct) and `utils/FirestoreUtils.kt` (utility helper — acceptable).
> - **#5 (orphan indexes):** ✅ RESOLVED — `firestore.indexes.json` is now 18 indexes, all matching real query call sites. **Round 9 also fixed two broken indexes** that referenced non-existent collection names (`jobs` → `jobmetadata`, `reports` → `job_reports`); the queries those indexes were meant to cover were running un-indexed in production.
> - **#6 (`JobListing` god-model):** 🟡 OVERSTATED — actual `JobListing.kt` is 31 fields / 66 lines, not the claimed 50/200+. Still worth a `JobCard` vs `JobDetail` split, but lower priority than audit suggested.
> - **#8 (Room `fallbackToDestructiveMigration`):** ✅ RESOLVED — already restricted to v1–6 via `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)`; v7+ throws `IllegalStateException` if Migration missing. Schema export wired.
>
> Items still genuinely open: **#2** (referralStats dual-write), **#4** (state singletons), **#7** (`isVerified` client-writable), **#9** (one-shot+listener duplicate reads partially audited), **#10** (god-ViewModels partially split).
>
> Round 9 also centralized `referral_stats`, `withdrawals`, `app_config`, `announcements`, `job_details` collection names into `FirestoreCollections.kt` (was 5 stray inline string literals).

---

## 0.1 Top 10 problems you must fix before 5M users

| # | Problem | Severity | Evidence |
|---|---|---|---|
| 1 | `firestore.rules` is **triplicated** — 1296 lines, three complete rule blocks at L1, L491, L954. Only the **last** one is active on deploy. Any edit to L1–490 is dead. | **P0** | [firestore.rules](../../firestore.rules) — `rules_version = '2'` appears at L1, L491, L954 |
| 2 | `referralStats` (19 fields) is **dual-written** to both `/users/{uid}` and `/referral_stats/{uid}`. Two sources of truth, double write cost, guaranteed drift. | **P0** | [functions/src/referral-system.ts](../../functions/src/referral-system.ts) L388, L459, L649, L1002 |
| 3 | `FirebaseFirestore.getInstance()` used directly in **8+ screens/workers** — bypasses DI, makes testing impossible, and creates parallel Firestore clients. | **P0** | [RegisterScreen.kt](../../app/src/main/java/com/example/dutype/auth/RegisterScreen.kt) L869–877, [LoginBottomSheet.kt](../../app/src/main/java/com/example/dutype/components/LoginBottomSheet.kt) L883–1078, [EmployerHomeScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerHomeScreen.kt) L148–166, [EmployerProfileCache.kt](../../app/src/main/java/com/example/dutype/cache/EmployerProfileCache.kt) L37, [GuestEngagementWorker.kt](../../app/src/main/java/com/example/dutype/workers/GuestEngagementWorker.kt) L149, [AppConfigRepository.kt](../../app/src/main/java/com/example/dutype/repositories/AppConfigRepository.kt) (injected but pattern leaks) |
| 4 | **3 global singleton state managers** (`AppStateManager`, `ApplicationStateManager`, `ProfileSetupStateManager`) shadow ViewModel state — hidden global coupling, state drift after role switch. | **P0** | [app/src/main/java/com/example/dutype/state/](../../app/src/main/java/com/example/dutype/state/) |
| 5 | **3 orphan/broken Firestore indexes** reference fields that **do not exist** in the canonical `users` schema (`isBlocked`, `successfulReferrals`, `userRole`). | **P1** | [firestore.indexes.json](../../firestore.indexes.json) indexes #13, #15, #16 |
| 6 | `JobListing` model is a **50-field god-object** used for both list cards and full detail. Every home feed query deserializes detail-only fields (description, benefits, workingHours, etc.). | **P1** | [JobListing.kt](../../app/src/main/java/com/example/dutype/models/JobListing.kt) |
| 7 | `isVerified` on `employer_profiles` is **client-writable** per rules but comment says CF-only — any worker can self-verify via direct Firestore write. **Security hole.** | **P1** | `firestore.rules` L173 (first copy) |
| 8 | Room `fallbackToDestructiveMigration()` is enabled — next schema bump silently wipes every user's local cache. | **P1** | [DutyPeDatabase.kt](../../app/src/main/java/com/example/dutype/database/DutyPeDatabase.kt) |
| 9 | `loadReferralData()` was firing **two reads** per open (one-shot + snapshot listener). Same pattern likely in Worker/Employer home, MyJobs, Applications — write amplification under load. | **P1** | Refer-Earn fixed in this session; others not yet audited |
| 10 | **27 ViewModels**, several >400 lines (FirestoreJobViewModel, AllJobsViewModel, AIJobPostingViewModel). God-ViewModels mix UI state, pagination, prefetching, caching, and network. | **P2** | `app/src/main/java/com/example/dutype/viewmodels/` |

## 0.2 What is actually good (don't touch these)

- **App Check + Firebase init order** in [DutyPeApplication.kt](../../app/src/main/java/com/example/dutype/DutyPeApplication.kt) — correct sequence, `logFirebaseBinding` now deferred.
- **SQLCipher + keystore-bound passphrase** in [DatabasePassphraseProvider.kt](../../app/src/main/java/com/example/dutype/database/security/DatabasePassphraseProvider.kt).
- **Deterministic application doc IDs** `{jobId}_{workerId}` — prevents duplicate applications at the rule layer.
- **jobmetadata vs job_details split** — already list-vs-detail at the Firestore layer (but the Android model squashes them back together — see issue #6).
- **Admin-editable referral config** at `/app_config/referral` with CF-side cache and realtime Android `StateFlow` — shipped this session, use as the template for all future feature-flag config.
- **WorkManager footprint is small** — 3 workers (JobSync, PendingAppNotification, GuestEngagement), no duplicate work with CF.

## 0.3 Where the architecture lies to you

- Package names **pretend** layering exists but don't enforce it. `data/`, `repositories/`, `services/`, `services/firestore/`, `managers/`, `firestore/`, `cache/`, `database/` all touch the same concerns with no module boundaries.
- `state/` exists **in parallel with** `viewmodels/` — Compose screens read from both. There is no single source of truth for "what the UI is showing".
- `metadata/` sounds like it's constants — it's actually a runtime Firestore reader (`UserMetadata`, `JobMetadata`, `AppMetadata`) that **opens snapshot listeners at app start**.
- A "Repository" here is not a repository. Most repositories inject Firestore services that inject `FirebaseFirestore` directly and expose suspend functions — there is no cache/network coordinator pattern.

## 0.4 Quick-wins you can ship this week

1. **Delete `firestore.rules` L491–1296.** Keeping only the first rule block is a 5-minute fix that prevents deploy surprises.
2. **Remove orphan indexes** (`firestore.indexes.json` #6, #10, #13, #15, #16) — zero runtime risk, saves write cost.
3. **Migrate 8 screens off `FirebaseFirestore.getInstance()`** to injected `firestore` (Hilt). Mechanical change.
4. **Make `isVerified`, `isActive`, `rating`, `totalRatings`, `totalHires`, `totalJobs` CF-only** via rules, matching the comment's intent.
5. **Kill `referral_stats/{uid}` collection OR kill `referralStats` on `/users/{uid}`.** Pick one, never both.
6. **Add `key = { it.id }`** to every `LazyColumn`/`LazyRow` in the worker/employer home feeds. Already cheap cost, stops unnecessary recompose.
7. **Collapse duplicate one-shot + snapshot reads** in `ReferralViewModel`, `WorkerHomeViewModel`, `AllJobsViewModel`, `EmployerApplicationViewModel`, `SavedJobsViewModel` — same pattern as the Refer & Earn fix.

## 0.5 How to navigate the rest of these docs

- **01_brutal_issue_list.md** — every problem with severity, evidence, and fix.
- **02_firebase_collection_field_minimization.md** — every collection, every field, KEEP/DERIVE/REMOVE.
- **03_target_system_design.md** — target Android + Firebase architecture for 5M users.
- **04_refactor_rebuild_plan.md** — 6 phases, ordered by risk.
- **05_deletion_list.md** — what to delete, file by file.
- **06_single_source_of_truth_matrix.md** — who owns what.
- **07_performance_and_scale_checklist.md** — concrete checklist.
