# DutyPe Database Optimization Analysis - Part 3: Applications & Referrals

---

### Job Applications Collection (12 core + 30 enrichment fields)

#### Core Fields (Stored in Firestore)

| Field | Type | Usage | Frequency | Recommendation |
|-------|------|-------|-----------|----------------|
| `id` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Primary key |
| `jobId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Relation |
| `workerId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Relation |
| `employerId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Relation |
| `status` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Core |
| `appliedAt` | timestamp | Read/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Sorting |
| `updatedAt` | timestamp | Read/Write | ⭐⭐⭐⭐ | Keep - Tracking |
| `active` | boolean | Read/Filter | ⭐⭐⭐⭐ | Keep - Soft delete |
| `jobTitle` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Denormalized |
| `jobLocation` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Denormalized |
| `companyName` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Denormalized |
| `workerName` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Denormalized |

#### Runtime Enrichment Fields (NOT stored, fetched from User profile)

These 30+ fields are populated dynamically by ViewModel:
- `workerEmail`, `workerPhone`, `workerLocation`
- `workExperience[]`, `skills[]`, `education[]`
- `workerAadhaarVerified`, `workerPhoneVerified`
- `workerProfileImageUrl`, `resumeUrl`

**Analysis:** Excellent architecture! Runtime enrichment avoids data duplication.

**Recommendation:** ✅ Keep current design. This is industry best practice (LinkedIn, Indeed pattern).

---

### Referrals Collection (11 fields)

| Field | Type | Usage | Frequency | Recommendation |
|-------|------|-------|-----------|----------------|
| `id` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Primary key |
| `referrerUserId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Core |
| `referredUserId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Core |
| `referralCode` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Core |
| `status` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Core |
| `rewardAmount` | number | Read/Display | ⭐⭐⭐⭐ | Keep - Display |
| `bonusAmount` | number | Read/Display | ⭐⭐⭐ | Keep - Milestones |
| `createdAt` | timestamp | Read/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Sorting |
| `completedAt` | timestamp | Read/Filter | ⭐⭐⭐⭐ | Keep - Analytics |
| `deviceFingerprint` | string | Read/Filter | ⭐⭐⭐ | Keep - Fraud detection |
| `referredUserName` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Denormalized |
| `referredUserRole` | string | Read/Display | ⭐⭐⭐ | Keep - Denormalized |

**Analysis:** Minimal, well-designed schema. Follows Dropbox/PayPal patterns.

---

### Notifications Collection (8 fields)

| Field | Type | Usage | Frequency | Recommendation |
|-------|------|-------|-----------|----------------|
| `id` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Primary key |
| `recipientId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Core |
| `title` | string | Read/Display | ⭐⭐⭐⭐⭐ | Keep - Display |
| `message` | string | Read/Display | ⭐⭐⭐⭐⭐ | Keep - Display |
| `type` | string | Read/Filter | ⭐⭐⭐⭐ | Keep - Routing |
| `data` | map | Read | ⭐⭐⭐⭐ | Keep - Deep links |
| `createdAt` | timestamp | Read/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Sorting |
| `isRead` | boolean | Read/Write/Filter | ⭐⭐⭐⭐ | Keep - Status |

**Analysis:** Minimal, efficient schema. Follows Urban Company/TaskRabbit patterns.

**Recommendation:** Add TTL policy to auto-delete notifications older than 90 days.

