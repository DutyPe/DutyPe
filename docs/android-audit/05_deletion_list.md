# 05 — Deletion List

> Explicit. If it's on this list, delete it. If deletion is risky, mark the PR as `needs-migration` and link to the backfill task.

---

## 5.1 Files / code paths to delete

### Firestore rules (immediate)

- **`firestore.rules` lines 491–1296.** Duplicate rule blocks. Only L1–490 is canonical.

### Android — global state singletons (Phase 2)

- [`app/src/main/java/com/example/dutype/state/AppStateManager.kt`](../../app/src/main/java/com/example/dutype/state/AppStateManager.kt)
- [`app/src/main/java/com/example/dutype/state/ApplicationStateManager.kt`](../../app/src/main/java/com/example/dutype/state/ApplicationStateManager.kt)
- [`app/src/main/java/com/example/dutype/state/ProfileSetupStateManager.kt`](../../app/src/main/java/com/example/dutype/state/ProfileSetupStateManager.kt)
- Corresponding `@Provides` in [`app/src/main/java/com/example/dutype/di/AppModule.kt`](../../app/src/main/java/com/example/dutype/di/AppModule.kt) L113–117.

### Android — duplicate / dead Composables (Phase 6)

- `private fun HowItWorksSection()` in [WorkerReferEarnScreen.kt](../../app/src/main/java/com/example/dutype/worker/screens/WorkerReferEarnScreen.kt) L732 — never called.
- `private fun RewardsSection()` in same file L771 — never called.
- `private fun RedemptionInstructionsSection()` in same file L813 — never called.
- `private fun EmployerHowItWorksCard()`, `EmployerRewardsCard()`, `EmployerRedemptionInstructionsCard()` in [EmployerReferEarnScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerReferEarnScreen.kt) — confirm with `grep` that they are uncalled (high likelihood they mirror worker side).
- Dead block comment block in [EnhancedLoginScreen.kt](../../app/src/main/java/com/example/dutype/auth/EnhancedLoginScreen.kt) around L260–330 (commented-out referral application path ending with `*/` at L328) — either uncomment and test, or delete.

### Android — duplicate routes (Phase 6)

- `Routes.WORKER_ALL_JOBS_FILTERED` (Routes.kt L26) and its `WorkerNavGraph` composable L152–172. Collapse into `WORKER_ALL_JOBS` with query param.
- `Routes.PROFILE_SETUP_WITH_RETURN`. Collapse into `PROFILE_SETUP` with optional `returnRoute` arg.

### Android — duplicate models (Phase 4)

- `JobListingSummary` once `JobCard` / `JobDetail` split lands (replaces both).
- Inline Firestore constants duplicated inside services — centralize in [FirestoreCollections.kt](../../app/src/main/java/com/example/dutype/firestore/FirestoreCollections.kt), delete inline string literals.

### Android — FCM path (Phase 1)

- The Firestore-read block inside [DutyPeMessagingService.kt](../../app/src/main/java/com/example/dutype/services/DutyPeMessagingService.kt) around L95. FCM payload must be self-contained.

### Android — incomplete TODOs (Phase 6)

- `PostJobScreen.kt` L798 "TODO: Send to fraud detection system" — ship or delete the dead code path.
- `AIJobPostingViewModel.kt` L313 "TODO: Fetch from Firestore" — ship or delete.
- `EmployerHomeScreen.kt` L444 "TODO: Uncomment for future release — AI/Voice features next version" — delete.
- `UserMetadata.kt` L428 "TODO: Calculate from data" — either implement or delete the stub.

### Android — static Firebase access (Phase 2 refactor; deletion of static-access sites)

Remove each of these specific static accesses (replace with Hilt-injected Firestore/Auth/Functions):

- [EmployerProfileCache.kt](../../app/src/main/java/com/example/dutype/cache/EmployerProfileCache.kt) L37
- [GuestEngagementWorker.kt](../../app/src/main/java/com/example/dutype/workers/GuestEngagementWorker.kt) L149
- [MainNavGraph.kt](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt) L126
- [RegisterScreen.kt](../../app/src/main/java/com/example/dutype/auth/RegisterScreen.kt) L869–877 (kill the entire inline `ReferralService(...)` builder block)
- [LoginBottomSheet.kt](../../app/src/main/java/com/example/dutype/components/LoginBottomSheet.kt) L883–1078
- [EmployerHomeScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerHomeScreen.kt) L148–166
- [EmployerCompanyDetailsScreen.kt](../../app/src/main/java/com/example/dutype/employer/screens/EmployerCompanyDetailsScreen.kt) L135
- `AuthManager.kt` L185

### Android — top-level packages to remove after content migration (Phase 6)

- `app/src/main/java/com/example/dutype/managers/` → contents to `feature/auth/data/`
- `app/src/main/java/com/example/dutype/state/` → emptied in Phase 2; delete the folder
- `app/src/main/java/com/example/dutype/metadata/` → renamed to `core/aggregations/` (or merged into repositories)
- `app/src/main/java/com/example/dutype/cache/` → contents merge into repositories
- `app/src/main/java/com/example/dutype/data/` → contents move to feature folders

---

## 5.2 Firestore fields to delete

| Collection | Field | Delete path |
|---|---|---|
| `/users/{uid}` | `referralStats.*` (19 sub-fields) | CF backfill: copy to `/referral_stats/{uid}` if missing, then `FieldValue.delete()`. Android `User.kt` stops parsing. |
| `/users/{uid}` | `isVerified`, `isActive` | remove from client write allowlist in rules; CF migrates if needed |
| `/users/{uid}` | any field not listed in canonical schema (02.1) | audit per user doc; delete |
| `/worker_profiles/{uid}` | any stored `rating`/`totalRatings`/`totalJobs` written by clients | rewrite via CF aggregation |
| `/employer_profiles/{uid}` | `isVerified` (client-written version) | CF takes ownership |
| `/job_details/{jobId}` | `applicationCount` | replace with COUNT() aggregation at read time |
| `/jobmetadata/{jobId}` | detail-only fields leaked from legacy writes (`description`, `benefits`, `workingHours`, `educationRequired`, `experienceRequired`, `shiftTiming`, `whatsappNumber`, `contactNumber`, `vacancies`) | CF backfill moves to `/job_details/{jobId}` and `FieldValue.delete()` here |
| `/referrals/{id}` | `rewardAmount`, `bonusAmount`, `referredUserReward`, `deviceFingerprint` if unused, alias field names (`referrerUserId`) | consolidate via CF; Android drops alias parsing |
| `/notifications/{id}` | `readAt`, `archivedAt`, `priority`, `relatedJobId`, `relatedApplicationId`, `actionData` if not written today | confirm via CF audit; Android [NotificationModels.kt](../../app/src/main/java/com/example/dutype/models/NotificationModels.kt) stops reading these |

---

## 5.3 Collections to retire or gate

- **`announcements`** — if product isn't actively using, delete the collection + `AnnouncementService` + `AnnouncementViewModel`. If it must stay, gate behind `app_config.announcements.enabled` flag.
- **Any `*_v2` / shadow collections** not enumerated in 02.x — investigate and delete.
- **`users/{uid}/withdrawals`** subcollection — keep, CF-only writes, client read on own subcollection.

---

## 5.4 Indexes to delete

From [firestore.indexes.json](../../firestore.indexes.json):

- Index #6 — `saved_jobs.workerId + createdAt`
- Index #10 — `referrals.status + completedAt`
- Index #13 — `referrals.referrerUserId + createdAt` **(rename to `referrerId`, don't just delete)**
- Index #15 — `users.isBlocked + successfulReferrals`
- Index #16 — `users.userRole + isBlocked + successfulReferrals`

Deploy `firebase deploy --only firestore:indexes` after each removal.

---

## 5.5 Metadata / runtime dead weight

- `InAppReviewTriggerServiceHolder`, `BirthdayServiceHolder`, `PaginationHelper` listed among the 27 ViewModels — if they are thin holders, inline them and delete.
- Ads are disabled in [DutyPeApplication.kt](../../app/src/main/java/com/example/dutype/DutyPeApplication.kt) L75. If there is no near-term plan to re-enable, delete `ads/` package and the AdMob dependency.

---

## 5.6 Fallback / compatibility logic to delete

- [ReferralModels.kt](../../app/src/main/java/com/example/dutype/models/ReferralModels.kt) L42–60 accepts `referrerId`/`referrerUserId`, `reward`/`rewardAmount`, `milestoneBonus`/`bonusAmount`, `signupBonusAmount`/`referredUserReward`. Pick one per pair, run CF backfill, then **delete the fallback readers**.
- [User.kt](../../app/src/main/java/com/example/dutype/models/User.kt) — remove any alt-name parsing; canonical field names only.
- Any `getOrDefault` on fields that no longer exist after schema cleanup.

---

## 5.7 Gradle / build hygiene

- Confirm no unused dependencies remain after deletion (run `./gradlew app:dependencyInsight --dependency <name>` for each suspected orphan).
- Remove any BuildConfig flags for dropped features.
- Remove `fallbackToDestructiveMigration()` call (Phase 1).
