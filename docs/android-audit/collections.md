
Symbols: ✅ canonical writer in current code · 🔧 Cloud Function only · 🧑‍💼 web admin only · 📱 Android only · ⚠️ legacy (read but no writer)

1. users/{userId}
Writers: 📱 UserFirestoreService.createOrUpdateUser, ProfileCompletionService.kt:640, FCMTokenManager, LocaleHelper, 🧑‍💼 web/app/api/admin/users/route.ts, 🔧 referral-system.ts.

Field	Type	Source
userId	string	doc id mirror
phone	string	normalized via PhoneNumberUtils
fullName	string	
profileImageUrl	string?	only when non-blank
roles	string[]	from User.roleFieldsFor() — ["WORKER"] or ["EMPLOYER"]
activeRole	"WORKER" | "EMPLOYER"	
location	{lat:number, lng:number}	always written by createOrUpdateUser
geohash	string	GeoUtils.encodeGeohash
fcmToken	string?	written by FCM token manager
language	"en" \| "te"	LocaleHelper
isActive	boolean?	only via updateUserProfile
isVerified	boolean?	only via updateUserProfile
createdAt	Timestamp	
lastActiveAt	Timestamp	written on every update
referralCode	string?	🔧 ReferralService
referredByCode, referredByUserId	string?	🔧 set during signup-bonus tx
⚠️ role	string	legacy single-role; some readers still fall back
Embedded referralStats map mirror is deprecated — current code writes to top-level referral-system.ts:301.

2. worker_profiles/{userId}
Writer: 📱 ProfileCompletionService.saveWorkerProfileData.

Field	Type	Notes
userId	string	doc id
skills	string[]	(also read as jobTypes legacy fallback)
isAvailable	boolean	default true
dateOfBirth	string?	only when provided
gender	string?	
experience	string?	
lastActiveAt	Timestamp	
🔧 rating, totalRatings, totalJobs	number	CF aggregates only
⚠️ legacy: jobTypes[] (still read, no longer written).

3. employer_profiles/{userId}
Writer: 📱 ProfileCompletionService.saveEmployerProfileData, 🧑‍💼 route.ts (merge:true).

Field	Type
userId	string
companyName	string
lastActiveAt	Timestamp
isVerified	boolean (admin merge sets true)
🔧 rating, totalRatings, totalHires	number
4. jobmetadata/{jobId} — card data, ~200 bytes
Writer: 📱 JobFirestoreService.kt:388, 🧑‍💼 POST /api/admin/jobs.

Field	Type
employerId	string
title	string
companyName	string (resolved from employer profile)
jobType	string
salary	number
salaryType	"HOURLY"|"DAILY"|"WEEKLY"|"MONTHLY"|"FIXED"
location	{lat, lng}
geohash	string
addressText	string
companyCity	string
urgency	"LOW"|"MEDIUM"|"HIGH"
status	"open"|"closed"|"expired" (admin-only writer can also set explicit)
createdAt	Timestamp
expiresAt	Timestamp (default +15 days)
🧑‍💼 admin-only extra	isVerified
⚠️ legacy fallbacks readers still tolerate: payAmount, payType, address, latitude, longitude, isActive, isFilled, category, company, employerName, businessName, company_name.

5. job_details/{jobId} — full data, ~1KB
Writer: same Android batch + admin POST.

Field	Type
description	string
contactNumber	string
gender	"Any"|"Male"|"Female"
experienceRequired	string
shiftTiming	string
vacancies	number
benefits	string[]
applicationCount	number (init 0; CF increments on apply)
whatsappNumber?	string
workingHours?	string
6. applications/{jobId_workerId}
Writer: 📱 JobApplication.toFirestoreMap (strict 5-field schema).

Field	Type
jobId	string
workerId	string
employerId	string
status	"applied"|"shortlisted"|"rejected"|"hired"
createdAt	Timestamp
Status-only mutations via update("status", …). No other fields are written client-side anymore — UI enrichment (workerName, jobTitle, coverLetter, etc.) lives only in memory / Room cache, never persisted to Firestore.

7. saved_jobs/{userId_jobId}
Writer: 📱 ApplicationFirestoreService.saveJob.

Field	Type
userId	string
jobId	string
createdAt	Timestamp
8. ratings/{jobId_fromUserId}
Writer: 📱 RatingService.submitRating.

Field	Type
jobId	string
fromUserId	string
toUserId	string
rating	number
review	string (may be empty)
createdAt	Timestamp
9. job_reports/{reporterId_jobId}
Writer: 📱 ReportingService.kt:117, 🔧 index.ts adds moderation fields.

Field	Type
jobId	string
reporterId	string
reportType	"SCAM"|"FAKE"|"INAPPROPRIATE"|"DUPLICATE"|"MISLEADING"|"HARASSMENT"|"SPAM"|"OTHER"
description	string
status	"PENDING" initially; CF flips to processed values
createdAt	Timestamp
🔧 reviewedBy?, reviewedAt?, actionTaken?	from CF moderation
10. notifications/{notificationId}
Writers: 📱 NotificationService.sendNotification (gated by CLIENT_NOTIFICATION_WRITES_ENABLED; in current build only self-notifications persist), 🔧 index.ts, notification-fanout.ts, referral-system.ts, 🧑‍💼 /api/admin/notifications.

Field	Type
recipientId	string
title	string
message	string
type	string (NotificationType.name from app, or "SYSTEM_UPDATE"/"GENERAL" from admin)
data	map (see below)
isRead	boolean (init false)
createdAt	Timestamp
expiresAt	Timestamp/number (admin: createdAt + 45d)
targetRole?	string (admin route)
locale?	"en"\|"te" (admin route, when localized)
data map common keys: deepLink, notificationId, targetRole, jobId, applicationId, workerId, referralId, amount, preset, source, pushTopic.

11. admin_notification_campaigns/{id} 🧑‍💼
Writer: POST /api/admin/notifications.

Field	Type
title, message, type, preset, targetRole, pushTopic, deepLink	string
sendPush	boolean
recipientCount	number
translations	{ en?: {title, message}, te?: {title, message} } \| null
createdAt, updatedAt	Date
12. announcements/{id} 🧑‍💼
Writer: route.ts:102.

Field	Type
title	string
message	string
type	"INFO"|"SUCCESS"|"WARNING"|"ERROR"|"FEATURE"|"PROMOTION"
priority	"LOW"|"MEDIUM"|"NORMAL"|"HIGH"|"URGENT"
targetRole	"ALL"|"WORKER"|"EMPLOYER"
imageUrl	string ("" when absent)
deepLink?, actionText?	string
isActive	boolean (init true)
createdAt, expiresAt	Timestamp
13. referrals/{referralId}
Writer: 🔧 referral-system.ts:850 (processReferralOnSignup + fraud detector).

Field	Type
id	string
idempotencyKey	string
referrerId	string
referrerUserId	string (legacy alias, also set)
referredUserId	string
referralCode	string
status	"PENDING"|"COMPLETED"|"REJECTED"|"EXPIRED"|"FLAGGED"|"PASSED"
rewardAmount	number
bonusAmount	number
referredUserReward	number
reward	number (= referrer total)
referredUserName	string
referredUserRole	string
createdAt, completedAt	Timestamp
Fraud-detector adds: fraudScore, fraudSignals[], fraudCheckedAt, rejectionReason, deviceFingerprint?, ipAddress?, expiresAt?, needsReview?	
14. referral_stats/{userId} 🔧
Writer: referral-system.ts (atomic increments).

Field	Type
userId, userRole, referralCode	string
totalReferrals, successfulReferrals, pendingReferrals, expiredReferrals, rejectedReferrals	number
totalEarnings, pendingEarnings, availableBalance, withdrawnAmount	number
canWithdraw	boolean
currentTier, nextMilestone	string/number
freeJobPostings	number
freeJobPostingsExpiry	Timestamp | null
signupBonusReceived	boolean
signupBonusAmount	number
totalWithdrawals	number
lastWithdrawalAt, lastUpdated	Timestamp
referredByCode?, referredByUserId?	string
15. referral_codes/{code} 🔧
Writer: referral-system.ts.

Field	Type
code	string (doc id)
userId	string
userRole	"WORKER"|"EMPLOYER"
userName	string
isActive	boolean
createdAt	Timestamp
totalUsed, successfulReferrals	number (incremented on completed referral)
16. users/{userId}/withdrawals/{withdrawalId} 🔧
Writer: referral-system.ts requestWithdrawal callable (line 1402).

Field	Type
id, userId, userRole	string
amount	number
status	"PENDING" (CF/admin later: "COMPLETED"|"FAILED")
paymentMethod	"UPI"|"BANK_TRANSFER"
upiId	string | null
bankAccountNumber	string | null
ifscCode	string | null
accountHolderName	string | null
createdAt	Timestamp (server)
17. referrals/{referralId}/audit_logs/{id} 🔧
Field	Type
eventType	"REWARD_CREDITED"|"SIGNUP_BONUS_CREDITED"|"REFERRAL_EXPIRED"
userId, userRole?	string
amount?, bonusAmount?	number
referralId?, referrerId?, referredUserId?, withdrawalId?, newTier?	string
timestamp	Timestamp (server)
18. users/{userId}/audit_logs/{id} 🔧
Currently only event:

eventType: "WITHDRAWAL_REQUESTED"
userId, withdrawalId: string
amount: number
timestamp: Timestamp
(Legacy stats mirror writes to referral_stats doc as legacyStatsRef — same fields as above, kept for backward read-compat.)

19. notification_tracking/{userId}/sent/{id} 🔧
Writer: scheduled-notifications.ts trackNotificationSent / WithKey.

Field	Type
type	string (e.g. BIRTHDAY, JOB_EXPIRY, PENDING_APPLICATIONS, SMART_ENGAGEMENT)
sentAt	number (epoch ms)
key?	string (dedup key)
20. app_config/referral 🔧
Writer: app-config.ts:115 (single doc; admin-mutable feature flags & rewards).

Schema is whatever app-config.ts writes (typed as ReferralConfig). Loaded by both functions and Android via RemoteConfigService. No fixed top-level field list; treat the whole doc as a typed JSON payload (rewards table, tier thresholds, fraud thresholds).

21. Collections that DON'T exist in current code (but appear in older docs)
Confirmed by grep: no writer + not in firestore.rules. Either drop them from the schema doc or migrate any stale data.

conversations
messages
work_locations (only string id work_locations in strings.xml)
withdrawal_requests (current path is users/{uid}/withdrawals)
top-level applications extended fields (workerName, verification.*, coverLetter, statusHistory, verificationCode, etc.) — none are written by current Android/CF code
Schema-divergence cleanup targets (live, not historical)
⚠️ users.role (single-role legacy) vs users.roles[] + activeRole — User.roleFieldsFor() writes both; readers should consume activeRole only.
⚠️ Embedded users.referralStats mirror — no canonical writer; safe to delete from existing docs.
⚠️ worker_profiles.jobTypes[] — no writer; some readers still fall back to it. Migrate by copying to skills once.
⚠️ jobmetadata legacy fields (payAmount/payType/address/latitude/longitude/category/isActive/isFilled) — readers fallback only; new writes use canonical.
⚠️ applications enrichment fields stored historically (workerName, jobTitle, verification map) — current writer no longer emits them; safe to leave but new code should not depend on them.
This is the actual live shape produced by the current codebase (Apr 21, 2026). Anything not listed here is not being written by any current code path.

Continued with Autopilot: Task not yet complete

Live audit delivered: 20 active collections grouped by writer (Android service, Cloud Function, web admin), each with exact field shapes pulled from the current code (firestore.rules, functions/src/*.ts, Android services/firestore/* + ProfileCompletionService + JobApplicationModels + RatingService + ReportingService + NotificationService, and web/app/api/admin/*). Also flagged 5 collections from older docs that have no writer or rule today (conversations, messages, work_locations, withdrawal_requests, extended applications enrichment fields) and 5 live divergence-cleanup targets.
