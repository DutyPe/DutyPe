# DutyPe Database Optimization Analysis - Part 8: Scalability Improvements

---

## 6️⃣ Scalability Improvements

### Current Scale
- **Users:** ~10,000
- **Jobs:** ~5,000
- **Applications:** ~20,000
- **Notifications:** ~50,000
- **Referrals:** ~15,000

### Target Scale (1M+ Users)
- **Users:** 1,000,000+
- **Jobs:** 500,000+
- **Applications:** 2,000,000+
- **Notifications:** 10,000,000+
- **Referrals:** 500,000+

---

### 1. Data Partitioning Strategy

#### Time-Based Partitioning (Notifications)

**Problem:** Notifications table will grow to 10M+ documents  
**Solution:** Partition by month

```typescript
// Current (single collection)
db.collection("notifications")

// Optimized (partitioned by month)
db.collection("notifications_2026_03")
db.collection("notifications_2026_04")
// ... auto-create new partition each month
```

**Benefits:**
- Faster queries (smaller collection size)
- Easy archival (move old partitions to cold storage)
- Better index performance

**Implementation:**
```typescript
function getNotificationCollection(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return db.collection(`notifications_${year}_${month}`);
}
```

---

#### Geographic Partitioning (Jobs)

**Problem:** Jobs queries will slow down with 500K+ documents  
**Solution:** Partition by region/city

```typescript
// Current (single collection)
db.collection("jobs")

// Optimized (partitioned by city)
db.collection("jobs_aligarh")
db.collection("jobs_delhi")
db.collection("jobs_mumbai")
// ... 100+ cities
```

**Benefits:**
- 10x faster queries (5K docs vs 500K docs)
- Better geo-locality
- Easier to scale horizontally

**Implementation:**
```typescript
function getJobsCollection(city: string) {
  const normalizedCity = city.toLowerCase().replace(/\s+/g, '_');
  return db.collection(`jobs_${normalizedCity}`);
}
```

---

### 2. Caching Strategy

#### Redis Cache Layer

**What to Cache:**
- User profiles (1 hour TTL)
- Job listings (5 minutes TTL)
- Referral stats (10 minutes TTL)
- Category metadata (1 day TTL)

**Implementation:**
```typescript
// Cache user profile
async function getUserProfile(userId: string) {
  // Check cache first
  const cached = await redis.get(`user:${userId}`);
  if (cached) return JSON.parse(cached);
  
  // Fetch from Firestore
  const user = await db.collection("users").doc(userId).get();
  
  // Cache for 1 hour
  await redis.setex(`user:${userId}`, 3600, JSON.stringify(user.data()));
  
  return user.data();
}
```

**Benefits:**
- 90% reduction in Firestore reads
- Sub-10ms response times
- Lower costs

---

### 3. Read Optimization

#### Pagination (Cursor-Based)

**Current:** Offset-based pagination (slow at scale)
```typescript
// BAD: Offset-based (reads all skipped documents)
db.collection("jobs")
  .orderBy("postedAt", "desc")
  .limit(20)
  .offset(100); // Reads 120 documents, returns 20
```

**Optimized:** Cursor-based pagination
```typescript
// GOOD: Cursor-based (reads only needed documents)
db.collection("jobs")
  .orderBy("postedAt", "desc")
  .startAfter(lastDocSnapshot)
  .limit(20); // Reads only 20 documents
```

**Benefits:**
- Constant-time pagination (O(1) vs O(n))
- 80% fewer reads
- Faster response times

---

#### Batch Reads

**Current:** Sequential reads (slow)
```typescript
// BAD: Sequential reads (N network calls)
for (const jobId of jobIds) {
  const job = await db.collection("jobs").doc(jobId).get();
}
```

**Optimized:** Batch reads
```typescript
// GOOD: Batch reads (1 network call)
const jobRefs = jobIds.map(id => db.collection("jobs").doc(id));
const jobs = await db.getAll(...jobRefs);
```

**Benefits:**
- 10x faster (1 network call vs N calls)
- Lower latency
- Better user experience

---

### 4. Write Optimization

#### Batch Writes

**Current:** Sequential writes (slow)
```typescript
// BAD: Sequential writes (N network calls)
for (const notification of notifications) {
  await db.collection("notifications").add(notification);
}
```

**Optimized:** Batch writes
```typescript
// GOOD: Batch writes (1 network call, atomic)
const batch = db.batch();
for (const notification of notifications) {
  const ref = db.collection("notifications").doc();
  batch.set(ref, notification);
}
await batch.commit();
```

**Benefits:**
- 10x faster
- Atomic (all-or-nothing)
- Lower costs

---

### 5. TTL Policies

#### Auto-Delete Old Data

```typescript
// Notifications: Delete after 90 days
db.collection("notifications")
  .where("createdAt", "<", Date.now() - 90 * 24 * 60 * 60 * 1000)
  .get()
  .then(snapshot => {
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    return batch.commit();
  });

// Activity Logs: Delete after 30 days
db.collection("activity_logs")
  .where("timestamp", "<", Date.now() - 30 * 24 * 60 * 60 * 1000)
  .get()
  .then(snapshot => {
    const batch = db.batch();
    snapshot.docs.forEach(doc => batch.delete(doc.ref));
    return batch.commit();
  });
```

**Schedule:** Run daily via Cloud Scheduler

**Benefits:**
- Keeps collections small
- Faster queries
- Lower storage costs

