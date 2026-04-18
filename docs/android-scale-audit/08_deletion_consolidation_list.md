# 08 — Deletion & Consolidation List

> Concrete files / symbols / config to remove or merge. Each row includes evidence and impact verification.

---

## A. Files to delete

| # | Path | Why | Caller cleanup |
|---|------|-----|----------------|
| A.1 | `app/src/main/java/com/example/dutype/services/WorkLocationManager.kt` | All methods return dummy values (`emptyList()` / `null`); confirmed dead by prior audit | Refactor 4 caller screens to remove dependency before delete |
| A.2 | `app/src/main/java/com/example/dutype/viewmodels/ViewModelCleaner.kt` | Logging-only placeholder; provides no behavior | None — verify no Hilt-injected callers |
| A.3 | `app/src/main/java/com/example/dutype/viewmodels/BirthdayServiceHolder.kt` (if exists) | ViewModel-as-DI workaround pattern; replace with `EntryPointAccessors` or move logic into screen-owning ViewModel | Update Composables that consume it |
| A.4 | `app/src/main/java/com/example/dutype/viewmodels/InAppReviewTriggerServiceHolder.kt` (if exists) | Same as A.3 | Same |

---

## B. Symbols (methods) to delete

| # | Symbol | File | Why |
|---|--------|------|-----|
| B.1 | `JobApplicationService.updateApplicationStatusInternal()` | `app/src/main/java/com/example/dutype/services/JobApplicationService.kt` | Duplicates `ApplicationManagementService.updateApplicationStatus()`; route callers through canonical service |
| B.2 | `SmartNotificationManager.notifyNearbyWorkersAboutNewJob()` | `app/src/main/java/com/example/dutype/services/SmartNotificationManager.kt` | Empty stub; comment says "moved to Cloud Functions"; verify Cloud Function is active before delete |

---

## C. Logic to merge

| # | Source | Target | How |
|---|--------|--------|-----|
| C.1 | `JobApplicationService.updateApplicationStatusInternal` | `ApplicationManagementService.updateApplicationStatus` | Update all callers of the internal method to call the canonical one; delete the internal method; add unit test |
| C.2 | Multiple separate `MutableStateFlow`s in `WorkerHomeViewModel`, `AllJobsViewModel`, `MetadataManager` | One `data class UiState` per ViewModel | Combine into single immutable state class; expose single `StateFlow<UiState>` |
| C.3 | Three NavGraph owners (`MainNavGraph`, `WorkerMainScreen`, `EmployerMainScreen` for nav) | One root `NavHost` + nested feature graphs | Phase 3 of refactor plan |
| C.4 | Deep-link routing path (MainActivity broadcast → MainNavGraph receiver → DeepLinkHandler) | `composable<X>(deepLinks = ...)` per destination | Phase 3 of refactor plan |

---

## D. Routes to remove (none confirmed)

Prior audits verified zero unused routes in `Routes.kt`. **Re-verify** before any future deletion: search for each `Routes.X` constant referenced at least once outside `Routes.kt` itself.

---

## E. Build / config cleanups

| # | File | Lines | Action |
|---|------|-------|--------|
| E.1 | `app/build.gradle.kts` | L45 | Remove `buildConfigField("String", "AI_BACKEND_URL", ...)` |
| E.2 | `app/build.gradle.kts` | L46 | Remove `buildConfigField("String", "AI_BACKEND_API_KEY", ...)` |
| E.3 | `gradle.properties` | (search) | Remove any `AI_BACKEND_URL`/`AI_BACKEND_API_KEY` properties if present |
| E.4 | `app/google-services.json` | — | No action; verify no AI-related config block present |
| E.5 | `AndroidManifest.xml` | L100-110 | Remove `chat` host from `dutype://` intent filter if chat feature is permanently removed |

---

## F. Documentation to delete (if user agrees)

The repository root contains many one-off completion / status / fix-summary markdown files. These were useful at the time but are now noise:

| Pattern | Examples | Recommendation |
|---------|----------|----------------|
| `*_SUMMARY.md` at repo root | `AUTHENTICATION_FIX_SUMMARY.md`, `DUAL_ROLE_FIX_SUMMARY.md`, `REFERRAL_FIX_SUMMARY.md`, `REFERRAL_SYSTEM_FIXES_SUMMARY.md` | Move to `docs/historical/` or delete after team confirms |
| `*_COMPLETE.md` at repo root | `DUAL_ROLE_IMPLEMENTATION_COMPLETE.md` | Same |
| `*_DIAGNOSIS.md` | `LOGIN_ISSUE_DIAGNOSIS.md` | Archive |
| `DATABASE_OPTIMIZATION_PART*.md` (10 files) under `docs/` | All 10 part files + summary | Consolidate into single `DATABASE_OPTIMIZATION.md` once changes land |

**Not deleted in this audit** — flagged for team decision.

---

## G. Pre-known cleanup checklist (already-completed prior phases — verify they hold)

| # | Item | Status |
|---|------|--------|
| G.1 | AI backend (`services/ai/`, `AIBackendRepository`, `AIJobPostingViewModel`, `AIJobPostingScreen`, `AIModule`) | ✅ Removed |
| G.2 | `HomeStates.kt`, `ComposableUtils.kt`, `JobCardModels.kt` | ✅ Removed |
| G.3 | `ProfileCompletionDialog`, `CompactProfileCompletionBanner` | ✅ Removed |
| G.4 | `SmartNotificationWorker` | ✅ Removed (replaced with Cloud Function) |
| G.5 | Firestore cache size capped at 100 MB (was UNLIMITED) | ✅ Applied |

---

## H. Anti-checklist — DO NOT delete

These look removable but are required:

| Item | Why required |
|------|-------------|
| `WorkManagerInitializer` provider disabled in manifest | Required because we wire WorkManager via Hilt; default initializer would conflict |
| `DutyPeApplication.workManagerConfiguration` override | Provides `HiltWorkerFactory` |
| `Firebase.initialize()` synchronous call before App Check | App Check requires Firebase init first |
| `installSplashScreen()` before `super.onCreate()` | Splash Screen API contract |
| `popUpTo(Routes.SELECT_ROLE) { inclusive = true }` in role-selection flow | Required to prevent back-navigation to role select after role chosen |
| `AllJobsViewModel.PAGE_SIZE = 10` vs `FirestoreJobViewModel.PAGE_SIZE = 30` | Intentional UX split; documented in prior audit |
| `BuildConfig.DEBUG` Crashlytics gate | Required to avoid noisy dev crashes in dashboard |
| `fallbackToDestructiveMigration()` | Acceptable until Phase 5 introduces real migrations; **do not remove until replacement is in place** |

---

## I. Summary of impact

| Action | Files removed | LoC removed (est.) | Caller updates |
|--------|---------------|--------------------|----------------|
| Phase 0 cleanups | 0 (config only) | ~10 | 1 (PostJobScreen popUpTo) |
| Phase 2 deletions (A.1-A.4) | 3-4 | ~600-800 | ~6-10 callers |
| Phase 2 method merges (B, C) | 0 (methods only) | ~200 | ~5 callers |
| Phase 3 nav consolidation | 1 (`DeepLinkHandler.kt`) | ~150 | All deep-link entry points |
| **Total estimated cleanup** | **~5 files** | **~1,000 LoC** | **~15 callers** |

Each step is small, testable, and reversible.
