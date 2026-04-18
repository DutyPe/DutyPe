# 02 — Firebase Collection + Field Minimization Report

> Rule: a field only exists if a screen renders it OR a query filters/orders on it OR a CF needs it to run. Otherwise delete.

Legend: **KEEP** = required · **DERIVE** = compute at read time (client or CF) · **REMOVE** = dead/dup · **MOVE** = relocate to another collection · **CF-ONLY** = restrict writes to Cloud Functions via rules.

---

## 2.1 `users/{uid}` — identity document

**Purpose:** single authoritative identity per user. Canonical owner of `phone`, `fullName`, `roles`, session tokens.

| Field | Decision | Reason |
|---|---|---|
| `userId` | KEEP | doc ID |
| `phone` | KEEP, immutable | verified on signup |
| `fullName` | KEEP | displayed everywhere |
| `profileImageUrl` | KEEP | cards, chats |
| `roles: [WORKER\|EMPLOYER]` | KEEP | role-gating |
| `activeRole` | KEEP | UI routing |
| `location: {lat, lng}` | KEEP | hyperlocal |
| `geohash` | KEEP | indexable query key |
| `fcmToken` | KEEP | push delivery |
| `createdAt` | KEEP, immutable | analytics, lifecycle |
| `lastActiveAt` | KEEP, CF-only-or-debounced | activity signal |
| `referralCode` | KEEP | exposed to share |
| `referredByCode` | KEEP, immutable-after-set | joins referral attribution |
| `referredByUserId` | KEEP, CF-only | referral attribution target |
| `isVerified` | **CF-ONLY** | remove from client allowlist |
| `isActive` | **CF-ONLY** | remove from client allowlist |
| `referralStats.*` (19 sub-fields) | **REMOVE** | moved to `/referral_stats/{uid}` — see I-002 |
| anything else seen in prod | **REMOVE** | if not listed above, it is not canonical |

**List payload:** never needed — users are never listed publicly.

**Write hotspots to watch:** `fcmToken` and `lastActiveAt`. Debounce `lastActiveAt` to once per hour on the client. Every write here triggers every composite index that touches `users`.

**Rules change required:**
- Drop `isVerified`, `isActive`, `rating`, `totalRatings`, `totalJobs`, `totalHires`, all `referralStats.*` from the client write allowlist.

---

## 2.2 `worker_profiles/{uid}` — worker extension

**Purpose:** worker-specific profile data, one-to-one with `users`.

| Field | Decision | Reason |
|---|---|---|
| `userId` | KEEP | doc ID |
| `jobTypes` | KEEP, client-writable, max 20 | filter key |
| `skills` | KEEP, client-writable, max 30 | filter key |
| `isAvailable` | KEEP, client-writable | filter key |
| `lastActiveAt` | KEEP, mirror of `users.lastActiveAt` | only if workers are listed by activity — otherwise DELETE |
| `rating` | **CF-ONLY** | aggregate of `/ratings` |
| `totalRatings` | **CF-ONLY** | aggregate |
| `totalJobs` | **CF-ONLY** | aggregate of `/applications?status=hired` |

**List payload (worker feed, search):** `{userId, fullName, profileImageUrl, jobTypes, rating, totalRatings, location, geohash}` — join with `users` at query time via batched reads of 10 IDs at once, or denormalize `fullName`+`profileImageUrl` as CF-maintained fields on `worker_profiles`.

**Recommendation:** denormalize `fullName` + `profileImageUrl` here (CF-maintained from `/users/{uid}` onUpdate) to avoid N+1 join on worker search.

---

## 2.3 `employer_profiles/{uid}` — employer extension

| Field | Decision | Reason |
|---|---|---|
| `userId` | KEEP | doc ID |
| `companyName` | KEEP | every job card |
| `gstNumber` | KEEP, optional | verification |
| `lastActiveAt` | KEEP or DELETE | same logic as workers |
| `isVerified` | **CF-ONLY** | trust signal — see I-005 |
| `rating`, `totalRatings`, `totalHires` | **CF-ONLY** | aggregates |

Same denormalization recommendation as workers: mirror `fullName`/`profileImageUrl` onto `employer_profiles` to kill the join on job cards.

---

## 2.4 `jobmetadata/{jobId}` — job LIST card (keep lean!)

**Purpose:** hot path. Every home feed query hits this. Must stay small.

| Field | Decision | Reason |
|---|---|---|
| `jobId` | KEEP | doc ID |
| `employerId` | KEEP | owner |
| `companyName` | KEEP, denormalized | card renders it |
| `title` | KEEP | card |
| `jobType` | KEEP | filter |
| `salary` | KEEP | card |
| `salaryType` | KEEP | card |
| `location {lat,lng}` | KEEP | distance |
| `geohash` | KEEP | query key |
| `addressText` | KEEP, short | card secondary line |
| `urgency` | KEEP | badge |
| `status` | KEEP | filter |
| `createdAt` | KEEP | order key |
| `expiresAt` | KEEP | sweep key |
| `idempotencyKey` | KEEP, CF-only | dedup |
| `verified` / `isVerified` employer mirror | KEEP if needed on card, CF-only | otherwise join to `employer_profiles` |
| `applicationCount` | **REMOVE** or **CF-ONLY** | denormalized counter — see I-012; prefer server-side COUNT() aggregation |
| anything not on the card | **MOVE** to `/job_details/{jobId}` |

**Do not add** `description`, `benefits`, `workingHours`, `educationRequired`, `experienceRequired`, `shiftTiming`, `whatsappNumber`, `contactNumber`, `vacancies`. They belong in `job_details`.

**Read budget:** target a 1 KB doc. Current is likely 2–3 KB because of legacy writes that still include detail fields — write a backfill that strips them.

---

## 2.5 `job_details/{jobId}` — job DETAIL payload (fetched on click)

| Field | Decision | Reason |
|---|---|---|
| `description` | KEEP | detail screen |
| `contactNumber` | KEEP | call CTA |
| `whatsappNumber` | KEEP, optional | whatsapp CTA |
| `gender` | KEEP, optional | requirement |
| `experienceRequired` | KEEP, optional | requirement |
| `shiftTiming` | KEEP, optional | detail |
| `workingHours` | KEEP, optional | detail |
| `educationRequired` | KEEP, optional | requirement |
| `vacancies` | KEEP | counter |
| `benefits[]` | KEEP, max 20 | detail list |
| `applicationCount` | **REMOVE** | see I-012 |

**Read pattern:** only on `JobDetailScreen` open, never on list feed.

---

## 2.6 `applications/{jobId}_{workerId}` — apply record

**Purpose:** single source of truth for "who applied to what".

| Field | Decision | Reason |
|---|---|---|
| `jobId` | KEEP | query filter |
| `workerId` | KEEP | query filter |
| `employerId` | KEEP | query filter (employer views own job's applicants) |
| `status` | KEEP | query filter |
| `createdAt` | KEEP | order |
| `updatedAt` | KEEP if status transitions tracked | CF-only |

Runtime-only enrichment (never stored): `jobTitle`, `jobLocation`, `companyName`, `workerName`, `workerPhone`, `workerProfileImageUrl`, `coverLetter`, `statusHistory`. These come from joins at read time — already correctly implemented in `JobApplicationModels.kt.toFirestoreMap()` which enforces the 6-field write.

**DO NOT** add a `coverLetter` field to this collection. Cover letters should go to `/cover_letters/{applicationId}` if persisted at all, otherwise be generated on demand.

---

## 2.7 `saved_jobs/{userId}_{jobId}` — bookmark marker

| Field | Decision | Reason |
|---|---|---|
| `userId` | KEEP | query filter |
| `jobId` | KEEP | query filter |
| `createdAt` | KEEP | order |

Three fields. Do not add anything. This is a marker table, not a mini-job-cache.

---

## 2.8 `ratings/{jobId}_{fromUserId}`

| Field | Decision | Reason |
|---|---|---|
| `jobId` | KEEP | pair scope |
| `fromUserId` | KEEP | identity |
| `toUserId` | KEEP | aggregate target |
| `rating` 1–5 | KEEP | value |
| `review` | KEEP, max 1000 chars, optional | content |
| `createdAt` | KEEP | ordering |

Nothing else. CF reads this and maintains `rating`/`totalRatings` on `worker_profiles` or `employer_profiles`.

---

## 2.9 `job_reports/{reporterId}_{jobId}`

| Field | Decision | Reason |
|---|---|---|
| `jobId` | KEEP | target |
| `reporterId` | KEEP | identity |
| `reportType` | KEEP | enum |
| `description` | KEEP, ≤1000 chars | content |
| `status` | KEEP | moderation |
| `createdAt` | KEEP | order |

No `reporterPhone` (join to `/users` — already correctly omitted).

---

## 2.10 `referrals/{referrerId}_{referredUserId}`

| Field | Decision | Reason |
|---|---|---|
| `referrerId` | KEEP | owner |
| `referredUserId` | KEEP | target |
| `referralCode` | KEEP, 7–10 chars | audit |
| `status` | KEEP, CF-managed | |
| `createdAt` | KEEP | |
| `completedAt` | KEEP, CF-only | |

**Do NOT store per-referral reward amounts.** Rewards come from `/app_config/referral` at query time — if policy changes retroactively, no historical-drift problem.

Android model [ReferralModels.kt](../../app/src/main/java/com/example/dutype/models/ReferralModels.kt) currently accepts `referrerId` OR `referrerUserId`, `reward` OR `rewardAmount` — **delete the aliases**, pick one name, and run a CF-side backfill renaming legacy fields. Also delete `deviceFingerprint` unless fraud pipeline actually consumes it.

---

## 2.11 `referral_stats/{uid}`

**Canonical owner of the 19 fields listed in [functions/src/referral-system.ts](../../functions/src/referral-system.ts) `DEFAULT_REFERRAL_STATS`.**

Rules: client read, CF-only write — **already correct**. Fix is in I-002: stop mirroring these fields onto `/users/{uid}`.

---

## 2.12 `referral_codes/{code}`

| Field | Decision | Reason |
|---|---|---|
| `code` | KEEP | doc ID, O(1) lookup |
| `userId` | KEEP, CF-only | reverse lookup |
| `isActive` | KEEP, CF-only | disable path |

Do NOT store `userName`/`userRole` here — derive via join to `/users/{uid}` if needed.

---

## 2.13 `users/{uid}/withdrawals/{wId}`

| Field | Decision | Reason |
|---|---|---|
| `amount` | KEEP, CF-only | |
| `status` | KEEP, CF-only | |
| `upiId` | KEEP, CF-only | |
| `paymentMethod` | KEEP, CF-only | enum |
| `createdAt` | KEEP | |
| `processedAt` | KEEP, optional | |
| `transactionId` | KEEP, optional | |

Already correctly CF-only write; client read on own subcollection.

---

## 2.14 `notifications/{nId}`

| Field | Decision | Reason |
|---|---|---|
| `recipientId` | KEEP | query filter |
| `title` | KEEP | render |
| `message` | KEEP | render |
| `type` | KEEP | channel routing |
| `targetRole` | KEEP | conditional rendering |
| `data: map` | KEEP | deep link + entity ids |
| `createdAt` | KEEP | order |
| `expiresAt` | KEEP | TTL sweep |
| `isRead` | KEEP, client-writable | UX |
| `readAt` | REMOVE | derive from `isRead` + `updatedAt` if needed, rarely rendered |
| `archivedAt`, `priority`, `relatedJobId`, `relatedApplicationId`, `actionData` | **REMOVE** or keep inside `data{}` | Android [NotificationModels.kt](../../app/src/main/java/com/example/dutype/models/NotificationModels.kt) inflates these at deserialize-time — they are not stored |

---

## 2.15 `app_config/{doc}` — public config

| Field | Decision |
|---|---|
| `referral` doc | KEEP (shipped this session) |
| `features` doc (future) | OK for feature flags |

Never more than one or two docs. Keep the read budget trivial.

**Do not** move feature flags into `/users/{uid}` as per-user fields — that is config-drift hell.

---

## 2.16 Collections to challenge / retire

- `announcements` — used? [AnnouncementService.kt](../../app/src/main/java/com/example/dutype/services/AnnouncementService.kt) opens a snapshot listener at L72. If product is not actively pushing announcements, the listener is pure cost. Gate behind an `app_config.announcements.enabled` flag or delete the collection + listener.
- `_rate_limits` — CF-internal; rules correctly deny. Confirm the sweep worker (noted in CF work) actually deletes old docs.
- `employer_landing` / `worker_landing` — CF returns structured payloads; confirm no client writes ever reach these paths (rules should block if written).

---

## 2.17 Index policy (enforces minimalism)

**Delete now** (`firestore.indexes.json`):
- #6 `saved_jobs.workerId + createdAt` (field doesn't exist)
- #10 `referrals.status + completedAt` (no query)
- #13 `referrals.referrerUserId + createdAt` (rename to `referrerId`)
- #15 `users.isBlocked + successfulReferrals` (fields not canonical)
- #16 `users.userRole + isBlocked + successfulReferrals` (fields not canonical)

**Policy:** an index exists only if a committed Android query references it. Add index deletion to a quarterly review.

---

## 2.18 Listener policy

**Allowed snapshot listeners:**
1. `app_config/referral` — single doc, cheap, realtime config.
2. `/users/{uid}` — current user's profile (only while logged in).
3. `referral_stats/{uid}` — only on Refer & Earn screen, scoped via `WhileSubscribed(5_000)`.
4. The active job detail doc **only while JobDetailScreen is in foreground**.

**Disallow:**
- Listeners on collections (`announcements`, `notifications` list). Use paginated one-shot reads + pull-to-refresh, or CF-pushed updates via FCM data messages.
- Listeners in singleton `@Singleton` services with no scope binding.
