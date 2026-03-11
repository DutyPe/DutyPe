# DutyPe Database Optimization Analysis - Part 2: Field Usage Analysis

---

## 2️⃣ Field Usage Report

### Users Collection (20 fields)

| Field | Type | Usage | Frequency | Recommendation |
|-------|------|-------|-----------|----------------|
| `id` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Primary key |
| `phone` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Auth identifier |
| `fullName` | string | Read/Write/Display | ⭐⭐⭐⭐⭐ | Keep - Core field |
| `email` | string | Read/Write/Filter | ⭐⭐⭐ | Keep - Optional contact |
| `profileImageUrl` | string | Read/Write/Display | ⭐⭐⭐⭐ | Keep - UI critical |
| `roles` | array | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Dual-role core |
| `activeRole` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Dual-role core |
| `latitude` | number | Read/Write/Filter | ⭐⭐⭐⭐ | Keep - Location-based |
| `longitude` | number | Read/Write/Filter | ⭐⭐⭐⭐ | Keep - Location-based |
| `address` | string | Read/Write/Display | ⭐⭐⭐⭐ | Keep - Display |
| `workLocations` | array | Read/Write | ⭐⭐⭐ | Keep - Uber pattern |
| `bio` | string | Read/Write/Display | ⭐⭐ | Keep - Profile |
| `skills` | string | Read/Write/Display | ⭐⭐⭐ | Keep - Job matching |
| `experience` | string | Read/Write/Display | ⭐⭐ | Keep - Profile |
| `companyName` | string | Read/Write/Display | ⭐⭐⭐ | Keep - Employer |
| `trustTier` | string | Read/Display | ⭐⭐⭐ | Keep - Trust system |
| `fcmToken` | string | Read/Write | ⭐⭐⭐⭐ | Keep - Push notifications |
| `createdAt` | timestamp | Read/Filter | ⭐⭐⭐ | Keep - Analytics |
| `isActive` | boolean | Read/Filter | ⭐⭐⭐⭐ | Keep - Status |
| `profileCompleted` | boolean | Read/Filter | ⭐⭐⭐⭐ | Keep - Onboarding |
| `referralCode` | string | Read/Display | ⭐⭐⭐⭐ | Keep - Referral system |
| `referralStats` | object | Read/Write | ⭐⭐⭐⭐ | Keep - Denormalized |
| `savedJobs` | array | Read/Write | ⭐⭐⭐⭐ | Keep - Denormalized |

**Analysis:** All 20 fields are actively used. No removals recommended.

---

### Jobs Collection (18 fields)

| Field | Type | Usage | Frequency | Recommendation |
|-------|------|-------|-----------|----------------|
| `id` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Primary key |
| `employerId` | string | Read/Write/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Owner |
| `title` | string | Read/Write/Filter/Display | ⭐⭐⭐⭐⭐ | Keep - Core |
| `companyName` | string | Read/Write/Display | ⭐⭐⭐⭐ | Keep - Display |
| `description` | string | Read/Write/Display | ⭐⭐⭐⭐⭐ | Keep - Core |
| `location` | string | Read/Write/Filter/Display | ⭐⭐⭐⭐⭐ | Keep - Core |
| `latitude` | number | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Geo queries |
| `longitude` | number | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Geo queries |
| `payAmount` | string | Read/Write/Filter/Display | ⭐⭐⭐⭐⭐ | Keep - Core |
| `payType` | string | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Filter |
| `shiftTiming` | string | Read/Write/Display | ⭐⭐⭐⭐ | Keep - Display |
| `isActive` | boolean | Read/Write/Filter | ⭐⭐⭐⭐⭐ | Keep - Status |
| `isFilled` | boolean | Read/Write/Filter | ⭐⭐⭐⭐ | Keep - Status |
| `postedAt` | timestamp | Read/Filter/Index | ⭐⭐⭐⭐⭐ | Keep - Sorting |
| `contactNumber` | string | Read/Write/Display | ⭐⭐⭐⭐ | Keep - Contact |
| `vacancies` | number | Read/Write/Display | ⭐⭐⭐ | Keep - Display |
| `jobType` | string | Read/Write/Filter | ⭐⭐⭐⭐ | Keep - Filter |
| `gender` | string | Read/Write/Filter | ⭐⭐⭐ | Keep - Filter |

**Analysis:** All 18 fields are actively used. Well-optimized schema.

