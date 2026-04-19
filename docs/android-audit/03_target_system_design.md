# 03 — Target System Design (5M-user ready)

> Target state. Use this as the north star when taking any refactor task. No file should exist that doesn't fit into one of these layers.

---

## 3.1 Architecture layers

```
┌──────────────────────────────────────────────────────────┐
│  Presentation (Compose)                                  │
│  - role-scoped NavGraphs                                 │
│  - one screen = one file, <300 lines                     │
│  - reads ViewModel `state: StateFlow<UiState>`           │
│  - sends events via `onEvent(UiEvent)`                   │
└────────────────────────────────▲─────────────────────────┘
                                 │
┌────────────────────────────────┴─────────────────────────┐
│  ViewModels (state holders)                              │
│  - `@HiltViewModel`, `SavedStateHandle` where relevant   │
│  - immutable `UiState`, single `StateFlow`               │
│  - no direct Firestore access                            │
└────────────────────────────────▲─────────────────────────┘
                                 │
┌────────────────────────────────┴─────────────────────────┐
│  Repositories (domain boundary)                          │
│  - one repository per feature (job, application,         │
│    user, referral, notification, config)                 │
│  - returns domain models, not Firestore snapshots        │
│  - owns cache coordination (memory + Room)               │
└────────────────┬──────────────────────┬─────────────────┘
                 ▼                      ▼
┌─────────────────────────┐  ┌───────────────────────────┐
│  Remote data sources    │  │  Local data sources       │
│  - FirestoreService     │  │  - Room DAOs              │
│  - CFCallableService    │  │  - DataStore              │
│  - StorageService       │  │  - In-memory LRU          │
│  - MessagingRegistrar   │  │                           │
└─────────────────────────┘  └───────────────────────────┘
```

**No layer skipping.** A Composable that calls `FirebaseFirestore.getInstance()` is a bug.

---

## 3.2 Module / package boundaries (target)

Single Gradle module now; consider feature modules later. Package-first enforcement via a custom lint rule or static check.

```
com.dutype.app/
  core/
    di/                    – Hilt modules, split by feature
    firebase/              – FirebaseAuth/Firestore/Storage/Functions providers
    network/               – retrofit/okhttp for AI backend
    logging/               – Timber setup, Crashlytics
  feature/
    auth/
      data/                – AuthRepository
      ui/                  – LoginScreen, OtpScreen
      viewmodel/
    jobs/
      data/                – JobRepository (remote + local + memory)
      model/               – JobCard, JobDetail, JobEntity
      ui/
      viewmodel/
    applications/
    referrals/
    notifications/
    profile/
    employer/
    admin/                 – only if admin-only screens ship in consumer app
  shared/
    ui/                    – design system, theme, common composables
    models/                – shared domain types
    util/
```

Delete: `managers/`, `services/`, `state/`, `metadata/`, `cache/`, `firestore/`, `data/` as top-level packages. Their classes either **move into** the feature they belong to, or **delete** if duplicative.

---

## 3.3 Navigation ownership

- Single `NavHost` in `MainActivity`. Nested `NavHost` per role graph (Worker/Employer).
- **Start-destination resolver** is a pure function in `core/startup/StartDestinationResolver.kt`:
  ```
  suspend fun resolve(auth: AuthState, profile: ProfileState): Route
  ```
  Run once in `MainActivity.onCreate` before `setContent`. Rendered UI never re-evaluates it.
- Deep links: one `DeepLinkRouter` that takes `Uri` and returns a `Route`. `MainActivity` calls it once on `onNewIntent` / `onCreate`.
- `RoleSwitchManager` stays (well-scoped), but becomes a **cache invalidator** only — not a state owner.

---

## 3.4 State ownership

- Every screen has exactly one `ViewModel` with one `UiState`.
- `UiState` is `data class UiState(...)`, immutable, produced by `stateIn(scope, WhileSubscribed(5_000), default)`.
- No global `*StateManager` singletons. Session data (`activeRole`, `userId`) comes from a single `SessionStore` (backed by DataStore + in-memory) injected into ViewModels that need it.
- Kill `AppStateManager`, `ApplicationStateManager`, `ProfileSetupStateManager`.

---

## 3.5 Background work strategy

- **Foreground critical path has zero Firestore I/O** beyond the active screen's `ViewModel`.
- **WorkManager** only for: (a) network-resilient queued mutations (submit application offline), (b) periodic cleanup (delete expired drafts). **Not** for cron jobs — those run on Cloud Functions.
- Workers inject dependencies via `@HiltWorker`; never call `FirebaseFirestore.getInstance()`.

---

## 3.6 Notification / deep-link ownership

- **Single `NotificationRepository`** owns: posting local, marking read, subscribing to the recipient's latest N notifications via **paginated queries**, not snapshot listeners on a collection.
- Push payload is self-contained (`title`, `body`, `type`, `deepLink`, `entityId`, `channelId`). `DutyPeMessagingService` does **no Firestore reads**. Fix I-017.
- `DeepLinkRouter` is the single entry point from FCM intent extras, browser intents, and in-app click.

---

## 3.7 Cache strategy

```
  read ──► in-memory LRU (sec) ──► Room (offline) ──► Firestore (server)
  write ─► Firestore directly (with offline persistence) ──► Room mirror on success
```

- In-memory LRU: short-lived (30s–5min), per-query result caching in `Repository`.
- Room: long-lived offline mirror for `jobs`, `saved_jobs`, `applications`.
- Firestore SDK offline persistence (100 MB cap, already set) is the final cache layer — **trust it, don't build a parallel one**.
- Never store derived/aggregate fields locally. When in doubt: re-fetch.

---

## 3.8 Target Firestore architecture

Canonical collections (no more, no less):

| Collection | Doc ID | Purpose | Client writes | CF writes |
|---|---|---|---|---|
| `users/{uid}` | auth uid | identity | limited allowlist | full |
| `worker_profiles/{uid}` | auth uid | worker fields | limited allowlist | aggregates only |
| `employer_profiles/{uid}` | auth uid | employer fields | limited allowlist | aggregates only |
| `jobmetadata/{jobId}` | random | job card | employer-owned | mirror on aggregate |
| `job_details/{jobId}` | matches jobId | job detail | employer-owned | — |
| `applications/{jobId}_{workerId}` | deterministic | apply record | worker create, employer status update | — |
| `saved_jobs/{userId}_{jobId}` | deterministic | bookmark | owner only | — |
| `ratings/{jobId}_{fromUserId}` | deterministic | review | gated by HIRED | — |
| `job_reports/{reporterId}_{jobId}` | deterministic | report | reporter only | — |
| `referrals/{referrerId}_{referredUserId}` | deterministic | referral pair | gated creation | status & rewards |
| `referral_stats/{uid}` | auth uid | aggregates | deny | full |
| `referral_codes/{code}` | code | reverse lookup | deny | full |
| `users/{uid}/withdrawals/{wId}` | random | payout log | deny | full |
| `notifications/{nId}` | random | inbox | `isRead` only | full |
| `app_config/{doc}` | static | admin-editable config | deny | admin-callable |

Remove from the universe: `announcements` (unless product demands it), any `*_v2` shadow collection if present, anything that looks like a duplicate projection of `users`.

### Summary vs detail

- **Every list query reads the lean summary doc.** `jobmetadata`, `worker_profiles`, `employer_profiles`, `notifications` (paginated).
- **Detail doc is fetched only on navigation to the detail screen.** `job_details`, full user doc for chat (if shipped).

### Denormalization (limited, CF-maintained)

- `jobmetadata.companyName` — denormed from `employer_profiles` on create, updated by a CF onUpdate trigger for the employer doc. Reduces a join on every feed query.
- `worker_profiles.fullName` / `profileImageUrl` — denormed from `users` for worker search results. CF maintains.

Never client-maintained.

---

## 3.9 Backend (CF) contract from Android's view

Callable endpoints (region `asia-south1`):

- `applyReferralCode({ code })` → `{ success, bonus }`
- `requestWithdrawal({ amount, upiId })` → `{ withdrawalId, status }`
- `updateReferralConfig(patch)` → admin-only
- `getReferralConfigCallable()` → public (fallback for rules-blocked reads)
- `reportJob({ jobId, reportType, description })` → `{ reportId }`
- `verifyEmployer({ uid, verified })` → admin-only
- `submitJobForReview({ draft })` → AI/fraud prep, returns `{ jobId, status }` (replaces client-side posting on hot paths later)

Android calls these via an injected `CloudFunctionsClient` (thin wrapper over `FirebaseFunctions`). Never `FirebaseFunctions.getInstance()`.

---

## 3.10 Scalability posture

- **Read cost budget: <50 Firestore reads per session per typical user.** Track with Crashlytics custom metric.
- **Write cost budget: <10 writes per session** (apply + mark-read + presence).
- **Index count cap: 30 composite indexes.** Prune quarterly.
- **Snapshot listeners simultaneously open per session: ≤3.** (app_config, own user, active detail).
- **Payload size budget: `jobmetadata` doc <1 KB, `users` doc <2 KB.**
- **No hot counters.** If a feature needs a count, use server-side `COUNT()` or sharded counters.
- **FCM data-only messages** for transient updates (notification delivered, withdrawal state change) — avoid wake-ups that open Firestore listeners.
