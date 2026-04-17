# Firebase Schema Migration Ready Guide

Last updated: 2026-04-14

Related inventory: [FIREBASE_COLLECTIONS_INVENTORY.md](./FIREBASE_COLLECTIONS_INVENTORY.md)

## Goal

This document is the second-pass cleanup view of the Firebase schema:

- `Target canonical schema` = what the app should build against going forward
- `Deprecate` = fields or collections that should be phased out
- `Action` = practical cleanup or migration work needed

## High Priority Fixes

### 1. Normalize user role fields

Target canonical fields in `users/{userId}`:

- `roles: string[]`
- `activeRole: string`

Deprecate:

- `role`

Action:

- Stop writing `users.role` from admin APIs.
- Update admin tools to read `activeRole` first and `roles` second.
- Backfill any user doc that only has `role` into `roles` and `activeRole`.

### 2. Pick one referral stats source of truth

Target canonical source:

- `referral_stats/{userId}`

Keep in `users/{userId}` only:

- `referralCode`
- `referredByCode`
- `referredByUserId`

Deprecate:

- `users.referralStats`
- top-level referral stat fields inside `users` like `availableBalance`, `withdrawnAmount`, `pendingReferrals`, `expiredReferrals`, `totalWithdrawals`, `lastUpdated`

Action:

- Choose `referral_stats` as the single stats document.
- Convert product readers to stop merging `users` referral fields with `referral_stats`.
- Add one migration job to copy latest embedded `users.referralStats` into `referral_stats` where missing.
- After cutover, stop all writes to embedded and top-level legacy referral stat fields on `users`.

### 3. Normalize withdrawal storage

Target canonical path:

- `users/{userId}/withdrawals/{withdrawalId}`

Deprecate:

- top-level `withdrawal_requests`

Action:

- Update admin referrals API to read from collection group `withdrawals` or from user subcollections directly.
- Remove write dependence on `withdrawal_requests`.
- If old admin UI still needs historical rows, migrate legacy docs into user subcollections or create a read-only export.

### 4. Normalize jobs schema

Target canonical storage:

- `jobs/{jobId}` for summary card data
- `job_details/{jobId}` for full detail payload

Deprecate in `jobs`:

- `location` as string
- `category`
- `payAmount`
- `payType`
- `shift`
- `requirements`
- `contactPhone`
- `contactEmail`
- `isActive`
- `postedBy`
- `source`
- `latitude`
- `longitude`
- `address`
- `postedAt`
- `company`
- `employerName`
- `businessName`
- `company_name`

Action:

- Make web admin job create and update routes write canonical `salary`, `salaryType`, `location {lat,lng}`, `addressText`, `shiftTiming`, `jobType`, and `contactNumber`.
- Keep UI-level normalization only temporarily.
- Add a one-time migration for legacy jobs to compute canonical `salary`, `salaryType`, lat/lng location object, and `companyName`.

### 5. Align applications schema with actual product usage

Problem:

- Rules allow a minimal application shape, but the product writes and reads a much richer document.

Target canonical `applications/{applicationId}`:

- `jobId`
- `workerId`
- `employerId`
- `status`
- `createdAt`
- `updatedAt`
- `appliedAt`
- `workerName`
- `jobTitle`
- `companyName`
- `jobLocation`
- `statusHistory`
- `verification`

Deprecate:

- duplicate `id`
- duplicate `applicationId` inside the document if doc id is already authoritative
- ad hoc worker enrichment fields that belong to a profile snapshot layer rather than application storage

Action:

- Decide whether the document id remains `${jobId}_${workerId}` or becomes a generated id.
- Update rules to allow the actual live application payload.
- Reduce enrichment-only fields that can be fetched from `users` or `worker_profiles`.

### 6. Decide on notification expiry support

Problem:

- App code reads `expiresAt`, scheduled functions clean up on `expiresAt`, but rules-centered minimal inbox writes do not treat it as a stable canonical field.

Target canonical `notifications/{notificationId}`:

- `recipientId`
- `title`
- `message`
- `type`
- `data`
- `isRead`
- `createdAt`
- `expiresAt`

Action:

- Make `expiresAt` a first-class canonical field in rules and all notification writers.
- Standardize whether `createdAt` and `expiresAt` are Firestore timestamps or epoch numbers. Prefer Firestore timestamps in storage.

## Canonical Collections

### `users/{userId}`

Target canonical fields:

- `userId`
- `phone`
- `fullName`
- `profileImageUrl`
- `roles`
- `activeRole`
- `location`
- `geohash`
- `isVerified`
- `isActive`
- `fcmToken`
- `createdAt`
- `lastActiveAt`
- `referralCode`
- `referredByCode`
- `referredByUserId`

Deprecate:

- `role`
- referral-stat counters stored directly on the user document
- `profileCompleted` if still present

Action:

- Keep `users` identity-focused only.

---

### `worker_profiles/{userId}`

Target canonical fields:

- `userId`
- `jobTypes`
- `skills`
- `isAvailable`
- `rating`
- `totalRatings`
- `totalJobs`
- `lastActiveAt`

Deprecate:

- using `jobTypes` as a silent fallback for `skills`, or vice versa, without writing both intentionally

Action:

- Pick one semantic meaning:
- `jobTypes` = categories of work
- `skills` = freeform or structured skills
- Backfill missing arrays where current readers depend on fallback behavior.

---

### `employer_profiles/{userId}`

Target canonical fields:

- `userId`
- `companyName`
- `rating`
- `totalRatings`
- `totalHires`
- `isVerified`
- `lastActiveAt`

Deprecate:

- any employer identity fields duplicated into unrelated collections when they can be read from profile joins

Action:

- Keep employer profile lean and consistent with `users`.

---

### `jobs/{jobId}`

Target canonical summary fields:

- `employerId`
- `companyName`
- `isVerified`
- `title`
- `jobType`
- `salary`
- `salaryType`
- `gender`
- `experienceRequired`
- `shiftTiming`
- `applicationCount`
- `location`
- `geohash`
- `companyCity`
- `addressText`
- `urgency`
- `status`
- `createdAt`
- `expiresAt`

Allowed supportive fields:

- `jobId`
- `updatedAt`
- `lastApplicationAt`
- `hasActiveWorker`
- `lastWorkerStartedAt`
- `moderationStatus`
- `idempotencyKey`

Deprecate:

- all admin-console legacy job fields that duplicate canonical fields under different names

Action:

- Put descriptive and contact-heavy fields in `job_details`.

---

### `job_details/{jobId}`

Target canonical detail fields:

- `employerId`
- `companyName`
- `isVerified`
- `title`
- `jobType`
- `salary`
- `salaryType`
- `description`
- `contactNumber`
- `addressText`
- `companyCity`
- `location`
- `geohash`
- `gender`
- `experienceRequired`
- `shiftTiming`
- `vacancies`
- `benefits`
- `urgency`
- `applicationCount`
- `status`
- `createdAt`
- `expiresAt`
- `whatsappNumber`
- `workingHours`
- `educationRequired`

Deprecate:

- storing detailed job body fields directly in `jobs` unless needed for search cards

Action:

- Keep `jobs` and `job_details` synchronized in one service only.

---

### `applications/{applicationId}`

Target canonical fields:

- `jobId`
- `workerId`
- `employerId`
- `status`
- `createdAt`
- `updatedAt`
- `appliedAt`
- `workerName`
- `companyName`
- `jobTitle`
- `jobLocation`
- `statusHistory`
- `verification`

Optional canonical supportive fields:

- `coverLetter`
- `completedAt`
- `workStartedAt`
- `source`
- `active`

Deprecate:

- `id`
- duplicate `applicationId` field if doc id is enough
- profile-derived fields that constantly drift from source of truth

Action:

- Decide whether applications should contain a minimal stable snapshot or just identifiers plus workflow state.

---

### `saved_jobs/{saveId}`

Target canonical fields:

- `id`
- `userId`
- `jobId`
- `createdAt`

Deprecate:

- `workerId`
- numeric `createdAt` where rules expect Firestore timestamp

Action:

- Standardize the owner key as `userId` only.

---

### `ratings/{ratingId}`

Target canonical fields:

- `jobId`
- `applicationId`
- `raterUserId`
- `raterUserRole`
- `ratedUserId`
- `ratedUserRole`
- `overallRating`
- `feedback`
- `tags`
- `createdAt`
- `isActive`

Optional detailed breakdown:

- `communicationRating`
- `paymentRating`
- `professionalismRating`
- `punctualityRating`
- `qualityRating`

Deprecate:

- old minimal only shape if product has already standardized on multi-dimension ratings
- `fromUserId` and `toUserId` once all readers are migrated to `raterUserId` and `ratedUserId`
- `review` once all readers are migrated to `feedback`

Action:

- Pick one rating schema and align rules to it.

---

### `job_reports/{reportId}`

Target canonical fields:

- `reportId`
- `jobId`
- `reporterId`
- `reporterPhone`
- `reportType`
- `description`
- `timestamp`
- `status`
- `reviewedBy`
- `reviewedAt`
- `actionTaken`

Action:

- Keep this collection as the moderation trail and avoid spreading moderation status across ad hoc fields.

---

### `referrals/{referralId}`

Target canonical fields:

- `referrerId`
- `referredUserId`
- `referralCode`
- `status`
- `rewardAmount`
- `bonusAmount`
- `referredUserReward`
- `createdAt`
- `completedAt`
- `expiresAt`
- `deviceFingerprint`
- `ipAddress`
- `fraudScore`
- `fraudSignals`
- `needsReview`
- `fraudCheckedAt`
- `rejectionReason`
- `referredUserName`
- `referredUserRole`

Deprecate:

- `reward`
- `referrerUserId`
- mixed status casing like `pending` and `PENDING`

Action:

- Standardize status values in one casing only. Prefer uppercase enum values.

---

### `referral_stats/{userId}`

Target canonical fields:

- `userId`
- `userRole`
- `userName`
- `referralCode`
- `totalReferrals`
- `successfulReferrals`
- `pendingReferrals`
- `expiredReferrals`
- `rejectedReferrals`
- `totalEarnings`
- `pendingEarnings`
- `withdrawnAmount`
- `availableBalance`
- `canWithdraw`
- `nextMilestone`
- `currentTier`
- `freeJobPostings`
- `freeJobPostingsExpiry`
- `signupBonusReceived`
- `signupBonusAmount`
- `totalWithdrawals`
- `isBlocked`
- `blockReason`
- `referredByCode`
- `referredByUserId`
- `lastWithdrawalAt`
- `lastUpdated`

Action:

- This should become the only referral metrics source of truth.

---

### `referral_codes/{code}`

Target canonical fields:

- `code`
- `userId`
- `userRole`
- `userName`
- `isActive`
- `createdAt`
- `totalUsed`
- `successfulReferrals`

Deprecate:

- `userRoles`

Action:

- Keep one role field only unless multi-role ownership is truly required.

---

### `announcements/{announcementId}`

Target canonical fields:

- `title`
- `message`
- `type`
- `targetRole`
- `isActive`
- `priority`
- `createdAt`
- `updatedAt`
- `actionText`
- `actionRoute`
- `imageUrl`
- `startDate`
- `endDate`
- `isDismissible`
- `createdBy`

Action:

- Since readers already support the richer shape, bring admin write APIs up to this model if those features matter.

---

### `notifications/{notificationId}`

Target canonical fields:

- `recipientId`
- `title`
- `message`
- `type`
- `data`
- `isRead`
- `createdAt`
- `expiresAt`
- `sentAt`
- `status`

Supportive processing fields:

- `fcmMessageId`
- `processing`
- `processingStartedAt`
- `error`

Deprecate:

- top-level `targetRole` if it is only being duplicated from `data.targetRole`

Action:

- Keep transport-processing fields optional but documented.

---

### `conversations/{conversationId}`

Target canonical fields:

- `participants`
- `participantDetails`
- `jobId`
- `lastMessage`
- `lastMessageAt`
- `lastMessageBy`
- `unreadCount`
- `createdAt`
- `updatedAt`

Action:

- If chat is active product scope, add a single owned creator service and document the write path.
- If chat is not active, treat this as dormant schema and avoid partial implementations.

---

### `messages/{messageId}`

Target canonical fields:

- `conversationId`
- `senderId`
- `recipientId`
- `message`
- `type`
- `isRead`
- `createdAt`

Action:

- Match this to `conversations` rollout status. Do not keep rules without a maintained writer for long.

---

### `work_locations/{userId}/locations/{locationId}`

Target canonical fields:

- `label`
- `address`
- `normalizedAddress`
- `latitude`
- `longitude`
- `addedAt`
- `updatedAt`
- `usageCount`

Action:

- Already relatively clean. No major schema cleanup needed.

---

### `notification_tracking/{userId}/sent/{eventId}`

Target canonical fields:

- `type`
- `sentAt`
- `key`
- `applicationId`

Action:

- Keep this operational and internal. No user-facing app code should depend on it.

## Recommended Migration Order

### Phase 1

- Stop writing `users.role`
- Stop writing new data to `withdrawal_requests`
- Update admin job APIs to canonical `jobs` shape
- Standardize notification `expiresAt`

### Phase 2

- Migrate referral stats out of `users`
- Migrate legacy jobs fields into canonical fields
- Update `applications` rules to match real application documents
- Standardize `ratings` schema

### Phase 3

- Remove legacy reader fallbacks from app and web
- Delete unused legacy collections after verification
- Tighten Firestore rules to the final canonical schemas only

## Final Recommendation

If you want the cleanest long-term structure, the strongest target is:

- `users` for identity
- `worker_profiles` and `employer_profiles` for role-specific profile data
- `jobs` for search cards
- `job_details` for full posting details
- `applications` for workflow state and small snapshots only
- `referrals`, `referral_stats`, and `referral_codes` as the referral domain
- `users/{userId}/withdrawals` for payouts
- `notifications` for inbox items

The biggest cleanup wins will come from removing duplicate role fields, duplicate referral stats, legacy job aliases, and the old top-level withdrawal collection.
