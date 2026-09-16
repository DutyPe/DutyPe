# DutyPe Complete Field Mapping - All Collections

**Generated:** March 11, 2026  
**Purpose:** Complete reference of all database fields across all collections

---

## Core Collections

### 1. users (20 fields)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | id | string | ✅ (PK) | Read/Write/Filter | Primary key |
| 2 | phone | string | ✅ (Unique) | Read/Write/Filter | Auth identifier |
| 3 | fullName | string | | Read/Write/Display | Display name |
| 4 | email | string | ✅ (Sparse) | Read/Write/Filter | Optional contact |
| 5 | profileImageUrl | string | | Read/Write/Display | Avatar URL |
| 6 | roles | array | ✅ | Read/Write/Filter | ["WORKER", "EMPLOYER"] |
| 7 | activeRole | string | ✅ | Read/Write/Filter | Current active role |
| 8 | latitude | number | | Read/Write/Filter | Current location |
| 9 | longitude | number | | Read/Write/Filter | Current location |
| 10 | address | string | | Read/Write/Display | Formatted address |
| 11 | workLocations | array | | Read/Write | Saved work locations |
| 12 | bio | string | | Read/Write/Display | Profile bio |
| 13 | skills | string | | Read/Write/Display | Comma-separated |
| 14 | experience | string | | Read/Write/Display | Work experience |
| 15 | companyName | string | | Read/Write/Display | Employer only |
| 16 | trustTier | string | | Read/Display | NEW/BRONZE/SILVER/GOLD |
| 17 | fcmToken | string | | Read/Write | Push notifications |
| 18 | createdAt | timestamp | | Read/Filter | Account creation |
| 19 | isActive | boolean | ✅ | Read/Filter | Account status |
| 20 | profileCompleted | boolean | ✅ | Read/Filter | Onboarding status |
| 21 | referralCode | string | ✅ (Unique) | Read/Display | User's referral code |
| 22 | referralStats | object | | Read/Write | Embedded stats |
| 23 | savedJobs | array | | Read/Write | Saved job IDs |

**Total:** 23 fields (20 core + 3 denormalized)

---

### 2. jobs (18 fields)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | id | string | ✅ (PK) | Read/Write/Filter | Primary key |
| 2 | employerId | string | ✅ | Read/Write/Filter | Owner |
| 3 | title | string | | Read/Write/Filter/Display | Job title |
| 4 | companyName | string | | Read/Write/Display | Denormalized |
| 5 | description | string | | Read/Write/Display | Job description |
| 6 | location | string | | Read/Write/Filter/Display | Location name |
| 7 | latitude | number | ✅ | Read/Write/Filter | Geo queries |
| 8 | longitude | number | ✅ | Read/Write/Filter | Geo queries |
| 9 | payAmount | string | | Read/Write/Filter/Display | "400", "15000" |
| 10 | payType | string | ✅ | Read/Write/Filter | HOURLY/DAILY/MONTHLY |
| 11 | shiftTiming | string | | Read/Write/Display | Shift details |
| 12 | isActive | boolean | ✅ | Read/Write/Filter | Job status |
| 13 | isFilled | boolean | | Read/Write/Filter | Vacancy status |
| 14 | postedAt | timestamp | ✅ | Read/Filter | Posting time |
| 15 | contactNumber | string | | Read/Write/Display | Contact info |
| 16 | vacancies | number | | Read/Write/Display | Number of openings |
| 17 | jobType | string | ✅ | Read/Write/Filter | FULL_TIME/PART_TIME |
| 18 | gender | string | ✅ | Read/Write/Filter | MALE/FEMALE/ANY |

**Total:** 18 fields

---

### 3. job_applications (12 core + 30 runtime enrichment)

#### Core Fields (Stored in Firestore)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | id | string | ✅ (PK) | Read/Write/Filter | Primary key |
| 2 | jobId | string | ✅ | Read/Write/Filter | Job reference |
| 3 | workerId | string | ✅ | Read/Write/Filter | Worker reference |
| 4 | employerId | string | ✅ | Read/Write/Filter | Employer reference |
| 5 | status | string | ✅ | Read/Write/Filter | Application status |
| 6 | appliedAt | timestamp | ✅ | Read/Filter | Application time |
| 7 | updatedAt | timestamp | | Read/Write | Last update |
| 8 | active | boolean | ✅ | Read/Filter | Soft delete flag |
| 9 | jobTitle | string | | Read/Display | Denormalized |
| 10 | jobLocation | string | | Read/Display | Denormalized |
| 11 | companyName | string | | Read/Display | Denormalized |
| 12 | workerName | string | | Read/Display | Denormalized |

#### Runtime Enrichment Fields (NOT stored, fetched from User profile)

- workerEmail, workerPhone, workerLocation
- workerGender, workerDateOfBirth, workerProfileImageUrl
- workExperience[], skills[], education[]
- certifications[], languages[], availability
- workerAadhaarVerified, workerPhoneVerified
- workerJobsInArea, workerLocalRating, workerTotalReviews
- workerBackgroundCheckPassed, workerIdentityVerified
- resumeUrl, expectedSalary

**Total:** 12 stored + 30 runtime = 42 fields

---

### 4. notifications (8 fields)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | id | string | ✅ (PK) | Read/Write/Filter | Primary key |
| 2 | recipientId | string | ✅ | Read/Write/Filter | User ID |
| 3 | title | string | | Read/Display | Notification title |
| 4 | message | string | | Read/Display | Notification body |
| 5 | type | string | ✅ | Read/Filter | Notification type |
| 6 | data | map | | Read | Deep link data |
| 7 | createdAt | timestamp | ✅ | Read/Filter | Creation time |
| 8 | isRead | boolean | ✅ | Read/Write/Filter | Read status |

**Total:** 8 fields

---

### 5. referral_codes (6 fields)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | code | string | ✅ (PK) | Read/Filter | Document ID |
| 2 | userId | string | | Read | Owner ID |
| 3 | userRole | string | | Read | WORKER/EMPLOYER |
| 4 | userName | string | | Read/Display | Display name |
| 5 | isActive | boolean | | Read/Filter | Code status |
| 6 | createdAt | timestamp | | Read | Creation time |
| 7 | totalUsed | number | | Read/Write | Usage count |

**Total:** 7 fields

---

### 6. referrals (11 fields)

| # | Field | Type | Indexed | Usage | Notes |
|---|-------|------|---------|-------|-------|
| 1 | id | string | ✅ (PK) | Read/Write/Filter | Primary key |
| 2 | referrerUserId | string | ✅ | Read/Write/Filter | Referrer ID |
| 3 | referredUserId | string | ✅ | Read/Write/Filter | Referred user ID |
| 4 | referralCode | string | ✅ | Read/Write/Filter | Code used |
| 5 | status | string | ✅ | Read/Write/Filter | PENDING/COMPLETED |
| 6 | rewardAmount | number | | Read/Display | ₹25 |
| 7 | bonusAmount | number | | Read/Display | Milestone bonus |
| 8 | createdAt | timestamp | ✅ | Read/Filter | Creation time |
| 9 | completedAt | timestamp | | Read/Filter | Completion time |
| 10 | deviceFingerprint | string | | Read/Filter | Fraud detection |
| 11 | referredUserName | string | | Read/Display | Denormalized |
| 12 | referredUserRole | string | | Read/Display | Denormalized |

**Total:** 12 fields

---

## Summary Statistics

| Collection | Fields | Indexed Fields | Denormalized Fields |
|------------|--------|----------------|---------------------|
| users | 23 | 6 | 3 |
| jobs | 18 | 8 | 1 |
| job_applications | 12 (+30 runtime) | 6 | 4 |
| notifications | 8 | 4 | 0 |
| referral_codes | 7 | 1 | 0 |
| referrals | 12 | 5 | 2 |
| **TOTAL** | **80** | **30** | **10** |

**Note:** Runtime enrichment fields (30) are NOT counted in storage totals.

