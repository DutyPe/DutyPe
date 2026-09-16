# DutyPe Database Optimization Analysis - Part 9: Final Architecture

---

## 7️⃣ Final Recommended Database Architecture

### Production-Grade Schema (Optimized for 1M+ Users)

```
DutyPe Database Architecture
├── Core Collections (8)
│   ├── users (1M docs)
│   │   ├── Dual-role support (roles[], activeRole)
│   │   ├── Denormalized: referralStats, savedJobs[], fcmToken
│   │   └── Indexes: phone, email, roles, activeRole
│   │
│   ├── jobs (500K docs, partitioned by city)
│   │   ├── jobs_aligarh (5K docs)
│   │   ├── jobs_delhi (50K docs)
│   │   ├── jobs_mumbai (100K docs)
│   │   └── Indexes: employerId+postedAt, isActive+postedAt
│   │
│   ├── job_applications (2M docs)
│   │   ├── Runtime enrichment (30+ fields from User profile)
│   │   ├── Denormalized: jobTitle, jobLocation, companyName
│   │   └── Indexes: workerId+appliedAt, employerId+appliedAt
│   │
│   ├── notifications (partitioned by month)
│   │   ├── notifications_2026_03 (100K docs)
│   │   ├── notifications_2026_04 (100K docs)
│   │   ├── TTL: 90 days
│   │   └── Indexes: recipientId+createdAt, recipientId+isRead+createdAt
│   │
│   ├── referral_codes (1M docs)
│   │   ├── O(1) lookup (code as document ID)
│   │   └── No indexes needed
│   │
│   ├── referrals (500K docs)
│   │   ├── Minimal schema (11 fields)
│   │   └── Indexes: referrerUserId+createdAt, status+createdAt
│   │
│   ├── referral_events (1M docs)
│   │   ├── Audit trail
│   │   └── Indexes: userId+timestamp
│   │
│   └── withdrawal_requests (10K docs)
│       ├── Payout tracking
│       └── Indexes: userId+createdAt, status+createdAt
│
├── Supporting Collections (14)
│   ├── phone_roles (1M docs) - Pre-login role check
│   ├── fraud_signals (10K docs) - Fraud detection
│   ├── rate_limits (5K docs) - Rate limiting
│   ├── subscriptions (1K docs) - Premium users
│   ├── metadata (10 docs) - App config
│   ├── announcements (50 docs) - In-app banners
│   ├── conversations (100K docs) - Chat threads
│   ├── messages (1M docs) - Chat messages
│   ├── ratings (500K docs) - User ratings
│   ├── activity_logs (partitioned, TTL: 30 days)
│   ├── moderation_queue (1K docs) - Job moderation
│   ├── suspicious_ips (500 docs) - Fraud IPs
│   ├── achievements (100K docs) - Gamification
│   └── userPreferences (1M docs) - User settings
│
└── Deprecated Collections (3) - TO BE DELETED
    ├── fcm_tokens ❌ (migrated to users.fcmToken)
    ├── saved_jobs ❌ (migrated to users.savedJobs[])
    └── rating_summaries ❌ (migrated to users.ratingSummary)
```

---

### Best Practices Implemented

#### 1. Denormalization (Uber/Airbnb Pattern)

**What We Denormalize:**
- `users.referralStats` - Avoids separate collection lookup
- `users.savedJobs[]` - Avoids separate collection
- `users.fcmToken` - Avoids separate collection
- `jobs.companyName` - Avoids user lookup for every job card
- `applications.jobTitle/jobLocation/companyName` - Avoids job lookup for every application card

**Benefits:**
- 80% fewer reads
- 10x faster list views
- Better user experience

---

#### 2. Runtime Enrichment (LinkedIn Pattern)

**What We Enrich at Runtime:**
- Application worker details (30+ fields from User profile)
- Only fetched when viewing application details
- Not stored in applications collection

**Benefits:**
- Avoids data duplication
- Always shows latest data
- Smaller collection size

---

#### 3. O(1) Lookups (Stripe Pattern)

**What We Optimize:**
- Referral codes (code as document ID)
- User profiles (userId as document ID)
- FCM tokens (userId as document ID)

**Benefits:**
- Constant-time lookups
- No indexes needed
- Scales to billions

---

#### 4. Event Sourcing (Stripe Pattern)

**What We Track:**
- Referral events (audit trail)
- Application status changes (history)
- Fraud signals (investigation)

**Benefits:**
- Complete audit trail
- Replay capability
- Compliance-ready

---

#### 5. Partitioning (Google Pattern)

**What We Partition:**
- Notifications by month
- Jobs by city (future)
- Activity logs by month

**Benefits:**
- Faster queries
- Easy archival
- Better scalability

---

### Performance Metrics (Projected at 1M Users)

| Operation | Current | Optimized | Improvement |
|-----------|---------|-----------|-------------|
| Load job feed | 500ms | 50ms | 10x faster |
| Load applications | 800ms | 80ms | 10x faster |
| Search jobs | 1200ms | 120ms | 10x faster |
| Load user profile | 200ms | 20ms | 10x faster |
| Apply to job | 1000ms | 100ms | 10x faster |

**Cost Savings:**
- Reads: -80% (denormalization + caching)
- Writes: -50% (batch operations)
- Storage: -20% (TTL policies)

**Total Monthly Cost (1M users):**
- Current: ~$500/month
- Optimized: ~$200/month
- Savings: $300/month ($3,600/year)

