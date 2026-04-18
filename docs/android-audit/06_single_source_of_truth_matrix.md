# 06 — Single Source of Truth Matrix

> Every domain value has exactly one owner. Everything else is a cached projection.

| Domain value | Current owners (bad) | Problem | New canonical owner | Consumers (read-only) |
|---|---|---|---|---|
| User identity (`phone`, `fullName`, `profileImageUrl`) | `users/{uid}`, optionally denormed in `/worker_profiles` + `/employer_profiles` + cached in `EmployerProfileCache` + in multiple ViewModels | Four+ mirrors, drift guaranteed | `/users/{uid}` | worker/employer profile CF mirrors `fullName`+`profileImageUrl`; every VM reads via `UserRepository` |
| Active role (`activeRole`) | `/users/{uid}.activeRole`, `RoleCacheManager`, `RoleSwitchManager`, `AppStateManager` | Role-switch drift bugs | `/users/{uid}.activeRole` + `SessionStore` (DataStore) as ephemeral cache | Navigation, ViewModels. `RoleSwitchManager` becomes invalidator, not owner. |
| Referral stats (19 fields) | `/users/{uid}.referralStats` AND `/referral_stats/{uid}` | Dual-write, drift on retry | `/referral_stats/{uid}` only | Refer & Earn screen via `ReferralRepository` |
| Referral config (rewards, min withdrawal) | Hardcoded in app strings historically, now in `/app_config/referral` | Previously 3+ files hardcoded `₹25` | `/app_config/referral` | Android `AppConfigRepository`, CF `getReferralConfig()`, web admin |
| Job summary (card data) | `/jobmetadata/{jobId}` + legacy detail fields leaked in | `jobmetadata` bloated with detail fields | `/jobmetadata/{jobId}` (lean, ≤14 fields) | Feed, search, nearby, map |
| Job detail | `/job_details/{jobId}` + duplicated in `jobmetadata` + `JobEntity` Room | Triple-stored | `/job_details/{jobId}` | Detail screen only. Room mirrors detail after user navigates. |
| Application status | `/applications/{jobId}_{workerId}.status`, mirrored into `ApplicationStateManager`, partly mirrored into `ApplicationEntity` Room | Three owners, drift on offline writes | `/applications/{jobId}_{workerId}` | Worker apps list, employer applicants list. Room is sync mirror only. |
| Saved jobs | `/saved_jobs/{userId}_{jobId}` + `SavedJobEntity` Room | Two owners | `/saved_jobs/{userId}_{jobId}` | Room is offline mirror. |
| Ratings | `/ratings/{jobId}_{fromUserId}` + `worker_profiles.rating` (aggregate) + `employer_profiles.rating` (aggregate) | Aggregates are CF-maintained from `/ratings` — fine, but must be **CF-only writes** | `/ratings/*` canonical; aggregates CF-derived | Profile screens (aggregates), review screens (rows) |
| Application count per job | `/job_details.applicationCount` + counted from `/applications` at CF | Denormalized hot counter | Removed. Server-side `COUNT()` on demand + 60s cache on employer landing | Employer applicants screen |
| Trust score / verification | Field on `employer_profiles.isVerified` writable by clients (bug) | Client forgery | CF-only claim via admin callable; field CF-only | Worker job cards |
| FCM token | `/users/{uid}.fcmToken` | OK | `/users/{uid}.fcmToken` | Messaging fan-out CF |
| User location | `/users/{uid}.location` + `/users/{uid}.geohash` + `LocationPreferences` DataStore | Three mirrors | `/users/{uid}` for server-visible truth; `LocationPreferences` for ephemeral UI prefill | Job feed query, map |
| Profile-setup progress | `ProfileSetupStateManager` singleton + `ProfileCompletionViewModel` state + Firestore `/users/{uid}` fields | Three owners, drift when app killed mid-setup | `ProfileCompletionViewModel` + Firestore persist-on-step-complete. Kill the singleton. | Setup screen only |
| Application in progress (draft) | `ApplicationStateManager` singleton + `ApplicationFormDataStore` + VM state | Three owners | `ApplicationFormDataStore` + VM consumes it | Job apply screen |
| Job posting draft | `JobDraftDataStore` + VM state + possibly global state | Two owners (acceptable) | `JobDraftDataStore` | Post job VM |
| Notification unread count | None denormalized (derived from query). Current implementation: query-and-count in `NotificationService`. | OK as long as query is paginated. | Derived via `count()` aggregation query, cached per session | Badge count in bottom nav |
| Announcements (if kept) | `/announcements/*` + live listener in `AnnouncementService` | Pure cost if unused | Pull-based: one-shot read on cold start, cached 10min. Kill the listener. | Announcement screen |
| Session / auth state | `FirebaseAuth` (source of truth for token) + `AuthManager` + scattered `FirebaseAuth.getInstance()` | Multiple readers | `FirebaseAuth` (injected) + thin `AuthRepository` with `authState: Flow<AuthState>` | Every VM that needs uid |
| Withdrawals | `/users/{uid}/withdrawals/{wId}` | OK, single owner, CF-only | `/users/{uid}/withdrawals/{wId}` | Refer & Earn history |
| Referral code reverse lookup | `/referral_codes/{code}` | OK | `/referral_codes/{code}` | Apply-code flow |

---

## 6.1 Ownership rules of thumb

1. **Write once, mirror via CF only.** Client never writes denormalized fields.
2. **Room + DataStore are caches, not sources of truth.** They may lag, never lead.
3. **Global singletons are a smell.** If a class is `@Singleton` and holds mutable state, justify it in a code comment referencing this matrix — otherwise delete.
4. **Aggregates are CF-only.** Rules enforce. No client can write a count, a rating, a total.
5. **Config is in `/app_config/*`.** Not in user docs, not in compiled strings.
