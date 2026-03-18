# DutyPe Final Clean Firestore Schema (No Legacy)

**Generated:** March 18, 2026  
**Status:** Final target architecture for 5L+ users  
**Rule:** No legacy fields, no duplicate fields, no denormalized arrays in user documents.

---

## 0. Brutal Truth (Current State)

| Area | Current Risk | Impact at Scale |
|---|---|---|
| users | Mixed profile + app state + denormalized arrays | Higher write amplification and larger document reads |
| jobs | Core and details mixed | Job feed reads become expensive |
| applications | Duplicate patterns (job_applications vs applications) | Data divergence and query confusion |
| referrals/analytics | Fraud and analytics fields mixed with product docs | Slow reads and schema drift |
| geo | Inconsistent geoHash/geohash and mixed location formats | Missed matches and unstable search quality |

---

## 1. Final Architecture Principle

Split all data into exactly three concerns:

1. **CORE**: query-critical, small, indexed documents.
2. **DISPLAY**: fetch on detail screen only.
3. **ANALYTICS**: backend pipeline only (Cloud Functions/BigQuery), not client collections.

---

## 2. Final Allowed Collections

Only these collections are allowed in app data path:

1. users
2. worker_profiles
3. employer_profiles
4. jobs
5. job_details
6. applications
7. saved_jobs
8. ratings
9. referrals
10. notifications

Everything else is removed from client schema.

---

## 3. Final Schemas (Strict)

### 3.1 users (CORE)

Document ID: userId

| Field | Type | Required | Notes |
|---|---|---|---|
| phone | string | yes | Primary login phone |
| fullName | string | yes | Single source of truth for name |
| profileImageUrl | string | no | Optional avatar |
| roles | array<string> | yes | Allowed values: WORKER, EMPLOYER |
| activeRole | string | yes | Must exist in roles |
| location.lat | number | yes | Current latitude |
| location.lng | number | yes | Current longitude |
| geohash | string | yes | Lowercase geohash |
| isVerified | boolean | yes | Identity verification flag |
| isActive | boolean | yes | Account active flag |
| fcmToken | string | no | Push token |
| createdAt | timestamp | yes | Creation timestamp |
| lastActiveAt | timestamp | yes | Last active timestamp |

Hard removals from users:
- name, phoneNumber, photoUrl
- role (legacy single-role)
- isProfileComplete, profileCompleted
- blocked_count, points, referralStats, trustTier
- savedJobs array
- email, bio, skills, companyName, workLocations

---

### 3.2 worker_profiles (CORE)

Document ID: userId

| Field | Type | Required | Notes |
|---|---|---|---|
| userId | string | yes | Must equal doc ID |
| jobTypes | array<string> | yes | Example: driver, helper |
| isAvailable | boolean | yes | Availability switch |
| rating | number | yes | Cached aggregate |
| totalRatings | number | yes | Count |
| totalJobs | number | yes | Completed jobs count |
| lastActiveAt | timestamp | yes | Presence signal |

---

### 3.3 employer_profiles (CORE)

Document ID: userId

| Field | Type | Required | Notes |
|---|---|---|---|
| userId | string | yes | Must equal doc ID |
| companyName | string | yes | Employer display name |
| rating | number | yes | Cached aggregate |
| totalRatings | number | yes | Count |
| totalHires | number | yes | Hire count |

---

### 3.4 jobs (CORE)

Document ID: jobId

| Field | Type | Required | Notes |
|---|---|---|---|
| employerId | string | yes | Owner |
| title | string | yes | Feed title |
| jobType | string | yes | Worker matching category |
| salary | number | yes | Numeric only |
| salaryType | string | yes | HOURLY/DAILY/MONTHLY/FIXED |
| location.lat | number | yes | Job lat |
| location.lng | number | yes | Job lng |
| geohash | string | yes | Query key |
| urgency | string | yes | LOW/MEDIUM/HIGH |
| status | string | yes | open/closed/expired |
| createdAt | timestamp | yes | Created time |
| expiresAt | timestamp | yes | Expiry time |

Hard removals from jobs:
- description (moved to job_details)
- contactPhone/contactNumber duplication (kept only in job_details)
- applicationCount
- companyName duplication
- moderation/fraud fields

---

### 3.5 job_details (DISPLAY)

Document ID: jobId

| Field | Type | Required |
|---|---|---|
| description | string | yes |
| contactNumber | string | yes |
| addressText | string | no |

Fetch only when user opens job details.

---

### 3.6 applications (CORE)

Document ID: applicationId (recommended: `{jobId}_{workerId}`)

| Field | Type | Required | Notes |
|---|---|---|---|
| jobId | string | yes | Target job |
| workerId | string | yes | Applicant |
| employerId | string | yes | Job owner |
| status | string | yes | applied/shortlisted/rejected/hired |
| createdAt | timestamp | yes | Apply time |

Hard rule: keep only `applications`, remove `job_applications` entirely.

Worker history: query applications where workerId == currentUserId.

---

### 3.7 saved_jobs (CORE)

Document ID: saveId (recommended: `{userId}_{jobId}`)

| Field | Type | Required |
|---|---|---|
| userId | string | yes |
| jobId | string | yes |
| createdAt | timestamp | yes |

Hard rule: no saved jobs array inside users.

---

### 3.8 ratings (CORE)

Document ID: ratingId

| Field | Type | Required |
|---|---|---|
| jobId | string | yes |
| fromUserId | string | yes |
| toUserId | string | yes |
| rating | number | yes |
| review | string | no |
| createdAt | timestamp | yes |

No multi-type rating payloads.

---

### 3.9 referrals (CORE)

Document ID: referralId

| Field | Type | Required |
|---|---|---|
| referrerId | string | yes |
| referredUserId | string | yes |
| status | string | yes |
| reward | number | yes |
| createdAt | timestamp | yes |

---

### 3.10 notifications (DISPLAY)

Document ID: notificationId

| Field | Type | Required |
|---|---|---|
| recipientId | string | yes |
| title | string | yes |
| message | string | yes |
| type | string | yes |
| data | map | no |
| isRead | boolean | yes |
| createdAt | timestamp | yes |

---

## 4. Geo + Matching Architecture (Final)

1. Query jobs by geohash prefix + status=open (+ optional jobType).
2. Compute exact distance with Haversine on client/server.
3. Keep jobs where distance <= 10 km.
4. If empty, run fallback where distance <= 15 km.
5. Return max 30 jobs.

Job card payload should include only:
- jobId
- title
- salary
- salaryType
- distance
- urgency

---

## 5. Required Indexes

1. jobs: geohash ASC, jobType ASC, status ASC, createdAt DESC
2. jobs: geohash ASC, status ASC, createdAt DESC
3. applications: workerId ASC, createdAt DESC
4. applications: jobId ASC, createdAt DESC
5. saved_jobs: userId ASC, createdAt DESC
6. notifications: recipientId ASC, createdAt DESC

---

## 6. Critical Edge Cases

1. Duplicate apply: enforce deterministic application ID `{jobId}_{workerId}`.
2. No jobs nearby: fallback radius 10 km -> 15 km.
3. Too many jobs: hard cap response to 30.
4. Employer spam: enforce daily job-post cap in backend.
5. Expired jobs: scheduled function marks/cleans expired jobs.

---

## 7. Migration Strategy (No Legacy End-State)

### Phase 1: Prepare New Collections

Create only the final collections listed in section 2.

### Phase 2: Backfill (one-time script)

Map old fields to new fields:
- users.phoneNumber -> users.phone
- users.name -> users.fullName
- users.photoUrl -> users.profileImageUrl
- users.geoHash -> users.geohash
- users.latitude/longitude -> users.location.lat/lng
- jobs.payAmount -> jobs.salary
- jobs.payType -> jobs.salaryType
- jobs.description -> job_details.description
- jobs.contactNumber -> job_details.contactNumber
- job_applications -> applications

### Phase 3: Read Cutover

Release app version that reads only final schema. No reads from old collections/fields.

### Phase 4: Write Cutover

Release app version that writes only final schema. No dual writes after cutover.

### Phase 5: Hard Cleanup

Delete old collections and fields permanently:
- job_applications
- referral_codes
- moderation_queue
- employer_blocked_attempts
- metadata analytics docs in client path
- any legacy fields listed in section 3

Final state is zero legacy.

---

## 8. DOB Validation Policy (18+)

Removed from final users schema to keep the core document ultra-lightweight.
If age compliance is required, store and validate it in backend-only analytics/verification flows,
not in the client-facing core document.

---

## 9. Final Verdict

If schema remains mixed, read costs and latency will continue to rise with growth.  
If this final split is enforced, feed reads stay small, write paths stay simple, and scaling to 5L+ users remains operationally safe.

