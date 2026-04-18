# 05 — Firebase & Data Audit

---

## A. Firestore collections

Source: [`firestore/FirestoreCollections.kt`](../../app/src/main/java/com/example/dutype/firestore/FirestoreCollections.kt)

| Collection | Estimated cardinality at 5M users | Hot read paths |
|------------|-----------------------------------|----------------|
| `users` | 5M docs | login, role check |
| `worker_profiles` | ~3-4M docs | worker home, applications |
| `employer_profiles` | ~1M docs | employer home, post job |
| `jobmetadata` (JOBS) | ~10K-100K active | worker home feed |
| `job_details` | == jobmetadata | job detail screen |
| `applications` | ~10M docs | applications list (per worker), applicants list (per job) |
| `saved_jobs` | ~5M docs | worker saved tab |
| `ratings` | ~1M docs | profile, job detail |
| `job_reports` | < 100K | moderation |
| `notifications` | ~50M docs | notifications screen |
| `referrals` | ~1M docs | referral tracking |
| `referral_codes` | ~5M docs | referral entry |

**13 top-level collections** — within Firestore best-practice (< 100 top-level collections recommended for clarity).

## B. Active snapshot listeners

5 confirmed `addSnapshotListener` call sites:

| # | File:Line | Pattern | Lifecycle |
|---|-----------|---------|-----------|
| 1 | [`AppConfigRepository.kt:47`](../../app/src/main/java/com/example/dutype/repositories/AppConfigRepository.kt#L47) | `callbackFlow` → `StateFlow` | App-scoped (Singleton) |
| 2 | [`AnnouncementService.kt:72`](../../app/src/main/java/com/example/dutype/services/AnnouncementService.kt#L72) | Live announcements | App-scoped |
| 3 | [`JobFirestoreService.kt:699`](../../app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt#L699) | Live job updates | Screen-scoped (must verify removal on dispose) |
| 4 | [`ReferralService.kt:224`](../../app/src/main/java/com/example/dutype/services/ReferralService.kt#L224) | Referral tracking | Screen-scoped |
| 5 | [`ReferralService.kt:724`](../../app/src/main/java/com/example/dutype/services/ReferralService.kt#L724) | Referral tracking | Screen-scoped |

**Risk:** If any of #3-5 is attached without explicit `ListenerRegistration.remove()` on screen leave, it leaks per-session at scale.

**Action:** Audit each non-callbackFlow listener for explicit removal in `DisposableEffect` / `viewModelScope.onCleared`.

## C. Query inventory

- **30 `whereEqualTo` callsites** detected by grep.
- **Sample fields queried:**
  - `workerId` — covered by some composite indexes
  - `userId` — covered for `saved_jobs`
  - `employerId` — covered for `applications`
  - `status`, `salaryType`, `jobType` — single-field queries (Firestore auto-indexes single fields)
  - `recipientId` + `isRead` — covered by `notifications` index

### Indexed (per [`firestore.indexes.json`](../../firestore.indexes.json))

| Collection | Index |
|-----------|-------|
| `jobmetadata` | `geohash` + `jobType` + `status` |
| `applications` | `workerId` + `createdAt` |
| `applications` | `jobId` + `createdAt` |
| `applications` | `employerId` + `createdAt` |
| `saved_jobs` | `userId` + `createdAt` |
| `notifications` | `recipientId` + (`isRead`) + `createdAt` |
| `referrals` | `referrerId` + `createdAt` |
| `ratings` | `jobId` + `fromUserId` + `toUserId` |

### Likely missing / under-tested combos

| Query (suspected) | Why risky |
|-------------------|-----------|
| `applications` where `status` + `workerId` + `orderBy(createdAt)` | Triple combo not in index file |
| `jobmetadata` where `status` + `salaryType` + `orderBy(createdAt)` | Sort + filter combo |
| `notifications` where `recipientId` + `type` + `orderBy(createdAt)` | If filtering by notification type |

**Action:**
1. Run an audit script that walks every `.whereEqualTo`, `.whereIn`, `.orderBy` chain in `services/` and verifies index existence.
2. Add the missing composite indexes to `firestore.indexes.json` proactively.
3. Add a CI check that fails when a new query combo is added without an accompanying index entry.

## D. Read-cost hot spots

| Screen | Reads on entry | Optimization |
|--------|---------------|--------------|
| `WorkerHomeScreen` | Job feed (paginated 30) + saved-job IDs + worker profile + announcements + recommendations | Cache aggressively; offline-first via Room is already partial |
| `EmployerHomeScreen` | Posted jobs + per-job applicant counts | Aggregate counts via Cloud Function + denormalization (don't `count()` client-side) |
| `ApplicationsListScreen` (worker) | All applications for worker, paginated | Use composite `(workerId, createdAt)` index ✅ |
| `NotificationsScreen` | All recent notifications | Paginate with `(recipientId, createdAt)` ✅ |

## E. Offline persistence

| Setting | Value | Source |
|---------|-------|--------|
| `setPersistenceEnabled` | `true` | [`AppModule.kt` L88-98](../../app/src/main/java/com/example/dutype/di/AppModule.kt#L88) |
| `setCacheSizeBytes` | 100 MB (P1 fix already applied; was UNLIMITED) | Same |
| Source-of-truth strategy | Mixed: jobs are offline-first via Room; others are Firestore-first with Firestore cache | `OfflineFirstJobRepository.kt` |

## F. Room database

| Aspect | Value |
|--------|-------|
| Database | `DutyPeDatabase` |
| Encryption | SQLCipher |
| Version | 6 (current) |
| Migration | `fallbackToDestructiveMigration()` for v1-6 → drops cache |
| Entities | Job, Application, SavedJob (verify actual list) |
| DAO providers | [`AppModule.kt` L112-127](../../app/src/main/java/com/example/dutype/di/AppModule.kt#L112) |

**Risk:** `fallbackToDestructiveMigration` drops user-cached jobs/applications on schema change. Acceptable today; **must replace with proper `Migration` objects from v6 → v7 forward**.

## G. Cloud Functions interaction

- `FirebaseFunctions` provided in `AppModule.kt`.
- `SmartNotificationManager.notifyNearbyWorkersAboutNewJob` was migrated to Cloud Functions (per code comment in `DutyPeApplication.kt` L78).
- `functions/` directory exists at workspace root — out of scope for this audit but consumed by client.

**Recommendation:** Document which client services trigger Cloud Functions (callable vs HTTP) so on-call engineers can correlate client errors with CF logs.

## H. Security rules

- File: [`firestore.rules`](../../firestore.rules) — out of detailed scope here, but key rule audit areas:
  - Users can only read their own `users/{uid}` doc.
  - `applications` write requires either applicant or employer of job.
  - `jobmetadata` writes require employer ownership.
  - **Verify `notifications` collection cannot be read by other users.**
  - **Verify `referral_codes` cannot be enumerated.**

**Action:** Cross-link `firestore.rules` with each `whereEqualTo` query — every client query must be permitted by rules + index.

## I. Recommendations summary

| # | Action | Severity |
|---|--------|----------|
| 1 | Audit every query against `firestore.indexes.json`; add missing composite indexes | P1 |
| 2 | Verify all snapshot listeners have explicit `remove()` on screen dispose | P1 |
| 3 | Replace `fallbackToDestructiveMigration` with proper Room migrations | P2 |
| 4 | Generalize offline-first pattern from jobs to applications + saved_jobs | P2 |
| 5 | Document Cloud Functions ↔ client service mapping in repo README | P3 |
| 6 | Add a CI step that diffs query patterns against indexes.json | P2 |
| 7 | Move per-job applicant count to a denormalized field updated by Cloud Function (avoid client-side `count()`) | P2 |
| 8 | Add Firestore cost dashboard (Cloud Console + budget alerts) | P1 (ops) |
