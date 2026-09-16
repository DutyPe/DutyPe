# DutyPe Database Optimization Analysis - Part 7: Indexing Strategy

---

## 5️⃣ Indexing Recommendations

### Critical Indexes (Must Have)

#### Users Collection

```javascript
// Single-field indexes
db.collection("users").createIndex({ phone: 1 }, { unique: true });
db.collection("users").createIndex({ email: 1 }, { sparse: true });
db.collection("users").createIndex({ referralCode: 1 }, { unique: true, sparse: true });

// Array-contains indexes
db.collection("users").createIndex({ roles: 1 });

// Composite indexes
db.collection("users").createIndex({ activeRole: 1, createdAt: -1 });
db.collection("users").createIndex({ isActive: 1, createdAt: -1 });
```

**Query Patterns:**
- Find user by phone: `where("phone", "==", phone)`
- Find users by role: `where("roles", "array-contains", "WORKER")`
- List active workers: `where("activeRole", "==", "WORKER").where("isActive", "==", true)`

---

#### Jobs Collection

```javascript
// Single-field indexes
db.collection("jobs").createIndex({ employerId: 1 });
db.collection("jobs").createIndex({ isActive: 1 });
db.collection("jobs").createIndex({ postedAt: -1 });

// Composite indexes (CRITICAL for performance)
db.collection("jobs").createIndex({ employerId: 1, postedAt: -1 });
db.collection("jobs").createIndex({ isActive: 1, postedAt: -1 });
db.collection("jobs").createIndex({ payType: 1, postedAt: -1 });
db.collection("jobs").createIndex({ jobType: 1, postedAt: -1 });
db.collection("jobs").createIndex({ gender: 1, postedAt: -1 });

// Geo indexes (for location-based queries)
db.collection("jobs").createIndex({ latitude: 1, longitude: 1 });
```

**Query Patterns:**
- Get employer's jobs: `where("employerId", "==", id).orderBy("postedAt", "desc")`
- Get active jobs: `where("isActive", "==", true).orderBy("postedAt", "desc")`
- Filter by pay type: `where("payType", "==", "DAILY").orderBy("postedAt", "desc")`
- Geo queries: `where("latitude", ">=", minLat).where("latitude", "<=", maxLat)`

---

#### Job Applications Collection

```javascript
// Single-field indexes
db.collection("job_applications").createIndex({ workerId: 1 });
db.collection("job_applications").createIndex({ employerId: 1 });
db.collection("job_applications").createIndex({ jobId: 1 });
db.collection("job_applications").createIndex({ status: 1 });
db.collection("job_applications").createIndex({ active: 1 });

// Composite indexes (CRITICAL for performance)
db.collection("job_applications").createIndex({ workerId: 1, appliedAt: -1 });
db.collection("job_applications").createIndex({ employerId: 1, appliedAt: -1 });
db.collection("job_applications").createIndex({ jobId: 1, appliedAt: -1 });
db.collection("job_applications").createIndex({ status: 1, appliedAt: -1 });
db.collection("job_applications").createIndex({ active: 1, appliedAt: -1 });

// Multi-field composite (for filtered queries)
db.collection("job_applications").createIndex({ 
  workerId: 1, 
  active: 1, 
  appliedAt: -1 
});
db.collection("job_applications").createIndex({ 
  employerId: 1, 
  status: 1, 
  appliedAt: -1 
});
```

**Query Patterns:**
- Worker's applications: `where("workerId", "==", id).orderBy("appliedAt", "desc")`
- Employer's applications: `where("employerId", "==", id).orderBy("appliedAt", "desc")`
- Applications by status: `where("status", "==", "PENDING").orderBy("appliedAt", "desc")`
- Active applications: `where("active", "==", true).orderBy("appliedAt", "desc")`

---

#### Referrals Collection

```javascript
// Single-field indexes
db.collection("referrals").createIndex({ referrerUserId: 1 });
db.collection("referrals").createIndex({ referredUserId: 1 });
db.collection("referrals").createIndex({ referralCode: 1 });
db.collection("referrals").createIndex({ status: 1 });

// Composite indexes
db.collection("referrals").createIndex({ referrerUserId: 1, createdAt: -1 });
db.collection("referrals").createIndex({ status: 1, createdAt: -1 });
```

**Query Patterns:**
- User's referrals: `where("referrerUserId", "==", id).orderBy("createdAt", "desc")`
- Pending referrals: `where("status", "==", "PENDING").orderBy("createdAt", "desc")`

---

#### Notifications Collection

```javascript
// Single-field indexes
db.collection("notifications").createIndex({ recipientId: 1 });
db.collection("notifications").createIndex({ type: 1 });
db.collection("notifications").createIndex({ createdAt: -1 });

// Composite indexes
db.collection("notifications").createIndex({ recipientId: 1, createdAt: -1 });
db.collection("notifications").createIndex({ recipientId: 1, isRead: 1, createdAt: -1 });
db.collection("notifications").createIndex({ type: 1, createdAt: -1 });
```

**Query Patterns:**
- User's notifications: `where("recipientId", "==", id).orderBy("createdAt", "desc")`
- Unread notifications: `where("recipientId", "==", id).where("isRead", "==", false)`

---

### Index Size Estimation

| Collection | Documents | Indexes | Estimated Size |
|------------|-----------|---------|----------------|
| users | 10,000 | 5 | ~2MB |
| jobs | 5,000 | 8 | ~1.5MB |
| job_applications | 20,000 | 10 | ~4MB |
| referrals | 15,000 | 4 | ~1MB |
| notifications | 50,000 | 4 | ~3MB |

**Total Index Size:** ~11.5MB (negligible for Firestore)

