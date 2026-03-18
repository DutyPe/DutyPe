# DutyPe Firebase Firestore Deep Field Research

Generated on: 2026-03-18
Scope: Firestore rules, indexes, Cloud Functions (`functions/src`), Android (`app/src`), Web (`web`), existing schema docs.

## Methodology

This report was built from:
- `firestore.rules` (authorization + declared collections)
- `firestore.indexes.json` (query-critical fields)
- Cloud Functions writes/updates (`functions/src/index.ts`, `functions/src/referral-system.ts`, `functions/src/scheduled-notifications.ts`, `functions/src/job-posting.ts`)
- Web admin/product Firestore usage (`web/components`, `web/lib`)
- Android Firestore usage (`app/src/main/java/...`)
- Existing schema docs (`docs/COMPLETE_FIELD_MAPPING.md` and optimization docs)

Field confidence labels used below:
- Confirmed: directly read/written/queried in live code/rules/indexes
- Legacy: still referenced but superseded by newer model
- Inferred: used in app models/docs but not always written in current server code

---

## 1) `users` (core identity + profile + referral stats)

Document ID: Firebase Auth UID (`userId`)

### Confirmed and actively used fields
- `id`: user id mirror (legacy duplicate of doc id)
- `phone`: normalized phone for login
- `phoneNumber`: legacy alias used in fallback lookups
- `fullName`: primary display name
- `name`: legacy display-name alias
- `email`: optional contact
- `profileImageUrl`: profile avatar URL
- `photoUrl`: legacy avatar alias
- `roles`: list of roles (`WORKER`, `EMPLOYER`)
- `activeRole`: current active mode
- `role`: legacy single-role field
- `address`: human-readable location
- `latitude`: user coordinate
- `longitude`: user coordinate
- `dateOfBirth`: birthday logic + profile
- `gender`: profile and filtering
- `bio`: worker profile text
- `skills`: profile skill text/list
- `experience`: profile experience text
- `companyName`: employer company label
- `companySize`: employer company size (inferred usage)
- `industry`: employer industry (inferred usage)
- `trustTier`: trust badge tier
- `trustScore`: trust/ranking score (inferred/legacy)
- `isVerified`: account verification flag
- `isActive`: account active/suspended flag
- `profileCompleted`: profile completion milestone
- `isProfileComplete`: legacy completion alias
- `profileCompletionPercentage`: onboarding progress
- `fcmToken`: push notification token
- `createdAt`: account create time
- `updatedAt`: profile update time
- `lastActiveAt`: activity timestamp
- `lastLoginAt`: login timestamp
- `referralCode`: immutable user referral code
- `referralCodeCreatedAt`: referral code issue timestamp
- `savedJobs`: favorite job ids array
- `workLocations`: multiple preferred locations array
- `averageRating`: employer/overall rating aggregate
- `totalRatings`: employer/overall rating count
- `workerAverageRating`: worker-side rating aggregate
- `workerTotalRatings`: worker-side rating count
- `companyRating`: employer-company public rating
- `postedJobsCount`: employer jobs posted counter

### Confirmed nested object: `referralStats`
- `totalReferrals`: total referred users
- `successfulReferrals`: completed/credited referrals
- `pendingReferrals`: pending referrals count
- `expiredReferrals`: expired referrals count
- `rejectedReferrals`: rejected referrals count
- `totalEarnings`: cumulative referral earnings
- `pendingEarnings`: pending earnings amount
- `withdrawnAmount`: withdrawn earnings amount
- `availableBalance`: available referral wallet
- `canWithdraw`: withdrawal eligibility
- `nextMilestone`: next reward threshold
- `currentTier`: referral tier
- `freeJobPostings`: employer reward posts count
- `freeJobPostingsExpiry`: expiry for free postings reward
- `signupBonusReceived`: whether signup bonus was credited
- `signupBonusAmount`: signup bonus amount
- `totalWithdrawals`: number of withdrawals made
- `isBlocked`: referral system blocked flag
- `blockReason`: reason for referral block
- `lastUpdated`: stats update time
- `referredByCode`: code used by this user
- `referredByUserId`: referrer user id
- `lastWithdrawalAt`: last withdrawal time
- `lastReferralAt`: last referral event time (inferred)

### Other user-related legacy/admin flags seen in code/docs
- `blocked_count`: anti-abuse counter (legacy)
- `last_blocked_at`: anti-abuse timestamp (legacy)
- `is_suspended`: account suspended (legacy)
- `suspended_at`: suspension timestamp (legacy)
- `points`: gamification points (legacy/inferred)

### Subcollections under users
- `users/{userId}/tokens`
  - `token`, `platform`, `updatedAt` (and doc id token key)
- `users/{userId}/activity`
  - `action`, `timestamp`, `metadata`

---

## 2) `phone_roles` (pre-login role lookup)

Document ID: normalized phone number

Fields:
- `role`: current role for phone (`WORKER`/`EMPLOYER`)
- `phoneNumber`: optional explicit field mirroring doc id (legacy pattern)

Use:
- Fast role resolution before full auth/profile read.

---

## 3) `jobs` (job posting lifecycle)

Document ID: auto id

### Core fields
- `id`: job id mirror (duplicate of doc id)
- `employerId`: owner user id
- `title`: job title
- `companyName`: denormalized company text
- `description`: full job description
- `category`: job category
- `jobType`: type (`FULL_TIME`, `PART_TIME`, etc.)
- `gender`: preference (`MALE`, `FEMALE`, `ANY`)
- `location`: location text
- `latitude`: location latitude
- `longitude`: location longitude
- `geoHash` / `geohash`: geospatial query key (case variant seen)
- `payAmount`: compensation amount/text
- `payType`: compensation unit
- `shiftTiming`: shift schedule text
- `vacancies`: total openings
- `isActive`: visibility flag
- `isFilled`: filled flag
- `vacancyStatus`: `OPEN`/`FILLED`/etc. (newer derived status)
- `postedAt`: posting timestamp (legacy primary)
- `createdAt`: creation timestamp (newer primary in indexes)
- `updatedAt`: update timestamp
- `expiresAt`: expiry timestamp
- `contactNumber`: contact phone
- `contactPhone`: legacy alias
- `jobImageUrl`: optional image URL
- `urgency`: urgency level
- `applicationCount`: app count (some code also uses plural below)
- `applicationsCount`: plural variant used by batch vacancy function
- `idempotencyKey`: duplicate-post prevention key

### Moderation/fraud fields
- `moderationStatus`: moderation state (`AUTO_APPROVED`, `PENDING_REVIEW`, `REJECTED`, `HIDDEN_BY_REPORTS`, etc.)
- `moderationReason`: moderation reason string
- `fraudScore`: duplicate/fraud numeric score
- `signals`: fraud signal array (queue item style)
- `moderatedBy`: moderator id
- `moderatedAt`: moderation timestamp
- `moderatorNotes`: moderator notes
- `reportCount`: total reports count (used in queue/reporting)
- `lastApplicationAt`: timestamp of latest application
- `lastReportedAt`: last report timestamp (legacy/inferred)

Use:
- Marketplace listing, geolocation filtering, employer analytics, moderation, notification triggers.

---

## 4) `job_applications` (worker-employer application relation)

Document ID: auto id

### Stored fields
- `id`: application id mirror
- `jobId`: target job id
- `workerId`: applicant user id
- `employerId`: employer owner id
- `status`: state (`PENDING`, `APPLIED`, `ACCEPTED`, `SHORTLISTED`, `REJECTED`, `WITHDRAWN`, `COMPLETED`, etc.)
- `active`: soft-active flag
- `appliedAt`: application timestamp
- `updatedAt`: last update timestamp
- `jobTitle`: denormalized title
- `jobLocation`: denormalized location
- `companyName`: denormalized company
- `workerName`: denormalized worker name
- `coverLetter`: optional note
- `lastPendingNotificationSent`: anti-spam reminder timestamp
- `verificationStatus`: verification state index field
- `verificationCode`: per-application verification code
- `verification`: nested verification payload (web model)

### Nested `verification` object fields (web product flow)
- `verificationId`
- `applicationId`
- `jobId`
- `jobTitle`
- `workerId`
- `workerName`
- `employerId`
- `employerName`
- `verificationCode`
- `qrCodeData`
- `status` (`PENDING`, `VERIFIED`, `EXPIRED`, `CANCELLED`)
- `generatedAt`
- `expiresAt`
- `verifiedAt`
- `verifiedByEmployerId`
- `verifiedLocation` (`lat`, `lng` or `latitude`, `longitude` variants)

Use:
- Worker apply flow, employer pipeline, reminders, verification, hiring status transitions.

---

## 5) `notifications` (inbox + push pipeline)

Document ID: auto id

Fields:
- `id`: notification id mirror
- `recipientId`: target user id (primary security field)
- `userId`: legacy recipient alias in some writes
- `title`: heading
- `message`: body text
- `body`: legacy body alias
- `type`: semantic notification type
- `data`: structured payload map
- `jobId`: direct top-level id (legacy write path)
- `applicationId`: direct top-level id (legacy/normalized by client)
- `conversationId`: conversation deep-link id
- `createdAt`: creation timestamp
- `updatedAt`: update timestamp (inferred)
- `isRead`: read flag
- `processing`: in-flight send lock
- `processingStartedAt`: lock start timestamp
- `sentAt`: send completion timestamp
- `fcmMessageId`: FCM response id
- `error`: send failure detail
- `skipped`: duplicate skip flag
- `skipReason`: duplicate reason
- `duplicateOf`: source notification id when deduped
- `expiresAt`: cleanup expiry timestamp
- `targetRole`: target role for role-aware rendering (used by app notification models)

Use:
- In-app inbox, push dispatch, dedupe, analytics, deep linking.

---

## 6) `notification_tracking` and `notification_tracking/{userId}/sent`

Top-level document id: `userId`

Subcollection `sent` fields:
- `type`: tracked notification type
- `sentAt`: unix millis timestamp
- `key`: dedupe key for per-job/per-day reminders
- `jobId`: tracked job id for dedupe
- `applicationId`: tracked app id for dedupe
- `timestamp`: Firestore server timestamp backup

Use:
- Rate limiting and deduplication for scheduled notification jobs.

---

## 7) `announcements`

Document ID: auto id

### Confirmed fields in admin/web write path
- `title`
- `message`
- `type` (`INFO`, `WARNING`, `SUCCESS`, `ERROR`)
- `targetRole` (`ALL`, `WORKER`, `EMPLOYER`)
- `isActive`
- `priority`
- `createdAt`

### Additional app-model fields (inferred but strongly expected)
- `actionText`
- `actionRoute`
- `imageUrl`
- `startDate`
- `endDate`
- `isDismissible`
- `createdBy`

Use:
- Worker/employer home banners with role/date filtering and optional CTA routing.

---

## 8) `app_feedback`

Document ID: auto id

Fields seen/expected:
- `message` (confirmed)
- `userId` (inferred common pattern)
- `rating` (inferred optional)
- `createdAt` (inferred optional)

Use:
- Feedback submission from app bottom sheet and quality monitoring.

---

## 9) `job_reports`

Document ID: auto id

Fields:
- `jobId`
- `reporterId`
- `reason`
- `description`
- `timestamp`
- `createdAt` (legacy/inferred variant)

Use:
- Community moderation; threshold-based auto-hide flow.

---

## 10) `moderation_queue`

Document ID: auto id

Fields:
- `jobId`
- `employerId`
- `type` (e.g., `DUPLICATE_SUSPECTED`)
- `reason` (e.g., `COMMUNITY_REPORTS`)
- `fraudScore`
- `signals` (array)
- `reportCount`
- `priority`
- `status` (`PENDING`, `APPROVED`, `REJECTED`)
- `moderatorId`
- `moderatorNotes`
- `createdAt`
- `completedAt`

Use:
- Human review queue for suspicious/reported jobs.

---

## 11) `employer_blocked_attempts`

Document ID: likely employer id / attempt id

Fields (rules + naming patterns):
- `attemptId` (doc id or explicit)
- `count`
- `reason`
- `createdAt`
- `updatedAt` (inferred)

Use:
- Persist repeated blocked attempts from anti-fraud checks.

---

## 12) `ratings`

Document ID: generated deterministic id (web) or auto id (service)

### Confirmed cross-platform fields
- `id`
- `ratingId`
- `applicationId`
- `jobId`
- `raterId`
- `raterUserId`
- `raterName`
- `raterRole` / `raterUserRole`
- `targetUserId` / `ratedUserId`
- `targetUserName`
- `targetRole` / `ratedUserRole`
- `rating` (Android simple model)
- `overallRating`
- `punctualityRating`
- `qualityRating`
- `communicationRating`
- `professionalismRating`
- `paymentRating`
- `review` / `feedback`
- `tags`
- `isActive`
- `createdAt`
- `updatedAt` (inferred)

Use:
- Worker/employer mutual ratings, profile summary updates, trust surfaces.

---

## 13) `withdrawal_requests`

Document ID: auto id

Fields:
- `id`
- `userId`
- `userRole`
- `amount`
- `status` (`PENDING`, `COMPLETED`, `FAILED`, etc.)
- `paymentMethod` (`UPI`, `BANK_TRANSFER`)
- `upiId`
- `bankAccountNumber`
- `ifscCode`
- `accountHolderName`
- `createdAt`
- `processedAt`
- `transactionId` (inferred admin settlement)

Use:
- Referral earning withdrawals and payout ops.

---

## 14) `referral_codes`

Document ID: referral code itself

Fields:
- `code`
- `userId`
- `userRole`
- `userName`
- `isActive`
- `createdAt`
- `totalUsed`
- `successfulReferrals`

Use:
- O(1) code lookup and ownership mapping.

---

## 15) `referrals`

Document ID: auto id

Fields:
- `id`
- `idempotencyKey`
- `referrerUserId`
- `referrerRole`
- `referrerUserName` (legacy/admin view)
- `referredUserId`
- `referredRole`
- `referredUserRole` (legacy alias)
- `referralCode`
- `status` (`PENDING`, `COMPLETED`, `EXPIRED`, `REJECTED`, `CANCELLED`)
- `rewardAmount`
- `bonusAmount`
- `referredUserReward`
- `referredUserName`
- `referredUserPhone`
- `deviceFingerprint`
- `ipAddress`
- `fraudScore`
- `fraudSignals`
- `needsReview`
- `rejectionReason`
- `createdAt`
- `completedAt`
- `expiresAt`
- `fraudCheckedAt`

Use:
- Referral ledger, anti-fraud analytics, timeline/history.

---

## 16) `referral_stats` (legacy but still actively used)

Document ID: user id

Fields:
- `userId`
- `userRole`
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

Use:
- Backward-compatible referral stats store, leaderboard, fraud controls.

---

## 17) `referral_events`

Document ID: auto id

Fields:
- `eventType` (`REWARD_CREDITED`, `SIGNUP_BONUS_CREDITED`, `WITHDRAWAL_REQUESTED`, `REFERRAL_EXPIRED`, etc.)
- `userId`
- `referralId`
- `withdrawalId`
- `amount`
- `bonusAmount`
- `newTier`
- `timestamp`

Use:
- Audit/event-sourcing stream for referral lifecycle.

---

## 18) `metadata` (config + aggregates)

Document IDs confirmed in code:
- `platform_stats`
- `category_stats`
- `trending`
- `recent_jobs`
- `location_stats`
- `pay_stats`
- `feature_flags`

### `metadata/platform_stats` fields
- `totalJobs`
- `totalWorkers`
- `totalEmployers`
- `totalApplications`
- `totalUsers` (app-side optional)
- `totalRatings` (app-side optional)
- `activeJobs` (app-side optional)
- `jobsPostedToday` (app-side optional)
- `applicationsToday` (app-side optional)
- `lastUpdated`

### `metadata/category_stats` fields
- `categories` map
- each category payload commonly includes `jobCount`, `averagePay`, and app-side may read `activeJobs`
- `lastUpdated`

### `metadata/trending` fields
- `trendingCategories` array with `category`, `jobCount`, `averagePay`
- app-side may read trend maps/counters
- `lastUpdated`

### `metadata/location_stats` fields
- location map payloads, app-side commonly expects `activeJobs`
- `lastUpdated` (inferred)

### `metadata/pay_stats` fields
- `averagePay`
- `medianPay`
- `minPay`
- `maxPay`
- `lastUpdated` (inferred)

### `metadata/feature_flags` fields
- `isChatEnabled`
- `isMapViewEnabled`
- `isSubscriptionEnabled`
- `isReferralEnabled`
- `isWorkVerificationEnabled`
- `isRatingEnabled`
- `isWhatsAppApplyEnabled`
- `isDigitalCardEnabled`
- `maxFreeJobPosts`
- `maxFreeApplications`
- `jobExpiryDays`
- `maintenanceMode`
- `maintenanceMessage`
- `minAppVersion`
- `forceUpdateVersion`

Use:
- App-level toggles, admin controls, KPI dashboards.

---

## 19) `broadcast_notifications`

Document ID: auto id

Fields:
- `title`
- `message`
- `topic` (`all_users`, `workers`, `employers`, `app_updates`)
- `type`
- `sentAt`
- `fcmMessageId`
- `status` (`sent`, `failed`)
- `error`

Use:
- Admin/system mass push broadcasting.

---

## 20) `conversations` (chat)

Document ID: conversation id

Fields:
- `participants` (user ids)
- `participantDetails` map: each entry includes `name`, `profileImage`, `role`
- `jobId`
- `lastMessage`
- `lastMessageAt`
- `lastMessageBy`
- `unreadCount` map (`userId -> number`)
- `createdAt`
- `updatedAt`

Use:
- Conversation list and unread counters for worker/employer messaging.

---

## 21) `messages` (chat messages)

Document ID: message id

Fields:
- `conversationId`
- `senderId`
- `recipientId`
- `message`
- `type` (default `TEXT`)
- `isRead`
- `createdAt`

Use:
- Real-time message thread data.

---

## 22) `applications` (legacy collection still referenced)

Document ID: application id

Fields seen in triggers/services:
- `workerId`
- `employerId`
- `jobId`
- `jobTitle`
- `workerName`
- `status`

Use:
- Legacy notification and in-app review trigger paths.
- Note: primary modern collection is `job_applications`.

---

## 23) Deprecated or documented-only collections (cleanup candidates)

- `worker_profiles`
- `employer_profiles`
- `savedJobs`
- `saved_jobs`
- `fcm_tokens`
- `rating_summaries`
- `referral_clicks`
- `userPreferences`
- `user_activity`
- `query_metrics` (documented optimization path)
- partition/shard examples in docs: `notifications_YYYY_MM`, `jobs_city`

---

## Global A-Z Firestore Key Inventory (from code scan)

This is the raw cross-code unique key inventory used to ensure no field-like token was missed. Some entries are computed values or aliases; they are retained intentionally for completeness.

`active`, `activeJobs`, `activeRole`, `agreedAmount`, `applicationCount`, `applicationId`, `applicationsToday`, `appliedAt`, `averagePay`, `averageRating`, `averageResponseTime`, `blocked_count`, `category`, `companyName`, `companyRating`, `companySize`, `completedAt`, `completedJobsCount`, `contactNumber`, `contactPhone`, `conversationId`, `createdAt`, `dateOfBirth`, `deviceFingerprint`, `email`, `employerAverageRating`, `employerId`, `employerTotalRatings`, `experience`, `expiresAt`, `fcmToken`, `filter`, `forceUpdateVersion`, `fullName`, `geohash`, `hoursWorked`, `industry`, `ipAddress`, `is_suspended`, `isActive`, `isBlocked`, `isChatEnabled`, `isDigitalCardEnabled`, `isFilled`, `isGstVerified`, `isMapViewEnabled`, `isPaid`, `isProfileComplete`, `isRatingEnabled`, `isReferralEnabled`, `isSubscriptionEnabled`, `isVerified`, `isWhatsAppApplyEnabled`, `isWorkVerificationEnabled`, `jobExpiryDays`, `jobId`, `jobTitle`, `jobsPostedToday`, `lastActiveAt`, `lastPendingNotificationSent`, `last_blocked_at`, `maintenanceMessage`, `maintenanceMode`, `maxFreeApplications`, `maxFreeJobPosts`, `maxPay`, `medianPay`, `minAppVersion`, `minPay`, `name`, `notificationId`, `participants`, `payAmount`, `phone`, `phoneNumber`, `photoUrl`, `points`, `postedAt`, `postedJobsCount`, `profileCompleted`, `profileCompletionPercentage`, `profileImageUrl`, `recipientId`, `referralCode`, `referredUserId`, `referredUserName`, `referrerUserId`, `returnRoute`, `role`, `roles`, `sent`, `skills`, `status`, `suspended_at`, `timestamp`, `title`, `totalApplications`, `totalEmployers`, `totalJobs`, `totalRatings`, `totalUsers`, `totalWorkers`, `trustScore`, `trustTier`, `type`, `userId`, `userRole`, `vacancyStatus`, `verificationCode`, `voiceQuery`, `workerAverageRating`, `workerId`, `workerName`, `workerTotalRatings`

---

## Important consistency issues found

- `role` vs `roles` + `activeRole` coexist
- `phone` vs `phoneNumber`
- `name` vs `fullName`
- `photoUrl` vs `profileImageUrl`
- `applicationCount` vs `applicationsCount`
- `postedAt` vs `createdAt`
- `geoHash` vs `geohash`
- `job_applications` (current) vs `applications` (legacy)

---

## Final note

This is the deepest possible static code/schema extraction from the current repository state. If you want, next I can generate a second file with:
- one migration plan per inconsistent field pair,
- exact Firestore backfill script pseudo-code,
- and safe rollout sequence (read-old/write-new, dual-write, cutover, cleanup).
