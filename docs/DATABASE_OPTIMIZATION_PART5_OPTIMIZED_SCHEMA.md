# DutyPe Database Optimization Analysis - Part 5: Optimized Schema

---

## 4️⃣ Optimized Collection Schema

### Core Collections (8) - Production-Ready

#### 1. Users Collection ✅ OPTIMIZED

```typescript
interface User {
  // Core Identity (5 fields)
  id: string;                    // Primary key
  phone: string;                 // Auth identifier (indexed)
  fullName: string;              // Display name
  email?: string;                // Optional contact
  profileImageUrl?: string;      // Avatar
  
  // Dual-Role System (2 fields) - Uber/Airbnb pattern
  roles: string[];               // ["WORKER", "EMPLOYER"]
  activeRole: "WORKER" | "EMPLOYER";
  
  // Location (3 fields)
  latitude: number;              // Current location
  longitude: number;             // Current location
  address: string;               // Formatted address
  
  // Work Locations (1 field) - Uber/Swiggy pattern
  workLocations: WorkLocation[]; // Saved locations
  
  // Profile (3 fields)
  bio?: string;
  skills?: string;               // Comma-separated
  experience?: string;
  
  // Employer Fields (2 fields)
  companyName?: string;
  trustTier: "NEW" | "BRONZE" | "SILVER" | "GOLD";
  
  // System (4 fields)
  fcmToken?: string;             // Push notifications
  createdAt: number;
  isActive: boolean;
  profileCompleted: boolean;
  
  // Denormalized Data (2 fields) - Performance optimization
  referralCode?: string;         // User's referral code
  referralStats?: ReferralStats; // Embedded stats
  savedJobs: string[];           // Saved job IDs
}

interface WorkLocation {
  id: string;
  label: string;                 // "Office", "Home", etc.
  address: string;
  latitude: number;
  longitude: number;
  addedAt: number;
  usageCount: number;
}
```

**Indexes Required:**
- `phone` (unique)
- `email` (sparse)
- `roles` (array-contains)
- `activeRole`
- `referralCode` (unique, sparse)

---

#### 2. Jobs Collection ✅ OPTIMIZED

```typescript
interface Job {
  // Core (5 fields)
  id: string;
  employerId: string;            // Indexed
  title: string;
  companyName: string;           // Denormalized
  description: string;
  
  // Location (3 fields)
  location: string;
  latitude: number;              // Geo queries
  longitude: number;             // Geo queries
  
  // Compensation (2 fields)
  payAmount: string;             // "400", "15000"
  payType: "HOURLY" | "DAILY" | "MONTHLY";
  
  // Details (6 fields)
  shiftTiming: string;
  contactNumber: string;
  vacancies: number;
  jobType: "FULL_TIME" | "PART_TIME" | "CONTRACT";
  gender: "MALE" | "FEMALE" | "ANY";
  
  // Status (3 fields)
  isActive: boolean;
  isFilled: boolean;
  postedAt: number;              // Indexed
}
```

**Indexes Required:**
- `employerId` + `postedAt` (composite)
- `isActive` + `postedAt` (composite)
- `latitude` + `longitude` (geohash for geo queries)
- `payType` + `postedAt` (composite)
- `jobType` + `postedAt` (composite)

---

#### 3. Job Applications Collection ✅ OPTIMIZED

```typescript
interface JobApplication {
  // IDs (4 fields)
  id: string;
  jobId: string;                 // Indexed
  workerId: string;              // Indexed
  employerId: string;            // Indexed
  
  // Status (3 fields)
  status: ApplicationStatus;     // Indexed
  appliedAt: number;             // Indexed
  updatedAt: number;
  
  // Soft Delete (1 field)
  active: boolean;               // Indexed
  
  // Denormalized Display (4 fields)
  jobTitle: string;
  jobLocation: string;
  companyName: string;
  workerName: string;
  
  // Optional
  coverLetter?: string;
  source: "MOBILE_APP" | "WEB_PORTAL" | "REFERRAL";
  
  // Notification Tracking
  lastPendingNotificationSent?: number;
  
  // Runtime Enrichment (NOT stored in Firestore)
  // These 30+ fields are fetched from User profile dynamically
  // workerEmail, workerPhone, workExperience[], skills[], etc.
}
```

**Indexes Required:**
- `workerId` + `appliedAt` (composite)
- `employerId` + `appliedAt` (composite)
- `jobId` + `appliedAt` (composite)
- `status` + `appliedAt` (composite)
- `active` + `appliedAt` (composite)

