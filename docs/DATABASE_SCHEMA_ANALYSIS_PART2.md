# 🗄️ DutyPe Database Schema Analysis - Part 2

## 3️⃣ UNNECESSARY FIELDS TO REMOVE

### High Priority Removals (Immediate)

#### users collection
```diff
- workLocations: List<WorkLocation>
  Reason: Used by < 5% of users, adds 200-500 bytes per document
  Impact: Reduces user document size by ~15%
  Alternative: Store in separate work_locations subcollection if needed
  
- savedJobs: List<string>
  Reason: REDUNDANT - already stored in saved_jobs subcollection
  Impact: Reduces user document size by ~10%
  Alternative: Query saved_jobs subcollection
```

#### job_applications collection
```diff
- source: ApplicationSource
  Reason: Not used in any query, filter, or UI display
  Impact: Minimal (enum = 1 byte), but unnecessary
  Alternative: Remove entirely or add to analytics if needed
  
- lastPendingNotificationSent: long?
  Reason: Only used by background worker, not core application data
  Impact: Reduces document size slightly
  Alternative: Store in separate notification_log collection
```

#### jobs collection
```diff
- vacancies: int
  Reason: 90% of jobs use default value 1
  Impact: Minimal (4 bytes), but unnecessary storage
  Alternative: Only store if vacancies > 1, default to 1 in code
```

---

### Medium Priority Removals (Next Sprint)

#### users collection
```diff
- bio: string?
  Reason: < 30% filled, not used in search or filters
  Impact: Can save 100-500 bytes per document
  Alternative: Move to separate user_profiles subcollection
  
- experience: string?
  Reason: < 40% filled, not used in search or filters
  Impact: Can save 50-200 bytes per document
  Alternative: Move to separate user_profiles subcollection
```

#### work_verifications collection
```diff
- verifiedLocation: Map<string, double>?
  Reason: < 20% of verifications use this
  Impact: Saves 50 bytes per document
  Alternative: Only store if location verification is required
```

---

### Low Priority (Consider for Future)

#### Timestamp Standardization
```diff
Current: createdAt, postedAt, appliedAt, generatedAt, etc.
Proposed: Standardize to createdAt + updatedAt for all collections
Reason: Consistency, easier to maintain
Impact: Requires migration script
```

#### Field Naming Consistency
```diff
Current: Mix of snake_case and camelCase
Examples:
  - job_applications (snake_case collection name)
  - jobId (camelCase field name)
  - referral_codes (snake_case collection name)
  - referralCode (camelCase field name)
  
Proposed: Use camelCase for all field names, snake_case for collection names
Reason: Consistency with Kotlin/Android conventions
Impact: Requires migration script
```

---

## 4️⃣ OPTIMIZED COLLECTION SCHEMA

### Recommended Schema (Industry Best Practices)

Based on patterns from:
- **Uber:** Dual-role accounts, location-based queries, fraud detection
- **Airbnb:** Trust tiers, verification badges, ratings
- **Stripe:** Idempotency, atomic transactions, audit trails
- **LinkedIn:** Profile completion, saved items, application tracking

---

### 1. users (OPTIMIZED)

```typescript
{
  // Core Identity (5 fields)
  id: string,                    // Firebase UID
  phone: string,                 // +919876543210 (indexed, unique)
  fullName: string,              // "Vamsi Krishna"
  email: string?,                // Optional (indexed)
  profileImageUrl: string?,      // Firebase Storage URL
  
  // Role Management (2 fields)
  roles: string[],               // ["WORKER", "EMPLOYER"]
  activeRole: "WORKER" | "EMPLOYER",
  
  // Location (4 fields) - ADDED geohash
  latitude: number,              // 28.6139
  longitude: number,             // 77.2090
  address: string,               // "Connaught Place, New Delhi"
  geohash: string,               // "ttcyqh" (for efficient location queries)
  
  // Profile (2 fields) - REMOVED bio, experience
  skills: string?,               // "Plumbing, Electrical"
  companyName: string?,          // For employers
  
  // Trust & Verification (2 fields)
  trustTier: "VERIFIED" | "TRUSTED" | "BUSINESS",
  verificationStatus: {
    phoneVerified: boolean,
    selfieVerified: boolean,
    gstVerified: boolean,
    gstNumber: string?
  },
  
  // System (4 fields)
  fcmToken: string?,
  createdAt: number,             // Unix timestamp
  updatedAt: number,             // Unix timestamp
  profileCompleted: boolean,
  
  // Referral (2 fields)
  referralCode: string?,         // "vamsi9843" (indexed, unique)
  referralStats: {               // Nested object (OPTIMIZED)
    totalReferrals: number,
    successfulReferrals: number,
    totalEarnings: number,
    availableBalance: number,
    canWithdraw: boolean,
    currentTier: "BRONZE" | "SILVER" | "GOLD" | "PLATINUM" | "DIAMOND"
  }
  
  // REMOVED: workLocations, savedJobs, bio, experience
}

// Total: 20 fields (down from 25)
// Size reduction: ~20-30%
```

**Indexes:**
```javascript
// Composite indexes
users.phone (ascending)
users.email (ascending)
users.referralCode (ascending)
users.geohash (ascending) + users.activeRole (ascending)
users.roles (array-contains) + users.profileCompleted (ascending)
```

---

### 2. jobs (OPTIMIZED)

```typescript
{
  // Core (5 fields)
  id: string,
  employerId: string,            // (indexed)
  title: string,                 // "Delivery Boy Needed"
  companyName: string,           // "ABC Logistics"
  description: string,
  
  // Location (4 fields) - ADDED geohash
  location: string,              // "Connaught Place, New Delhi"
  latitude: number,
  longitude: number,
  geohash: string,               // "ttcyqh" (for efficient location queries)
  
  // Pay (2 fields)
  payAmount: string,             // "400" (stored as string for flexibility)
  payType: "HOURLY" | "DAILY" | "MONTHLY",
  
  // Timing (1 field)
  shiftTiming: string,           // "9 AM - 5 PM"
  
  // Status (3 fields)
  isActive: boolean,             // (indexed)
  isFilled: boolean,
  postedAt: number,              // Unix timestamp (indexed)
  
  // Contact (1 field)
  contactNumber: string,         // "+919876543210"
  
  // Optional (2 fields) - REMOVED vacancies (default to 1)
  jobType: "FULL_TIME" | "PART_TIME" | "SHIFT_BASED",
  gender: "ANY" | "MALE" | "FEMALE",
  
  // Metadata (2 fields)
  category: string,              // "Delivery" (auto-detected, indexed)
  expiresAt: number,             // postedAt + 30 days
  
  // REMOVED: vacancies (default to 1 in code)
}

// Total: 20 fields (down from 18, but added geohash + category)
// Size: Similar, but better queryability
```

**Indexes:**
```javascript
// Composite indexes
jobs.employerId (ascending) + jobs.postedAt (descending)
jobs.isActive (ascending) + jobs.category (ascending) + jobs.postedAt (descending)
jobs.geohash (ascending) + jobs.isActive (ascending) + jobs.postedAt (descending)
jobs.isActive (ascending) + jobs.payType (ascending) + jobs.gender (ascending)
```

---

### 3. job_applications (OPTIMIZED)

```typescript
{
  // IDs (4 fields)
  id: string,
  jobId: string,                 // (indexed)
  workerId: string,              // (indexed)
  employerId: string,            // (indexed)
  
  // Status (3 fields)
  status: "PENDING" | "UNDER_REVIEW" | "ACCEPTED" | "REJECTED" | "COMPLETED" | "WITHDRAWN",
  appliedAt: number,             // Unix timestamp (indexed)
  updatedAt: number,             // Unix timestamp
  
  // Active Flag (1 field)
  active: boolean,               // (indexed, for soft delete)
  
  // Denormalized Display (4 fields)
  jobTitle: string,              // "Delivery Boy Needed"
  jobLocation: string,           // "Connaught Place, New Delhi"
  companyName: string,           // "ABC Logistics"
  workerName: string,            // "Vamsi Krishna"
  
  // Optional (1 field) - REMOVED source, lastPendingNotificationSent
  coverLetter: string?,
  
  // REMOVED: source, lastPendingNotificationSent
  // Runtime enrichment: Worker profile fields fetched dynamically from users collection
}

// Total: 13 fields (down from 12 core + 20 enriched)
// Size reduction: ~10%
```

**Indexes:**
```javascript
// Composite indexes
job_applications.workerId (ascending) + job_applications.status (ascending) + job_applications.appliedAt (descending)
job_applications.employerId (ascending) + job_applications.status (ascending) + job_applications.appliedAt (descending)
job_applications.jobId (ascending) + job_applications.status (ascending)
job_applications.active (ascending) + job_applications.status (ascending)
```

---

### 4. notifications (OPTIMIZED)

```typescript
{
  // Core (4 fields)
  id: string,
  recipientId: string,           // (indexed)
  title: string,
  message: string,
  
  // Type (1 field)
  type: "APPLICATION_STATUS" | "NEW_APPLICATION" | "JOB_UPDATE" | "BIRTHDAY" | "REFERRAL_MILESTONE" | ...,
  
  // Data (1 field)
  data: {
    relatedJobId: string?,
    relatedApplicationId: string?,
    actionUrl: string?
  },
  
  // Timestamps (2 fields) - ADDED readAt
  createdAt: number,             // (indexed)
  readAt: number?,               // When notification was read
  
  // Status (1 field)
  isRead: boolean,               // (indexed)
  
  // Priority (1 field) - ADDED
  priority: "LOW" | "NORMAL" | "HIGH" | "URGENT"
}

// Total: 10 fields (up from 8, added readAt + priority)
```

**Indexes:**
```javascript
// Composite indexes
notifications.recipientId (ascending) + notifications.isRead (ascending) + notifications.createdAt (descending)
notifications.recipientId (ascending) + notifications.type (ascending) + notifications.createdAt (descending)
```

---

### 5. referral_codes (NO CHANGES)

```typescript
{
  code: string,                  // Document ID (e.g., "vamsi9843")
  userId: string,                // (indexed)
  userRole: "WORKER" | "EMPLOYER",
  userName: string,
  isActive: boolean,             // (indexed)
  createdAt: number
}

// Total: 6 fields (optimal, no changes needed)
```

---

### 6. referrals (OPTIMIZED)

```typescript
{
  // Core (4 fields)
  id: string,
  referrerUserId: string,        // (indexed)
  referredUserId: string,        // (indexed)
  referralCode: string,
  
  // Status (1 field)
  status: "PENDING" | "COMPLETED" | "EXPIRED" | "CANCELLED" | "REJECTED",
  
  // Rewards (2 fields)
  rewardAmount: number,          // 25.0
  bonusAmount: number,           // Milestone bonus
  
  // Timestamps (2 fields)
  createdAt: number,             // (indexed)
  completedAt: number?,
  
  // Fraud Prevention (1 field)
  deviceFingerprint: string?,
  
  // Denormalized (2 fields)
  referredUserName: string,
  referredUserRole: string,
  
  // Idempotency (1 field) - ADDED
  idempotencyKey: string         // "referrerUserId_referredUserId" (indexed, unique)
}

// Total: 13 fields (up from 10, added idempotencyKey)
```

**Indexes:**
```javascript
// Composite indexes
referrals.referrerUserId (ascending) + referrals.status (ascending) + referrals.createdAt (descending)
referrals.referredUserId (ascending)
referrals.idempotencyKey (ascending) // Unique index
```

---

### 7. withdrawal_requests (OPTIMIZED)

```typescript
{
  // Core (3 fields)
  id: string,
  userId: string,                // (indexed)
  amount: number,
  
  // Status (1 field)
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED",
  
  // Payment (2 fields)
  paymentMethod: "UPI" | "BANK_TRANSFER" | "PAYTM" | "PHONEPE" | "GPAY",
  upiId: string?,
  
  // Timestamps (2 fields)
  createdAt: number,             // (indexed)
  processedAt: number?,
  
  // Transaction (1 field)
  transactionId: string?,
  
  // Rejection (1 field) - ADDED
  rejectionReason: string?       // For failed withdrawals
}

// Total: 10 fields (up from 9, added rejectionReason)
```

---

### 8. work_verifications (OPTIMIZED)

```typescript
{
  // Core (5 fields)
  verificationId: string,
  jobId: string,                 // (indexed)
  applicationId: string,
  workerId: string,              // (indexed)
  employerId: string,            // (indexed)
  
  // Verification (2 fields)
  verificationCode: string,      // (indexed) "DTP-7X9K"
  qrCodeData: string,
  
  // Status (1 field)
  status: "PENDING" | "VERIFIED" | "EXPIRED" | "CANCELLED",
  
  // Timestamps (3 fields)
  createdAt: number,             // Renamed from generatedAt
  verifiedAt: number?,
  expiresAt: number,
  
  // Verification Details (1 field) - REMOVED verifiedLocation
  verifiedByEmployerId: string?,
  
  // Denormalized (3 fields)
  workerName: string,
  jobTitle: string,
  employerName: string
  
  // REMOVED: verifiedLocation (rarely used)
}

// Total: 15 fields (same as before, but renamed generatedAt to createdAt)
```

---

