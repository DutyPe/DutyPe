# 06 — Target Scalable System Design (5M-User Architecture)

> Reference target — informs the phased plan in `07_refactor_plan.md`. Not a "rewrite from scratch" — every step is a viable PR on the existing codebase.

---

## A. Module structure

Today: single `:app` module.

Target (multi-module, layered):

```
:app
  └── glue-only: Application, MainActivity, root NavHost, DI graph wiring

:core
  ├── :core:common         (Result, error types, dispatcher providers, Logger facade)
  ├── :core:design-system  (Compose theme, typography, color, base widgets)
  ├── :core:ui             (shared composables: ProfileBanner, etc.)
  ├── :core:navigation     (Routes, type-safe destinations, NavExtensions)
  └── :core:testing        (test utilities, fake DI modules)

:data
  ├── :data:firestore      (Firestore services, query helpers, ListenerRegistry)
  ├── :data:room           (DutyPeDatabase, DAOs, Entity ↔ Domain mappers)
  ├── :data:datastore      (preferences DataStore: role, locale, draft, profile-cache)
  ├── :data:cache          (in-memory caches, LRU)
  └── :data:repository     (offline-first repositories, MergeStrategy)

:domain
  ├── :domain:model        (immutable domain models — annotated @Immutable)
  └── :domain:usecase      (cross-screen workflows: ApplyToJobUseCase, SubmitProfileUseCase, …)

:feature
  ├── :feature:auth        (Login, Register, Verification)
  ├── :feature:onboarding  (Onboarding, RoleSelect, MandatoryProfileSetup)
  ├── :feature:worker      (WorkerHome, AllJobs, JobDetail, Applications, Profile)
  ├── :feature:employer    (EmployerHome, PostJob, EditJob, Applicants, Analytics)
  ├── :feature:notifications
  └── :feature:referrals

:work                       (WorkManager workers + scheduling)

:baselineprofile            (Macrobenchmark + profile generation)
```

### Dependency direction

```
:app ──► :feature:* ──► :domain ──► :data ──► :core
                            │                   │
                            └────► :core ◄──────┘
```

`:core` may not depend on anything app-specific. `:data` may not import `androidx.compose.*`.

---

## B. Per-screen architecture

```
Composable Screen          (UI)
   │ binds
   ▼
ScreenViewModel            (state holder)
   │ exposes: UiState (single immutable data class)
   │ consumes: UseCases
   ▼
UseCase                    (one workflow, suspend fun)
   │ orchestrates
   ▼
Repository                 (offline-first façade)
   │ merges
   ├── LocalDataSource     (Room / DataStore)
   └── RemoteDataSource    (FirestoreService)
```

### Mandatory rules

1. **No service injected directly into a Composable.** Composables receive `state: UiState` + lambdas.
2. **One `UiState` per screen.** No dangling `MutableStateFlow<X>`s for individual fields.
3. **All side effects originate in a UseCase.** ViewModels orchestrate, don't implement business rules.
4. **Repositories return `Flow<DomainModel>`.** Mapping is the repo's job.
5. **All DTO ↔ domain mapping in one place** (per repo).

---

## C. Navigation model

- **One root `NavHost`** in MainActivity.
- **Type-safe destinations** via `kotlinx.serialization`:
  ```kotlin
  @Serializable data object WorkerHome
  @Serializable data class JobDetail(val jobId: String)
  ```
- **Nested graphs** per feature, each owned by `:feature:*` module:
  ```kotlin
  fun NavGraphBuilder.workerGraph() { … }
  ```
- **Deep links inline** on each `composable<JobDetail>(deepLinks = listOf(navDeepLink { uriPattern = "dutype://job/{jobId}" }))`.
- **`StartDestinationResolver`** returns synchronously from cached state (DataStore). No more async start-destination compute.

---

## D. State management

| Area | Today | Target |
|------|-------|--------|
| Per-screen state | 4-8 separate `MutableStateFlow`s | 1 `data class UiState` |
| State preservation | ViewModel only | ViewModel + `SavedStateHandle` (filters, role, draft) |
| State annotations | None | `@Immutable` on every UI model; `@Stable` on event lambdas |
| Collection types | `List<X>` | `kotlinx.collections.immutable.ImmutableList<X>` for shared models |
| Compose Compiler metrics | Off | On in CI, results posted to PR |

---

## E. Data layer

```
┌──────────────┐
│ Repositories │ ── Offline-first, single source of truth = Room
└──────┬───────┘
       │ Flow<DomainModel>
       ├──────────────► LocalDataSource (Room/DataStore)
       └──────────────► RemoteDataSource (Firestore)
                              │
                              ▼
                       ListenerRegistry
                       (centralizes listener lifecycle)
```

### `ListenerRegistry`

A single class that owns all `addSnapshotListener` registrations, scoped per feature/screen, with explicit cleanup. Eliminates leaks.

### Caching strategy

| Data | Local | TTL |
|------|-------|-----|
| User profile | DataStore | until logout |
| Jobs feed | Room | 15 min |
| Applications | Room | 5 min (subject to push refresh) |
| Saved jobs | Room | 1 hour |
| Announcements | In-memory | App lifetime |
| Metadata stats | DataStore | 1 hour |

---

## F. Background work

- All workers in `:work` module.
- One `WorkScheduler` injectable that other modules call (no direct `WorkManager.getInstance(...)` outside `:work`).
- Each worker documents idempotency contract.
- `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` for user-visible work (e.g., job application retry on network return).

---

## G. Performance guardrails

| Guardrail | Mechanism |
|-----------|-----------|
| Cold start | Baseline Profile + Macrobenchmark in CI; fail PR if regress > 5% |
| Recomposition | Compose Compiler metrics in CI; fail PR if any new "unstable" UI model |
| Frame timing | Firebase Performance traces per screen entry |
| ANR | StrictMode in debug; ANR Watcher in release; alert on Crashlytics |
| Memory | LeakCanary in debug; Firebase Performance memory metric |
| Firestore reads | Per-screen read budget tracked via custom trace |

---

## H. Release pipeline

| Stage | Action |
|-------|--------|
| PR | Compose metrics, Lint, Detekt, unit tests |
| Merge to main | Macrobenchmark cold-start regression check |
| Release branch | Generate Baseline Profile + Startup Profile; bundle into AAB |
| Pre-prod | `adb shell pm verify-app-links` validation; smoke deep-link suite |
| Prod | Crashlytics + Firebase Performance dashboards; budget alerts |

---

## I. Observability

- Crashlytics user identification (already in place).
- Crashlytics breadcrumbs per route change.
- Firebase Performance Monitoring for cold-start, top 5 screens, Firestore queries.
- Custom Compose trace via `Trace.beginSection("WorkerHomeScreen.firstFrame")`.
- Play Vitals — ANR < 0.20%, Crash-free sessions > 99.5%.

---

## J. What stays the same

- **Hilt** for DI (mature, well-integrated).
- **Firebase** stack (Auth, Firestore, Functions, Crashlytics, Messaging).
- **Compose** + Material 3.
- **WorkManager** with HiltWorkerFactory.
- **Room** with SQLCipher.
- **Coil** for images.
- Modern Splash Screen API.
- R8 + resource shrinking config.

---

## K. What changes (summary)

1. `:app` monolith → multi-module.
2. Service-in-Composable → ViewModel-only.
3. String routes → type-safe Compose Navigation 2.8.
4. Async start destination → cached + synchronous.
5. Multiple flows per VM → one `UiState`.
6. Unannotated UI models → `@Immutable`.
7. No Baseline Profile → Baseline + Startup Profile in every release.
8. Implicit listener lifecycle → `ListenerRegistry`.
9. Destructive Room migration → real `Migration` objects.
10. Dead `WorkLocationManager` / `SmartNotificationManager` stub → deleted.
