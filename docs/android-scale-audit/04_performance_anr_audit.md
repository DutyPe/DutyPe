# 04 — Performance & ANR Audit

---

## A. Cold start

### Current sequence

| Step | Where | Sync/Async | Notes |
|------|-------|-----------|-------|
| `Firebase.initialize()` | [`DutyPeApplication.kt` L59](../../app/src/main/java/com/example/dutype/DutyPeApplication.kt#L59) | **Sync, main thread** | Required before App Check |
| `initializeAppCheck()` | L62 | Sync, main thread | Required before any Firestore call |
| `installSplashScreen()` | [`MainActivity.kt` L83](../../app/src/main/java/com/example/dutype/MainActivity.kt#L83) | Sync (must precede `super.onCreate`) | Modern API ✅ |
| 13+ setup blocks in `onCreate` | `MainActivity.kt` `onCreate` | Mixed | Includes 5 `@Inject` field reads |
| `keepSplashOnScreen` flag | L83-85 | Sync | Splash dismissed in `LaunchedEffect(Unit)` of first composable |
| Notification channel creation | `DutyPeApplication.kt` L69 | Async (Dispatchers.IO) | ✅ |
| `JobSyncWorker` schedule | L81 | Async (WorkManager queues) | ✅ |
| `PendingApplicationNotificationWorker` schedule | L84 | Async | ✅ |
| Start destination resolution | [`MainNavGraph.kt` L73-180](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt#L73) | **Async** — DataStore + Firestore role lookup + profile-completion async check | ⚠️ Blocks first content render |

### Issues

- **No Baseline Profile** → Compose + Hilt + Firebase classes JIT-compiled on first run.
- **No App Startup library** → init order is implicit, hard to test.
- **Start destination is async** → UI shows nothing until DataStore + (sometimes) Firestore returns.
- **MainActivity is heavy** → 5 `@Inject` + ~13 setup blocks; activity recreation cost.

### Recommendations

| Action | Priority |
|--------|----------|
| Add `:baselineprofile` Macrobenchmark module; cover cold-start to `WorkerHome` and `EmployerHome` | P1 |
| Adopt `androidx.startup.Initializer` for ordered, testable init | P2 |
| Cache `(authState, role, profileComplete)` to DataStore, resolve start destination synchronously | P1 |
| Move 5 `@Inject` services from MainActivity into screen ViewModels where used | P2 |
| Add cold-start trace via Firebase Performance Monitoring | P2 |

---

## B. Recomposition / Compose perf

### Findings

| Issue | Evidence | Impact |
|-------|----------|--------|
| `WorkerHomeScreen.kt` ≈ 2,000 LoC monolith | [worker/screens/WorkerHomeScreen.kt](../../app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreen.kt) | High recomposition cost; slow incremental builds |
| Zero `@Stable` / `@Immutable` annotations on UI models | grep `models/` returns no matches | All `data class` with `List`/`Map` inferred unstable → unnecessary recomposition |
| Multiple separate `MutableStateFlow`s per ViewModel rather than one `UiState` | [`AllJobsViewModel.kt` L183-193](../../app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt#L183) (4 flows), `MetadataManager.kt` (4 flows) | Recomposition triggered per-flow; no atomic state |
| Compose Compiler metrics not enabled | `build.gradle.kts` | No way to systematically find unstable types |
| Most `LazyColumn` use `key = ...` (good), `contentType` not verified | Across screens | Mixed-type lists can recycle wrongly |

### Recommendations

| Action | Priority |
|--------|----------|
| Decompose `WorkerHomeScreen` into 5+ child composables | P1 |
| Annotate every UI-bound data class with `@Immutable`; replace `List`/`Map` with `kotlinx.collections.immutable.*` where shared across screens | P1 |
| Enable Compose Compiler metrics in `build.gradle.kts`: `freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$buildDir/compose_metrics")` | P3 |
| Audit every `LazyColumn` for `key` + `contentType` | P2 |
| Combine multi-flow ViewModels into one `data class UiState` per screen | P2 |

---

## C. List & image performance

| Aspect | Status | Notes |
|--------|--------|-------|
| Coil for images | ✅ | Configured in DI |
| Memory cache size set | ❓ Verify in `AppModule` Coil block | Default is 25% of avail RAM — usually OK |
| Disk cache size set | ❓ Verify | — |
| `placeholder`/`error` always set | ❓ Audit | Avoid jank from layout shift |
| `LazyColumn` with `key` | ✅ Most cases | — |
| `LazyColumn` with `contentType` | ⚠️ Not verified | Add for mixed-type lists (e.g., banner + jobs in WorkerHome) |
| Pagination | ⚠️ Custom (PAGE_SIZE 10/30) — no `androidx.paging` | Acceptable today; consider Paging 3 at scale |

---

## D. ANR risk

### Strengths

| Item | Where |
|------|-------|
| `MainThreadChecker` + `ANRHandler` initialized | [`DutyPeApplication.kt` L77](../../app/src/main/java/com/example/dutype/DutyPeApplication.kt#L77) |
| Notification channel creation off main | L69 (Dispatchers.IO) |
| Background workers properly scheduled (network-constrained) | `worker/sync/JobSyncWorker.kt` L50-73 |

### Risks

| Risk | Evidence | Severity |
|------|----------|----------|
| `Firebase.initialize()` + `initializeAppCheck()` synchronous on main thread (required by Firebase) | `DutyPeApplication.kt` L59-62 | Acceptable (required); profile to ensure < 50 ms |
| Possible `.await()` on Firestore from Composable scopes | Audit needed across screens | Medium |
| `LocaleHelper.setLocale()` writes to `SharedPreferences` from `attachBaseContext` | `utils/LocaleHelper.kt` | Low (small write) |
| MainActivity `@Inject` of services: if any of the 5 services hits Disk/Network in init | `MainActivity.kt` L65-77 | Medium — Hilt eagerly constructs `@Singleton`s on first inject |
| `StrictMode` not enabled in debug | `DutyPeApplication.kt` | Low — would catch new violations early |

### Recommendations

| Action | Priority |
|--------|----------|
| Enable `StrictMode.ThreadPolicy` + `VmPolicy` in debug | P2 |
| Audit all `runBlocking` and `.await()` callsites; ensure none on main | P1 |
| Verify each `@Singleton` constructor is cheap (no I/O) | P1 |
| Add Firebase Performance trace per screen entry | P2 |

---

## E. Memory

| Aspect | Status |
|--------|--------|
| Firestore offline cache capped at 100 MB | ✅ ([`AppModule.kt` L88-98](../../app/src/main/java/com/example/dutype/di/AppModule.kt#L88)) |
| Room (SQLCipher) — bounded by query patterns | ✅ |
| Coil cache | ❓ Default unless configured |
| LeakCanary in debug | ❓ Not detected — recommended |
| Bitmap downscaling for uploads | ⚠️ Verify in `ImageUploadUtils` |

---

## F. Network

| Aspect | Status |
|--------|--------|
| Firestore offline-first | ✅ |
| Retry on transient failure (WorkManager) | ✅ for background; ⚠️ ad-hoc for UI calls |
| Connection state observed | ⚠️ Verify (some code in `utils/`) |
| Bulk operations / batched writes | ⚠️ Audit `JobFirestoreService`, `ApplicationManagementService` for N+1 patterns |

---

## G. Top wins ranked by ROI

| # | Action | Effort | Impact |
|---|--------|--------|--------|
| 1 | Publish `assetlinks.json` | 1 hr | High (App Link funnel) |
| 2 | Annotate `@Immutable` on UI models | 1 day | High (recomposition reduction) |
| 3 | Decompose `WorkerHomeScreen` | 3 days | High (jank, builds) |
| 4 | Add Baseline Profile module | 1 day | High (cold-start 20-30%) |
| 5 | Cache start destination synchronously | 1 day | High (perceived startup) |
| 6 | Audit `whereEqualTo` against indexes.json | 1 day | High (Firestore cost + tail latency) |
| 7 | Enable StrictMode in debug | 30 min | Medium (early detection) |
| 8 | Move services out of MainActivity | 1 day | Medium (recreation cost) |
