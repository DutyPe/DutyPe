# Current DB Schema (Android Source of Truth)

Generated on: 2026-03-19

This document lists the current database schema used by the Android app, derived from active Kotlin write/read paths and model definitions.

## Firestore Collections

### users
Document ID: userId

Fields:
- phone
- fullName
- profileImageUrl
- roles (array)
- activeRole
- location.lat
- location.lng
- geohash
- isVerified
- isActive
- fcmToken
- createdAt
- lastActiveAt

Additional fields used by referral flows:
- referralCode
- referralStats (object)
  - totalReferrals
  - successfulReferrals
  - pendingReferrals
  - expiredReferrals
  - rejectedReferrals
  - totalEarnings
  - pendingEarnings
  - withdrawnAmount
  - availableBalance
  - canWithdraw
  - nextMilestone
  - currentTier
  - freeJobPostings
  - freeJobPostingsExpiry
  - lastUpdated
  - referredByCode
  - referredByUserId
  - lastWithdrawalAt
  - totalWithdrawals
  - isBlocked
  - blockReason

Primary sources:
- app/src/main/java/com/example/dutype/services/firestore/UserFirestoreService.kt
- app/src/main/java/com/example/dutype/services/ReferralService.kt
- app/src/main/java/com/example/dutype/models/ReferralModels.kt

### jobs
Document ID: jobId

Fields:
- employerId
- title
- jobType
- salary
- salaryType
- location.lat
- location.lng
- geohash
- urgency
- status
- createdAt
- expiresAt

Primary source:
- app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt

### job_details
Document ID: jobId (same id as jobs document)

Fields:
- description
- contactNumber
- addressText

Primary source:
- app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt

### applications
Document ID: applicationId (typically jobId_workerId)

Fields:
- jobId
- workerId
- employerId
- status
- createdAt

Primary source:
- app/src/main/java/com/example/dutype/models/JobApplicationModels.kt

### saved_jobs
Document ID: userId_jobId

Fields:
- userId
- jobId
- createdAt

Primary source:
- app/src/main/java/com/example/dutype/services/firestore/ApplicationFirestoreService.kt

### worker_profiles
Document ID: userId

Fields:
- userId
- jobTypes (array)
- isAvailable
- rating
- totalRatings
- totalJobs
- lastActiveAt

Primary source:
- app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt

### employer_profiles
Document ID: userId

Fields:
- userId
- companyName
- rating
- totalRatings
- totalHires

Primary source:
- app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt

### notifications
Document ID: generated uuid

Fields:
- id
- recipientId
- title
- message
- type
- targetRole
- data (map)
- createdAt
- expiresAt
- isRead

Primary sources:
- app/src/main/java/com/example/dutype/models/NotificationModels.kt
- app/src/main/java/com/example/dutype/services/NotificationService.kt

### announcements
Document ID: announcementId

Fields:
- title
- message
- type
- priority
- targetRole
- actionText
- actionRoute
- imageUrl
- startDate
- endDate
- isDismissible
- isActive
- createdBy
- createdAt

Primary sources:
- app/src/main/java/com/example/dutype/models/Announcement.kt
- app/src/main/java/com/example/dutype/services/AnnouncementService.kt

### referral_codes
Document ID: referralCode

Fields:
- code
- userId
- userRole
- userName
- isActive
- createdAt

Primary source:
- app/src/main/java/com/example/dutype/models/ReferralModels.kt

### referrals
Document ID: referralId

Fields:
- id
- referrerUserId
- referredUserId
- referralCode
- status
- rewardAmount
- bonusAmount
- referredUserReward
- createdAt
- completedAt
- deviceFingerprint
- referredUserName
- referredUserRole

Primary source:
- app/src/main/java/com/example/dutype/models/ReferralModels.kt

### users/{userId}/withdrawals (subcollection)
Document ID: withdrawalId

Fields:
- id
- userId
- amount
- status
- paymentMethod
- upiId
- createdAt
- processedAt
- transactionId

Primary sources:
- app/src/main/java/com/example/dutype/models/ReferralModels.kt
- app/src/main/java/com/example/dutype/services/ReferralService.kt

### work_locations/{userId}/locations (subcollection)
Document ID: locationId

Fields:
- label
- address
- latitude
- longitude
- addedAt
- usageCount

Primary source:
- app/src/main/java/com/example/dutype/services/WorkLocationManager.kt

### app_feedback
Document ID: auto-generated

Fields:
- userId
- userEmail
- userRole
- rating
- category
- feedback
- timestamp
- appVersion
- appVersionCode
- platform
- deviceModel
- androidVersion

Primary source:
- app/src/main/java/com/example/dutype/components/FeedbackBottomSheet.kt

## Room (Local Android DB)

Database:
- name: dutype_database
- version: 6

Primary source:
- app/src/main/java/com/example/dutype/database/DutyPeDatabase.kt

### jobs table
Fields:
- id
- employerId
- title
- jobType
- salary
- salaryType
- lat
- lng
- geohash
- urgency
- status
- createdAt
- expiresAt
- description
- contactNumber
- addressText
- isSynced
- cachedAt

Primary source:
- app/src/main/java/com/example/dutype/database/entity/JobEntity.kt

### applications table
Fields:
- applicationId
- jobId
- workerId
- employerId
- status
- appliedAt
- updatedAt
- jobTitle
- jobLocation
- companyName
- workerName
- workerPhone
- coverLetter
- cachedAt
- isSynced
- isPendingSubmission

Primary source:
- app/src/main/java/com/example/dutype/database/entity/ApplicationEntity.kt

### saved_jobs table
Fields:
- id
- workerId
- jobId
- savedAt
- isActive
- cachedAt
- isSynced
- pendingAction

Primary source:
- app/src/main/java/com/example/dutype/database/entity/SavedJobEntity.kt
