# DutyPe — Interview Prep Cheat Sheet

> Last updated: April 28, 2026
> A one-pager you can rehearse before any Android / mobile interview.

## 1. One-line pitch

DutyPe is a job-discovery + hiring app for blue-collar Indian workers and small employers — workers find nearby jobs from their phone, employers post jobs and receive applications without needing a recruiter. Built natively in Kotlin + Jetpack Compose with a Firebase backend.

## 2. Stack at a glance

| Layer | Technology |
| --- | --- |
| Language | Kotlin (JDK 11 target) |
| UI | Jetpack Compose + Material 3, Compose Compiler 1.5.14 |
| DI | Hilt (Dagger 2.52) |
| Async | Kotlin Coroutines + Flow |
| Local storage | Room (encrypted via SQLCipher 4.14, key wrapped by Android Keystore) + DataStore Preferences |
| Backend | Firebase Auth, Firestore, Cloud Storage, Cloud Functions, Crashlytics, Performance, Messaging (FCM), App Check (Play Integrity) |
| Maps | Google Maps SDK + Maps Compose + Places SDK |
| Background work | WorkManager + Hilt-Work |
| Image loading | Coil (with GIF decoder) |
| Build / shipping | AGP 8.7.3, R8 full mode, Baseline Profiles, Macrobenchmark module, App Bundle (AAB) |

## 3. Architecture — MVVM + clean layering

DutyPe follows **MVVM** (Model–View–ViewModel) with a unidirectional data flow on top of an opinionated layered package structure. UI never talks to Firebase or Room directly; it always goes through a ViewModel which delegates to a Service / Repository.

```
┌─────────────────────────────────────────────────────────────────┐
│  UI layer   (Jetpack Compose)                                   │
│  • Screens (worker/screens, employer/screens, common/screens)   │
│  • Reusable components (components/, *.JobCard, BottomBars)     │
│  • Theme (ui/theme/Theme.kt, Color.kt — token-based dark mode)  │
│  • Navigation (navigation/ — Nav-Compose graphs)                │
│        ▲ state                  │ events                        │
└────────┼────────────────────────┼────────────────────────────────┘
         │                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  ViewModel layer  (viewmodels/)                                 │
│  • Hilt @HiltViewModel, exposes StateFlow<UiState>              │
│  • Holds NO Android framework refs, pure Kotlin + Coroutines    │
│        ▲                        │                                │
└────────┼────────────────────────┼────────────────────────────────┘
         │                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  Domain / Service layer  (services/, engine/, auth/)            │
│  • JobApplicationService, ProfileCompletionService,             │
│    ReferralService, BirthdayService, AuthManager …              │
│  • Pure business rules, returns Result<T> / Flow<T>             │
│        ▲                        │                                │
└────────┼────────────────────────┼────────────────────────────────┘
         │                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  Data layer  (repositories/, data/, database/, firestore/,      │
│               cache/, location/)                                │
│  • Firestore wrappers (firestore/FirestoreCollections.kt)       │
│  • Room DAOs + encrypted DB                                     │
│  • DataStore (theme, drafts, preferences)                       │
│  • In-memory caches (cache/JobCacheManager.kt)                  │
│  • LocationService, NotificationStore                           │
└─────────────────────────────────────────────────────────────────┘

Cross-cutting:
  • di/        — Hilt modules (AppModule, ServiceModule, etc.)
  • workers/   — WorkManager jobs (PendingApplicationNotificationWorker,
                 GuestEngagementWorker, JobPostingWorker)
  • models/    — Plain Kotlin data classes shared across layers
  • core/, utils/ — extensions, formatters, error mapping
```

### Why this shape

- **Separation of concerns** — each layer has one job, easy to test and to swap (e.g., Firestore → REST later).
- **Testability** — ViewModels take services via constructor injection (Hilt), so unit tests use fakes.
- **Compose-friendly** — UI subscribes to `StateFlow`s with `collectAsStateWithLifecycle()`; recompositions are cheap because state holders are stable.
- **Offline-first feel** — Room (encrypted) for durable data, in-memory cache for hot paths (job lists), Firestore as source of truth.

### Conventions worth memorising

- All ViewModels live under `viewmodels/` and end with `ViewModel`.
- All long-running business logic lives in `services/` or `engine/`, never in a ViewModel.
- Firestore collection names live in **one** file: `firestore/FirestoreCollections.kt`.
- Theme tokens (`WorkerColors`, `EmployerColors`) are `@Composable @ReadOnlyComposable` getters that return different `Color`s based on `LocalDarkMode`. Onboarding screens are wrapped in `ForceLightTheme {}` so the brand intro stays light regardless of system setting.
- Hilt `@EntryPoint` is used to read DataStore from non-composable code paths.
- Background sync uses WorkManager + Hilt-Work; no AlarmManager.

## 4. Key product flows (1 line each)

- **Worker**: phone OTP → role pick → mandatory profile setup → home (location-based job feed) → job description → apply.
- **Employer**: phone OTP → role pick → mandatory profile setup → post job (3-step wizard, address management, image upload) → my jobs → applicants list.
- **Refer & earn**: shareable code → referee installs + signs up → ledger entry → tier-based reward unlocked.

## 5. Performance + size practices

- R8 full mode, resource shrinking, baseline-profile bundled via the `:baselineprofile` module + `androidx.profileinstaller`.
- **R8 mapping reuse** — `app/mapping/release-mapping.txt` is committed via Git LFS and fed back to R8 (`-applymapping`) on every release so class/method names stay stable across versions and Play update patches stay small.
- Compose Compiler metrics + reports available via `-Pcom.dutype.enableComposeMetrics=true`.
- 16 KB-page-size compatible: SQLCipher 4.14, `useLegacyPackaging = false` for jniLibs (Android 15 Play requirement).
- Image pipeline uses Coil with WebP-native and a custom `OptimizedJobImage` wrapper.

## 6. Security + privacy

- Phone-number auth via Firebase Auth, plus Google Sign-In via Credential Manager.
- App Check with Play Integrity provider in release; debug provider in debug builds.
- Room database encrypted with SQLCipher; passphrase wrapped by Android Keystore using `EncryptedSharedPreferences`.
- Firestore Security Rules enforced server-side (`firestore.rules`); client-side checks are UX only.
- No secrets in the repo: `local.properties`, `keystore.properties`, and `google-services.json` are git-ignored / per-developer.

## 7. Things to mention in interviews

- "I built it solo, end-to-end — schema, services, Compose UI, CI, Play release process."
- "I treat the data layer as the source of truth — UI is dumb, ViewModel is a thin state-holder."
- "I shipped real production fixes: Play update-size churn (R8 mapping reuse), 16 KB page size readiness, encrypted Room DB."
- "I deliberately keep dependencies minimal — recently audited and removed five unused libraries to keep AAB lean."

## 8. Gotchas I've actually hit

- Composable getters can't be called inside `LaunchedEffect` lambdas — read them in composable scope first, capture into the lambda.
- `PowerShell -replace` is case-insensitive — use `.Replace()` via `[System.IO.File]::ReadAllText` for safe bulk edits.
- KSP can leave stale Hilt-generated Java files that break `:app:projectHealth` — clean ksp output before running dependency-analysis tasks.
- R8 mapping reuse takes effect from the **second** release after enabling — the first release after enabling still ships large because the previously published binary used different obfuscated names.
