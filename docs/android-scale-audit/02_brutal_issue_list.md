# 02 — Brutal Issue List

> One row per concrete issue. **Severity:** P0 (block release) · P1 (fix this quarter) · P2 (next quarter) · P3 (when convenient).
> Categories: `STARTUP`, `NAV`, `DEEPLINK`, `COMPOSE`, `STATE`, `DATA`, `WORK`, `BUILD`, `RELEASE`, `DEAD-CODE`, `DUPLICATE`, `SECURITY`, `OBSERVABILITY`.

---

## P0 — Critical (block next release)

### P0-1 · DEEPLINK · `assetlinks.json` — RESOLVED ✅
- **Status:** Already published via Next.js route at [`web/app/.well-known/assetlinks.json/route.ts`](../../web/app/.well-known/assetlinks.json/route.ts) with both Play upload + release SHA-256 fingerprints. Audit assumption was wrong — file exists as a dynamic route, not a static file in `public/`.
- **Verification:** `curl https://dutype.in/.well-known/assetlinks.json` should return both fingerprints.
- **Action:** Run `adb shell pm verify-app-links --re-verify com.dutype.app` once after each release to confirm verification status.

### P0-2 · BUILD · Stale AI buildConfigFields after AI backend deletion — RESOLVED ✅
- **Status:** `AI_BACKEND_URL` and `AI_BACKEND_API_KEY` `buildConfigField`s and the release-time AI HTTPS validator block removed from [app/build.gradle.kts](../../app/build.gradle.kts).

### P0-3 · DEAD-CODE · `WorkLocationManager` returns dummy values but is wired — RESOLVED ✅
- **Status:** Deleted [`WorkLocationManager.kt`](../../app/src/main/java/com/example/dutype/services/). Replaced with [`SavedWorkLocationsStore`](../../app/src/main/java/com/example/dutype/services/SavedWorkLocationsStore.kt) — an in-memory `@Singleton` `MutableStateFlow<List<WorkLocation>>` that's honest about being session-scoped (no fake "saved successfully" toasts). Refactored 3 caller screens ([PostJobScreen](../../app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt), [EditJobScreen](../../app/src/main/java/com/example/dutype/employer/screens/EditJobScreen.kt), [EmployerAddressManagementScreen](../../app/src/main/java/com/example/dutype/employer/screens/settings/EmployerAddressManagementScreen.kt)) to read via `collectAsState()` and write synchronously. Dropped `workLocationManager` field from `WorkerHomeViewModel` + `FirestoreJobViewModel`; removed Hilt provider.

---

## P1 — High (next sprint)

### P1-1 · STARTUP · No Baseline Profile — RESOLVED ✅ (scaffold)
- **Status:** Created [`:baselineprofile`](../../baselineprofile/build.gradle.kts) Macrobenchmark module (`com.android.test` plugin + `androidx.baselineprofile` v1.2.4) with [`BaselineProfileGenerator`](../../baselineprofile/src/main/java/com/dutype/app/baselineprofile/BaselineProfileGenerator.kt) covering cold-start → first scroll. Wired the consumer plugin into [`:app`](../../app/build.gradle.kts), added `androidx.profileinstaller` runtime, and `"baselineProfile"(project(":baselineprofile"))` dependency. Plugin classpath alignment verified (`./gradlew :baselineprofile:tasks` BUILD SUCCESSFUL; `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL).
- **Generation:** `./gradlew :app:generateBaselineProfile` runs the generator on the configured Gradle Managed Device (Pixel 6 / API 34 / AOSP). Output lands at `app/src/<variant>/generated/baselineProfiles/baseline-prof.txt` and is auto-bundled into the release AAB by the AndroidX plugin. First run downloads ~1GB AVD image; subsequent runs reuse cache.
- **Action required:** Run `./gradlew :app:generateBaselineProfile` once on a workstation with virtualization enabled and commit the produced `baseline-prof.txt` so production builds ship with the profile.

### P1-2 · COMPOSE · `WorkerHomeScreen.kt` ≈ 2,000 LoC — PARTIAL ✅ (Phase 1 shipped)
- **Why bad:** Single composable holds layout, state, filtering, scroll alpha, banner state, recommendation logic. Large compile units → slow incremental builds + recomposition cost.
- **Round 8 — Phase 1 (dead-state purge, shipped):** Removed ≈120 LoC of orchestrator state that was assigned but never read or only consumed by commented-out / disabled code:
  - `birthdayInfo` + `showBirthdayBanner` `MutableState` pair (never set, never read)
  - `permissionsRequested`, `isFirstTimeUser`, `bottomSheetsShownInSession` flags (assigned, never read)
  - `hasNotificationPermission` (assigned, never read — the actual permission check happens in `notificationPermissionManager`)
  - `clickedJobId` (assigned in `onJobClick` but never consumed; lambda body now empty)
  - `tabTitles`, `tabIcons`, `coroutineScope` (duplicate of `scope`), `pagerState` + the pager-driven status-bar `LaunchedEffect` (left over from a removed tab-pager layout)
  - `voiceSearchLauncher` + the matching commented-out Voice Search FAB block (≈40 LoC)
  - `playStoreUrl` constant + `shareToWhatsApp` lambda (≈35 LoC of dead WhatsApp-share code; live copies remain in `WorkerProfile`, `DigitalVisitingCardScreen`, refer-earn screens where they are actually invoked)
  - 9 unused imports + the now-redundant `ExperimentalPagerApi` opt-in
- **Outstanding (deferred to dedicated session):** Extract the remaining orchestrator into `home/WorkerHomeAppBar.kt`, `home/WorkerHomeBannerSection.kt`, `home/WorkerHomeJobList.kt` and consolidate hoisted state into `WorkerHomeUiState`. Each subsequent phase needs device QA on cold-start / scroll / banner alpha / location flow before it can land.
- **Validation:** `:app:compileDebugKotlin` BUILD SUCCESSFUL in 3m 36s after the cleanup.

### P1-3 · COMPOSE · No `@Stable` / `@Immutable` on UI models — RESOLVED ✅
- **Status:** All 14 UI-bound model files in `models/` annotated with `@Immutable` (or `@Stable` if they hold `var` fields like `JobListing`, `JobListingSummary`, `NotificationData`).
### P1-4 · NAV · `popUpTo` inline string in PostJobScreen — RESOLVED ✅
- **Status:** Replaced inline `popUpTo("employer_home")` with `popUpTo(Routes.EMPLOYER_HOME)` in [PostJobScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt).

### P1-5 · DUPLICATE · `JobApplicationService.updateApplicationStatusInternal` duplicates `ApplicationManagementService.updateApplicationStatus` — RESOLVED ✅ (false alarm)
- **Status:** Verified `JobApplicationService.updateApplicationStatusInternal` does not exist anywhere in the codebase — the audit's grep was stale. The single canonical write path is [`JobApplicationService.updateApplicationStatus`](../../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L1052). [`EmployerApplicationViewModel.updateApplicationStatus`](../../app/src/main/java/com/example/dutype/viewmodels/EmployerApplicationViewModel.kt#L321) is the only other reference and correctly delegates to the service rather than duplicating logic. `ApplicationManagementService` was already deleted in Round 2 (see P2-9b).

### P1-6 · DATA · Unindexed `whereEqualTo` queries — RESOLVED ✅
- **Status:** Audited all 90 `whereEqualTo`/`orderBy` chains across the app. Added 4 missing composite indexes to [firestore.indexes.json](../../firestore.indexes.json):
  - `jobs`: `status` + `salaryType` + `createdAt`↓  — `JobFirestoreService.searchJobsWithFilters` payType filter path
  - `applications`: `workerId` + `status`  — `JobApplicationService.getApplicationsByStatus`
  - `referrals`: `status` + `completedAt`↓  — `ReferralService.getSuccessStories`
  - `reports`: `reporterId` + `timestamp`↓  — `ReportingService.getUserReports`
- **Action required:** Deploy `firestore.indexes.json` (`firebase deploy --only firestore:indexes`) before next release that exercises these query paths.

### P1-7 · STARTUP · `MainNavGraph` start-destination compute is async + heavy — RESOLVED ✅
- **Status:** Added [`StartDestinationCache`](../../app/src/main/java/com/example/dutype/navigation/StartDestinationCache.kt) — a `SharedPreferences`-backed synchronous cache. [`MainNavGraph`](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) reads it at composition time and seeds `startDestination` instantly on cold start; the existing async resolver still runs to reconcile against DataStore + Firestore and corrects the route if state diverged. Cache is updated when the resolver settles, refreshed on role switch ([`RoleSwitchManager`](../../app/src/main/java/com/example/dutype/managers/RoleSwitchManager.kt)), and cleared on logout ([`ProfessionalLogoutDialog`](../../app/src/main/java/com/example/dutype/components/ProfessionalLogoutDialog.kt)).

### P1-8 · DEAD-CODE · `SmartNotificationManager.notifyNearbyWorkersAboutNewJob` stub — RESOLVED ✅
- **Status:** Method deleted; sole caller in `JobFirestoreService.createJob` removed; `SmartNotificationManager` constructor parameter dropped from `JobFirestoreService`. Cloud Function handles nearby-worker notifications.

### P1-8b · DATA · Firestore listener leak audit — RESOLVED ✅
- **Status:** Audited all 4 `addSnapshotListener` call sites ([AppConfigRepository](../../app/src/main/java/com/example/dutype/repositories/AppConfigRepository.kt), [AnnouncementService](../../app/src/main/java/com/example/dutype/services/AnnouncementService.kt), [JobFirestoreService.getJobsByEmployerRealtime](../../app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt), [ReferralService.getReferralStatsFlow + getReferralHistoryFlow](../../app/src/main/java/com/example/dutype/services/ReferralService.kt)). All correctly wrapped in `callbackFlow { ... awaitClose { registration.remove() } }`. No leaks.

---

## P2 — Medium (this quarter)

### P2-1 · COMPOSE · Multiple separate `MutableStateFlow`s per ViewModel — RESOLVED ✅
- **Status (AllJobsViewModel):** [`AllJobsViewModel`](../../app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt) consolidated. The previously separate `_selectedChip`, `_searchQuery`, `_filters`, `_initialCategory` `MutableStateFlow`s collapsed into fields on `AllJobsUiState`; every mutation now routes through `_uiState.update { it.copy(...) }`. Public `selectedChip` / `searchQuery` / `filters` StateFlows preserved as derived (`_uiState.map { ... }.distinctUntilChanged().stateIn(...)`).
- **Status (WorkerHomeViewModel):** [`WorkerHomeViewModel`](../../app/src/main/java/com/example/dutype/viewmodels/WorkerHomeViewModel.kt) consolidated. `_jobVacancyStatuses` folded into `WorkerHomeUiState.jobVacancyStatuses`; `combine(_uiState, _jobVacancyStatuses)` collapsed to a single `_uiState.map`. Dead `_userSkills` MutableStateFlow (privately held, never publicly exposed, hardcoded to `emptyList()` at the only consumer) deleted. Public `jobVacancyStatuses` StateFlow preserved as derived so [`WorkerHomeScreen`](../../app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreen.kt) call sites are unchanged.
- **Status (MetadataManager):** [`MetadataManager`](../../app/src/main/java/com/example/dutype/metadata/MetadataManager.kt) is a `@Singleton`, **not** a `ViewModel`. Its 4 flows (`_isInitialized`, `_isLoading`, `_lastRefreshed`, `_cacheStats`) expose orthogonal lifecycle telemetry consumed independently by different surfaces. Folding into one container would force unrelated subscribers to wake on every emission — the inverse of what the audit recommendation aims for. Left as-is by design.

### P2-2 · STATE · No `SavedStateHandle` use — RESOLVED ✅
- **Status (AllJobsViewModel):** [`AllJobsViewModel`](../../app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt) now persists `selectedChip`, `searchQuery`, `initialCategory`, and every field of `JobFilters` to `SavedStateHandle` on every setter and seeds them on init.
- **Status (JobDraft):** Already resolved by [`JobDraftDataStore`](../../app/src/main/java/com/example/dutype/data/JobDraftDataStore.kt) — a `@Singleton` `DataStore<Preferences>`-backed store with 24-hour TTL. DataStore survives **process death AND app restart**, which is strictly stronger than `SavedStateHandle` (which only survives configuration changes + low-memory recreates within the same install). No additional work needed.
- **Status (current role):** Already resolved by [`RoleCacheManager`](../../app/src/main/java/com/example/dutype/cache/RoleCacheManager.kt). [`RoleSwitchManager`](../../app/src/main/java/com/example/dutype/managers/RoleSwitchManager.kt) is a `@Singleton`, not a `ViewModel`, so it cannot receive `SavedStateHandle`; current role lives in DataStore-backed `RoleCacheManager` plus Firestore as the source of truth. Same DataStore-vs-SavedStateHandle reasoning as above.

### P2-3 · NAV · String-based routes (not type-safe) — PARTIAL ✅ (scaffold shipped)
- **Why bad:** Compose Navigation 2.8 (and Jetpack Navigation 3, stable since Nov 2025) supports type-safe routes via Kotlin Serialization; the existing 61 `const val` strings in [`Routes.kt`](../../app/src/main/java/com/example/dutype/navigation/Routes.kt) give zero compile-time guarantees on path arguments.
- **Round 8 — scaffold (shipped):**
  - Applied `org.jetbrains.kotlin.plugin.serialization` to [`:app`](../../app/build.gradle.kts) so destinations can be serialized.
  - Added [`navigation/destinations/Destinations.kt`](../../app/src/main/java/com/example/dutype/navigation/destinations/Destinations.kt) — a single sealed `Destination` interface with one `@Serializable` `object` / `data class` per existing route (61 entries, mirroring `Routes.kt` 1:1). The shape is intentionally compatible with `androidx.navigation3.runtime.NavKey`: when the migration to Nav 3 happens, only the marker interface needs to change.
  - Declared Navigation 3 versions in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml) (`nav3Core = "1.0.0"`, `lifecycleViewmodelNav3 = "2.10.0-rc01"`) plus `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-lifecycle-viewmodel-navigation3` library entries. **Not yet implementation()-wired into `:app`** because Nav 3 requires `compileSdk = 36` (project is on 35); compileSdk bump is a separate, dedicated change.
- **Migration playbook (per route, do once compileSdk=36 lands or stay on Compose Nav 2.8):**
  ```kotlin
  // BEFORE (Routes.kt + MainNavGraph.kt + caller):
  const val JOB_DETAIL = "job_detail_route/{jobId}"
  composable(
      route = Routes.JOB_DETAIL,
      arguments = listOf(navArgument("jobId") { type = NavType.StringType })
  ) { backStackEntry -> JobDetailScreen(jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable) }
  navController.navigate("job_detail_route/$id")

  // AFTER (Compose Nav 2.8 path — no compileSdk bump needed):
  composable<Destination.JobDetail> { entry -> JobDetailScreen(jobId = entry.toRoute<Destination.JobDetail>().jobId) }
  navController.navigate(Destination.JobDetail(jobId = id))

  // AFTER (Nav 3 path — once compileSdk=36 + Nav3 deps wired):
  entry<Destination.JobDetail> { key -> JobDetailScreen(jobId = key.jobId) }
  navigator.goTo(Destination.JobDetail(jobId = id))
  ```
- **Migration order** (lowest blast radius first):
  1. **Wave 1 — Leaf routes with no args** (≈18 routes: `Onboarding`, `SelectRole`, `WorkerHome`, `EmployerHome`, `WorkerProfile`, etc.) using `@Serializable data object`.
  2. **Wave 2 — Single-arg routes** (≈25 routes: `JobDetail`, `WorkerProfileView`, etc.) using `@Serializable data class`.
  3. **Wave 3 — Multi/nullable args + the Compose Nav `?optional=` query-string routes** (`EnhancedLogin(role: String?)`, `ProfileSetupWithReturn(returnRoute)`).
- **Outstanding:** Wire the destinations into `MainNavGraph`/screen call sites and delete `Routes.kt`. 200–300 edit points across ≈50 screens; do as a focused multi-day task with device regression testing after each wave.
- **Validation:** Plugin + destinations file + libs catalog entries compile green (`:app:compileDebugKotlin` BUILD SUCCESSFUL).

### P2-4 · DEEPLINK · Routing via broadcast indirection — RESOLVED ✅
- **Status:** Replaced the `LocalBroadcastManager` relay between [MainActivity](../../app/src/main/java/com/example/dutype/MainActivity.kt) and [MainNavGraph](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) with a Hilt `@Singleton` [`DeepLinkBus`](../../app/src/main/java/com/example/dutype/navigation/DeepLinkBus.kt) (`MutableSharedFlow<Uri>` with `replay = 1` so deep links arriving during nav-graph resolution are not dropped). `MainActivity.onNewIntent` calls `deepLinkBus.emit(uri)`; `MainNavGraph` collects `deepLinkBus.events` inside a `LaunchedEffect` and routes through `DeepLinkHandler.handleDeepLinkUri`. Three indirections (Intent broadcast → `BroadcastReceiver` → handler) collapsed to one (`tryEmit` → `collect`). No `BroadcastReceiver` lifetimes to manage.

### P2-5 · DATA · Room `fallbackToDestructiveMigration()` for v1-6 — RESOLVED ✅
- **Status:** Existing policy already restricts destructive behavior to legacy versions only via `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)` in [AppModule.kt](../../app/src/main/java/com/example/dutype/di/AppModule.kt#L164-L172). From v7 onward Room throws `IllegalStateException` if a Migration is missing — exactly the safety the audit demanded. Additionally enabled `exportSchema = true` on [DutyPeDatabase](../../app/src/main/java/com/example/dutype/database/DutyPeDatabase.kt) and wired `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in [app/build.gradle.kts](../../app/build.gradle.kts) so future Migration objects can be unit-tested with `MigrationTestHelper`.

### P2-6 · DUPLICATE · "Manager" sprawl — PLAN READY 📝
- **Why bad:** ~16 surviving `*Manager.kt` + 5 distinct notification-related classes spread across 4 packages — overlapping responsibilities make it unclear which one to use.
- **Inventory (verified by grep, post Round 1–7 cleanup):**
  | Bucket | Classes | Verdict |
  |--------|---------|---------|
  | **Auth/Session** | `AuthManager`, `SessionManager`, `FirebasePNVManager` | Keep separate — distinct SDKs/responsibilities |
  | **Role** | `RoleSwitchManager`, `RoleCacheManager` | Keep separate — orchestrator vs pure DataStore I/O |
  | **Cache** | `JobCacheManager`, `RoleCacheManager`, `MetadataManager` | Keep separate — different cache shapes (LRU vs DataStore vs Firestore) |
  | **Notifications** | `FCMTokenManager` (services/), `NotificationService` (services/), `LocalNotificationService` (notifications/), `InAppNotificationManager` (notifications/), `SmartNotificationManager` (services/), `NotificationPermissionManager` (utils/) | **Consolidate** — see playbook below |
  | **Review/Update** | `InAppReviewManager`, `InAppUpdateManager` | Keep separate — distinct Play Core APIs |
  | **Observability** | `ObservabilityManager` | Keep — already a single facade |
  | **Ads** | `AdManager` | Keep |
- **Notification consolidation playbook** (the only genuine win, ~1 day):
  1. Move `FCMTokenManager` and `SmartNotificationManager` from `services/` into `notifications/`.
  2. Add a thin `NotificationCoordinator` facade in `notifications/` that injects all 5 sub-components and exposes the 3 lifecycle hooks call sites actually use:
     ```kotlin
     @Singleton
     class NotificationCoordinator @Inject constructor(
         val tokens: FCMTokenManager,
         val push: NotificationService,        // Firestore-backed push
         val local: LocalNotificationService,  // in-app feed
         val inApp: InAppNotificationManager,  // in-app banners
         val smart: SmartNotificationManager,  // server-trigger logic
     ) {
         suspend fun bootstrap(role: String?) { tokens.registerTokenWithRole(role ?: "") }
         suspend fun onLogout() { tokens.unregister() }
     }
     ```
  3. Migrate the *only* call sites that use >1 sub-component (likely `MainActivity`, `RoleSwitchManager`, `SessionManager`) to depend on the facade. Sub-components stay; new code only learns about the facade.
- **Already retired during Rounds 1–7 (net Manager count down ~20%):** `WorkLocationManager` (P0-3), `ViewModelCleaner` (P2-9), `BirthdayServiceHolder` + `InAppReviewTriggerServiceHolder` (P2-10).
- **Why not auto-shipped:** The notification subsystem touches push registration, token rotation on role switch, and Firestore write paths. A safe consolidation requires running the app on a device with FCM enabled and verifying tokens still register, topics still subscribe, and in-app banners still fire after each call-site swap. Genuinely a focused half-day with code review.

### P2-7 · OBSERVABILITY · No Firebase Performance Monitoring — RESOLVED ✅
- **Status:** Applied `com.google.firebase.firebase-perf` Gradle plugin v1.4.2 at root + `:app` so Firebase auto-instruments network calls and screen rendering. Added a manual `app_cold_start` trace in [MainActivity](../../app/src/main/java/com/example/dutype/MainActivity.kt) that begins at the top of `onCreate` and stops when Compose reports fully drawn.

### P2-8 · OBSERVABILITY · No `StrictMode` in debug — RESOLVED ✅
- **Status:** Thread + VM `StrictMode` policies enabled in `DutyPeApplication.onCreate()` under `BuildConfig.DEBUG`.

### P2-9 · DEAD-CODE · `ViewModelCleaner` is a logging-only placeholder — RESOLVED ✅
- **Status:** File deleted; Hilt provider removed; `RoleSwitchManager` constructor param dropped; sole call site replaced with comment explaining ViewModels clear automatically via composable scope.

### P2-9b · DEAD-CODE · `ApplicationManagementService` unused — RESOLVED ✅
- **Status:** File deleted; Hilt provider removed. (Audit assumption that `JobApplicationService.updateApplicationStatusInternal` was a duplicate was wrong — that method does not exist; `ApplicationManagementService` was the unused component.)

### P2-10 · DUPLICATE · `BirthdayServiceHolder` / `InAppReviewTriggerServiceHolder` ViewModel-as-DI workaround — RESOLVED ✅
- **Status:** Both holder ViewModels deleted. Replaced with [`ComposeServiceEntryPoint`](../../app/src/main/java/com/example/dutype/di/ComposeServiceEntryPoint.kt) (Hilt `@EntryPoint` on `SingletonComponent`) plus `rememberBirthdayService()` / `rememberInAppReviewTriggerService()` composable accessors in [`ComposeServiceAccessors.kt`](../../app/src/main/java/com/example/dutype/di/ComposeServiceAccessors.kt). All 8 call sites (1 birthday + 7 review-trigger) migrated; no per-screen ViewModel is spawned just to pipe a singleton service into Compose.

---

## P3 — Low (eventually)

### P3-1 · BUILD · No Compose Compiler metrics enabled — RESOLVED ✅
- **Status:** Opt-in metrics+reports wired in [app/build.gradle.kts](../../app/build.gradle.kts) `kotlinOptions`. Run `./gradlew :app:compileDebugKotlin -Pcom.dutype.enableComposeMetrics=true` to dump reports under `app/build/compose-metrics` and `app/build/compose-reports`. Off by default to keep CI fast.

### P3-2 · MULTI-MODULE · Whole app is `:app` monolith — PLAN READY 📝
- **Why bad:** Single Gradle module → every change recompiles everything; no architectural enforcement; cross-feature dependencies invisible.
- **Phased extraction blueprint** (each phase = 1–3 days, do in this exact order so each phase compiles independently):
  1. **`:design-system`** — Move `ui/theme/`, `ui/components/`, `ui/responsive/` (purely visual, zero business logic). Lowest risk, highest reuse.
  2. **`:core:common`** — Move `models/` (already `@Immutable`-annotated per P1-3), `utils/` pure functions, `firestore/FirestoreCollections.kt` constants. Pure Kotlin module if possible.
  3. **`:core:data`** — Move `repositories/`, `services/firestore/`, `database/` (Room), `data/` (DataStore). Depends on `:core:common`.
  4. **`:feature:auth`** — Move `auth/`. Depends on `:core:data`, `:design-system`.
  5. **`:feature:worker`** — Move `worker/`. Depends on `:core:data`, `:design-system`.
  6. **`:feature:employer`** — Move `employer/`. Depends on `:core:data`, `:design-system`.
  7. **`:app`** — Becomes thin shell (`MainActivity`, `MainNavGraph`, Hilt aggregation, manifest, application class).
- **Per-phase template:**
  ```kotlin
  // settings.gradle.kts
  include(":design-system")

  // design-system/build.gradle.kts
  plugins {
      alias(libs.plugins.android.library)
      alias(libs.plugins.kotlin.android)
      alias(libs.plugins.kotlin.compose)
  }
  android {
      namespace = "com.dutype.designsystem"
      compileSdk = 35
      defaultConfig { minSdk = 24 }
  }
  dependencies {
      implementation(platform(libs.androidx.compose.bom))
      implementation(libs.androidx.material3)
      implementation(libs.androidx.ui)
  }
  ```
- **Hilt across modules:** Use `@EntryPoint` (already established by `ComposeServiceEntryPoint` in Round 4) for cross-module access. Each feature module declares its own `@Module @InstallIn(SingletonComponent::class)`; `:app` aggregates via `@HiltAndroidApp`.
- **Why not auto-shipped:** Multi-module migration is the highest-blast-radius refactor in this audit. Every Gradle dependency, every `import` statement, every Hilt graph touch needs verification. Properly executed: 1–2 weeks total + extensive regression testing. **Strongly recommend doing this AFTER P1-2 + P2-3 completes, not before.**

### P3-3 · WORK · Worker idempotency not documented — RESOLVED ✅
- **Status:** All 4 WorkManager workers ([PendingApplicationNotificationWorker](../../app/src/main/java/com/example/dutype/workers/PendingApplicationNotificationWorker.kt), [GuestEngagementWorker](../../app/src/main/java/com/example/dutype/workers/GuestEngagementWorker.kt), [JobSyncWorker](../../app/src/main/java/com/example/dutype/worker/sync/JobSyncWorker.kt), [JobPostingWorker](../../app/src/main/java/com/example/dutype/employer/sync/JobPostingWorker.kt)) carry an explicit "Idempotency contract" KDoc block describing why retries converge.

### P3-4 · NAV · `popUpTo(0) { inclusive = true }` aggressive clears — RESOLVED ✅
- **Status:** All 7 call sites converted to `popUpTo(navController.graph.startDestinationId) { inclusive = true }` — the modern named-root pop pattern. Sites: [AuthFlowHelpers](../../app/src/main/java/com/example/dutype/auth/AuthFlowHelpers.kt) (3), [RegisterScreen](../../app/src/main/java/com/example/dutype/auth/RegisterScreen.kt), [EnhancedLoginScreen](../../app/src/main/java/com/example/dutype/auth/EnhancedLoginScreen.kt), [RoleSwitchManager](../../app/src/main/java/com/example/dutype/managers/RoleSwitchManager.kt), [ProfessionalLogoutDialog](../../app/src/main/java/com/example/dutype/components/ProfessionalLogoutDialog.kt) (2).

### P3-5 · COMPOSE · No screenshot tests — PLAN READY 📝
- **Why bad:** Visual regressions ship to production undetected.
- **Recommendation:** Adopt **Compose Preview Screenshot Testing** (Google-supported, GA in AGP 8.5+; we are on 8.7.3 ✅). Beats Paparazzi for our stack because it works directly on the `@Preview` functions you already write.
- **Setup** (15 minutes, isolated to a new `screenshotTest/` source set so it cannot break the main build):
  ```kotlin
  // app/build.gradle.kts
  plugins {
      // existing plugins...
      id("com.android.compose.screenshot") version "<verify latest stable on Maven Central>"
  }

  android {
      experimentalProperties["android.experimental.enableScreenshotTest"] = true
  }

  dependencies {
      screenshotTestImplementation(libs.androidx.ui.tooling)
      screenshotTestImplementation(libs.androidx.ui.test.junit4)
  }
  ```
  ```kotlin
  // app/src/screenshotTest/java/com/example/dutype/JobCardScreenshotTest.kt
  class JobCardScreenshotTest {
      @Preview(showBackground = true)
      @Composable
      fun JobCard_Default() {
          dutypeTheme { JobCard(job = sampleJob()) }
      }

      @Preview(showBackground = true, fontScale = 2.0f)
      @Composable
      fun JobCard_LargeFont() {
          dutypeTheme { JobCard(job = sampleJob()) }
      }
  }
  ```
- **Workflow:**
  - Generate baseline: `./gradlew :app:updateDebugScreenshotTest`
  - Verify in CI: `./gradlew :app:validateDebugScreenshotTest` (fails if any preview diverges)
  - Reports: `app/build/reports/screenshotTest/preview/debug/index.html`
- **Recommended initial coverage** (5 components, 1 day):
  1. `JobCard` (worker-home most-recomposed component)
  2. `WorkerHomeAppBar` (after P1-2 Phase 2)
  3. `EmptyJobsState`
  4. `LoadingContent`
  5. `dutypeTheme` color tokens
- **Why not auto-shipped:** The `com.android.compose.screenshot` Gradle plugin is fast-moving and version-pinning it without empirical compatibility verification against our Compose BOM 2024.09.00 + KSP + Firebase plugin chain is risky in a final round. 15-minute experiment best done by an engineer who can revert if it breaks the build, then add a CI step that runs `validateDebugScreenshotTest` on every PR.

### P3-6 · BUILD · No version catalog usage check — RESOLVED ✅
- **Status:** Added `com.autonomousapps.dependency-analysis` plugin v2.5.0 to root [`build.gradle.kts`](../../build.gradle.kts). Run `./gradlew buildHealth` to surface unused / misused / transitive-leaked dependencies. Plugin tasks are off the critical path of normal builds.

### P3-7 · OBSERVABILITY · Crashlytics doesn't capture per-screen breadcrumbs — RESOLVED ✅
- **Status:** Added `LaunchedEffect(navController)` in `MainActivity` that collects `currentBackStackEntryFlow` and calls `FirebaseCrashlytics.getInstance().log("nav: $route")` on every destination change.

---

## Issue count summary

| Severity | Count |
|---------|-------|
| P0 | 3 |
| P1 | 8 |
| P2 | 10 |
| P3 | 7 |
| **Total** | **28** |
