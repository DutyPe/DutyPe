# Firebase Collections Inventory

Last updated: 2026-04-14

## Important Note

This inventory is exact from the current DutyPe codebase, Firestore rules, Cloud Functions, Android app, and web admin/product code.

This is **not** a live Firestore dump. That means:

- `Canonical` = fields explicitly allowed or expected by `firestore.rules` and the main active write paths.
- `Supportive / nested` = fields actively written by app, web, or functions to support product behavior.
- `Legacy / fallback / divergent` = fields still read, normalized, or written by older/admin code paths and therefore may still exist in production data.

## Primary Sources Reviewed

- `firestore.rules`
- `functions/src/referral-system.ts`
- `functions/src/index.ts`
- `functions/src/scheduled-notifications.ts`
- `app/src/main/java/com/example/dutype/models/*`
- `app/src/main/java/com/example/dutype/services/*`
- `app/src/main/java/com/example/dutype/services/firestore/*`
- `web/app/api/admin/*`
- `web/lib/product/*`
- `web/lib/firebase/*`

## Collections And Paths

### `users`

Document id: `userId`

Canonical fields:

- `userId: string`
- `phone: string`
- `fullName: string`
- `profileImageUrl?: string`
- `roles: string[]`
- `activeRole: string`
- `location?: { lat: number, lng: number }`
- `geohash?: string`
- `isVerified: boolean`
- `isActive: boolean`
- `fcmToken?: string`
- `createdAt: timestamp`
- `lastActiveAt: timestamp`
- `referralCode?: string`
- `referredByCode?: string`
- `referredByUserId?: string`

Supportive / nested fields actively written:

- `referralStats?: { ... }`
- `referralStats.totalReferrals: number`
- `referralStats.successfulReferrals: number`
- `referralStats.pendingReferrals: number`
- `referralStats.expiredReferrals: number`
- `referralStats.rejectedReferrals: number`
- `referralStats.totalEarnings: number`
- `referralStats.pendingEarnings: number`
- `referralStats.withdrawnAmount: number`
- `referralStats.availableBalance: number`
- `referralStats.canWithdraw: boolean`
- `referralStats.nextMilestone: number`
- `referralStats.currentTier: string`
- `referralStats.freeJobPostings: number`
- `referralStats.freeJobPostingsExpiry: timestamp | null`
- `referralStats.signupBonusReceived: boolean`
- `referralStats.signupBonusAmount: number`
- `referralStats.totalWithdrawals: number`
- `referralStats.isBlocked: boolean`
- `referralStats.blockReason: string | null`
- `referralStats.userId?: string`
- `referralStats.userRole?: string`
- `referralStats.referralCode?: string`
- `referralStats.referredByCode?: string`
- `referralStats.referredByUserId?: string`
- `referralStats.lastWithdrawalAt?: timestamp`
- `referralStats.lastUpdated?: timestamp`
- `referralCodeCreatedAt?: timestamp`

Legacy / fallback / divergent fields still seen in code:

- `role?: string`
- `pendingReferrals?: number`
- `expiredReferrals?: number`
- `rejectedReferrals?: number`
- `availableBalance?: number`
- `withdrawnAmount?: number`
- `lastWithdrawalAt?: timestamp`
- `totalWithdrawals?: number`
- `lastUpdated?: timestamp`
- `userRole?: string`
- `profileCompleted?: boolean` read by some UI/admin layers but not a current canonical write

---

### `worker_profiles`

Document id: `userId`

Canonical fields:

- `userId: string`
- `jobTypes?: string[]`
- `skills?: string[]`
- `isAvailable: boolean`
- `rating: number`
- `totalRatings: number`
- `totalJobs: number`
- `lastActiveAt: timestamp`

Important behavior:

- Current Android flow commonly writes both `skills` and `jobTypes`.
- Web product signup currently writes `jobTypes` and readers frequently use `skills ?? jobTypes`.

---

### `employer_profiles`

Document id: `userId`

Canonical fields:

- `userId: string`
- `companyName: string`
- `rating: number`
- `totalRatings: number`
- `totalHires: number`
- `isVerified?: boolean`
- `lastActiveAt?: timestamp`

---

### `jobs`

Document id: generated Firestore id; some client code also stores or expects `jobId`

Canonical fields:

- `employerId: string`
- `companyName?: string`
- `isVerified?: boolean`
- `title: string`
- `jobType?: string`
- `salary: number`
- `salaryType: string`
- `gender?: string`
- `experienceRequired?: string`
- `shiftTiming?: string`
- `applicationCount?: number`
- `location: { lat: number, lng: number }`
- `geohash: string`
- `companyCity?: string`
- `addressText?: string`
- `urgency: "LOW" | "MEDIUM" | "HIGH"`
- `status: "open" | "closed" | "expired"`
- `createdAt: timestamp`
- `expiresAt: timestamp`

Supportive / actively used extra fields:

- `jobId?: string`
- `description?: string`
- `contactNumber?: string`
- `whatsappNumber?: string`
- `workingHours?: string`
- `educationRequired?: string`
- `benefits?: string[]`
- `vacancies?: number`
- `updatedAt?: timestamp`
- `lastApplicationAt?: timestamp | number`
- `hasActiveWorker?: boolean`
- `lastWorkerStartedAt?: timestamp | number`
- `moderationStatus?: string`
- `idempotencyKey?: string`

Legacy / fallback / admin-console divergent fields:

- `location?: string` legacy string form written by admin API
- `category?: string`
- `payAmount?: string | number`
- `payType?: string`
- `shift?: string`
- `requirements?: string`
- `contactPhone?: string`
- `contactEmail?: string`
- `isActive?: boolean`
- `postedBy?: string`
- `source?: string`
- `company?: string`
- `employerName?: string`
- `businessName?: string`
- `company_name?: string`
- `latitude?: number`
- `longitude?: number`
- `address?: string`
- `isFilled?: boolean`
- `postedAt?: timestamp | number`

---

### `job_details`

Document id: usually same id as the matching `jobs/{jobId}` document

Canonical fields:

- `employerId?: string`
- `companyName?: string`
- `isVerified?: boolean`
- `title?: string`
- `jobType: string`
- `salary?: number`
- `salaryType?: string`
- `description: string`
- `contactNumber: string`
- `addressText: string`
- `companyCity?: string`
- `location?: { lat: number, lng: number }`
- `geohash?: string`
- `gender?: string`
- `experienceRequired?: string`
- `shiftTiming?: string`
- `vacancies: number`
- `benefits: string[]`
- `urgency?: string`
- `applicationCount?: number`
- `status?: string`
- `createdAt?: timestamp`
- `expiresAt?: timestamp`
- `whatsappNumber?: string`
- `workingHours?: string`
- `educationRequired?: string`

---

### `applications`

Document id:

- Rules canonical format: `${jobId}_${workerId}`
- Some web/admin normalizers also carry explicit `applicationId` or `id`

Canonical rules-backed fields:

- `jobId: string`
- `workerId: string`
- `employerId: string`
- `status: "applied" | "shortlisted" | "rejected" | "hired"`
- `createdAt: timestamp`

Supportive / actively used current fields:

- `applicationId?: string`
- `id?: string`
- `appliedAt?: timestamp | number`
- `updatedAt?: timestamp | number`
- `completedAt?: timestamp | number`
- `workStartedAt?: timestamp | number`
- `workerName?: string`
- `companyName?: string`
- `jobTitle?: string`
- `jobLocation?: string`
- `coverLetter?: string`
- `source?: string`
- `active?: boolean`
- `statusHistory?: Array<{ notes?: string, status?: string, systemUpdate?: boolean, timestamp?: timestamp | number, updatedAt?: timestamp | number, updatedBy?: string }>`
- `verification?: { ... }`
- `verificationCode?: string`
- `verificationId?: string`
- `verificationStatus?: string`

Embedded `verification` object shape:

- `verification.verificationId: string`
- `verification.jobId: string`
- `verification.applicationId: string`
- `verification.workerId: string`
- `verification.employerId: string`
- `verification.verificationCode: string`
- `verification.qrCodeData: string`
- `verification.status: "PENDING" | "VERIFIED" | "EXPIRED" | "CANCELLED"`
- `verification.generatedAt: number`
- `verification.verifiedAt: number | null`
- `verification.verifiedByEmployerId: string | null`
- `verification.verifiedLocation: { lat: number, lng: number } | null`
- `verification.expiresAt: number`
- `verification.workerName: string`
- `verification.jobTitle: string`
- `verification.employerName: string`

Legacy / enrichment / reader fallback fields seen in models and normalizers:

- `workerEmail?: string`
- `workerPhone?: string`
- `workerLocation?: string`
- `workerProfileImageUrl?: string`
- `workerLocalRating?: number`
- `workerTotalReviews?: number`
- `workerJobsInArea?: number`
- `workerAadhaarVerified?: boolean`
- `workerPhoneVerified?: boolean`
- `workerIdentityVerified?: boolean`
- `workerBackgroundCheckPassed?: boolean`
- `workerDateOfBirth?: string`
- `workerGender?: string`
- `skills?: string[]`
- `skillsText?: string`
- `workExperience?: string`
- `workExperienceText?: string`
- `education?: string`
- `certifications?: string[]`
- `languages?: string[]`
- `availability?: string`
- `expectedSalary?: string | number`
- `resumeUrl?: string`
- `additionalDocuments?: string[]`

---

### `saved_jobs`

Document id: typically `${userId}_${jobId}`

Canonical fields:

- `id?: string`
- `userId?: string`
- `workerId?: string` legacy owner alias
- `jobId: string`
- `createdAt: timestamp`

Important divergence:

- Some web writes use numeric epoch time for `createdAt`; rules expect Firestore `timestamp`.

---

### `ratings`

Document id: generated Firestore id

Canonical rules-backed fields:

- `jobId: string`
- `fromUserId: string`
- `toUserId: string`
- `rating: number`
- `review?: string`
- `createdAt: timestamp`

Supportive / current richer rating payload used by web review flow:

- `applicationId?: string`
- `communicationRating?: number`
- `companyName?: string`
- `feedback?: string`
- `isActive?: boolean`
- `jobTitle?: string`
- `overallRating?: number`
- `paymentRating?: number`
- `professionalismRating?: number`
- `punctualityRating?: number`
- `qualityRating?: number`
- `ratedUserId?: string`
- `ratedUserRole?: string`
- `raterUserId?: string`
- `raterUserRole?: string`
- `ratingId?: string`
- `tags?: string[]`

---

### `job_reports`

Document id: rules expect `${reporterId}_${jobId}`

Canonical fields:

- `reportId?: string`
- `jobId: string`
- `reporterId: string`
- `reporterPhone?: string`
- `reportType: "SCAM" | "FAKE" | "INAPPROPRIATE" | "DUPLICATE" | "MISLEADING" | "HARASSMENT" | "SPAM" | "OTHER"`
- `description: string`
- `timestamp: number`
- `status: string`

Supportive moderation fields seen in processing/admin code:

- `reviewedBy?: string`
- `reviewedAt?: timestamp`
- `actionTaken?: string`

Related side effect:

- Report moderation can also write `jobs/{jobId}.moderationStatus`.

---

### `referrals`

Document id:

- Rules format: `${referrerId}_${referredUserId}`
- Generated ids may also exist in older/admin paths

Canonical rules-backed fields:

- `referrerId: string`
- `referredUserId: string`
- `referralCode?: string`
- `status: string`
- `reward?: number`
- `createdAt: timestamp`

Supportive / active fields written by functions and app flows:

- `id?: string`
- `referrerUserId?: string` legacy alias read in some models
- `rewardAmount?: number`
- `bonusAmount?: number`
- `referredUserReward?: number`
- `completedAt?: timestamp`
- `deviceFingerprint?: string | null`
- `ipAddress?: string | null`
- `expiresAt?: timestamp`
- `fraudScore?: number`
- `fraudSignals?: string[]`
- `needsReview?: boolean`
- `fraudCheckedAt?: timestamp`
- `rejectionReason?: string`
- `referredUserName?: string`
- `referredUserRole?: string`

Statuses seen in code:

- `pending`
- `PENDING`
- `COMPLETED`
- `EXPIRED`
- `REJECTED`
- `FLAGGED`
- `PASSED`

---

### `referral_stats`

Document id: `userId`

Active fields:

- `userId: string`
- `userRole: string`
- `userName: string`
- `referralCode: string`
- `totalReferrals: number`
- `successfulReferrals: number`
- `pendingReferrals: number`
- `expiredReferrals: number`
- `rejectedReferrals: number`
- `totalEarnings: number`
- `pendingEarnings: number`
- `withdrawnAmount: number`
- `availableBalance: number`
- `canWithdraw: boolean`
- `nextMilestone: number`
- `currentTier: string`
- `freeJobPostings: number`
- `freeJobPostingsExpiry: timestamp | null`
- `signupBonusReceived: boolean`
- `signupBonusAmount: number`
- `totalWithdrawals: number`
- `isBlocked: boolean`
- `blockReason: string | null`
- `referredByCode?: string`
- `referredByUserId?: string`
- `lastWithdrawalAt?: timestamp`
- `lastUpdated?: timestamp`

---

### `referral_codes`

Document id: referral code string itself

Canonical fields:

- `code: string`
- `userId: string`
- `userRole: "WORKER" | "EMPLOYER"`
- `userName: string`
- `isActive: boolean`
- `createdAt: timestamp`

Supportive / legacy fields:

- `userRoles?: string[]`
- `totalUsed?: number`
- `successfulReferrals?: number`

---

### `users/{userId}/withdrawals`

Document id: generated withdrawal id

Active fields:

- `id: string`
- `userId: string`
- `userRole: string`
- `amount: number`
- `status: string`
- `paymentMethod: "UPI" | "BANK_TRANSFER"`
- `upiId: string | null`
- `bankAccountNumber: string | null`
- `ifscCode: string | null`
- `accountHolderName: string | null`
- `createdAt: timestamp`

Observed statuses:

- `PENDING`
- admin tools also use `COMPLETED`
- admin tools also use `FAILED`

---

### `withdrawal_requests`

Document id: generated withdrawal id

Status:

- This is a legacy or admin-divergent top-level collection still used by `web/app/api/admin/referrals/route.ts`.
- Current referral Cloud Functions write to `users/{userId}/withdrawals`, not here.

Fields confirmed in current admin code:

- `status?: string`
- `processedAt?: timestamp`
- `updatedAt?: timestamp`

Fields likely present from older withdrawal payloads and admin UI expectations:

- `id?: string`
- `userId?: string`
- `amount?: number`
- `paymentMethod?: string`
- `upiId?: string`
- `createdAt?: timestamp`
- `transactionId?: string`

---

### `announcements`

Document id: generated Firestore id

Fields actively written by current admin API:

- `title: string`
- `message: string`
- `type: string`
- `targetRole: string`
- `isActive: boolean`
- `priority: number`
- `createdAt: timestamp`
- `updatedAt: timestamp`

Additional fields supported by Android announcement model and readers:

- `actionText?: string`
- `actionRoute?: string`
- `imageUrl?: string`
- `startDate?: timestamp`
- `endDate?: timestamp | null`
- `isDismissible?: boolean`
- `createdBy?: string`

---

### `notifications`

Document id: generated Firestore id

Canonical rules-backed fields:

- `recipientId: string`
- `title: string`
- `message: string`
- `type: string`
- `data: map`
- `isRead: boolean`
- `createdAt: timestamp`

Supportive / processing fields written or read by functions and app:

- `expiresAt?: timestamp | number`
- `sentAt?: timestamp`
- `fcmMessageId?: string`
- `status?: string`
- `processing?: boolean`
- `processingStartedAt?: timestamp`
- `error?: string`
- `targetRole?: string` present in Android model, often carried inside `data` instead of top-level

Common keys stored inside `data`:

- `deepLink`
- `notificationId`
- `targetRole`
- `jobId`
- `applicationId`
- `conversationId`
- `amount`
- `referralId`
- `reportCount`
- `userName`

---

### `conversations`

Document id: generated Firestore id

No active creator was found in the current repo, but rules and web readers expect these fields:

- `participants: string[]`
- `participantDetails?: Record<string, { name?: string, profileImage?: string, role?: string }>`
- `jobId?: string`
- `lastMessage?: string`
- `lastMessageAt?: timestamp | number`
- `lastMessageBy?: string`
- `unreadCount?: Record<string, number>`
- `createdAt?: timestamp | number`
- `updatedAt?: timestamp | number`

---

### `messages`

Document id: generated Firestore id

No active creator was found in the current repo, but rules and web readers expect these fields:

- `conversationId: string`
- `senderId: string`
- `recipientId: string`
- `message: string`
- `type: string`
- `isRead: boolean`
- `createdAt: timestamp | number`

---

### `work_locations/{userId}/locations`

Document id: generated Firestore id

Canonical fields:

- `label: string`
- `address: string`
- `normalizedAddress: string`
- `latitude: number`
- `longitude: number`
- `addedAt: number`
- `updatedAt: number`
- `usageCount: number`

---

### `notification_tracking/{userId}/sent`

Document id: generated Firestore id

Fields:

- `type: string`
- `sentAt: number`
- `key?: string`
- `applicationId?: string`

Purpose:

- Used by scheduled notifications for deduplication and daily rate limiting.

---

### `referrals/{referralId}/audit_logs`

Document id: generated Firestore id

Observed event fields across referral functions:

- `eventType: string`
- `userId?: string`
- `userRole?: string`
- `referralId?: string`
- `amount?: number`
- `bonusAmount?: number`
- `newTier?: string`
- `withdrawalId?: string`
- `referrerId?: string`
- `referredUserId?: string`
- `timestamp: timestamp`

Observed event types:

- `REWARD_CREDITED`
- `SIGNUP_BONUS_CREDITED`
- `REFERRAL_EXPIRED`

---

### `users/{userId}/audit_logs`

Document id: generated Firestore id

Observed event fields:

- `eventType: string`
- `userId: string`
- `withdrawalId?: string`
- `amount?: number`
- `timestamp: timestamp`

Observed event types:

- `WITHDRAWAL_REQUESTED`

## Short Summary

If you need the safest schema to build against today, use these as primary collections:

- `users`
- `worker_profiles`
- `employer_profiles`
- `jobs`
- `job_details`
- `applications`
- `saved_jobs`
- `ratings`
- `job_reports`
- `referrals`
- `referral_stats`
- `referral_codes`
- `announcements`
- `notifications`

If you need cleanup or migration targets, the main divergent areas are:

- `users.role` versus `users.roles` and `users.activeRole`
- embedded `users.referralStats` versus top-level `referral_stats`
- `users/{userId}/withdrawals` versus top-level `withdrawal_requests`
- canonical `jobs.location` object versus admin legacy `jobs.location` string
- canonical `jobs.salary` and `jobs.salaryType` versus legacy `payAmount` and `payType`
- minimal rules-backed `applications` shape versus richer live application documents used by product flows
