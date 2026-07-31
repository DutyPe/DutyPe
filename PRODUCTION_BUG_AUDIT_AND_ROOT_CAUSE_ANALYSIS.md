# DutyPe — Production Bug Audit: Verification, Root Cause Analysis & Applied Fixes

**Document Version:** 3.0 (Evidence-Based Edition — supersedes v2.0)
**Date:** July 31, 2026
**Branch audited:** `latest-code-backup-20260319`
**Verification method:** every claim below was checked against the actual source tree. Each finding cites `file:line`. Fixes were applied and compiled (`:app:compileDebugKotlin` → BUILD SUCCESSFUL).

---

## 0. Verdict on Document v2.0

Version 2.0 of this document was **not grounded in this codebase** and should not be used as an implementation guide. Concrete problems:

| v2.0 claim | Reality |
| --- | --- |
| Document covers Bugs 1–7 | File ended mid-document at the `## Bug 6 & 7` heading. Bugs 6 and 7 had **no content at all**. |
| Fix for Bug 5 | Contained `rawJobId.isNull@NotBlank()` — **not valid Kotlin**; would not compile. |
| Fix for Bug 2B | Used `kotlinx.coroutines.tasks.await(result)` — **no such API**. The real API is the `Task<T>.await()` extension. |
| Fixes reference `JobRepository`, `AuthRepository`, `HiringRoomViewModel`, `HiringRoomScreen`, `com.example.dutype.core.utils` | **None of these types exist.** The real equivalents are `FirestoreJobRepository`, `AuthManager`/`AuthFlowService`, and `EmployerApplicationManagementScreen`. |
| Bug 2A: "snapshot listeners leak, wrap them in `callbackFlow` + `awaitClose`" | **Already done correctly everywhere.** Verified in `AppConfigRepository` (3 listeners), `SubscriptionRepository` (3), `ReferralService` (3), `JobFirestoreService`, `JobApplicationService`. No leak found. |
| Tech stack: "Coil 2.4.0 (Custom Bounded Disk Cache)" | A bounded loader was **written but never connected**. See Finding 2 — this was a real bug, but not the one v2.0 described. |
| Bug 1 fix: call `finish()` in `onCreate` when `!isTaskRoot` | **Actively harmful.** `MainActivity` is the deep-link and notification-tap target. Finishing on `!isTaskRoot` would break every notification tap and every `dutype://` / `https://dutype.in` deep link. **Not applied.** |
| Bug 1 fix: `intent.removeExtra("referrer")` | Play Install Referrer is not delivered as an Activity intent extra. No-op. |
| Bug 1 fix: `firestore.clearPersistence()` during logout | Throws `FAILED_PRECONDITION` unless called before any Firestore use. Would fail at exactly the moment it is proposed to run. **Not applied.** |

**Net result:** 1 of the v2.0 symptom descriptions (Bug 5, "empty Hiring Room") pointed at a genuine defect, but the prescribed fix was invalid. The remaining prescriptions were either already implemented, non-compiling, or harmful.

The rest of this document records what was **actually** found and fixed.

---

## 1. Findings Applied

### Finding 1 — Session lifetime constant is 24× shorter than documented

**Severity:** Medium · **Status:** Fixed

`SESSION_TIMEOUT` was declared as `7.hours` directly beneath a comment reading *"Session timeout: 7 days of inactivity"* — [SessionManager.kt](app/src/main/java/com/example/dutype/auth/SessionManager.kt#L58).

`checkSession()` compares this against `KEY_SESSION_START` and calls `endSession()` when exceeded, wiping `session_prefs`. Sessions were therefore being torn down after 7 hours rather than 7 days.

**Fix:** `7.hours` → `7.days`, import updated to `kotlin.time.Duration.Companion.days`.

---

### Finding 2 — The tuned Coil `ImageLoader` was never used (root cause of storage growth and low-RAM instability)

**Severity:** High · **Status:** Fixed

[AppModule.kt](app/src/main/java/com/example/dutype/di/AppModule.kt#L596) provides a carefully tuned `ImageLoader`: a 32 MB hard memory cap on low-RAM devices, `allowHardware(false)` on low-RAM, and a bounded disk cache.

That loader was **unreachable at runtime**:

- `DutyPeApplication` did **not** implement `coil.ImageLoaderFactory`.
- Nothing called `Coil.setImageLoader(...)`.
- **No** `AsyncImage` / `SubcomposeAsyncImage` call site passed `imageLoader = ...` (verified across all 8 files that load images, including [OptimizedImage.kt](app/src/main/java/com/example/dutype/components/OptimizedImage.kt#L59), [JobDescriptionScreen.kt](app/src/main/java/com/example/dutype/worker/screens/JobDescriptionScreen.kt#L1081), [EmployerJobCard.kt](app/src/main/java/com/example/dutype/employer/components/EmployerJobCard.kt#L114)).

Consequently every image load resolved `LocalContext.current.imageLoader`, which falls back to **Coil's default singleton**. That default applies a 25% memory cache on *every* device — including the ≤3 GB devices the code explicitly tried to protect — and a disk cache that grows to Coil's 250 MB ceiling.

This is the genuine root cause behind both reported symptoms: unbounded storage growth, and memory pressure / OOM on low-end hardware.

**Fix:** `DutyPeApplication` now implements `ImageLoaderFactory` and returns the Hilt-provided loader. Injected as `dagger.Lazy<ImageLoader>` so construction stays off the cold-start path, consistent with the existing startup-perf convention in `MainActivity`.

---

### Finding 3 — Token-refresh coroutines accumulated, then silently died

**Severity:** Medium · **Status:** Fixed

`scheduleTokenRefresh()` launched an unbounded `while` loop on the singleton `CoroutineScope` — [SessionManager.kt](app/src/main/java/com/example/dutype/auth/SessionManager.kt#L200). Two defects:

1. **Accumulation.** Every `startSession()` launched *another* loop with no cancellation of the previous one. `startSession()` is reachable from both the `addAuthStateListener` callback and `AuthManager.setLoggedIn(true)`, so loops stacked up for the process lifetime.
2. **Silent death.** The loop condition was `while (_sessionState.value is SessionState.Active)`. Because `ACTIVITY_TIMEOUT` is 30 minutes and `updateActivity()` is **never called anywhere in the app** (verified — zero call sites), `checkSession()` returns `SessionState.Inactive` 30 minutes after login. That flipped the loop condition false and stopped token refresh entirely.

**Fix:** a single `tokenRefreshJob` is tracked; a new schedule cancels the prior one; the loop is now gated on `isActive && firebaseAuth.currentUser != null`; `endSession()` cancels it.

> **Note (not fixed — needs a product decision):** `updateActivity()` remains dead code, and nothing in the app collects `sessionState` or calls `isSessionActive()`. `SessionManager`'s state machine is currently vestigial. It should either be wired to the UI or removed. Left in place because deleting public API is out of scope for a bug fix.

---

### Finding 4 — Hiring Room rendered empty on a blank `jobId`

**Severity:** High · **Status:** Fixed — this is the real defect behind v2.0's "Bug 5"

Both navigation hosts coerced a missing argument to an **empty string**:

- [EmployerMainScreen.kt](app/src/main/java/com/example/dutype/navigation/EmployerMainScreen.kt#L300)
- [MainNavGraph.kt](app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt#L562)

```kotlin
val jobId = backStackEntry.arguments?.getString("jobId") ?: ""   // ← ""
```

`EmployerApplicationManagementScreen` then branches on `jobId == null` only — [line 111](app/src/main/java/com/example/dutype/employer/screens/applications/EmployerApplicationManagementScreen.kt#L111):

```kotlin
if (jobId == null) { viewModel.loadEmployerApplications() }
else {
    viewModel.loadMatchedWorkers(jobId)   // called with ""
    viewModel.loadJobApplications(jobId)  // called with ""
    jobViewModel.getJobById(jobId) { … }  // called with ""
}
```

`""` is not `null`, so the screen took the job-specific branch with an empty id: header rendered *"Hiring Room"*, and the default **"Best matches"** tab called the `matchWorkersForJob` callable, which rejects it server-side (`validateString(..., minLength = 4)` — [auth-callables.ts](functions/src/auth-callables.ts#L721)). Result: a Hiring Room with a title, tabs, and nothing in it.

The inconsistency is provable within the same file: the *second* `LaunchedEffect` at [line 125](app/src/main/java/com/example/dutype/employer/screens/applications/EmployerApplicationManagementScreen.kt#L125) correctly uses `jobId.isNullOrBlank()`.

**Fix:** normalise at the navigation boundary in both hosts — `?.takeIf { it.isNotBlank() }` — so a blank argument degrades to the already-correct "All Applications" screen instead of a broken Hiring Room. Fixed at the nav layer rather than threading a normalised value through all ~30 `jobId` usages in the 1,950-line screen.

---

### Finding 5 — Stale start destination survived logout

**Severity:** High · **Status:** Fixed — this is the real defect behind v2.0's "Bug 1 Symptom B / Bug 4"

`StartDestinationCache` persists the last resolved route in its **own** `SharedPreferences` file (`start_destination_cache`) so the NavHost can pick a start destination synchronously on cold start — [StartDestinationCache.kt](app/src/main/java/com/example/dutype/navigation/StartDestinationCache.kt#L28).

Two gaps let a signed-out user cold-start directly into an authenticated screen:

1. **Incomplete auth gate.** [AppStartupViewModel.kt](app/src/main/java/com/example/dutype/viewmodels/AppStartupViewModel.kt#L39) guarded only `WORKER_HOME` and `EMPLOYER_HOME`. But `updateDestination()` also caches `PROFILE_SETUP` and `EMPLOYER_PROFILE_SETUP`, which equally require a signed-in `uid`. A user who logged out while their profile was incomplete would relaunch straight into the mandatory profile-setup flow with no authenticated user.
2. **Canonical logout didn't clear it.** `AuthManager.logout()` — documented in its own KDoc as *"the CANONICAL logout implementation"* — cleared `auth_prefs` but never touched `start_destination_cache`. Only [ProfessionalLogoutDialog.kt](app/src/main/java/com/example/dutype/components/ProfessionalLogoutDialog.kt#L244) cleared it, as a separate step. The account-deletion path at [AccountDeletionDialog.kt](app/src/main/java/com/example/dutype/components/AccountDeletionDialog.kt#L256) calls `authManager.logout()` directly and therefore **left the stale route behind**.

**Fix:** the auth gate now includes both profile-setup routes, and `AuthManager.logout()` clears the cache itself, so every current and future logout path is covered.

---

## 2. Claims Investigated and Rejected

| Claim (v2.0) | Evidence | Verdict |
| --- | --- | --- |
| Firestore snapshot listeners leak | All 11 `addSnapshotListener` sites use `callbackFlow` + `awaitClose { registration.remove() }`, or explicit `remove()` on a retained handle (`SavedWorkLocationsStore`) | **Rejected** — already correct |
| Unsafe auth dereferences cause NPEs | Repo-wide search for `currentUser!!`, `.uid!!` returned **zero** matches | **Rejected** |
| Play Store installer intent corrupts the task stack | `MainActivity` already normalises launch intents and re-`setIntent`s them; no crash mechanism identified | **Not reproducible** — proposed fix would break deep links |
| Room WAL files bloat storage | No evidence found; Room WAL is checkpointed automatically and is bounded | **Unsubstantiated** |

---

## 3. Open Issue — Not Fixed (requires backend work)

### Nearby-jobs fallback path has no server-side geo restriction

The primary worker feed is correct: [FirestoreJobRepository.getAllJobsSummary](app/src/main/java/com/example/dutype/repositories/FirestoreJobRepository.kt#L448) uses a proper 9-cell GeoFire query with progressive radius expansion (10 → 15 → 20 km …).

The **fallback** path, [JobFirestoreService.getAllJobsSummary](app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt#L576), has server-side geohash filtering deliberately disabled (documented in-code). It fetches the newest *N* jobs **globally**, ordered by `createdAt`, then filters client-side against `STRICT_NEARBY_RADIUS_KM = 10.0` ([FirestoreJobViewModel.kt](app/src/main/java/com/example/dutype/viewmodels/FirestoreJobViewModel.kt#L232)).

**Consequence:** in a low-density city, if the newest *N* jobs nationwide are all elsewhere, a worker sees **zero** nearby jobs even though qualifying jobs exist — they simply fall outside the recency window. This matches the "location / filtration failures" symptom class that v2.0 left undocumented.

**Why not fixed here:** a correct fix needs a multi-cell geohash query merged with `orderBy("createdAt")`, which requires **new Firestore composite indexes** deployed first, plus a `geoHash` backfill for legacy job documents. That is a backend change with a data migration — out of scope for a client-side patch and unsafe to land blind.

Secondary observation: [AllJobsViewModel.kt](app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt#L505) treats a null distance as a match (`dist == null || dist <= filters.maxDistance`), so jobs lacking coordinates appear under "Within 2 km". This looks intentional (avoid hiding jobs) but is worth a product decision.

---

## 4. Files Changed

| File | Change |
| --- | --- |
| [DutyPeApplication.kt](app/src/main/java/com/example/dutype/DutyPeApplication.kt) | Implements `ImageLoaderFactory`; returns Hilt-provided `ImageLoader` via `dagger.Lazy` |
| [SessionManager.kt](app/src/main/java/com/example/dutype/auth/SessionManager.kt) | `7.hours` → `7.days`; single tracked `tokenRefreshJob`, cancelled on new schedule and on `endSession()` |
| [AuthManager.kt](app/src/main/java/com/example/dutype/auth/AuthManager.kt) | `logout()` clears `StartDestinationCache` |
| [AppStartupViewModel.kt](app/src/main/java/com/example/dutype/viewmodels/AppStartupViewModel.kt) | Auth gate extended to profile-setup routes |
| [EmployerMainScreen.kt](app/src/main/java/com/example/dutype/navigation/EmployerMainScreen.kt) | Blank `jobId` → `null` |
| [MainNavGraph.kt](app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) | Blank `jobId` → `null` |

**Verification:** `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**. No new errors or warnings; all remaining warnings are pre-existing (Material 3 `Divider`/`menuAnchor` deprecations, unchecked casts).

**Build environment note:** `local.properties` is gitignored and absent from the repo; it must be created with `sdk.dir` pointing at the Android SDK. Gradle must run on JDK 17–21 (Android Studio's bundled JBR works); JDK 25 is not supported by Gradle 8.13.

---

## 5. Regression Risk Assessment

| Fix | Blast radius | Risk |
| --- | --- | --- |
| Coil `ImageLoaderFactory` | All image loading | **Low–Medium.** Behaviour now matches the loader the team already wrote and intended. Low-RAM devices get a smaller cache and software decoding — a deliberate trade. Worth a visual smoke test on image-heavy screens (job cards, profile photos). |
| `SESSION_TIMEOUT` | Local session bookkeeping only | **Low.** `endSession()` does not sign out of Firebase; it clears local tracking prefs. |
| `tokenRefreshJob` | Background token refresh | **Low.** Strictly fewer coroutines; refresh now survives past 30 minutes as originally intended. |
| Blank `jobId` → `null` | Hiring Room entry | **Low.** The `null` path is the pre-existing, already-correct "All Applications" behaviour. |
| Start-destination auth gate | Cold start after logout | **Low.** Only adds routes to an existing guard; the async resolver still reconciles afterwards. |
| `logout()` clears cache | All logout paths | **Low.** `ProfessionalLogoutDialog` already did this; the call is now idempotent and centralised. |

---

## 6. Recommended Next Steps

1. **Backend:** deploy composite indexes and backfill `geoHash`, then enable server-side geo filtering in the fallback query (Section 3). This is the highest-value remaining item.
2. **Decide the fate of `SessionManager`:** either surface `sessionState` in the UI and call `updateActivity()` from `MainActivity.onResume()`, or delete the vestigial state machine.
3. **Add regression tests** for the two navigation fixes — a blank `jobId` and a post-logout cold start are both cheap to cover and were both silent failures.
4. **Instrument** Coil disk-cache size post-release to confirm Finding 2 resolves the storage reports.
