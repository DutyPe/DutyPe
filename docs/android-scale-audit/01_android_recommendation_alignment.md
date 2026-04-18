# 01 — Android Recommendation Alignment Report

Each row scores **current state** against the **recommended Android pattern**, with concrete file:line evidence and the **target state**.

Scoring: ✅ aligned · ⚠️ partial drift · ❌ missing / wrong direction

---

## 1. Layered Architecture (UI → State Holder → Domain → Data)

| Aspect | Status | Evidence | Gap | Target |
|--------|--------|----------|-----|--------|
| UI layer separated | ✅ | `worker/screens`, `employer/screens`, `auth/`, `common/screens` | — | Keep |
| State holder per screen | ⚠️ | 25 `@HiltViewModel` classes; some screens use Hilt-injected services directly inside `@Composable` (e.g., `BirthdayServiceHolder`, `firestoreFromHilt(context)`) | Composables read from services bypassing ViewModels | All side-effect calls must originate in a ViewModel |
| Explicit domain layer | ❌ | No `domain/` package; "use cases" are inlined into ViewModels and services | No reusable use cases for cross-screen logic (e.g., "apply to job" exists in `JobApplicationService` + `SmartJobApplicationViewModel` + `ApplicationManagementService`) | Introduce `domain/usecase/*` for shared workflows |
| Data layer split (local + remote) | ✅ | `OfflineFirstJobRepository` merges Room + Firestore | Could be more — only jobs are offline-first today | Apply pattern to applications, saved jobs, profile |
| Dependency direction | ⚠️ | A few "manager" classes touch UI state via injected `LocalContext` style helpers | Slight inversion; not severe | Repos must not import `androidx.compose.*` |

**Severity:** Medium. **Fix:** Add `domain/usecase/`, push all Compose-side service access into ViewModels, retire `*ServiceHolder` ViewModels.

---

## 2. UDF + State Holder Pattern

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Private mutable / public read-only state | ✅ | Consistent across `viewmodels/` (e.g., `AllJobsViewModel.kt` L183-193) | — |
| Single `UiState` per screen | ⚠️ | Some screens hold 5-8 separate `MutableStateFlow`s instead of one `data class UiState` | `WorkerHomeScreen` consumes `jobUiState`, `filteredJobs`, `vacancyStatuses`, `currentLocation`, etc. as separate flows |
| State survives config change | ✅ | ViewModel-scoped | — |
| `SavedStateHandle` use for process death | ❌ | Not detected anywhere | Auth state, role, current job filters lost on process death; recovery falls through to Firestore re-read |
| State exposed only via `StateFlow` | ✅ | — | — |

**Severity:** Medium. **Fix:** Adopt `data class WorkerHomeUiState(...)` per screen; thread `SavedStateHandle` into key ViewModels (filters, draft, role).

---

## 3. Compose State Handling

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `@Stable`/`@Immutable` on UI models | ❌ | Zero matches in `models/` | All `data class` with `List`/`Map` → unstable → unnecessary recomposition |
| State hoisting in screens | ⚠️ | Mixed: many screens hoist; `WorkerHomeScreen` re-fetches ViewModels in nested composables (just fixed, item completed) | Continue audit on `EmployerHomeScreen`, `PostJobScreen` |
| Avoid business logic in composables | ⚠️ | `WorkerHomeScreen` does filter computation in composable (since cleaned via `derivedStateOf`); other screens still do `.filter {}` directly | Move all `.filter`/`.sort`/`.map` into ViewModel |
| `LazyColumn` keys + content type | ⚠️ | Most lists use `items(list, key = ...)`; not verified for content types | Audit each `LazyColumn`; add `key` and `contentType` |
| `derivedStateOf` use | ✅ | Used in `WorkerHomeScreen` for scroll-derived alpha | — |
| Compose Compiler metrics | ❌ | Not enabled in `build.gradle.kts` | No way to find unstable types systematically |

**Severity:** High (recomposition cost ≈ user-perceived perf). **Fix:** Annotate `@Immutable` on all UI data classes; enable Compose Compiler metrics; eliminate work in composables.

---

## 4. Navigation

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Single `Routes.kt` source of truth | ✅ | 61 const vals + helper builders | — |
| Type-safe routes (Kotlin Serialization) | ❌ | All string-based routes | Compose Navigation 2.8 supports type-safe; current code is stringly typed → arg mismatch risk |
| Single `NavHost` per persona | ⚠️ | Two NavGraphs: `MainNavGraph` (auth/onboarding) + `WorkerNavGraph`/`EmployerMainScreen` (in-tab); plus `WorkerMainScreen` Composable doing nested nav | Three places define screen registration → drift risk |
| Args explicitly declared | ✅ | `composable(route)` with named args | — |
| `popUpTo` / `inclusive` discipline | ⚠️ | Inconsistent: `popUpTo(0) { inclusive = true }` (RegisterScreen.kt L248), `popUpTo("employer_home")` inline string (PostJobScreen.kt L559) | Use Routes constants always |
| Start destination is deterministic | ❌ | Computed from onboarding + auth + DataStore role + Firestore role + profile completion in MainNavGraph L73-180 | Can take seconds; user sees blank navigation |

**Severity:** High. **Fix:** Migrate to type-safe Compose Navigation (Kotlin Serialization). Make start destination deterministic via a single `StartDestinationResolver` that returns immediately from cached state.

---

## 5. Deep Links

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `<intent-filter>` per scheme/host | ✅ | `dutype://` with 9 hosts | — |
| Routes registered with `deepLinks = listOf(navDeepLink { ... })` | ⚠️ | Manual broadcast pipe via MainActivity → MainNavGraph receiver → DeepLinkHandler | Bypasses Navigation's built-in deep link system |
| Handler ↔ manifest 1:1 | ✅ | All 9 hosts routed in `DeepLinkHandler.kt` | — |
| Path params validated | ⚠️ | Some routes accept `{jobId}` without bounds check | Defensive parsing missing |

**Severity:** Medium. **Fix:** Use Compose Navigation `deepLinks = listOf(navDeepLink { uriPattern = "..." })` in each `composable()` block. Retire the broadcast indirection.

---

## 6. App Links (Digital Asset Links)

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `android:autoVerify="true"` on intent filter | ✅ | `AndroidManifest.xml` L128-135 | — |
| Only HTTPS for verified scheme | ✅ | HTTP removed (per comment) | — |
| `assetlinks.json` published at `https://dutype.in/.well-known/assetlinks.json` | ✅ | Served via Next.js route at `web/app/.well-known/assetlinks.json/route.ts` (both Play upload + release fingerprints) |
| Multiple package fingerprints (debug + release) listed | ✅ | 2 SHA-256 entries present |
| Tested with `adb shell pm verify-app-links` | ❌ | Not in CI — recommended pre-release smoke |

**Severity:** Critical (silent UX failure). **Fix:** Generate `assetlinks.json` with release SHA-256 fingerprint, publish to `https://dutype.in/.well-known/assetlinks.json`, validate via Statement List Generator.

---

## 7. Startup Optimization

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Splash Screen API | ✅ | `MainActivity.kt` L83-85 | — |
| App Startup library used | ❌ | No custom `Initializer<T>` classes | Init logic lives directly in `Application.onCreate()` |
| Heavy work deferred to background | ✅ | Most work in `applicationScope.launch { ... }` | One sync block: `Firebase.initialize` + `initializeAppCheck` (line 59-62) |
| WorkManager init disabled (manual via Hilt) | ✅ | Manifest provider L157-164 | — |
| Cold-start measured + tracked | ❌ | No `MacrobenchmarkRule` or Startup Profile | — |
| **Baseline Profile module** | ❌ | Not in `settings.gradle.kts` | **20-30% startup regression unrealised; hot-path classes JIT-compiled at runtime** |

**Severity:** Critical for scale. **Fix:** Add `:baselineprofile` Macrobenchmark module; generate Baseline + Startup Profile per release; wire into CI.

---

## 8. ANR Prevention

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `MainThreadChecker` + `ANRHandler` present | ✅ | `performance/MainThreadChecker.kt`, `ANRHandler.kt`; init at `DutyPeApplication` L77 | — |
| No `.await()` on main thread | ⚠️ | 1 case in `PostJobScreen.kt`-ish range (Firestore .await inside composable scope) | Audit all `.await()` callsites |
| Disk I/O off main | ⚠️ | DataStore is suspended; `SharedPreferences` used in `LocaleHelper` `setLocale()` runs on whatever thread caller is on (often main during attachBaseContext) | Acceptable for tiny prefs; flag if it grows |
| Deferred non-critical init | ✅ | Notification channels deferred to IO | — |
| Strict mode enabled in debug | ❌ | Not detected | Recommended for dev builds |

**Severity:** Medium. **Fix:** Enable `StrictMode` in debug; audit all `await()` sites; replace any remaining `runBlocking` if found.

---

## 9. Offline-First Data

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Firestore offline persistence enabled | ✅ | `AppModule.kt` L88-98, 100 MB cap | — |
| Room DB present (encrypted) | ✅ | `database/DutyPeDatabase.kt` (Job, Application, SavedJob) | — |
| Repository merges local + remote | ⚠️ | `OfflineFirstJobRepository` does this for jobs only | Apply to applications, saved jobs |
| Conflict resolution strategy documented | ❌ | No `MergeStrategy` / `ConflictResolver` | If local edits + remote update collide, last-write-wins implicit |
| `NetworkBoundResource` pattern or equivalent | ⚠️ | Custom impl in `OfflineFirstJobRepository`; not generalized | Extract reusable abstraction |
| Schema migration plan | ⚠️ | `fallbackToDestructiveMigration()` for v1-6 | Acceptable for early app; not safe at 5M users with valuable local cache |

**Severity:** Medium-High. **Fix:** Generalize offline-first pattern; define proper migrations before next release; add merge strategy.

---

## 10. WorkManager Usage

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Hilt integration | ✅ | `HiltWorkerFactory` injected; default initializer disabled | — |
| Periodic vs one-time discipline | ✅ | JobSync (30 min periodic, network constraint), PendingApp (6 hr periodic), GuestEngagement (one-time, 1 hr delay) | — |
| Idempotent workers | ⚠️ | Not explicitly tested; periodic workers could run twice on doze exit | Add idempotency keys |
| ExpeditedWork / Foreground policy | ❌ | None | If you ever need urgent uploads, plan now |
| WorkManager observed via Play Vitals | ❌ | No metric channel | Add `setExpedited` for user-visible work (e.g., job application submit retry) |

**Severity:** Low for current set of workers. **Fix:** Document idempotency contract per worker.

---

## 11. Baseline Profiles

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `:baselineprofile` module | ❌ | Absent in `settings.gradle.kts` | — |
| Macrobenchmark tests | ❌ | Absent | — |
| Startup Profile generated | ❌ | Absent | — |
| Profile bundled in release AAB | ❌ | Absent | — |

**Severity:** Critical for 5M scale. **Fix:** **Required before next major release.** Use Studio template or Gradle plugin `androidx.baselineprofile`; cover cold-start to `WorkerHomeScreen` + `EmployerHomeScreen`.

---

## 12. R8 / App Optimization

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| `isMinifyEnabled = true` (release) | ✅ | `app/build.gradle.kts` L74 | — |
| `isShrinkResources = true` | ✅ | L75 | — |
| ProGuard rules narrow (no `-keep class **`) | ✅ | `proguard-rules.pro` reviewed | — |
| ABI splits | ⚠️ | Comment about arm64-v8a + armeabi-v7a — needs verification in current `splits {}` block | — |
| 16 KB page-size compatibility | ✅ | `useLegacyPackaging = false`, `keepDebugSymbols += "**/*.so"` (`build.gradle.kts` L99-105) | — |
| R8 full mode (`-Pandroid.enableR8.fullMode=true`) | ❓ | Not verified — check `gradle.properties` | Default in AGP 8+ |

**Severity:** Low. **Fix:** Verify R8 full mode + ABI split; otherwise solid.

---

## 13. Release-quality Monitoring

| Aspect | Status | Evidence | Gap |
|--------|--------|----------|-----|
| Crashlytics enabled (release only) | ✅ | `DutyPeApplication.kt` L416 | — |
| User identification on Crashlytics | ✅ | `ErrorHandler.kt` L257 sets `setUserId` | — |
| Performance Monitoring (Firebase) | ❌ | Not detected | Add for screen render / network traces |
| Custom screen-trace instrumentation | ❌ | Not detected | Required to find P99 hot screens at scale |
| Play Vitals (ANR / crash) baseline | N/A | External — set up dashboards | — |

**Severity:** Medium. **Fix:** Add Firebase Performance Monitoring; instrument top 5 screens with `Trace.beginSection` or Compose `recordSubFrame` equivalents.

---

## Scorecard summary

| Area | Score |
|------|-------|
| Architecture | B |
| UDF / State holder | B+ |
| Compose state handling | C |
| Navigation | C+ |
| Deep links | B- |
| **App Links** | **D** (assetlinks.json missing) |
| Startup | B- |
| ANR prevention | B |
| Offline-first | B |
| WorkManager | A- |
| **Baseline Profiles** | **F** (missing) |
| R8 | A- |
| Monitoring | C+ |

**Composite: B-** — solid foundation, three critical gaps (App Links / Baseline Profile / Compose stability).
