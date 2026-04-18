# 07 — Phased Refactor Plan

> Each phase is a self-contained body of work that can ship to production. No phase requires the next to be completed. Build verification: `:app:compileDebugKotlin` after every PR.

---

## Phase 0 — Quick wins (this week)

| # | Task | Files | Effort | Risk |
|---|------|-------|--------|------|
| 0.1 | Publish `web/.well-known/assetlinks.json` with release SHA-256 fingerprint | `web/.well-known/assetlinks.json` (new) | 1 hr | None |
| 0.2 | Remove stale `AI_BACKEND_URL` and `AI_BACKEND_API_KEY` `buildConfigField`s | `app/build.gradle.kts` L45-46 | 5 min | None — verified no callers |
| 0.3 | Replace inline `popUpTo("employer_home")` with `popUpTo(Routes.EMPLOYER_HOME)` | `app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt` L559 | 5 min | None |
| 0.4 | Verify if `chat` deep-link host is intentional (chat removed); remove from manifest if dead | `AndroidManifest.xml` | 15 min | Low |
| 0.5 | Enable `StrictMode` in `DutyPeApplication` when `BuildConfig.DEBUG` | `DutyPeApplication.kt` | 30 min | None |

**Outcome:** App-Link funnel restored; misleading config removed; nav consistency restored.

---

## Phase 1 — Critical perf foundation (next sprint)

| # | Task | Files / Modules | Effort |
|---|------|-----------------|--------|
| 1.1 | Add `:baselineprofile` Macrobenchmark module via Studio template | `settings.gradle.kts`, new `baselineprofile/` module | 1 day |
| 1.2 | Generate Baseline + Startup Profile covering cold-start → `WorkerHomeScreen` and `EmployerHomeScreen` | New module | 1 day |
| 1.3 | Bundle profile in release AAB; verify Play Console upload | `app/build.gradle.kts` | 0.5 day |
| 1.4 | Annotate every UI-bound data class with `@Immutable` | `models/`, screen-local UI state classes | 1 day |
| 1.5 | Enable Compose Compiler metrics in `app/build.gradle.kts`; review report | `app/build.gradle.kts`, CI | 0.5 day |
| 1.6 | Cache `(authState, role, profileComplete)` in DataStore at login/logout; resolve start destination synchronously | `MainNavGraph.kt`, new `StartDestinationResolver` | 2 days |
| 1.7 | Audit all `whereEqualTo`/`orderBy` chains; add missing composite indexes | `firestore.indexes.json`, `services/` | 1 day |
| 1.8 | Audit all `addSnapshotListener` for explicit `remove()` on dispose | `services/` | 0.5 day |

**Outcome:** Cold-start improves 20-30%; Compose recomposition reduced; Firestore queries indexed and listeners leak-free.

---

## Phase 2 — Architectural cleanup (Q1)

| # | Task | Files |
|---|------|-------|
| 2.1 | Decompose `WorkerHomeScreen.kt` (2,000 LoC) into 5 sub-composables + single `WorkerHomeUiState` | `worker/screens/WorkerHomeScreen.kt` and new files |
| 2.2 | Combine multi-flow ViewModels into single `UiState` data class (`AllJobsViewModel`, `MetadataManager`, others) | `viewmodels/`, `metadata/` |
| 2.3 | Delete `JobApplicationService.updateApplicationStatusInternal`; route callers through `ApplicationManagementService.updateApplicationStatus` | `services/JobApplicationService.kt`, callers |
| 2.4 | Delete dead `WorkLocationManager`; refactor 4 caller screens to remove dependency | `services/WorkLocationManager.kt`, callers |
| 2.5 | Delete `SmartNotificationManager.notifyNearbyWorkersAboutNewJob` stub; verify Cloud Function does the work | `services/SmartNotificationManager.kt`, callers |
| 2.6 | Delete `ViewModelCleaner` (logging-only placeholder) | `viewmodels/ViewModelCleaner.kt` |
| 2.7 | Retire `BirthdayServiceHolder` / `InAppReviewTriggerServiceHolder` ViewModel-as-DI workaround | `viewmodels/` |
| 2.8 | Inject `SavedStateHandle` into key ViewModels (filters, JobDraft, role) | `viewmodels/` |

**Outcome:** Single mega-screen broken up; duplicate logic merged; dead code removed; state survives process death.

---

## Phase 3 — Navigation modernization (Q2)

| # | Task |
|---|------|
| 3.1 | Migrate `Routes.kt` constants to type-safe `@Serializable` destinations (Compose Navigation 2.8) — phased per feature |
| 3.2 | Move all deep links to `composable<X>(deepLinks = listOf(navDeepLink { ... }))` |
| 3.3 | Retire MainActivity broadcast indirection; FCM service constructs deep-link `PendingIntent` directly |
| 3.4 | Retire `DeepLinkHandler.kt` once migration complete |
| 3.5 | Document deep-link contract in repo `README.md` |

**Outcome:** Compile-time-safe routes; one source of truth for deep links; 6→3 indirection reduction.

---

## Phase 4 — Multi-module extraction (Q2-Q3)

Order of extraction (each is one PR; after each PR app still builds):

| # | Module | Source |
|---|--------|--------|
| 4.1 | `:core:common` | `core/`, `utils/` (subset) |
| 4.2 | `:core:design-system` | `ui/theme/`, `components/` (theme primitives) |
| 4.3 | `:core:navigation` | `navigation/` |
| 4.4 | `:data:room` | `database/`, Room DAOs |
| 4.5 | `:data:datastore` | DataStore preferences |
| 4.6 | `:data:firestore` | `services/firestore/`, `firestore/` |
| 4.7 | `:data:repository` | `repositories/` |
| 4.8 | `:domain:model`, `:domain:usecase` | `models/`, new use case extractions |
| 4.9 | `:feature:auth` | `auth/` |
| 4.10 | `:feature:onboarding` | `onboarding/` |
| 4.11 | `:feature:worker` | `worker/` |
| 4.12 | `:feature:employer` | `employer/` |
| 4.13 | `:feature:notifications` | `notifications/` |
| 4.14 | `:feature:referrals` | (extract referral screens from `common/`) |
| 4.15 | `:work` | `worker/sync/`, `workers/` |

**Outcome:** Faster incremental builds; clear ownership; ready for parallel feature teams.

---

## Phase 5 — Data-layer hardening (Q3)

| # | Task |
|---|------|
| 5.1 | Replace `fallbackToDestructiveMigration` with proper `Migration` objects from v6 forward |
| 5.2 | Generalize offline-first pattern (currently jobs-only) to applications, saved_jobs, user profile |
| 5.3 | Introduce `ListenerRegistry` to centralize Firestore listener lifecycle |
| 5.4 | Define `MergeStrategy` / `ConflictResolver` per repository |
| 5.5 | Move per-job applicant count to denormalized field updated by Cloud Function |
| 5.6 | Add CI step that diffs query patterns against `firestore.indexes.json` |

**Outcome:** Schema migrations are safe; data layer is uniform; Firestore reads bounded.

---

## Phase 6 — Release & observability (Q3)

| # | Task |
|---|------|
| 6.1 | Add Firebase Performance Monitoring; instrument cold-start + top 5 screens |
| 6.2 | Add Crashlytics breadcrumb on every nav route change |
| 6.3 | Set up Firestore cost dashboard + budget alerts |
| 6.4 | Generate Macrobenchmark suite for top 5 user flows |
| 6.5 | Add CI gate: cold-start regression > 5% blocks PR |
| 6.6 | Add CI gate: new Compose "unstable" type blocks PR |
| 6.7 | Set up `adb shell pm verify-app-links` smoke test in pre-prod |

**Outcome:** Performance regressions caught in CI; production observability in place.

---

## Phase 7 — Long-tail polish (Q4)

| # | Task |
|---|------|
| 7.1 | Adopt `androidx.startup.Initializer` for ordered, testable init |
| 7.2 | Adopt Paging 3 if jobs/applications list grows beyond simple pagination |
| 7.3 | Adopt Compose Preview Screenshot Testing or Paparazzi for UI regression |
| 7.4 | Adopt `dependencyAnalysis` Gradle plugin to detect unused deps after AI cleanup |
| 7.5 | Document worker idempotency contracts |
| 7.6 | Replace `popUpTo(0) { inclusive = true }` patterns with named-root pops |

---

## Cumulative outcome

After Phases 0-2, the app reaches **B+** scale-readiness — comfortably handles tens of thousands of DAU with predictable cold start.

After Phases 0-5, the app reaches **A-** — ready for 5M users with monitoring in place.

Phase 6-7 polish brings it to **A**.
