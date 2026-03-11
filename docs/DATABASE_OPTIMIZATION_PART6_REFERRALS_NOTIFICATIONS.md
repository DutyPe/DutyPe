# DutyPe Database Optimization Analysis - Part 6: Referrals & Notifications

---

#### 4. Referrals Collection ✅ OPTIMIZED

```typescript
interface Referral {
  // Core (4 fields)
  id: string;
  referrerUserId: string;        // Indexed
  referredUserId: string;        // Indexed
  referralCode: string;          // Indexed
  
  // Status (3 fields)
  status: "PENDING" | "COMPLETED" | "EXPIRED" | "CANCELLED";
  createdAt: number;
  completedAt?: number;
  
  // Rewards (2 fields)
  rewardAmount: number;          // ₹25
  bonusAmount: number;           // Milestone bonus
  
  // Fraud Detection (1 field)
  deviceFingerprint?: string;
  
  // Denormalized (2 fields)
  referredUserName: string;
  referredUserRole: string;
}
```

**Indexes Required:**
- `referrerUserId` + `createdAt` (composite)
- `referredUserId` (single)
- `referralCode` (single)
- `status` + `createdAt` (composite)

---

#### 5. Referral Codes Collection ✅ OPTIMIZED

```typescript
interface ReferralCode {
  code: string;                  // Document ID (O(1) lookup)
  userId: string;
  userRole: string;
  userName: string;
  isActive: boolean;
  createdAt: number;
  totalUsed: number;
}
```

**Indexes Required:**
- None (document ID is the code)

**Performance:** O(1) lookup by code

---

#### 6. Notifications Collection ✅ OPTIMIZED

```typescript
interface Notification {
  // Core (5 fields)
  id: string;
  recipientId: string;           // Indexed
  title: string;
  message: string;
  type: NotificationType;        // Indexed
  
  // Data (1 field)
  data: {
    jobId?: string;
    applicationId?: string;
    deepLink?: string;
    [key: string]: any;
  };
  
  // Status (2 fields)
  createdAt: number;             // Indexed
  isRead: boolean;
}

enum NotificationType {
  APPLICATION_STATUS = "APPLICATION_STATUS",
  NEW_APPLICATION = "NEW_APPLICATION",
  JOB_POSTED = "JOB_POSTED",
  NEW_JOB_ALERT = "NEW_JOB_ALERT",
  REFERRAL_MILESTONE = "REFERRAL_MILESTONE",
  BIRTHDAY = "BIRTHDAY",
  // ... 20+ types
}
```

**Indexes Required:**
- `recipientId` + `createdAt` (composite)
- `recipientId` + `isRead` + `createdAt` (composite)
- `type` + `createdAt` (composite)

**TTL Policy:** Auto-delete notifications older than 90 days

---

#### 7. Referral Events Collection ✅ OPTIMIZED

```typescript
interface ReferralEvent {
  id: string;
  eventType: "REWARD_CREDITED" | "SIGNUP_BONUS_CREDITED" | "MILESTONE_REACHED";
  userId: string;                // Indexed
  referralId: string;
  amount: number;
  bonusAmount?: number;
  newTier?: string;
  timestamp: number;             // Indexed
}
```

**Indexes Required:**
- `userId` + `timestamp` (composite)
- `referralId` (single)

**Purpose:** Audit trail for compliance and debugging

---

#### 8. Withdrawal Requests Collection ✅ OPTIMIZED

```typescript
interface WithdrawalRequest {
  id: string;
  userId: string;                // Indexed
  amount: number;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  paymentMethod: "UPI" | "BANK_TRANSFER" | "PAYTM";
  upiId?: string;
  createdAt: number;
  processedAt?: number;
  transactionId?: string;
}
```

**Indexes Required:**
- `userId` + `createdAt` (composite)
- `status` + `createdAt` (composite)

